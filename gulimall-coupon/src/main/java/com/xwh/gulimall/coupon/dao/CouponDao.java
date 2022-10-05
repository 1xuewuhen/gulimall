package com.xwh.gulimall.coupon.dao;

import com.xwh.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author xueWuHen
 * @email xueWuHen@gmail.com
 * @date 2022-10-05 12:52:58
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
