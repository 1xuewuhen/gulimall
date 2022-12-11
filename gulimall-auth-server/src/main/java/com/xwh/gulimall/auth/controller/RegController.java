package com.xwh.gulimall.auth.controller;


import com.alibaba.fastjson.TypeReference;
import com.xwh.common.constant.AuthServerConstant;
import com.xwh.common.exception.BizCodeEnum;
import com.xwh.common.utils.R;
import com.xwh.gulimall.auth.feign.MemberFeignService;
import com.xwh.gulimall.auth.feign.ThirdPartFeignService;
import com.xwh.gulimall.auth.util.VerificationCodeUtil;
import com.xwh.gulimall.auth.vo.UserRegisVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class RegController {

    @Autowired
    private ThirdPartFeignService thirdPartFeignService;

    @Autowired
    private BCryptPasswordEncoder encoder;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private MemberFeignService memberFeignService;

    @ResponseBody
    @GetMapping("/sms/sendCode")
    public R sendCode(@RequestParam("phone") String phone) {
        //TODO 接口放刷
        String redisCode = redisTemplate.opsForValue().get("AuthServerConstant.SMS_CODE_CACHE_PREFIX+phone");
        if (!StringUtils.isEmpty(redisCode)) {
            String[] s = redisCode.split("_");
            if (System.currentTimeMillis() - Long.parseLong(s[1]) < 60000) {
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getCode(), BizCodeEnum.SMS_CODE_EXCEPTION.getMessage());
            }
        }
        String code = VerificationCodeUtil.generateVerificationCode();
        String code1 = code + "_" + System.currentTimeMillis();
        redisTemplate.opsForValue().set(AuthServerConstant.SMS_CODE_CACHE_PREFIX + phone, code1, 10, TimeUnit.MINUTES);
        thirdPartFeignService.sendCode(phone, code);
        return R.ok();
    }

    @PostMapping("/regist")
    public String regist(@Valid UserRegisVo vo, BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            Map<String, String> collect = result.getFieldErrors().stream().collect(Collectors.toMap(FieldError::getField, DefaultMessageSourceResolvable::getDefaultMessage));
//            model.addAttribute("errors", collect);
            redirectAttributes.addFlashAttribute("errors", collect);
            return "redirect:http://auth.gulimall.com/reg.html";
        }
        String code = vo.getCode();
        String s = redisTemplate.opsForValue().get(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
        if (!StringUtils.isEmpty(s)) {
            String[] s1 = s.split("_");
            if (code.equals(s1[0])) {
                redisTemplate.delete(AuthServerConstant.SMS_CODE_CACHE_PREFIX + vo.getPhone());
                R r = memberFeignService.regist(vo);
                if (r.getCode() == 0) {
                    return "redirect:http://auth.gulimall.com/login.html";
                } else {
                    Map<String, String> map = new HashMap<>();
                    map.put("msg",r.get("msg").toString());
                    redirectAttributes.addFlashAttribute("errors",map);
                    return "redirect:http://auth.gulimall.com/reg.html";
                }
            } else {
                Map<String, String> map = new HashMap<>();
                map.put("code", "验证码错误");
                redirectAttributes.addFlashAttribute("errors", map);
                return "redirect:http://auth.gulimall.com/reg.html";
            }
        } else {
            Map<String, String> map = new HashMap<>();
            map.put("code", "验证码错误");
            redirectAttributes.addFlashAttribute("errors", map);
            return "redirect:http://auth.gulimall.com/reg.html";
        }
    }
}
