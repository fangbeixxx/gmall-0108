package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SpuAttrValueEntity;
import com.atguigu.gmall.pms.mapper.AttrMapper;
import com.atguigu.gmall.pms.mapper.SkuAttrValueMapper;
import com.atguigu.gmall.pms.mapper.SpuAttrValueMapper;
import com.atguigu.gmall.pms.vo.AttrValueVo;
import com.atguigu.gmall.pms.vo.GroupVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.pms.mapper.AttrGroupMapper;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupMapper, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    AttrMapper attrMapper;
    @Autowired
    SpuAttrValueMapper spuAttrValueMapper;

    @Autowired
    SkuAttrValueMapper skuAttrValueMapper;
    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<AttrGroupEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public List<AttrGroupEntity> queryCategoriesWithById(Long catId) {
        LambdaQueryWrapper<AttrGroupEntity> queryWrapper=new LambdaQueryWrapper<>();
        queryWrapper.eq(AttrGroupEntity::getCategoryId,catId);
        List<AttrGroupEntity> attrGroupEntityList = list(queryWrapper);
        if(CollectionUtils.isEmpty(attrGroupEntityList)){
            return null;
        }
        for (AttrGroupEntity attrGroupEntity : attrGroupEntityList) {
            List<AttrEntity> attrEntities = attrMapper.selectList(new LambdaQueryWrapper<AttrEntity>().eq(AttrEntity::getGroupId, attrGroupEntity.getId()).eq(AttrEntity::getType,1));
            attrGroupEntity.setAttrEntities(attrEntities);
        }
        return attrGroupEntityList;
    }

    @Override
    public List<GroupVo> queryGroupsWithAttrValuesByCidAndSpuIdAndSkuId(Long cid, Long spuId, Long skuId) {
//        根据cid找到规格参数组
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("category_id", cid));
         if(CollectionUtils.isEmpty(attrGroupEntities)){
             return null;
         }
        // 遍历分组查询组下的规格参数
        return  attrGroupEntities.stream().map(attrGroupEntity -> {
            GroupVo groupVo = new GroupVo();
            groupVo.setName(attrGroupEntity.getName());
            // 查询组下的规格参数
            List<AttrEntity> attrEntities = attrMapper.selectList(new QueryWrapper<AttrEntity>().eq("group_id", attrGroupEntity.getId()));
            if(!CollectionUtils.isEmpty(attrEntities)){
                List<AttrValueVo> attrValueVoList = attrEntities.stream().map(attrEntity -> {
                    AttrValueVo attrValueVo = new AttrValueVo();

                    attrValueVo.setAttrId(attrEntity.getId());
                    attrValueVo.setAttrName(attrEntity.getName());
//                    销售参数
                    if(attrEntity.getType()==1){
                        SpuAttrValueEntity spuAttrValueEntity = spuAttrValueMapper.selectOne(new QueryWrapper<SpuAttrValueEntity>().eq("spu_id", spuId).eq("attr_id", attrEntity.getId()));
                        if(spuAttrValueEntity!=null){
                            attrValueVo.setAttrValue(spuAttrValueEntity.getAttrValue());
                        }
                    }else{
                        SkuAttrValueEntity skuAttrValueEntity = skuAttrValueMapper.selectOne(new QueryWrapper<SkuAttrValueEntity>().eq("sku_id", skuId).eq("attr_id",attrEntity.getId()));
                        if(skuAttrValueEntity!=null){
                            attrValueVo.setAttrValue(skuAttrValueEntity.getAttrValue());
                        }
                    }
                    return attrValueVo;
                }).collect(Collectors.toList());
                groupVo.setAttrValues(attrValueVoList);
            }
            return groupVo;
        }).collect(Collectors.toList());
    }

}