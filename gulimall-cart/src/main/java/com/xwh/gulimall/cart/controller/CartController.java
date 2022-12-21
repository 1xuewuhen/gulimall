package com.xwh.gulimall.cart.controller;


import com.xwh.common.constant.AuthServerConstant;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpSession;

@Controller
public class CartController {

    @GetMapping("/cartList")
    public String cartListPage(HttpSession session){
        Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
        if (attribute == null){

        }else {

        }
        return "cartList";
    }
}