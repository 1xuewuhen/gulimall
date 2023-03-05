package com.xwh.gulimall.product.feign.fallback;

import com.xwh.common.exception.BizCodeEnum;
import com.xwh.common.utils.R;
import com.xwh.gulimall.product.feign.SeckillFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class SeckillFeignServiceImpl implements SeckillFeignService {
    @Override
    public R getSkuSeckillInfo(Long skuId) {
        log.info("融断方法执行....");
        return R.error(BizCodeEnum.TO_MANY_REQUEST.getCode(), BizCodeEnum.TO_MANY_REQUEST.getMessage());
    }
}
