package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class CartAsyncService {

    @Autowired
    CartMapper cartMapper;

    @Async
    public void updateCart(String userId,Cart cart,String skuId){
//        int a=10/0;
        //  更新数据库
        cartMapper.update(cart,new QueryWrapper<Cart>().eq("user_id",userId).eq("sku_id",skuId));
    }

    @Async
    public void insertCart(String userId,Cart cart){
        // mysql存入数据库
        cartMapper.insert(cart);
    }
    //删除购物车
    @Async
    public void deleteCartByUserId(String userId) {
        cartMapper.deleteById(userId);
    }
//    删除单条商品数据
    @Async
    public void deleteCart(String userId, Long skuId) {
        cartMapper.delete(new QueryWrapper<Cart>().eq("user_id",userId).eq("sku_id",skuId));
    }
}
