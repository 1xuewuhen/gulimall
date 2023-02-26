package com.xwh.gulimall.order.listener;


import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.xwh.gulimall.order.config.AlipayTemplate;
import com.xwh.gulimall.order.service.OrderService;
import com.xwh.gulimall.order.vo.PayAsyncVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
public class OrderPayedListener {

    @Autowired
    private OrderService orderService;

    @Autowired
    private AlipayTemplate alipayTemplate;
//    vleyeq0537@sandbox.com

    @PostMapping("/payed/notify")
    public String handleAliPayed(PayAsyncVo vo, HttpServletRequest request) throws AlipayApiException {
        // 验签
        Map<String, String> params = new HashMap<>();
        Map<String, String[]> requestParams = request.getParameterMap();
        for (String name : requestParams.keySet()) {
            String[] values = requestParams.get(name);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i]
                        : valueStr + values[i] + ",";
            }
//            valueStr = new String(valueStr.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
            params.put(name, valueStr);
        }
        boolean b = AlipaySignature.rsaCheckV1(params, alipayTemplate.getAlipay_public_key(), alipayTemplate.getCharset(), alipayTemplate.getSign_type());
        if (b) {
            log.info("success");
            return orderService.handleAliPayed(vo);
        } else {
            log.info("error");
            return "error";
        }
    }

    @PostMapping("/my/payed/notify")
    public void handleAliPayed(@RequestBody Map<String, String> vo) {
        PayAsyncVo payAsyncVo = JSON.parseObject(JSON.toJSONString(vo), PayAsyncVo.class);
        orderService.handleAliPayed(payAsyncVo);
    }

    @GetMapping("/my/test")
    public String tset() {
        return "rr2dasf12313";
    }

    @PostMapping("/my/test12")
    public String tests(@RequestBody Map<String, String> stringMap) {
        return "fsdafwqresfdsa324" + stringMap.toString();
    }
}
