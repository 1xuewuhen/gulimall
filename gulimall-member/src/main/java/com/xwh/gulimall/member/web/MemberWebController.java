package com.xwh.gulimall.member.web;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MemberWebController {

    @GetMapping("/memberOrder.html")
    public String memberOrder(){

        return "orderList";
    }
}
