package com.xwh.gulimall.thirdparty.component;


import com.xwh.gulimall.thirdparty.util.HttpUtils;
import org.apache.http.HttpResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class SmsComponent {

    @Value("${appCode}")
    private String appCode;

    @Value("${host}")
    private String host;

    @Value("${path}")
    private String path;

    @Value("${template_id}")
    private String template_id;


    public void sendSmsCode(String phone, String code) {
        String method = "POST";
        String appcode = appCode;
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Authorization", "APPCODE " + appcode);
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        Map<String, String> querys = new HashMap<String, String>();
        Map<String, String> bodys = new HashMap<String, String>();
        bodys.put("content", "code:" + code);
        bodys.put("phone_number", phone);
        bodys.put("template_id", template_id);

        try {
            HttpResponse response = HttpUtils.doPost(host, path, method, headers, querys, bodys);
            System.out.println(response.toString());
            //获取response的body
            //System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
