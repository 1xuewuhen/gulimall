package com.xwh.gulimall.auth.controller;


import com.alibaba.fastjson.TypeReference;
import com.xwh.common.constant.AuthServerConstant;
import com.xwh.common.utils.R;
import com.xwh.common.vo.MemberRespVo;
import com.xwh.gulimall.auth.feign.MemberFeignService;
import com.xwh.gulimall.auth.vo.UserLoginVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
public class LoginController {

    @Autowired
    private MemberFeignService memberFeignService;

    @PostMapping("/login")
    public String login(UserLoginVo vo, RedirectAttributes redirectAttributes, HttpSession session){
        R r = memberFeignService.login(vo);
        if (r.getCode() == 0){
            session.setAttribute(AuthServerConstant.LOGIN_USER,r.getDate(new TypeReference<MemberRespVo>(){}));
            return "redirect:http://gulimall.com";
        }else {
            Map<String,String> map = new HashMap<>();
            map.put("msg",r.getDate("msg",new TypeReference<String>(){}));
            redirectAttributes.addFlashAttribute("errors",map);
            return "redirect:http://auth.gulimall.com/login.html";
        }
    }

    @GetMapping("/login.html")
    public String loginPage(HttpSession session){
        Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
        if (attribute == null) {
            return "login";
        }else {
            return "redirect:http://gulimall.com";
        }
    }
}
