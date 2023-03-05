package com.xwh.gulimall.seckill.controller;


import com.xwh.common.utils.R;
import com.xwh.gulimall.seckill.service.SeckillService;
import com.xwh.gulimall.seckill.to.SecKillSkuRedisTo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    @GetMapping("/getCurrentSeckillSkus")
    @ResponseBody
    public R getCurrentSeckillSkus() {
        List<SecKillSkuRedisTo> vos = seckillService.getCurrentSeckillSkus();
        return R.ok().setDate(vos);
    }

    @ResponseBody
    @GetMapping("/sku/seckill/{skuId}")
    public R getSkuSeckillInfo(@PathVariable("skuId") Long skuId) {
        SecKillSkuRedisTo redisTo = seckillService.getSkuSeckillInfo(skuId);
        return R.ok().setDate(redisTo);
    }

    //    http://seckill.gulimall.com/kill?killId=4_22&key=c84208e7c5554da0884a0a83b69d9450&num=1
    @GetMapping("kill")
    public String secKill(@RequestParam(value = "killId") String killId,
                          @RequestParam("key") String key,
                          @RequestParam("num") Integer num, Model model) {

        String orderSn = seckillService.kill(killId, key, num);
        model.addAttribute("orderSn", orderSn);
        return "success";
    }
}
