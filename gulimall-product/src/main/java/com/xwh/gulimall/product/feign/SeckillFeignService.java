package com.xwh.gulimall.product.feign;


import com.xwh.common.utils.R;
import com.xwh.gulimall.product.feign.fallback.SeckillFeignServiceImpl;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "gilimall-seckill",fallback = SeckillFeignServiceImpl.class)
public interface SeckillFeignService {

    @GetMapping("/sku/seckill/{skuId}")
    R getSkuSeckillInfo(@PathVariable("skuId") Long skuId);
}
