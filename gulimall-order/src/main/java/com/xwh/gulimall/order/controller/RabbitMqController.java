package com.xwh.gulimall.order.controller;


import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class RabbitMqController {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/sendMessage/{num}")
    public String sendMessage(@PathVariable("num") Integer num){
        for (int i = 0; i < num; i++) {
            Map<String,Object> map = new HashMap<>();
            map.put("name","张三");
            map.put("age",26);
            rabbitTemplate.convertAndSend("hello-java-exchange","hello-1java",map,new CorrelationData(UUID.randomUUID().toString()));
//            log.info("消息发送完成{}",map);
        }
        return "ok";
    }
}
