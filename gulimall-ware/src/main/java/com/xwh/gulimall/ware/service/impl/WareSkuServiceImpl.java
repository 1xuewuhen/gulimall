package com.xwh.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xwh.common.exception.NoStockException;
import com.xwh.common.to.SkuHasStockVo;
import com.xwh.common.to.mq.StockDetailTo;
import com.xwh.common.to.mq.StockLockedTo;
import com.xwh.common.utils.PageUtils;
import com.xwh.common.utils.Query;
import com.xwh.common.utils.R;
import com.xwh.gulimall.ware.dao.WareSkuDao;
import com.xwh.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.xwh.gulimall.ware.entity.WareOrderTaskEntity;
import com.xwh.gulimall.ware.entity.WareSkuEntity;
import com.xwh.gulimall.ware.feign.OrderFeignService;
import com.xwh.gulimall.ware.feign.ProductFeignService;
import com.xwh.gulimall.ware.service.WareOrderTaskDetailService;
import com.xwh.gulimall.ware.service.WareOrderTaskService;
import com.xwh.gulimall.ware.service.WareSkuService;
import com.xwh.gulimall.ware.vo.OrderItemVo;
import com.xwh.gulimall.ware.vo.OrderVo;
import com.xwh.gulimall.ware.vo.WareSkuLockVo;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Slf4j
@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {

    @Autowired
    private WareSkuDao wareSkuDao;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private WareOrderTaskService wareOrderTaskService;

    @Autowired
    private WareOrderTaskDetailService wareOrderTaskDetailService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OrderFeignService orderFeignService;


    public void unLockStock(Long skuId, Long wareId, Integer num, Long tastDetailId) {
        wareSkuDao.unLockStock(skuId, wareId, num);
    }

    /**
     * skuId 11 wareId 1
     *
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<WareSkuEntity> wrapper = new LambdaQueryWrapper<>();
        String skuId = (String) params.get("skuId");
        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(skuId)) {
            wrapper.eq(WareSkuEntity::getSkuId, skuId);
        }
        if (!StringUtils.isEmpty(wareId)) {
            wrapper.eq(WareSkuEntity::getWareId, wareId);
        }
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public void addStock(Long skuId, Long wareId, Integer skuNum) {
        // 判断如果没有这个库存记录新增
        List<WareSkuEntity> entities = wareSkuDao.selectList(
                new LambdaQueryWrapper<WareSkuEntity>()
                        .eq(WareSkuEntity::getSkuId, skuId)
                        .eq(WareSkuEntity::getWareId, wareId)
        );
        if (null == entities || entities.size() == 0) {
            WareSkuEntity wareSkuEntity = new WareSkuEntity();
            wareSkuEntity.setSkuId(skuId);
            wareSkuEntity.setWareId(wareId);
            wareSkuEntity.setStock(skuNum);
            wareSkuEntity.setStockLocked(0);
            //TODO 远程查询sku名字,如果失败整个事物无需回滚
            // 自己catch异常
            // TODO 还可以用什么方法让异常出现以后不回滚
            try {
                R info = productFeignService.info(skuId);
                Map<String, Object> data = (Map<String, Object>) info.get("skuInfo");
                if (info.getCode() == 0) {
                    wareSkuEntity.setSkuName((String) data.get("skuName"));
                }
            } catch (Exception ignored) {

            }

            wareSkuDao.insert(wareSkuEntity);
        } else {
            wareSkuDao.addStock(skuId, wareId, skuNum);
        }
    }

    @Override
    public List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds) {
        return skuIds.stream().map(skuId -> {
            SkuHasStockVo vo = new SkuHasStockVo();
            // 查询当前sku总库存量
            Long count = baseMapper.getSkuStock(skuId);
            vo.setSkuId(skuId);
            vo.setHasStock(count != null && count > 0);
            return vo;
        }).collect(Collectors.toList());

    }

    /**
     * 为某个订单锁定库存
     * 库存解锁的场景
     * 下单成功，订单过期没有支付被系统自动取消、被用户手动取消。都要解锁库存
     * 下单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚。
     * 之前锁定的库存就要自动解锁
     *
     * @param vo
     * @return
     */
    @Transactional(rollbackFor = {NoStockException.class})
    @Override
    public Boolean orderLockStock(WareSkuLockVo vo) {
        /**
         * 保存库存工作单详情
         * 追溯
         */
        WareOrderTaskEntity taskEntity = new WareOrderTaskEntity();
        taskEntity.setOrderSn(vo.getOrderSn());
        wareOrderTaskService.save(taskEntity);
        List<OrderItemVo> locks = vo.getLocks();
        List<SkuWareHasStock> collect = locks.stream().map(item -> {
            SkuWareHasStock stock = new SkuWareHasStock();
            Long skuId = item.getSkuId();
            stock.setSkuId(skuId);
            List<Long> wareId = wareSkuDao.listWareIdHasSkuStock(skuId);
            stock.setWareId(wareId);
            stock.setNum(item.getCount());
            return stock;
        }).collect(Collectors.toList());
        for (SkuWareHasStock hasStock : collect) {
            boolean skuStocked = false;
            Long skuId = hasStock.getSkuId();
            List<Long> wareIds = hasStock.getWareId();
            if (wareIds == null || wareIds.size() == 0) {
                // 没有任何仓库有这个商品的库存
                throw new NoStockException(skuId);
            }
            // 如果没一个商品都锁定成功，将当前商品锁定了几件的工作单记录发给MQ
            // 锁定失败。前面保存的工作单信息回滚。发送的消息即使要解锁记录，
            // u由于查不到数据，所以就不用处理
            for (Long wareId : wareIds) {
                Long count = wareSkuDao.lockSkuStock(skuId, wareId, hasStock.getNum());
                if (count == 1) {
                    skuStocked = true;
                    // TODO 告诉MQ库存锁定成功
                    WareOrderTaskDetailEntity wareOrderTaskDetailEntity = new WareOrderTaskDetailEntity();
                    wareOrderTaskDetailEntity.setSkuId(skuId).setSkuName("").setTaskId(taskEntity.getId()).setSkuNum(hasStock.getNum()).setWareId(wareId).setLockStatus(1);
                    wareOrderTaskDetailService.save(wareOrderTaskDetailEntity);
                    StockLockedTo stockLockedTo = new StockLockedTo();
                    stockLockedTo.setId(taskEntity.getId());
                    // 只发id不行,防止回滚以后找不到信息
                    StockDetailTo detailTo = new StockDetailTo();
                    BeanUtils.copyProperties(wareOrderTaskDetailEntity, detailTo);
                    stockLockedTo.setDetailTo(detailTo);
                    rabbitTemplate.convertAndSend("stock-event-exchange", "stock.locked", stockLockedTo);
                    break;
                }
            }
            if (!skuStocked) {
                throw new NoStockException(skuId);

            }
        }

        return true;
    }

    @Override
    public void unlockStock(StockLockedTo to) {
        StockDetailTo detail = to.getDetailTo();
        Long detailId = detail.getId();
        // 解锁
        // 查询数据库关于这个订单的库存信息
        // 有 证明库存锁定成功
        //      解锁： 订单情况
        //          1、没有这个订单，必须解锁
        //          2、有这个订单。不是解库存
        //              订单状态： 已取消：解锁库存
        //                       没取消：不能解锁
        // 有 证明库存锁定成功
        // 没有：库存锁定失败了，库存回滚了。这种情况无需解锁。
        WareOrderTaskDetailEntity byId = wareOrderTaskDetailService.getById(detailId);
        if (byId != null) {
            // 解锁
            Long id = to.getId();
            WareOrderTaskEntity taskEntity = wareOrderTaskService.getById(id);
            String orderSn = taskEntity.getOrderSn();
            R r = orderFeignService.getOrderStatus(orderSn);
            if (r.getCode() == 0) {
                OrderVo date = r.getDate(new TypeReference<OrderVo>() {
                });
                if (date == null || date.getStatus() == 4) {
                    // 订单已经被取消了 订单不存在
                    unLockStock(detail.getSkuId(), detail.getWareId(), detail.getSkuNum(), detailId);
                }
            }else {
                throw new RuntimeException("远程服务失败");
            }
        }
    }

    @Data
    static class SkuWareHasStock {
        private Long skuId;
        private Integer num;
        private List<Long> wareId;
    }

}