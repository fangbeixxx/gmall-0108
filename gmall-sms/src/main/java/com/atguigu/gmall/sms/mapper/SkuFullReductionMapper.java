package com.atguigu.gmall.sms.mapper;

import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 商品满减信息
 * 
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2021-06-22 18:22:40
 */
@Repository
@Mapper
public interface SkuFullReductionMapper extends BaseMapper<SkuFullReductionEntity> {
	
}
