package com.atguigu.gmall.wms.mapper;

import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * εεεΊε­
 * 
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2021-06-23 19:15:28
 */
@Mapper
@Repository
public interface WareSkuMapper extends BaseMapper<WareSkuEntity> {

    public List<WareSkuEntity> check(@Param("skuId") Long skuId,@Param("count") Integer count);

    public int lock(@Param("id") Long id,@Param("count") Integer count);

    void unlock(Long wareSkuId, Integer count);
}
