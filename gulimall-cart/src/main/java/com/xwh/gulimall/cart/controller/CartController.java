package com.xwh.gulimall.cart.controller;


import com.xwh.gulimall.cart.interceptor.CartInterceptor;
import com.xwh.gulimall.cart.service.CartService;
import com.xwh.gulimall.cart.vo.Cart;
import com.xwh.gulimall.cart.vo.UserInfoTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.concurrent.ExecutionException;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping("/cart.html")
    public String cartListPage() {
        // 快速得到用户信息,userId，userKey
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();

        return "cartList";
    }

    /**
     * 添加商品
     *
     * @return
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam(value = "skuId") Long skuId,
                            @RequestParam(value = "num") Integer num,
                            Model model) {
        Cart.CartItem cartItem = null;
        try {
            cartItem = cartService.addToCart(skuId,num);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        model.addAttribute("item",cartItem);
        return "success";
    }


}