package com.atguigu.gmall.item.vo;

import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.vo.GroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class ItemVo {

    //面包屑所需参数
    //一二三级分类
    private List<CategoryEntity> categoryEntities;
//    品牌信息
    private Long brandId;
    private String brandName;
//    spu相关信息
    private Long spuId;
    private String spuName;

//    中间详情信息
    private Long skuId;
    private String title;
    private String subTitle;
    private BigDecimal price;
    private Integer weight;  //    重量
    private String defaultImage;

    // 营销信息
    private List<ItemSaleVo> sales;

    //是否有货
    private  Boolean store=false;

    //sku图片列表
    private List<SkuImagesEntity> images;

    //销售属性列表
    //[{attrId:3,attrName:'颜色'，attrValues：['白天白'，‘黑夜黑’]}]
    private List<SaleAttrValueVo> saleAttrs;

    //当前sku的销售属性：{3：‘白天白’，4：‘12g’,5:'128g'}
    private Map<Long,String> saleAttr;

    //为了页面跳转，需要销售属性组合与skuid的映射关系
    private String skuJsons;

    //商品描述 spu
    private List<String> spuImages;

    //规格参数分组列表
    private List<GroupVo> groups;

}
