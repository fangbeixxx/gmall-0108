package com.atguigu.gmall.wms.mapper;

import com.atguigu.gmall.wms.entity.WareEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.swagger.models.auth.In;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 仓库信息
 * 
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2021-06-23 19:15:28
 */
@Mapper
public interface WareMapper extends BaseMapper<WareEntity> {


	
}
