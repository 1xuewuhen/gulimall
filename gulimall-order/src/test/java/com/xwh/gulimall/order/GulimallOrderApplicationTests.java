package com.xwh.gulimall.order;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@SpringBootTest
class GulimallOrderApplicationTests {

    @Autowired
    private AmqpAdmin amqpAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void sendMessage(){
        Map<String,Object> map = new HashMap<>();
        map.put("name","张三");
        map.put("age",26);
        String s = "Hello World!!!";
        rabbitTemplate.convertAndSend("hello-java-exchange","hello-java",map);
        log.info("消息发送完成{}",map);
    }

    @Test
    public void createExchange() {
        Exchange directExchange = new DirectExchange("hello-java-exchange", true, false);
        amqpAdmin.declareExchange(directExchange);
        log.info("Exchange[{}]创建成功", "hello-java-exchange");
    }

    @Test
    public void createQueue() {
        Queue queue = new Queue("hello-java-queue", true, false, false);
        amqpAdmin.declareQueue(queue);
        log.info("Queue[{}]创建成功", "hello-java-queue");
    }

    @Test
    public void createBinding() {
        Binding binding = new Binding("hello-java-queue", Binding.DestinationType.QUEUE,
                "hello-java-exchange", "hello-java", null);
        amqpAdmin.declareBinding(binding);
        log.info("Binding[{}]创建成功", "hello-java-queue");
    }

    /**
     * 定制RabbitTemplate
     * 只要消息抵达服务器broker就ack=true
     * 对象创建完成以后，执行这个方法
     */
    @Test
    public void initRabbitTemplate() {
        /**
         * correlationData 当前消息的唯一标识
         * b 消息是否成功收到
         * s 失败的原因
         * (correlationData, b, s) -> {
         *             System.out.printf("confirm...correlationData[%s]==>b[%s]==>s[%s]",correlationData,b,s);
         *         }
         */
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            @Override
            public void confirm(CorrelationData correlationData, boolean b, String s) {
                System.out.printf("confirm...correlationData[%s]==>b[%s]==>s[%s]",correlationData,b,s);
            }
        });


        Map<String,Object> map = new HashMap<>();
        map.put("name","张三");
        map.put("age",26);
        String s = "Hello World!!!";
        rabbitTemplate.convertAndSend("hello-java-exchange","hello-java",map);

    }

}
