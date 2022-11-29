package com.xwh.gulimall.ware.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xwh.common.utils.PageUtils;
import com.xwh.common.utils.Query;

import com.xwh.gulimall.ware.dao.PurchaseDetailDao;
import com.xwh.gulimall.ware.entity.PurchaseDetailEntity;
import com.xwh.gulimall.ware.service.PurchaseDetailService;
import org.springframework.util.StringUtils;


@Service("purchaseDetailService")
public class PurchaseDetailServiceImpl extends ServiceImpl<PurchaseDetailDao, PurchaseDetailEntity> implements PurchaseDetailService {

    /**
     * status
     * wareId
     *
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<PurchaseDetailEntity> wrapper = new LambdaQueryWrapper<>();
        String key = (String) params.get("key");
        String status = (String) params.get("status");
        String wareId = (String) params.get("wareId");
        if (!StringUtils.isEmpty(key)) {
            wrapper.and(w -> {
                w.eq(PurchaseDetailEntity::getPurchaseId, key)
                        .or()
                        .eq(PurchaseDetailEntity::getSkuId, key);
            });
        }
        if (!StringUtils.isEmpty(status)) {
            wrapper.eq(PurchaseDetailEntity::getStatus, status);
        }
        if (!StringUtils.isEmpty(wareId)) {
            wrapper.eq(PurchaseDetailEntity::getWareId, wareId);
        }

        IPage<PurchaseDetailEntity> page = this.page(
                new Query<PurchaseDetailEntity>().getPage(params),
                wrapper
        );
        return new PageUtils(page);
    }

    @Override
    public List<PurchaseDetailEntity> listDetailByPurchaseId(Long id) {
        LambdaQueryWrapper<PurchaseDetailEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PurchaseDetailEntity::getPurchaseId,id);
        return this.list(wrapper);
    }

}