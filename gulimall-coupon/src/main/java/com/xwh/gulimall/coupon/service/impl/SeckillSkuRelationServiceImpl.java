package com.xwh.gulimall.coupon.service.impl;

import org.springframework.stereotype.Service;

import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xwh.common.utils.PageUtils;
import com.xwh.common.utils.Query;

import com.xwh.gulimall.coupon.dao.SeckillSkuRelationDao;
import com.xwh.gulimall.coupon.entity.SeckillSkuRelationEntity;
import com.xwh.gulimall.coupon.service.SeckillSkuRelationService;
import org.springframework.util.StringUtils;


@Service("seckillSkuRelationService")
public class SeckillSkuRelationServiceImpl extends ServiceImpl<SeckillSkuRelationDao, SeckillSkuRelationEntity> implements SeckillSkuRelationService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        QueryWrapper<SeckillSkuRelationEntity> wrapper = new QueryWrapper<>();
        String promotionSessionId = params.get("promotionSessionId").toString();
        if (!StringUtils.isEmpty(promotionSessionId)) {
            wrapper.eq("promotion_session_id", promotionSessionId);
        }
        /*String key = params.get("key").toString();
        if (!StringUtils.isEmpty(key)) {
            wrapper.eq("id", key).or().eq("promotion_id", key);
        }*/
        IPage<SeckillSkuRelationEntity> page = this.page(
                new Query<SeckillSkuRelationEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

}