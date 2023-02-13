package com.xwh.gulimall.order.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "gulimall.thread")
@Component
@Data
public class ThreadPoolConfigProperties {
    private Integer core;
    private Integer maxSize;
    private Integer KeepAliveTime;
}
