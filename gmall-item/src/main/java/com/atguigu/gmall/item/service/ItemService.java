package com.atguigu.gmall.item.service;

import com.atguigu.gmall.item.vo.ItemVo;

public interface ItemService {
    ItemVo loadData(Long skuId);
    public void asyncExecute(Long skuId);
}
