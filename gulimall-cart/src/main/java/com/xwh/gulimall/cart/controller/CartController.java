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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.concurrent.ExecutionException;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping("/cart.html")
    public String cartListPage(Model model) {
        // 快速得到用户信息,userId，userKey
        Cart cart = null;
        try {
            cart = cartService.getCart();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        model.addAttribute("cart", cart);
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
                            RedirectAttributes redirectAttributes) {
        try {
            cartService.addToCart(skuId, num);
            // 模拟session的
            // redirectAttributes.addFlashAttribute("skuId", skuId);d
            // 拼接地址
            redirectAttributes.addAttribute("skuId", skuId);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return "redirect:http://cart.gulimall.com/addToCartSuccess.html";
    }


    @GetMapping("/addToCartSuccess.html")
    public String addToCartSuccessPage(@RequestParam("skuId") Long skuId,
                                       Model model) {
        // 再查一次购物车
        Cart.CartItem item = cartService.getCartItem(skuId);
        model.addAttribute("item", item);
        return "success";
    }

    @GetMapping("/checkItem")
    public String checkItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("check") Integer check) {
        cartService.checkItem(skuId, check);
        return "redirect:http://cart.gulimall.com/cart.html";
    }

    @GetMapping("/countItem")
    public String countItem(@RequestParam("skuId") Long skuId,
                            @RequestParam("num") Integer num) {
        cartService.changeItemCount(skuId, num);
        return "redirect:http://cart.gulimall.com/cart.html";
    }

    @GetMapping("/deleteItem")
    public String deleteItem(@RequestParam("skuId") Long skuId) {
        cartService.deleteItem(skuId);
        return "redirect:http://cart.gulimall.com/cart.html";
    }
}