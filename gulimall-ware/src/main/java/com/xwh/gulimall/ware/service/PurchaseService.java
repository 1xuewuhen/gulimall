package com.xwh.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xwh.common.utils.PageUtils;
import com.xwh.gulimall.ware.entity.PurchaseEntity;
import com.xwh.gulimall.ware.vo.MergeVo;
import com.xwh.gulimall.ware.vo.PurchaseDoneVo;

import java.util.List;
import java.util.Map;

/**
 * 采购信息
 *
 * @author xueWuHen
 * @email xueWuHen@gmail.com
 * @date 2022-10-05 14:06:32
 */
public interface PurchaseService extends IService<PurchaseEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPageUnreceivePurchase(Map<String, Object> params);

    void mergePurchase(MergeVo vo);

    void received(List<Long> ids);

    void done(PurchaseDoneVo doneVo);
}

