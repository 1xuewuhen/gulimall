package com.xwh.gulimall.coupon.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xwh.gulimall.coupon.entity.SeckillSkuRelationEntity;
import com.xwh.gulimall.coupon.service.SeckillSkuRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xwh.common.utils.PageUtils;
import com.xwh.common.utils.Query;

import com.xwh.gulimall.coupon.dao.SeckillSessionDao;
import com.xwh.gulimall.coupon.entity.SeckillSessionEntity;
import com.xwh.gulimall.coupon.service.SeckillSessionService;


@Service("seckillSessionService")
public class SeckillSessionServiceImpl extends ServiceImpl<SeckillSessionDao, SeckillSessionEntity> implements SeckillSessionService {

    @Autowired
    private SeckillSkuRelationService seckillSkuRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SeckillSessionEntity> page = this.page(
                new Query<SeckillSessionEntity>().getPage(params),
                new QueryWrapper<SeckillSessionEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<SeckillSessionEntity> getLate3DaySession() {

        List<SeckillSessionEntity> list = this.list(new LambdaQueryWrapper<SeckillSessionEntity>().between(SeckillSessionEntity::getStartTime,
                startTime(),
                endTime()));
        if (list != null && list.size() > 0) {
            return list.stream().peek(session -> {
                Long id = session.getId();
                List<SeckillSkuRelationEntity> relationEntities = seckillSkuRelationService.list(new LambdaQueryWrapper<SeckillSkuRelationEntity>().eq(SeckillSkuRelationEntity::getPromotionSessionId, id));
                session.setRelationSkus(relationEntities);
            }).collect(Collectors.toList());
        }
        return null;
    }

    private String startTime() {
        //计算最近三天
        LocalDate now = LocalDate.now();
        LocalTime min = LocalTime.MIN;
        return LocalDateTime.of(now, min).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss"));
    }

    private String endTime() {
        LocalDate plus = LocalDate.now().plusDays(2);
        LocalTime max = LocalTime.MAX;
        return LocalDateTime.of(plus, max).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH-mm-ss"));
    }

}