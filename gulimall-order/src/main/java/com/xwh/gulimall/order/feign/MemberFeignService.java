package com.xwh.gulimall.order.feign;


import com.xwh.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("gulimall-member")
public interface MemberFeignService {

    @GetMapping("/member/memberreceiveaddress/{memberId}/address")
    R getAddress(@PathVariable("memberId") Long memberId);
}
