package com.atguigu.gmall.oms.mapper;

import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 订单项信息
 * 
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2021-07-19 22:40:20
 */
@Mapper
@Repository
public interface OrderItemMapper extends BaseMapper<OrderItemEntity> {
	
}
