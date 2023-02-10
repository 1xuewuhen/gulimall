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
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;


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

        String res = (String) cartOps.get(skuId.toString());
        if (StringUtils.isEmpty(res)) {
            // 购物车没有此商品
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
        } else {
            // 购物车有此商品
            Cart.CartItem cartItem = JSON.parseObject(res, Cart.CartItem.class);
            cartItem.setCount(cartItem.getCount() + num);
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
            return cartItem;
        }
    }

    /**
     * 获取购物车购物项
     *
     * @param skuId
     * @return
     */
    @Override
    public Cart.CartItem getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String res = (String) cartOps.get(skuId.toString());
        return JSON.parseObject(res, Cart.CartItem.class);
    }

    /**
     * 获取购物车
     *
     * @return
     */
    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        Cart cart = new Cart();
        if (userInfoTo.getUserId() != null) {
            // 登录
            String userKey = CART_PREFIX + userInfoTo.getUserId();
            String tempCartKey = CART_PREFIX + userInfoTo.getUserKey();
            List<Cart.CartItem> tempCartItems = getCartItems(tempCartKey);
            if (tempCartItems != null) {
                for (Cart.CartItem item : tempCartItems) {
                    addToCart(item.getSkuId(), item.getCount());
                }
                // 清除临时购物车数据
                clearCart(tempCartKey);
            }
            List<Cart.CartItem> cartItems = getCartItems(userKey);
            cart.setItems(cartItems);
        } else {
            // 没登录
            String userKey = CART_PREFIX + userInfoTo.getUserKey();
            List<Cart.CartItem> cartItems = getCartItems(userKey);
            cart.setItems(cartItems);
//            cart.setItems();
        }
        return cart;
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

    private List<Cart.CartItem> getCartItems(String cartKey) {
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(cartKey);
        List<Object> values = hashOps.values();
        if (values != null && values.size() > 0) {
            return values.stream().map(item ->
                    JSON.parseObject((String) item, Cart.CartItem.class)).collect(Collectors.toList());
        }
        return null;
    }

    /**
     * 清空购物车
     *
     * @param cartKey
     */
    @Override
    public void clearCart(String cartKey) {
        redisTemplate.delete(cartKey);
    }


    /**
     * 勾选购物项
     *
     * @param skuId
     * @param check
     */
    @Override
    public void checkItem(Long skuId, Integer check) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        Cart.CartItem cartItem = getCartItem(skuId);
        cartItem.setCheck(check == 1);
        String s = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(), s);
    }

    @Override
    public void changeItemCount(Long skuId, Integer num) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        Cart.CartItem cartItem = getCartItem(skuId);
        cartItem.setCount(num);
        String s = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(), s);
    }

    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }
}
