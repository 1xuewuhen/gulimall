package com.xwh.gulimall.order.web;


import com.xwh.common.utils.HttpUtils;
import com.xwh.gulimall.order.config.AlipayTemplate;
import com.xwh.gulimall.order.service.OrderService;
import com.xwh.gulimall.order.vo.PayVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;

@Controller
public class PayWebController {

    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private OrderService orderService;

    @ResponseBody
    @GetMapping(value = "/payOrder", produces = {"text/html"})
    public String payOrder(@RequestParam("orderSn") String orderSn) throws Exception {
        PayVo payVo = orderService.getOrderPay(orderSn);
        //        System.out.println(pay);
        HashMap<String, String> map = new HashMap<>();
        map.put("body", payVo.getBody());
        map.put("subject", payVo.getSubject());
        map.put("outTradeNo", payVo.getOut_trade_no());
        map.put("totalAmount", payVo.getTotal_amount());
        HashMap<String, String> hashMap = new HashMap<>();
        HttpUtils.doGet("http://127.0.0.1:50010", "/pay/myZhiFuBao", "get", hashMap, map);
        return alipayTemplate.pay(payVo);
    }
}
