package com.xwh.zhifubao.controller;


import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.xwh.common.utils.HttpUtils;
import com.xwh.zhifubao.vo.PayAsyncVo;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
public class PaySuccessController {

    @GetMapping("/pay/myZhiFuBao")
    public void payed(@RequestParam Map<String, String> map) throws Exception {
        PayAsyncVo payAsyncVo = new PayAsyncVo();
        payAsyncVo.setGmt_create(new Date().toString());
        payAsyncVo.setCharset("utf-8");
        payAsyncVo.setGmt_payment(new Date().toString());
        payAsyncVo.setNotify_time(new Date());
        payAsyncVo.setSubject(map.get("subject"));
        payAsyncVo.setSign(UUID.randomUUID().toString());
        payAsyncVo.setBuyer_id(UUID.randomUUID().toString());
        payAsyncVo.setBody(map.get("body"));
        payAsyncVo.setInvoice_amount(map.get("totalAmount"));
        payAsyncVo.setVersion("1.0");
        payAsyncVo.setNotify_id(UUID.randomUUID().toString());
        payAsyncVo.setFund_bill_list("123213");
        payAsyncVo.setNotify_type("xxx");
        payAsyncVo.setOut_trade_no(map.get("outTradeNo"));
        payAsyncVo.setTotal_amount(map.get("totalAmount"));
        payAsyncVo.setTrade_status("TRADE_SUCCESS");
        payAsyncVo.setTrade_no(UUID.randomUUID().toString());
        payAsyncVo.setAuth_app_id(UUID.randomUUID().toString());
        payAsyncVo.setReceipt_amount(map.get("totalAmount"));
        payAsyncVo.setPoint_amount(map.get("totalAmount"));
        payAsyncVo.setApp_id(UUID.randomUUID().toString());
        payAsyncVo.setBuyer_pay_amount(map.get("totalAmount"));
        payAsyncVo.setSign_type("RSA2");
        payAsyncVo.setSeller_id(UUID.randomUUID().toString());
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("Content-Type", "application/json");
        Map<String, String> stringMap = JSON.parseObject(JSON.toJSONString(payAsyncVo), new TypeReference<Map<String, String>>() {
        });
        HttpUtils.doPost("http://order.gulimall.com",
                "/my/payed/notify",
                "post", hashMap,
                new HashMap<>(),
                JSON.toJSONString(stringMap));
    }

}
