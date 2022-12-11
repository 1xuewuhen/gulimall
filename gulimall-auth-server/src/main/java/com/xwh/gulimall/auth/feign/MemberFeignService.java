package com.xwh.gulimall.auth.feign;

import com.xwh.common.utils.R;
import com.xwh.gulimall.auth.vo.UserLoginVo;
import com.xwh.gulimall.auth.vo.UserRegisVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-member")
public interface MemberFeignService {

    @PostMapping("/member/member/regist")
    R regist(@RequestBody UserRegisVo vo);

    @PostMapping("/member/member/login")
    R login(@RequestBody UserLoginVo vo);
}
