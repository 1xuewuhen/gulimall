package com.xwh.gulimall.cart.service;

import com.xwh.gulimall.cart.vo.Cart;

import java.util.concurrent.ExecutionException;

public interface CartService {
    Cart.CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    Cart.CartItem getCartItem(Long skuId);

    Cart getCart() throws ExecutionException, InterruptedException;

    void clearCart(String cartKey);

    void checkItem(Long skuId, Integer check);

    void changeItemCount(Long skuId, Integer num);

    void deleteItem(Long skuId);
}
