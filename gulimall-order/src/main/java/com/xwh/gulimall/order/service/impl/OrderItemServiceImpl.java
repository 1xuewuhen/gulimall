package com.xwh.gulimall.order.service.impl;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xwh.common.utils.PageUtils;
import com.xwh.common.utils.Query;

import com.xwh.gulimall.order.dao.OrderItemDao;
import com.xwh.gulimall.order.entity.OrderItemEntity;
import com.xwh.gulimall.order.service.OrderItemService;

//@RabbitListener(queues = {"hello-java-queue"})
@Service("orderItemService")
public class OrderItemServiceImpl extends ServiceImpl<OrderItemDao, OrderItemEntity> implements OrderItemService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderItemEntity> page = this.page(
                new Query<OrderItemEntity>().getPage(params),
                new QueryWrapper<OrderItemEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * {(Body:'{"name":"张三","age":26}' MessageProperties [headers={__ContentTypeId__=java.lang.Object, __KeyTypeId__=java.lang.Object, __TypeId__=java.util.HashMap}, contentType=application/json, contentEncoding=UTF-8, contentLength=0, receivedDeliveryMode=PERSISTENT, priority=0, redelivered=false, receivedExchange=hello-java-exchange, receivedRoutingKey=hello-java, deliveryTag=1, consumerTag=amq.ctag-mPBDthW2QfXk6w_yQGW4dw, consumerQueue=hello-java-queue])}
     * ===>类型:{class org.springframework.amqp.core.Message}
     *
     * @param message
     */
//    @RabbitHandler
    public void recieveMessage(Message message,
                               Map<String, Object> map,
                               Channel channel) {
        byte[] body = message.getBody();
        MessageProperties messageProperties = message.getMessageProperties();
        System.out.printf("接受到消息....内容:{%s}\n", map);
    }

}