package com.atguigu.gmall.order.pojo;

import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import lombok.Data;

import java.util.List;

@Data
public class OrderConfirmVo {
//    地址集合
    private List<UserAddressEntity> addresses;
//    商品列表
    private List<OrderItemVo> items;
//    购物积分
    private Integer bounds;

    //防重的唯一标识  页面一份，redis有一份
    private String orderToken;
}
