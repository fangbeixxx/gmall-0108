package com.atguigu.gmall.order.service;

import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.pojo.OrderConfirmVo;

public interface OrderService {
    OrderConfirmVo confirm();

    void submit(OrderSubmitVo submitVo);
}
