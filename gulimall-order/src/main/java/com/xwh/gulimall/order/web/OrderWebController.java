package com.xwh.gulimall.order.web;


import com.xwh.gulimall.order.service.OrderService;
import com.xwh.gulimall.order.vo.OrderConfirmVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/toTrade")
    public String toTrade(Model model) {
        OrderConfirmVo confirmVo = null;
        try {
            confirmVo = orderService.confirmOrder();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        model.addAttribute("orderConfirmData", confirmVo);
        return "confirm";
    }
}
