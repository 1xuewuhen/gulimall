package com.xwh.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xwh.common.exception.NoStockException;
import com.xwh.common.to.mq.OrderTo;
import com.xwh.common.utils.PageUtils;
import com.xwh.common.utils.Query;
import com.xwh.common.utils.R;
import com.xwh.common.vo.MemberRespVo;
import com.xwh.gulimall.order.constant.OrderConstant;
import com.xwh.gulimall.order.dao.OrderDao;
import com.xwh.gulimall.order.entity.OrderEntity;
import com.xwh.gulimall.order.entity.OrderItemEntity;
import com.xwh.gulimall.order.entity.PaymentInfoEntity;
import com.xwh.gulimall.order.enume.OrderStatusEnum;
import com.xwh.gulimall.order.feign.CartFeignService;
import com.xwh.gulimall.order.feign.MemberFeignService;
import com.xwh.gulimall.order.feign.ProductFeignService;
import com.xwh.gulimall.order.feign.WareFeignService;
import com.xwh.gulimall.order.interceptor.LoginUserInterceptor;
import com.xwh.gulimall.order.service.OrderItemService;
import com.xwh.gulimall.order.service.OrderService;
import com.xwh.gulimall.order.service.PaymentInfoService;
import com.xwh.gulimall.order.to.OrderCreateTo;
import com.xwh.gulimall.order.vo.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
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
    private PaymentInfoService paymentInfoService;

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

    @Autowired
    private RabbitTemplate rabbitTemplate;

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

    /**
     * 选择最终一直性
     *
     * @param vo
     * @return
     */
//    @GlobalTransactional
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
        Long result = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Collections.singletonList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId()), orderToken);
        if (result == 0L) {
            // 令牌验证失败
            response.setCode(1);
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
//                    response.setCode(3);
//                    int i = 10 / 0;
                    // TODO 订单创建成功发送消息
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrder());
                    return response;
                } else {
                    response.setCode(3);
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

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        return this.getOne(new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getOrderSn, orderSn));
    }

    @Override
    public void closeOrder(OrderEntity entity) {
        // 查询当前订单的最新状态
        OrderEntity orderEntity = this.getById(entity.getId());
        if (Objects.equals(orderEntity.getStatus(), OrderStatusEnum.CREATE_NEW.getCode())) {
            // 关单
            OrderEntity update = new OrderEntity();
            update.setId(orderEntity.getId());
            update.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(update);
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderEntity, orderTo);
            try {
                // TODO 保证消息一定会发送出去。没一个消息都可以做好日志记录
                // TODO 定期扫描数据库将失败的消息再发送一遍
                rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderTo);
            } catch (Exception e) {
                // TODO 将没发送的成功的消息进行重试发送
            }
        }
    }

    /**
     * @param orderSn
     * @return
     */
    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();
        OrderEntity orderEntity = this.getOrderByOrderSn(orderSn);
        List<OrderItemEntity> list = orderItemService.list(new LambdaQueryWrapper<OrderItemEntity>().eq(OrderItemEntity::getOrderSn, orderSn));
        OrderItemEntity itemEntity = list.get(0);
        payVo.setTotal_amount(orderEntity.getPayAmount().setScale(2, RoundingMode.UP).toString());
        payVo.setOut_trade_no(orderEntity.getOrderSn());
        payVo.setSubject(itemEntity.getSkuName());
        payVo.setBody(itemEntity.getSkuAttrsVals());
        return payVo;
    }

    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {

        MemberRespVo memberRespVo = LoginUserInterceptor.threadLocal.get();
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new LambdaQueryWrapper<OrderEntity>().eq(OrderEntity::getMemberId, memberRespVo.getId())
                        .orderByDesc(OrderEntity::getId)
        );
        List<OrderEntity> collect = page.getRecords().stream().peek(order -> {
            List<OrderItemEntity> itemEntities = orderItemService.list(new LambdaQueryWrapper<OrderItemEntity>().eq(OrderItemEntity::getOrderSn, order.getOrderSn()));
            order.setItemEntities(itemEntities);
        }).collect(Collectors.toList());
        page.setRecords(collect);
        return new PageUtils(page);
    }

    /**
     * 处理支付包处理结果
     * @param vo
     * @return
     */
    @Override
    public String handleAliPayed(PayAsyncVo vo) {
        // 保存交易流水
        PaymentInfoEntity infoEntity = new PaymentInfoEntity();
        infoEntity.setAlipayTradeNo(vo.getTrade_no());
        infoEntity.setOrderSn(vo.getOut_trade_no());
        infoEntity.setPaymentStatus(vo.getTrade_status());
        infoEntity.setCallbackTime(vo.getNotify_time());
        paymentInfoService.save(infoEntity);
        if ("TRADE_SUCCESS".equals(vo.getTrade_status()) || "TRADE_FINISHED".equals(vo.getTrade_status())) {
            // 支付成功
            String outTradeNo = vo.getOut_trade_no();
            this.baseMapper.updateOrderStatus(outTradeNo,OrderStatusEnum.PAYED.getCode());
        }
        return "success";
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
        FareVo fareResp = r.getDate(new TypeReference<FareVo>() {
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

