package com.xwh.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.xwh.common.utils.R;
import com.xwh.common.vo.MemberRespVo;
import com.xwh.gulimall.order.feign.CartFeignService;
import com.xwh.gulimall.order.feign.MemberFeignService;
import com.xwh.gulimall.order.feign.WareFeignService;
import com.xwh.gulimall.order.interceptor.LoginUserInterceptor;
import com.xwh.gulimall.order.vo.MemberAddressVo;
import com.xwh.gulimall.order.vo.OrderConfirmVo;
import com.xwh.gulimall.order.vo.OrderItemVo;
import com.xwh.gulimall.order.vo.SkuStockVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xwh.common.utils.PageUtils;
import com.xwh.common.utils.Query;

import com.xwh.gulimall.order.dao.OrderDao;
import com.xwh.gulimall.order.entity.OrderEntity;
import com.xwh.gulimall.order.service.OrderService;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private CartFeignService cartFeignService;

    @Autowired
    private ThreadPoolExecutor executor;

    @Autowired
    private WareFeignService wareFeignService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 订单确认页返回的数据
     *
     * @return
     */
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        MemberRespVo memberRespVo = LoginUserInterceptor.threadLocal.get();
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        CompletableFuture<Void> memberFuture = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(attributes);
            // 远程查询所有收货地址
            R r = memberFeignService.getAddress(memberRespVo.getId());
            List<MemberAddressVo> addressVos = r.getDate(new TypeReference<List<MemberAddressVo>>() {
            });
            confirmVo.setAddress(addressVos);
        }, executor);
        CompletableFuture<Void> CartFuture = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(attributes);
            // 远程查询购物车所有选中的购物项
            R r1 = cartFeignService.currentUserCartItem();
            List<OrderItemVo> items = r1.getDate(new TypeReference<List<OrderItemVo>>() {
            });
            confirmVo.setItems(items);
        }, executor).thenRunAsync(() -> {
            List<OrderItemVo> items = confirmVo.getItems();
            List<Long> collect = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
            R r = wareFeignService.getSkusHasStock(collect);
            List<SkuStockVo> date = r.getDate(new TypeReference<List<SkuStockVo>>() {
            });
            if (date != null) {
                Map<Long, Boolean> map = date.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(map);
            }
        }, executor);
        Integer integration = memberRespVo.getIntegration();
        confirmVo.setIntegration(integration);
        //其他数据自动计算
        CompletableFuture.allOf(memberFuture, CartFuture).get();
        //TODO 防重令牌
        return confirmVo;
    }

}