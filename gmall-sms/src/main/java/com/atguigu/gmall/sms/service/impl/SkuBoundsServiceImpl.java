package com.atguigu.gmall.sms.service.impl;

import com.atguigu.gmall.sms.entity.SkuFullReductionEntity;
import com.atguigu.gmall.sms.entity.SkuLadderEntity;
import com.atguigu.gmall.sms.mapper.SkuFullReductionMapper;
import com.atguigu.gmall.sms.mapper.SkuLadderMapper;
import com.atguigu.gmall.sms.service.SkuFullReductionService;
import com.atguigu.gmall.sms.service.SkuLadderService;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.sms.mapper.SkuBoundsMapper;
import com.atguigu.gmall.sms.entity.SkuBoundsEntity;
import com.atguigu.gmall.sms.service.SkuBoundsService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;


@Service("skuBoundsService")
public class SkuBoundsServiceImpl extends ServiceImpl<SkuBoundsMapper, SkuBoundsEntity> implements SkuBoundsService {

    @Autowired
    SkuFullReductionMapper skuFullReductionMapper;
    @Autowired
    SkuLadderMapper skuLadderMapper;
    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<SkuBoundsEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<SkuBoundsEntity>()
        );

        return new PageResultVo(page);
    }

    @Transactional
    @Override
    public void saveSales(SkuSaleVo skuBounds) {
//        存储积分优惠
        SkuBoundsEntity skuBoundsEntity = new SkuBoundsEntity();
        BeanUtils.copyProperties(skuBounds,skuBoundsEntity);
        List<Integer> work = skuBounds.getWork();
        if(!CollectionUtils.isEmpty(work) || work.size()==4){
            skuBoundsEntity.setWork(work.get(3)*8+work.get(2)*4+work.get(1)*2+work.get(0));

        }
        this.save(skuBoundsEntity);
//        价格满减优惠
        SkuFullReductionEntity skuFullReductionEntity = new SkuFullReductionEntity();
        BeanUtils.copyProperties(skuBounds,skuFullReductionEntity);
        skuFullReductionEntity.setAddOther(skuBounds.getFullAddOther());
        this.skuFullReductionMapper.insert(skuFullReductionEntity);
//         件数满减优惠
        SkuLadderEntity skuLadderEntity = new SkuLadderEntity();
        BeanUtils.copyProperties(skuBounds,skuLadderEntity);
        skuLadderEntity.setAddOther(skuBounds.getLadderAddOther());
        this.skuLadderMapper.insert(skuLadderEntity);
    }

    @Override
    public List<ItemSaleVo> querySkuBySkuId(Long skuId) {
        ArrayList<ItemSaleVo> itemSaleVos = new ArrayList<>();
//        查询积分优惠
        SkuBoundsEntity boundsEntity = this.getOne(new QueryWrapper<SkuBoundsEntity>().eq("sku_id", skuId));
        if(boundsEntity!=null){
            ItemSaleVo itemSaleVo1 = new ItemSaleVo();
            itemSaleVo1.setType("积分优惠");
            itemSaleVo1.setDesc("送"+boundsEntity.getGrowBounds()+"成长积分,送"+boundsEntity.getBuyBounds()+"购物积分");
            itemSaleVos.add(itemSaleVo1);
        }
        //查询减
        SkuFullReductionEntity skuFullReductionEntity = skuFullReductionMapper.selectOne(new QueryWrapper<SkuFullReductionEntity>().eq("sku_id", skuId));
        if(skuFullReductionEntity!=null){
            ItemSaleVo itemSaleVo2 = new ItemSaleVo();
            itemSaleVo2.setType("满减优惠");
            itemSaleVo2.setDesc("满"+skuFullReductionEntity.getFullPrice()+"钱，减"+skuFullReductionEntity.getReducePrice()+"钱");
            itemSaleVos.add(itemSaleVo2);
        }
        //查询打折
        SkuLadderEntity ladderEntity = skuLadderMapper.selectOne(new QueryWrapper<SkuLadderEntity>().eq("sku_id", skuId));
        if(ladderEntity!=null){
            ItemSaleVo itemSaleVo3 = new ItemSaleVo();
            itemSaleVo3.setType("折扣优惠");
            //这里需要转换 折扣数据/10
            itemSaleVo3.setDesc("买"+ladderEntity.getFullCount()+"件,打"+ladderEntity.getDiscount().divide(new BigDecimal(10))+"折");
            itemSaleVos.add(itemSaleVo3);
        }
        return itemSaleVos;
    }

}