package com.xwh.gulimall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xwh.common.to.SkuReductionTo;
import com.xwh.common.utils.PageUtils;
import com.xwh.gulimall.coupon.entity.SkuFullReductionEntity;

import java.util.Map;

/**
 * 商品满减信息
 *
 * @author xueWuHen
 * @email xueWuHen@gmail.com
 * @date 2022-10-05 12:52:58
 */
public interface SkuFullReductionService extends IService<SkuFullReductionEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSkuReduction(SkuReductionTo reductionTo);
}

