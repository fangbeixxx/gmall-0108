package com.atguigu.gmall.sms.mapper;

import com.atguigu.gmall.sms.entity.SkuLadderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 商品阶梯价格
 * 
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2021-06-22 18:22:40
 */
@Repository
@Mapper
public interface SkuLadderMapper extends BaseMapper<SkuLadderEntity> {
	
}
