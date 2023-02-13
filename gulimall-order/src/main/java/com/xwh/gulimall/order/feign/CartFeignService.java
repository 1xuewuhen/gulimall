package com.xwh.gulimall.order.feign;

import com.xwh.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("gulimall-cart")
public interface CartFeignService {

    @GetMapping("/currentUserCartItem")
    R currentUserCartItem();
}
