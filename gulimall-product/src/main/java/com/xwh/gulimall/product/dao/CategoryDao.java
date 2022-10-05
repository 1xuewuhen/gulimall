package com.xwh.gulimall.product.dao;

import com.xwh.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author xueWuHen
 * @email xueWuHen@gmail.com
 * @date 2022-10-05 11:14:32
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
