package com.atguigu.gmall.ums.mapper;

import com.atguigu.gmall.ums.entity.UserEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户表
 * 
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2021-07-13 19:15:17
 */
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {
	
}
