package com.xwh.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xwh.common.exception.NoStockException;
import com.xwh.common.utils.PageUtils;
import com.xwh.common.utils.Query;
import com.xwh.common.utils.R;
import com.xwh.common.vo.MemberRespVo;
import com.xwh.gulimall.order.constant.OrderConstant;
import com.xwh.gulimall.order.dao.OrderDao;
import com.xwh.gulimall.order.entity.OrderEntity;
import com.xwh.gulimall.order.entity.OrderItemEntity;
import com.xwh.gulimall.order.enume.OrderStatusEnum;
import com.xwh.gulimall.order.feign.CartFeignService;
import com.xwh.gulimall.order.feign.MemberFeignService;
import com.xwh.gulimall.order.feign.ProductFeignService;
import com.xwh.gulimall.order.feign.WareFeignService;
import com.xwh.gulimall.order.interceptor.LoginUserInterceptor;
import com.xwh.gulimall.order.service.OrderItemService;
import com.xwh.gulimall.order.service.OrderService;
import com.xwh.gulimall.order.to.OrderCreateTo;
import com.xwh.gulimall.order.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    private ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();
    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private CartFeignService cartFeignService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private ThreadPoolExecutor executor;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ProductFeignService productFeignService;

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
        //TODO 防重令牌
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId(), token, 30, TimeUnit.MINUTES);
        confirmVo.setOrderToken(token);

        CompletableFuture.allOf(memberFuture, CartFuture).get();
        return confirmVo;
    }

    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        MemberRespVo memberRespVo = LoginUserInterceptor.threadLocal.get();
        SubmitOrderResponseVo response = new SubmitOrderResponseVo();
        response.setCode(0);
        confirmVoThreadLocal.set(vo);
        // 1、验证令牌
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        String orderToken = vo.getOrderToken();
        // 原子验证令牌和删除令牌
        Long result = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), List.of(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId()), orderToken);
        if (result == 0L) {
            // 令牌验证失败
            return response;
        } else {
            //令牌验证成功
            // 下单：去创建订单，验令牌，验价格，锁库存...
            OrderCreateTo order = createOrder();
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                saveOrder(order);
                WareSkuLockVo lockVo = new WareSkuLockVo();
                lockVo.setOrderSn(order.getOrder().getOrderSn());
                List<OrderItemVo> locks = order.getOrderItems().stream().map(item -> {
                    OrderItemVo itemVo = new OrderItemVo();
                    itemVo.setSkuId(item.getSkuId());
                    itemVo.setCount(item.getSkuQuantity());
                    itemVo.setTitle(item.getSkuName());
                    return itemVo;
                }).collect(Collectors.toList());
                lockVo.setLocks(locks);
                //TODO 远程锁库存
                R r = wareFeignService.orderLockStock(lockVo);
                if (r.getCode() == 0) {
                    response.setOrder(order.getOrder());
                    return response;
                } else {
//                    response.setCode(3);
                    String msg = (String) r.get("msg");
//                    return response;
                    throw new NoStockException(msg);
                }
            } else {
                response.setCode(2);
                return response;
            }
        }
        /*if (orderToken != null && orderToken.equals(redisToken)) {
            redisTemplate.delete(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId());
        }else {

        }*/
    }

    private void saveOrder(OrderCreateTo order) {
        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);
        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);

    }

    private OrderCreateTo createOrder() {
        OrderCreateTo orderCreateTo = new OrderCreateTo();
        OrderSubmitVo submitVo = confirmVoThreadLocal.get();
        //1、生成订单号
        String orderSn = IdWorker.getTimeId();
        //创建订单号
        OrderEntity orderEntity = buildOrder(submitVo, orderSn);
        //2、获取到所有的订单项
        List<OrderItemEntity> itemEntities = buildOrderItems(orderSn);
        //验价
        //计算价格相关
        if (itemEntities != null) {
            computePrice(orderEntity, itemEntities);
        }
        orderCreateTo.setOrder(orderEntity);
        orderCreateTo.setOrderItems(itemEntities);
        return orderCreateTo;
    }

    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> itemEntities) {
        BigDecimal total = new BigDecimal("0.0");
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal integration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");
        BigDecimal gift = new BigDecimal("0.0");
        BigDecimal growh = new BigDecimal("0.0");

        for (OrderItemEntity entity : itemEntities) {
            coupon = coupon.add(entity.getCouponAmount());
            integration = integration.add(entity.getIntegrationAmount());
            promotion = promotion.add(entity.getPromotionAmount());
            total = total.add(entity.getRealAmount());
            gift = gift.add(new BigDecimal(entity.getGiftIntegration().toString()));
            growh = growh.add(new BigDecimal(entity.getGiftGrowth().toString()));

        }
        //1、订单价格相关
        orderEntity.setTotalAmount(total);
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setCouponAmount(coupon);
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setGrowth(growh.intValue());
        orderEntity.setIntegration(gift.intValue());
        orderEntity.setDeleteStatus(0);


    }

    private OrderEntity buildOrder(OrderSubmitVo submitVo, String orderSn) {
        MemberRespVo memberRespVo = LoginUserInterceptor.threadLocal.get();
        OrderEntity entity = new OrderEntity();
        entity.setOrderSn(orderSn);
        entity.setMemberId(memberRespVo.getId());
        //获取收货地址信息
        R r = wareFeignService.getFare(submitVo.getAddrId());
        FareVo fareResp = r.getDate(new TypeReference<>() {
        });
        entity.setFreightAmount(fareResp.getFare());
        //设置收获人信息
        entity.setReceiverCity(fareResp.getAddress().getCity());
        entity.setReceiverDetailAddress(fareResp.getAddress().getDetailAddress());
        entity.setReceiverName(fareResp.getAddress().getName());
        entity.setReceiverPhone(fareResp.getAddress().getPhone());
        entity.setReceiverProvince(fareResp.getAddress().getProvince());
        entity.setReceiverPostCode(fareResp.getAddress().getPostCode());
        entity.setReceiverRegion(fareResp.getAddress().getRegion());
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        entity.setAutoConfirmDay(7);

        return entity;
    }

    /**
     * 构建所有的订单项数据
     *
     * @return
     */
    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        //最后确定每个购物项的价格
        R r1 = cartFeignService.currentUserCartItem();
        List<OrderItemVo> currentUserCartItems = r1.getDate(new TypeReference<List<OrderItemVo>>() {
        });
        if (currentUserCartItems != null && currentUserCartItems.size() > 0) {
            List<OrderItemEntity> itemEntitys = currentUserCartItems.stream().map(cartItem -> {
                OrderItemEntity itemEntity = buildOrderItem(cartItem);
                itemEntity.setOrderSn(orderSn);
                return itemEntity;
            }).collect(Collectors.toList());
            return itemEntitys;
        }
        return null;
    }

    /**
     * 构建每一个订单项
     *
     * @param cartItem
     * @return
     */
    private OrderItemEntity buildOrderItem(OrderItemVo cartItem) {
        OrderItemEntity itemEntity = new OrderItemEntity();
        itemEntity.setSkuId(cartItem.getSkuId());
        itemEntity.setSkuName(cartItem.getTitle());
        itemEntity.setSkuPic(cartItem.getImage());
        itemEntity.setSkuPrice(cartItem.getPrice());
        String skuAttr = StringUtils.collectionToDelimitedString(cartItem.getSkuAttr(), ",");
        itemEntity.setSkuAttrsVals(skuAttr);
        itemEntity.setSkuQuantity(cartItem.getCount());
        itemEntity.setGiftGrowth(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        itemEntity.setGiftIntegration(cartItem.getPrice().multiply(new BigDecimal(cartItem.getCount().toString())).intValue());
        Long skuId = cartItem.getSkuId();
        R r = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo date = r.getDate(new TypeReference<SpuInfoVo>() {
        });
        itemEntity.setSpuId(date.getId());
        itemEntity.setSpuBrand(date.getBrandId().toString());
        itemEntity.setSpuName(date.getSpuName());
        itemEntity.setCategoryId(date.getCatalogId());
        itemEntity.setPromotionAmount(new BigDecimal("0.0"));
        itemEntity.setCouponAmount(new BigDecimal("0.0"));
        itemEntity.setIntegrationAmount(new BigDecimal("0.0"));
        BigDecimal origin = itemEntity.getSkuPrice().multiply(new BigDecimal(itemEntity.getSkuQuantity().toString()));
        BigDecimal subtract = origin.subtract(itemEntity.getPromotionAmount()).subtract(itemEntity.getPromotionAmount()).subtract(itemEntity.getIntegrationAmount());
        itemEntity.setRealAmount(subtract);


        return itemEntity;
    }

}

