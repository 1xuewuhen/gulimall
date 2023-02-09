package com.xwh.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.xwh.common.utils.R;
import com.xwh.gulimall.cart.feign.ProductFeignService;
import com.xwh.gulimall.cart.interceptor.CartInterceptor;
import com.xwh.gulimall.cart.service.CartService;
import com.xwh.gulimall.cart.vo.Cart;
import com.xwh.gulimall.cart.vo.SkuInfoVo;
import com.xwh.gulimall.cart.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;


@Slf4j
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ThreadPoolExecutor executor;

    @Autowired
    private ProductFeignService productFeignService;

    private final String CART_PREFIX = "gulimall:cart:";

    @Override
    public Cart.CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        // 商品添加到购物车
        Cart.CartItem cartItem = new Cart.CartItem();
        CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
            // 远程查询当前要添加的商品信息
            R info = productFeignService.info(skuId);
            SkuInfoVo date = info.getDate("skuInfo", new TypeReference<SkuInfoVo>() {
            });

            cartItem.setCheck(true);
            cartItem.setCount(num);
            cartItem.setImage(date.getSkuDefaultImg());
            cartItem.setTitle(date.getSkuTitle());
            cartItem.setSkuId(skuId);
            cartItem.setPrice(date.getPrice());
        }, executor);
        CompletableFuture<Void> getSkuSaleAttrValues = CompletableFuture.runAsync(() -> {
            // 远程查询sku的组合信息
            R r = productFeignService.getSkuSaleAttrValues(skuId);
            List<String> values = r.getDate(new TypeReference<List<String>>() {
            });
            cartItem.setSkuAttr(values);
        }, executor);

        CompletableFuture.allOf(getSkuInfoTask, getSkuSaleAttrValues).get();
        String s = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(), s);
        return cartItem;
    }

    /**
     * 获取要操作的购物车
     *
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey = "";
        if (userInfoTo.getUserId() != null) {
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        } else {
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }
        return redisTemplate.boundHashOps(cartKey);
    }
}
