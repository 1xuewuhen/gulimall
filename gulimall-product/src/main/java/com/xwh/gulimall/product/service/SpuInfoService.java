package com.xwh.gulimall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xwh.common.utils.PageUtils;
import com.xwh.gulimall.product.entity.SpuInfoDescEntity;
import com.xwh.gulimall.product.entity.SpuInfoEntity;
import com.xwh.gulimall.product.vo.SpuSaveVo;

import java.util.Map;

/**
 * spu信息
 *
 * @author xueWuHen
 * @email xueWuHen@gmail.com
 * @date 2022-10-05 11:14:33
 */
public interface SpuInfoService extends IService<SpuInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSpuInfo(SpuSaveVo vo);

    void saveBaseSpuInfo(SpuInfoEntity infoEntity);

    PageUtils queryPageByCondition(Map<String, Object> params);
}

