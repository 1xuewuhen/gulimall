package com.xwh.gulimall.order.dao;

import com.xwh.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author xueWuHen
 * @email xueWuHen@gmail.com
 * @date 2022-10-05 14:01:29
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
