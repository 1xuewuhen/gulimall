package com.xwh.gulimall.order.web;


import com.alipay.api.AlipayApiException;
import com.xwh.gulimall.order.config.AlipayTemplate;
import com.xwh.gulimall.order.service.OrderService;
import com.xwh.gulimall.order.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PayWebController {

    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private OrderService orderService;

    @ResponseBody
    @GetMapping("/payOrder")
    public String payOrder(@RequestParam("orderSn") String orderSn) throws AlipayApiException {

        PayVo payVo = orderService.getOrderPay(orderSn);
        alipayTemplate.pay(payVo);
        return "xxxx";
    }
}
