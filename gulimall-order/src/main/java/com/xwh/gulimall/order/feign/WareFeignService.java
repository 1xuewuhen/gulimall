package com.xwh.gulimall.order.feign;


import com.xwh.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("gulimall-ware")
public interface WareFeignService {


    @PostMapping("/ware/waresku/hasStock")
    R getSkusHasStock(@RequestBody List<Long> skuIds);
}
