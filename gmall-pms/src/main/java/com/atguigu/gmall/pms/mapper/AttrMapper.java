package com.atguigu.gmall.pms.mapper;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 商品属性
 * 
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2021-06-22 17:48:41
 */
@Repository
@Mapper
public interface AttrMapper extends BaseMapper<AttrEntity> {
	
}
