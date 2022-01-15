package com.atguigu.gmall.pms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuMapper;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.service.SkuAttrValueService;
import org.springframework.util.CollectionUtils;


@Service("skuAttrValueService")
public class SkuAttrValueServiceImpl extends ServiceImpl<SkuAttrValueMapper, SkuAttrValueEntity> implements SkuAttrValueService {
    @Autowired
    AttrMapper attrMapper;
    @Autowired
    SkuMapper skuMapper;
    @Autowired
    SkuAttrValueMapper skuAttrValueMapper;
    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuAttrValueEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuAttrValueEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<SkuAttrValueEntity>   searchQuerySkuAttrValueById(Long cid, Long skuId) {
        // 1.查询检索类型的规格参数
        List<AttrEntity> attrEntities = this.attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("category_id", cid).eq("search_type", 1));
        if (CollectionUtils.isEmpty(attrEntities)){
            return null;
        }

        // 2.查询检索类型的规格参数和值
        List<Long> attrIds = attrEntities.stream().map(AttrEntity::getId).collect(Collectors.toList());
        return this.list(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuId).in("attr_id", attrIds));

    }


    @Override
    public List<SaleAttrValueVo> querySaleAttrValuesBySpuId(Long spuId) {
//        根据spuId查找所有sku
        List<SkuEntity> skuEntities = skuMapper.selectList(new QueryWrapper<SkuEntity>().eq("spu_id", spuId));
        if(CollectionUtils.isEmpty(skuEntities)){
            return null;
        }
        // 查询所有sku 的销售属性
        List<Long> skuIds = skuEntities.stream().map(SkuEntity::getId).collect(Collectors.toList());
        List<SkuAttrValueEntity> attrValueEntities = this.list(new QueryWrapper<SkuAttrValueEntity>().in("sku_id", skuIds).orderByAsc("sku_id"));
        if(CollectionUtils.isEmpty(attrValueEntities)){
            return null;
        }
//        将集合转换为已attrid为key,销售属性作为v
        ArrayList<SaleAttrValueVo> saleAttrValueVos = new ArrayList<>();
        Map<Long, List<SkuAttrValueEntity>> map = attrValueEntities.stream().collect(Collectors.groupingBy(SkuAttrValueEntity::getAttrId));
        //遍历map集合 把map转换为kv结构的saleAttrValueVo模型
        map.forEach((attrId,skuAttrValueEntityList)->{
            SaleAttrValueVo saleAttrValueVo = new SaleAttrValueVo();
//            设置属性
            saleAttrValueVo.setAttrId(attrId);
//            因为是kv结构 至少有一个数据
            saleAttrValueVo.setAttrName(skuAttrValueEntityList.get(0).getAttrName());
            //将销售集合转换为set属性集合 因为list可能有重复
            saleAttrValueVo.setAttrValues(skuAttrValueEntityList.stream().map(SkuAttrValueEntity::getAttrValue).collect(Collectors.toSet()));
//            存入属性模型集合
            saleAttrValueVos.add(saleAttrValueVo);
        });
        return saleAttrValueVos;
    }

//    根据spuId所有销售属性组合和skuid的映射关系 使用自定义映射
    @Override
    public String queryMappingBySpuId(Long spuId) {
//        查询spu下所有的sku
        List<SkuEntity> skuEntities = skuMapper.selectList(new QueryWrapper<SkuEntity>().eq("spu_id", spuId));
        //获取所有skuid 通过stream流
        List<Long> skuIds = skuEntities.stream().map(SkuEntity::getId).collect(Collectors.toList());
//        查询映射关系 v,v -id
        List<Map<String,Object>> mapList = skuAttrValueMapper.queryMappingBySpuId(skuIds);
        if(CollectionUtils.isEmpty(mapList)){
            return null;
        }
//        将返回的map转换为数据类型
        Map<String, Long> collect = mapList.stream().collect(Collectors.toMap(map -> map.get("attr_values").toString(), map -> (Long) map.get("sku_id")));

        return JSON.toJSONString(collect);
    }

}