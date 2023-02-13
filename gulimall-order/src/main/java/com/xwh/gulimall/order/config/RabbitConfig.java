package com.xwh.gulimall.order.config;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;


@Configuration
public class RabbitConfig {

    @Autowired
    RabbitTemplate rabbitTemplate;


    /**
     * 定制RabbitTemplate
     * 只要消息抵达服务器broker就ack=true
     * 对象创建完成以后，执行这个方法
     *
     * 1、服务收到消息就回调
     *      1、spring.rabbitma.publisher-configms=true
     *      2、设置确认毁掉ConfirmCallback
     * 2、消息正确抵达队列进行回调
     *      1、spring.rabbitmq.publish-returns=true
     *         spring.rabbitmq.template.mandatory=true
     *      2、设置确认回调ReturnCallback
     * 3、消费端确认（保证每个消息被正确消费。此时才可以broker删除这个消息）
     *      1、默认是自动删除的。只要消息收到，客户端会自动确认，服务端就会移除这个消息
     */
    @PostConstruct
    public void init() {
        /**
         * correlationData 当前消息的唯一标识
         * b 消息是否成功收到
         * s 失败的原因
         * (correlationData, b, s) -> {
         *             System.out.printf("confirm...correlationData[%s]==>b[%s]==>s[%s]",correlationData,b,s);
         *         }
         */
        rabbitTemplate.setConfirmCallback((correlationData, b, s) -> System.out.printf("confirm...correlationData[%s]==>b[%s]==>s[%s]\n", correlationData, b, s));
        /*rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            *//**
             * 只要消息没有投递给制定的队列，就触发这个失败的回调
             * @param message 投递失败消息的消息详细信息
             * @param i 回复的状态码
             * @param s 回复的文本内容
             * @param s1 当时这个消息发给哪个交换机
             * @param s2 当时这个消息用哪个路由键
             *//*
            @Override
            public void returnedMessage(Message message, int i, String s, String s1, String s2) {
                System.out.printf("Fail Message[%s]==>i[%d]==>s[%s]==>s1[%s]==>s2[%s]\n", message, i, s, s1, s2);
            }
        });*/

        rabbitTemplate.setReturnsCallback(returnedMessage -> System.out.println("returnedMessage["+returnedMessage+"]"));
    }
}
