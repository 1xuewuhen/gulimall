package com.xwh.gulimall.ware.service.impl;

import com.xwh.common.constant.WareConstant;
import com.xwh.gulimall.ware.entity.PurchaseDetailEntity;
import com.xwh.gulimall.ware.service.PurchaseDetailService;
import com.xwh.gulimall.ware.service.WareSkuService;
import com.xwh.gulimall.ware.vo.MergeVo;
import com.xwh.gulimall.ware.vo.PurchaseDoneVo;
import com.xwh.gulimall.ware.vo.PurchaseItemDomeVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xwh.common.utils.PageUtils;
import com.xwh.common.utils.Query;

import com.xwh.gulimall.ware.dao.PurchaseDao;
import com.xwh.gulimall.ware.entity.PurchaseEntity;
import com.xwh.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    private PurchaseDetailService detailService;

    @Autowired
    private WareSkuService wareSkuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceivePurchase(Map<String, Object> params) {

        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>().eq("status", 0).or().eq("status", 1)
        );

        return new PageUtils(page);
    }


    @Transactional
    @Override
    public void mergePurchase(MergeVo vo) {
        Long purchaseId = vo.getPurchaseId();
        if (null == purchaseId) {
            // 新建一个采购单
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATE.getCode());
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            this.save(purchaseEntity);
            purchaseId = purchaseEntity.getId();
        }

        // TODO 确认采购单状态是0,1才可以合并

        List<Long> items = vo.getItems();
        Long finalPurchaseId = purchaseId;
        List<PurchaseDetailEntity> collect = items.stream().map(i -> {
            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
            detailEntity.setId(i);
            detailEntity.setPurchaseId(finalPurchaseId);
            detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
            return detailEntity;
        }).collect(Collectors.toList());

        detailService.updateBatchById(collect);
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseId);
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }

    @Transactional
    @Override
    public void received(List<Long> ids) {
        // 确认当前采购是新建或者以分配状态
        List<PurchaseEntity> collect = ids.stream()
                .map(this::getById)
                .filter(item -> Objects.equals(item.getStatus(), WareConstant.PurchaseStatusEnum.CREATE.getCode()) || Objects.equals(item.getStatus(), WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()))
                .peek(item -> {
                    item.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
                    item.setUpdateTime(new Date());
                })
                .collect(Collectors.toList());
        // 改变采购单的状态
        this.updateBatchById(collect);
        // 改变采购项的状态
        collect.forEach(item -> {
            List<PurchaseDetailEntity> entities = detailService.listDetailByPurchaseId(item.getId());
            List<PurchaseDetailEntity> detailEntities = entities.stream().map(entitie -> {
                PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
                detailEntity.setId(entitie.getId());
                detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
                return detailEntity;
            }).collect(Collectors.toList());
            detailService.updateBatchById(detailEntities);
        });
    }


    @Transactional
    @Override
    public void done(PurchaseDoneVo doneVo) {
        Long id = doneVo.getId();
        // 改变采购项状态
        List<PurchaseItemDomeVo> items = doneVo.getItems();
        List<PurchaseDetailEntity> updates = new ArrayList<>();
        boolean flag = true;
        for (PurchaseItemDomeVo item : items) {
            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
            if (Objects.equals(item.getStatus(), WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode())) {
                flag = false;
                detailEntity.setStatus(item.getStatus());
            } else {
                detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.FINISH.getCode());
                //将成功采购的进行入库
                PurchaseDetailEntity entity = detailService.getById(item.getItemId());
                wareSkuService.addStock(entity.getSkuId(),entity.getWareId(),entity.getSkuNum());
            }
            detailEntity.setId(item.getItemId());
            updates.add(detailEntity);
        }
        detailService.updateBatchById(updates);

        // 改变采购单状态
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(id);
        purchaseEntity.setStatus(flag ? WareConstant.PurchaseStatusEnum.FINISH.getCode() : WareConstant.PurchaseStatusEnum.HASERROR.getCode());
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);

    }

}