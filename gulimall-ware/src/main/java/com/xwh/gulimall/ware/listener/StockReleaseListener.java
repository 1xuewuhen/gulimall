package com.xwh.gulimall.ware.listener;


import com.alibaba.fastjson.TypeReference;
import com.rabbitmq.client.Channel;
import com.xwh.common.to.mq.OrderTo;
import com.xwh.common.to.mq.StockDetailTo;
import com.xwh.common.to.mq.StockLockedTo;
import com.xwh.common.utils.R;
import com.xwh.gulimall.ware.entity.WareOrderTaskDetailEntity;
import com.xwh.gulimall.ware.entity.WareOrderTaskEntity;
import com.xwh.gulimall.ware.service.WareSkuService;
import com.xwh.gulimall.ware.vo.OrderVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@RabbitListener(queues = {"stock.release.stock.queue"})
@Component
public class StockReleaseListener {

    @Autowired
    private WareSkuService wareSkuService;

    /**
     * 库存自动解锁
     * 下单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚。之前锁定的库存就要自动解锁
     * 订单失败
     * 锁库存失败
     * 只要解锁库存的消息失败。一定要告诉服务解锁失败
     *
     * @param to
     * @param message
     * @param channel
     */
    @RabbitHandler
    public void handleStockLockedRelease(StockLockedTo to, Message message, Channel channel) throws IOException {
        log.info("收到解锁库存的消息");
        try {
            wareSkuService.unLockStock(to);
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }

    }

    @RabbitHandler
    public void handleOrderCloseRelease(OrderTo to, Message message, Channel channel) throws IOException {
        log.info("订单关闭解锁库存的消息");
        try {
            wareSkuService.unLockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }

}
