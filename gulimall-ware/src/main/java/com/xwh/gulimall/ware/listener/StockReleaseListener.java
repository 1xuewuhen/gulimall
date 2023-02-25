package com.xwh.gulimall.ware.listener;


import com.rabbitmq.client.Channel;
import com.xwh.common.to.mq.OrderTo;
import com.xwh.common.to.mq.StockLockedTo;
import com.xwh.gulimall.ware.service.WareSkuService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@RabbitListener(queues = {"stock.release.stock.queue"})
@Service
@Slf4j
public class StockReleaseListener {
    @Autowired
    private WareSkuService wareSkuService;

    /**
     * 库存自动解锁
     * 下单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚。之前锁定的库存就要自动解锁
     * 订单失败
     * 锁库存失败
     *
     * @param to
     * @param message
     * @param channel
     */

    @RabbitHandler
    public void handleStockLockedRelease(StockLockedTo to, Message message, Channel channel) throws IOException {
        log.info("收到解锁库存的消息");
        try {
            // 当前消息是否是的二次及以后派发过来了
//            Integer receivedDelay = message.getMessageProperties().getReceivedDelay();
            wareSkuService.unlockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }

    @RabbitHandler
    public void handleOrderClose(OrderTo to, Message message, Channel channel) throws IOException {
        log.info("订单关闭，准备解锁库存...");
        try {
            wareSkuService.unlockStock(to);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(), true);
        }
    }
}
