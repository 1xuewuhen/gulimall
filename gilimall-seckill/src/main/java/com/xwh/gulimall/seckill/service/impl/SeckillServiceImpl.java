package com.xwh.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.xwh.common.utils.R;
import com.xwh.gulimall.seckill.feign.CouponFeignService;
import com.xwh.gulimall.seckill.feign.ProductFeignService;
import com.xwh.gulimall.seckill.service.SeckillService;
import com.xwh.gulimall.seckill.to.SecKillSkuRedisTo;
import com.xwh.gulimall.seckill.vo.SeckillSessionsWithSkus;
import com.xwh.gulimall.seckill.vo.SkuInfoVo;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
public class SeckillServiceImpl implements SeckillService {

    @Autowired
    private CouponFeignService couponFeignService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private RedissonClient redissonClient;

    private final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";

    private final String SKUKILL_CACHE_PREFIX = "seckill:skus";

    private final String SKU_STOCK_SEMEAPHORE = "seckill:stock:";

    @Override
    public void uploadSeckillSkuLatest3Days() {
        R r = couponFeignService.getLate3DaySession();
        if (r.getCode() == 0) {
            // 上架
            // 缓存到redis
            List<SeckillSessionsWithSkus> sessionData = r.getDate(new TypeReference<List<SeckillSessionsWithSkus>>() {
            });
            // 1、缓存活动信息
            saveSessionInfos(sessionData);
            // 2、缓存活动的商品信息
            saveSessionSkuInfos(sessionData);
        }
    }

    @Override
    public List<SecKillSkuRedisTo> getCurrentSeckillSkus() {
        long time = new Date().getTime();
        Set<String> keys = stringRedisTemplate.keys(SESSIONS_CACHE_PREFIX + "*");
        assert keys != null;
        for (String key : keys) {
            String replace = key.replace(SESSIONS_CACHE_PREFIX, "");
            String[] s = replace.split("_");
            long start = Long.parseLong(s[0]);
            long end = Long.parseLong(s[1]);
            if (time >= start && time <= end) {
                List<String> range = stringRedisTemplate.opsForList().range(key, -100, 100);
                BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
                List<String> list = hashOps.multiGet(range);
                if (list != null) {
                    return list.stream().map(item -> JSON.parseObject(item.toString(), SecKillSkuRedisTo.class)).collect(Collectors.toList());
                }
                break;
            }
        }
        return null;
    }

    @Override
    public SecKillSkuRedisTo getSkuSeckillInfo(Long skuId) {
        BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);
        Set<String> keys = hashOps.keys();
        if (keys != null && keys.size() > 0) {
            String regx = "\\d_" + skuId;
            for (String key : keys) {
                if (Pattern.matches(regx, key)) {
                    String s = hashOps.get(key);
                    SecKillSkuRedisTo redisTo = JSON.parseObject(s, SecKillSkuRedisTo.class);
                    Long startTime = redisTo.getStartTime();
                    long current = new Date().getTime();
                    Long endTime = redisTo.getEndTime();
                    if (current >= startTime && current <= endTime) {

                    }else {
                        redisTo.setRandomCode(null);
                    }
                    return redisTo;
                }

            }
        }
        return null;
    }

    private void saveSessionInfos(List<SeckillSessionsWithSkus> sessions) {
        sessions.forEach(session -> {
            long startTime = session.getStartTime().getTime();
            long endTime = session.getEndTime().getTime();
            String key = SESSIONS_CACHE_PREFIX + startTime + "_" + endTime;
            Boolean aBoolean = stringRedisTemplate.hasKey(key);
            if (Boolean.FALSE.equals(aBoolean)) {
                List<String> collect = session.getRelationSkus().stream().map(item -> item.getPromotionSessionId() + "_" + item.getSkuId().toString()).collect(Collectors.toList());
                if (key.length() > 0 && collect.size() > 0) {
                    stringRedisTemplate.opsForList().leftPushAll(key, collect);
                }
            }

        });
    }

    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessions) {
        sessions.forEach(session -> {
            // 准备hash操作
            BoundHashOperations<String, Object, Object> ops = stringRedisTemplate.boundHashOps(SKUKILL_CACHE_PREFIX);

            session.getRelationSkus().forEach(seckillSkuVo -> {
                // 4、随机码
                String token = UUID.randomUUID().toString().replace("-", "");
                if (Boolean.FALSE.equals(ops.hasKey(seckillSkuVo.getPromotionSessionId().toString() + "_" + seckillSkuVo.getSkuId().toString()))) {
                    // 缓存的数据
                    SecKillSkuRedisTo redisTo = new SecKillSkuRedisTo();
                    // 1、sku基本信息
                    R r = productFeignService.getSkuInfo(seckillSkuVo.getSkuId());
                    if (r.getCode() == 0) {
                        SkuInfoVo skuInfo = r.getDate("skuInfo", new TypeReference<SkuInfoVo>() {
                        });
                        redisTo.setSkuInfo(skuInfo);
                    }
                    // 2、sku秒杀信息
                    BeanUtils.copyProperties(seckillSkuVo, redisTo);
                    // 3、设置当前商品的时间信息
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());

                    redisTo.setRandomCode(token);

                    String jsonString = JSON.toJSONString(redisTo);
                    ops.put(seckillSkuVo.getPromotionSessionId().toString() + "_" + seckillSkuVo.getSkuId().toString(), jsonString);
                    // 如果当前这个场次的商品的库存信息上架就不需要上架了
                    // 5、使用库存作为分布式的信号量  限流
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMEAPHORE + token);
                    // 商品可以秒杀的数量作为信号量
                    semaphore.trySetPermits(seckillSkuVo.getSeckillCount().intValue());
                }
            });
        });
    }
}
