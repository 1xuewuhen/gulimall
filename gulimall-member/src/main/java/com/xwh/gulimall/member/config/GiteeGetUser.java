package com.xwh.gulimall.member.config;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;


@PropertySource("classpath:giteeGetUser.properties")
//@ConfigurationProperties(prefix = "")
@Data
@Component
public class GiteeGetUser {

    @Value("${host}")
    private String host;
    @Value("${path}")
    private String path;
}
