package com.xwh.gulimall.auth.feign;


import com.xwh.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("gulimall-third-party")
public interface ThirdPartFeignService {

    @PostMapping("/sms/sendCode")
    R sendCode(@RequestParam("phone") String phone,@RequestParam("code")String code);

}
