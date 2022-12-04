package com.xwh.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xwh.gulimall.product.service.CategoryBrandRelationService;
import com.xwh.gulimall.product.vo.Catelog2Vo;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xwh.common.utils.PageUtils;
import com.xwh.common.utils.Query;

import com.xwh.gulimall.product.dao.CategoryDao;
import com.xwh.gulimall.product.entity.CategoryEntity;
import com.xwh.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redisson;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        List<CategoryEntity> entities = baseMapper.selectList(null);
        return entities.stream()
                .filter(categoryEntity -> categoryEntity.getParentCid() == 0)
                .peek(categoryEntity -> categoryEntity.setChildren(getChildren(categoryEntity, entities)))
                .sorted(Comparator.comparingInt(menu -> (menu.getSort() == null ? 0 : menu.getSort())))
                .collect(Collectors.toList());
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 1、检查当前删除的菜单，是否被别的地方应用
        //逻辑删除
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        findCatelogParent(catelogId, paths);
        Collections.reverse(paths);
        return paths.toArray(new Long[paths.size()]);
    }

    @Caching(evict = {
            @CacheEvict(value = {"category"}, key = "'level1Categorys'"),
            @CacheEvict(value = {"category"}, key = "'getCatalogJson'")
    })
    @Transactional
    @Override
    public void updateCascode(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    @Cacheable(
            value = {"category"},
            key = "'level1Categorys'",
            sync = true
    )
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        return baseMapper.selectList(new LambdaQueryWrapper<CategoryEntity>().eq(CategoryEntity::getParentCid, 0));
    }


    @Cacheable(value = "category", key = "#root.methodName")
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        //查出所有分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);
        return level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //每一个以及分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
            // 封装上面的结果
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    // 找当前二级分类的三级分类封装成vo
                    List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());
                    if (level3Catelog != null) {
                        List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map(l3 -> {
                            // 封装制定格式
                            return new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(collect);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
    }

    //TODO 在虚拟机中有可能会有堆外溢出异常，在ubuntu系统中没有这个异常
    //  低版本的redis依赖有问题，目前使用的redis依赖没有出现异常
    //  升级后的lettuce没有问题可以直接使用
//    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson2() {

        /**
         * 加入空结果缓存，解决缓存穿透问题
         * 设置过期时间（加随机时间），解决缓存雪崩问题
         * 加锁，解决缓存击穿问题
         */

        // 加入缓存逻辑
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if (StringUtils.isEmpty(catalogJson)) {
            //查询数据库
            System.out.println("redis查数据库");
            Map<String, List<Catelog2Vo>> catalogJsonFromDb = getCatalogJsonFromDbWithRedissionisLock();
            return catalogJsonFromDb;
        }
        Map<String, List<Catelog2Vo>> stringListMap = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
        });
        System.out.println("redis缓存");
        return stringListMap;
    }


    //    @Override  从数据库中查询的数据 本地锁
    private Map<String, List<Catelog2Vo>> getCatalogJsonFromDb() {

        //TODO 本地锁：synchronized,JUC(Lock),在分布式的情况下，想要锁住所有，必须使用分布式锁

        synchronized (this) {
            return getDataFromDb();
        }

    }

    //    @Override  从数据库中查询的数据 分布式锁

    /**
     * 缓存里面的数据如何和数据库保持一致
     * 缓存数据的一致性
     * 双写模式
     * 失效模式
     *
     * @return
     */
    private Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedissionisLock() {
        //TODO 分布式锁Redission
        //  锁的名字。锁的粒度，越细越快
        //  锁的粒度：具体缓存的某个数据，
        RLock lock = redisson.getLock("catalogJson-lock");
        lock.lock();
        Map<String, List<Catelog2Vo>> dataFromDb = null;
        try {
            System.out.println("分布式锁成功");
            dataFromDb = getDataFromDb();
        } finally {
            lock.unlock();
        }
        return dataFromDb;

    }


    //    @Override  从数据库中查询的数据 redis分布式锁
    private Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedisisLock() {

        //TODO 分布式锁 去redis占坑
        String token = UUID.randomUUID().toString();
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", token, 30, TimeUnit.SECONDS);
        Map<String, List<Catelog2Vo>> dataFromDb = null;
        if (lock) {
            try {
                // 加锁成功 执行业务
//            redisTemplate.expire("lock",30,TimeUnit.SECONDS);
                System.out.println("分布式锁成功");
                dataFromDb = getDataFromDb();
            } finally {
                //            redisTemplate.delete("lock");
                // 必须进行原子操作， LUA脚本解锁
            /*String lockValue = redisTemplate.opsForValue().get("lock");
            if (token.equals(lockValue)){
                redisTemplate.delete("lock");
            }*/
                String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
                redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Collections.singletonList("lock"), token);
            }

            return dataFromDb;
        } else {
            System.out.println("等待重试");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return getCatalogJsonFromDbWithRedisisLock();
        }
    }

    private Map<String, List<Catelog2Vo>> getDataFromDb() {
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if (!StringUtils.isEmpty(catalogJson)) {
            System.out.println("二次redis数据查询");
            Map<String, List<Catelog2Vo>> stringListMap = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
            return stringListMap;
        }

        /**
         * 将数据库多次查询变为一次
         */
        System.out.println("一次数据库查询");
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        //查出所有分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

        Map<String, List<Catelog2Vo>> parent_id = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {


            //每一个以及分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
            // 封装上面的结果
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map(l2 -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    // 找当前二级分类的三级分类封装成vo
                    List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());
                    if (level3Catelog != null) {
                        List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map(l3 -> {
                            // 封装制定格式

                            return new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(collect);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        redisTemplate.opsForValue().set("catalogJson", JSON.toJSONString(parent_id), (int) (Math.random() * 10 + 1), TimeUnit.MINUTES);
        return parent_id;
    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parent_cid) {
//        return baseMapper.selectList(new LambdaQueryWrapper<CategoryEntity>().eq(CategoryEntity::getParentCid, v.getCatId()));
        return selectList.stream().filter(item -> Objects.equals(item.getParentCid(), parent_cid)).collect(Collectors.toList());
    }

    private void findCatelogParent(Long catelogId, List<Long> paths) {
        paths.add(catelogId);
        CategoryEntity categoryEntity = this.baseMapper.selectById(catelogId);
        if (categoryEntity.getParentCid() != 0) {
            findCatelogParent(categoryEntity.getParentCid(), paths);
        }
    }


    private List<CategoryEntity> getChildren(CategoryEntity root, List<CategoryEntity> all) {
        return all.stream()
                .filter(category -> Objects.equals(category.getParentCid(), root.getCatId()))
                .peek(category -> category.setChildren(getChildren(category, all)))
                .sorted(Comparator.comparingInt(menu -> (menu.getSort() == null ? 0 : menu.getSort())))
                .collect(Collectors.toList());
    }

}