package com.xwh.gulimall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xwh.common.utils.PageUtils;
import com.xwh.gulimall.ware.entity.WareInfoEntity;
import com.xwh.gulimall.ware.vo.FareVo;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 仓库信息
 *
 * @author xueWuHen
 * @email xueWuHen@gmail.com
 * @date 2022-10-05 14:06:32
 */
public interface WareInfoService extends IService<WareInfoEntity> {

    PageUtils queryPage(Map<String, Object> params);

    FareVo getFare(Long addrId);
}

