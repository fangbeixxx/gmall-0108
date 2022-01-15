package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.cart.pojo.Cart;

import java.util.List;

public interface CartService {
    void addCart(Cart cart);


    Cart queryCartBySkuId(Cart cart);

    List<Cart> queryCart();

    void updateNum(Cart cart);

    void deleteCart(Long skuId);

    List<Cart> queryCheckCartByUserId(Long userId);
}
