package com.xwh.gulimall.auth.controller;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.xwh.common.utils.HttpUtils;
import com.xwh.common.utils.R;
import com.xwh.common.vo.MemberRespVo;
import com.xwh.gulimall.auth.feign.MemberFeignService;
import com.xwh.gulimall.auth.vo.SocialUser;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
public class OAuth2Controller {


    @Value("${gitee_host}")
    private String gitee_host;

    @Value("${gitee_path}")
    private String gitee_path;

    @Value("${gitee_client_id}")
    private String gitee_client_id;

    @Value("${gitee_client_secret}")
    private String gitee_client_secret;

    @Value("${gitee_redirect_uri}")
    private String gitee_redirect_uri;

    @Value("${gitee_grant_type}")
    private String gitee_grant_type;

    @Autowired
    private MemberFeignService memberFeignService;

    @GetMapping("/oauth/success")
    public String gitee(@RequestParam("code") String code, HttpSession session) throws Exception {

        Map<String, String> map = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        map.put("client_id",gitee_client_id);
        map.put("grant_type",gitee_grant_type);
        map.put("code",code);
        map.put("redirect_uri",gitee_redirect_uri);
        map.put("client_secret",gitee_client_secret);
        HttpResponse response = HttpUtils.doPost(gitee_host, gitee_path, "post", headers, null, map);
        if (response.getStatusLine().getStatusCode() == 200) {
            String json = EntityUtils.toString(response.getEntity());
            SocialUser socialUser = JSON.parseObject(json, SocialUser.class);
            Map<String,String> map1 = getGiteeUserInfo(socialUser.getAccess_token());
            if (map1 == null){
                return "redirect:http://auth.gulimall.com/login.html";
            }
            socialUser.setUid(map1.get("id"));
            R r = memberFeignService.oauth2Login(socialUser);
            if (r.getCode() == 0){
                MemberRespVo date = r.getDate(new TypeReference<MemberRespVo>() {
                });
                //TODO 默认发的令牌。session=dsajkdjl。作用域：当前域（解决子域session共享问题）
                //TODO 使用JSON的序列化方式来序列化对象数据到redis中
                session.setAttribute("loginUser",date);
                return "redirect:http://gulimall.com";
            }else {
                return "redirect:http://auth.gulimall.com/login.html";
            }
        }else {
            return "redirect:http://auth.gulimall.com/login.html";
        }
//        HttpEntity entity = response.getEntity();
//        EntityUtils.toString(response.getEntity());
    }

//    https://gitee.com/api/v5/user

    private Map<String, String> getGiteeUserInfo(String access_token) throws Exception {
        Map<String,String> map = new HashMap<>();
        map.put("access_token",access_token);
        HttpResponse response = HttpUtils.doGet("https://gitee.com", "/api/v5/user", "get", new HashMap<>(), map);
        if (response.getStatusLine().getStatusCode() == 200){
            String s = EntityUtils.toString(response.getEntity());
            return JSON.parseObject(s,new TypeReference<>(){});
        }else {
            return null;
        }
    }

}
