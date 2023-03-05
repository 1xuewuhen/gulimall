package com.xwh.gulimall.seckill.controller;


import com.xwh.common.utils.R;
import com.xwh.gulimall.seckill.service.SeckillService;
import com.xwh.gulimall.seckill.to.SecKillSkuRedisTo;
import com.xwh.gulimall.seckill.vo.SeckillSessionsWithSkus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SeckillController {

    @Autowired
    private SeckillService seckillService;

    @GetMapping("/getCurrentSeckillSkus")
    public R getCurrentSeckillSkus() {
        List<SecKillSkuRedisTo> vos = seckillService.getCurrentSeckillSkus();
        return R.ok().setDate(vos);
    }

    @GetMapping("/sku/seckill/{skuId}")
    public R getSkuSeckillInfo(@PathVariable("skuId") Long skuId) {
        SecKillSkuRedisTo redisTo = seckillService.getSkuSeckillInfo(skuId);
        return R.ok().setDate(redisTo);
    }
}
