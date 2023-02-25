package com.xwh.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xwh.common.to.mq.OrderTo;
import com.xwh.common.to.mq.StockLockedTo;
import com.xwh.common.utils.PageUtils;
import com.xwh.gulimall.ware.entity.WareSkuEntity;
import com.xwh.common.to.SkuHasStockVo;
import com.xwh.gulimall.ware.vo.WareSkuLockVo;

import java.util.List;
import java.util.Map;

/**
 * 商品库存
 *
 * @author xueWuHen
 * @email xueWuHen@gmail.com
 * @date 2022-10-05 14:06:32
 */
public interface WareSkuService extends IService<WareSkuEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void addStock(Long skuId, Long wareId, Integer skuNum);

    List<SkuHasStockVo> getSkusHasStock(List<Long> skuIds);

    Boolean orderLockStock(WareSkuLockVo vo);

    void unlockStock(StockLockedTo to);

    void unlockStock(OrderTo to);
}

