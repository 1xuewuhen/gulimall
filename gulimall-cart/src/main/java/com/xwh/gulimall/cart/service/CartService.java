package com.xwh.gulimall.cart.service;

import com.xwh.gulimall.cart.vo.Cart;

import java.util.concurrent.ExecutionException;

public interface CartService {
    Cart.CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;
}
