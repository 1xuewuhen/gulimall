package com.xwh.gulimall.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xwh.common.to.mq.SeckillOrderTo;
import com.xwh.common.utils.PageUtils;
import com.xwh.gulimall.order.entity.OrderEntity;
import com.xwh.gulimall.order.vo.*;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * 订单
 *
 * @author xueWuHen
 * @email xueWuHen@gmail.com
 * @date 2022-10-05 14:01:29
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);

    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    SubmitOrderResponseVo submitOrder(OrderSubmitVo vo);

    OrderEntity getOrderByOrderSn(String orderSn);

    void closeOrder(OrderEntity entity);

    PayVo getOrderPay(String orderSn);

    PageUtils queryPageWithItem(Map<String, Object> params);

    String handleAliPayed(PayAsyncVo vo);

    void createSeckillOrder(SeckillOrderTo seckillOrder);
}

