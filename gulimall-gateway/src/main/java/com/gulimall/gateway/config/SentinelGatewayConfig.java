package com.gulimall.gateway.config;


import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.fastjson.JSON;
import com.xwh.common.exception.BizCodeEnum;
import com.xwh.common.utils.R;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Configuration
public class SentinelGatewayConfig {

    public SentinelGatewayConfig(){
        GatewayCallbackManager.setBlockHandler((serverWebExchange, throwable) -> {
            R r = R.error(BizCodeEnum.TO_MANY_REQUEST.getCode(), BizCodeEnum.TO_MANY_REQUEST.getMessage());
            String errorJSON = JSON.toJSONString(r);
            return ServerResponse.ok().body(Mono.just(errorJSON), String.class);
        });
    }
}
