package com.xwh.gulimall.ware.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xwh.common.utils.PageUtils;
import com.xwh.common.utils.Query;
import com.xwh.common.utils.R;
import com.xwh.gulimall.ware.dao.WareInfoDao;
import com.xwh.gulimall.ware.entity.WareInfoEntity;
import com.xwh.gulimall.ware.feign.MemberFeignService;
import com.xwh.gulimall.ware.service.WareInfoService;
import com.xwh.gulimall.ware.vo.FareVo;
import com.xwh.gulimall.ware.vo.MemberAddressVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Map;


@Service("wareInfoService")
public class WareInfoServiceImpl extends ServiceImpl<WareInfoDao, WareInfoEntity> implements WareInfoService {

    @Autowired
    private MemberFeignService memberFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        LambdaQueryWrapper<WareInfoEntity> wrapper = new LambdaQueryWrapper<>();
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.eq(WareInfoEntity::getId, key)
                    .or()
                    .like(WareInfoEntity::getName, key)
                    .or()
                    .like(WareInfoEntity::getAddress, key)
                    .or()
                    .like(WareInfoEntity::getAreacode, key);
        }
        IPage<WareInfoEntity> page = this.page(
                new Query<WareInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    /**
     * 根据用户的收货地址计算运费
     *
     * @param addrId
     * @return
     */
    @Override
    public FareVo getFare(Long addrId) {
        FareVo fareVo = new FareVo();
        R r = memberFeignService.info(addrId);
        MemberAddressVo date = r.getDate("memberReceiveAddress",new TypeReference<MemberAddressVo>() {
        });
        if (date != null) {
            String phone = date.getPhone();
            phone = phone.substring(phone.length() - 1);
            BigDecimal fare = new BigDecimal(phone);
            fareVo.setAddress(date);
            fareVo.setFare(fare);
        }
        return fareVo;
    }

}