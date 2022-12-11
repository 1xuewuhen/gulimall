package com.xwh.gulimall.thirdparty.controller;


import com.xwh.common.utils.R;
import com.xwh.gulimall.thirdparty.component.SmsComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sms")
public class SmsSendController {

    @Autowired
    private SmsComponent smsComponent;

    @PostMapping("/sendCode")
    public R sendCode(@RequestParam("phone") String phone,@RequestParam("code")String code){
        smsComponent.sendSmsCode(phone,code);
        return R.ok();
    }

}
