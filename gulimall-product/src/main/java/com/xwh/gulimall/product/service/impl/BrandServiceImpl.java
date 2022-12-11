package com.xwh.gulimall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xwh.gulimall.product.service.CategoryBrandRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xwh.common.utils.PageUtils;
import com.xwh.common.utils.Query;

import com.xwh.gulimall.product.dao.BrandDao;
import com.xwh.gulimall.product.entity.BrandEntity;
import com.xwh.gulimall.product.service.BrandService;
import org.springframework.util.StringUtils;


@Service("brandService")
public class BrandServiceImpl extends ServiceImpl<BrandDao, BrandEntity> implements BrandService {

    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<BrandEntity> page = this.page(
                new Query<BrandEntity>().getPage(params),
                new QueryWrapper<BrandEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void updateDetail(BrandEntity brand) {
        this.baseMapper.updateById(brand);
        if (!StringUtils.isEmpty(brand.getName())){
            categoryBrandRelationService.updateBrand(brand.getBrandId(),brand.getName());
            //TODO 更新其他关联关系
        }
    }

//    @Cacheable(value = "brand",key = "'brand:'+#root.args[0]")
    @Override
    public List<BrandEntity> getBrandByIds(List<Long> brandIds) {
        return baseMapper.selectList(new LambdaQueryWrapper<BrandEntity>().in(BrandEntity::getBrandId,brandIds));
    }

}