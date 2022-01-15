package com.atguigu.gmall.pms.api;

import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.GroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GmallPmsApi {
//    1.分页查询spu
    @PostMapping("pms/spu/page")
    public ResponseVo<List<SpuEntity>> jquerySpuByPage(@RequestBody PageParamVo paramVo);
//    根据spuId查询spu的信息
    @GetMapping("pms/spu/{id}")
    public ResponseVo<SpuEntity> querySpuById(@PathVariable("id") Long id);
    //根据spuId查询spu描述信息
    @GetMapping("pms/spudesc/{spuId}")
    public ResponseVo<SpuDescEntity> querySpuDescById(@PathVariable("spuId") Long spuId);
//2.根据spuId查询sku列表
    @GetMapping("pms/sku/spu/{spuId}")
    public ResponseVo<List<SkuEntity>> queryGroupAndSkuById(@PathVariable("spuId")Long spuId);
    //根据skuid查询sku
    @GetMapping("pms/sku/{id}")
    public ResponseVo<SkuEntity> querySkuById(@PathVariable("id") Long id);
    //根据skuid查询图片列表
    @GetMapping("pms/skuimages/sku/{skuId}")
    public ResponseVo<List<SkuImagesEntity>> querySkuImageBySkuId(@PathVariable("skuId")Long skuId);
//    4.根据品牌id查询品牌
    @GetMapping("pms/brand/{id}")
    public ResponseVo<BrandEntity> queryBrandById(@PathVariable("id") Long id);
//    5.根据分类id查询分类
    @GetMapping("pms/category/{id}")
    public ResponseVo<CategoryEntity> queryCategoryById(@PathVariable("id") Long id);
//    根据父分类id查询分类
    @GetMapping("pms/category/parent/{parentId}")
    public ResponseVo<List<CategoryEntity>> queryCategoriesByPid(@PathVariable("parentId") Long parentId);
//   根据父id查找二级分类和三级分类
    @GetMapping("pms/category/subs/{pid}")
    public ResponseVo<List<CategoryEntity>> queryLvl2WithSubsById(@PathVariable("pid")Long pid);
    //根据三级分类id查询一二三级分类
    @GetMapping("pms/category/subThree/{cid}")
    public ResponseVo<List<CategoryEntity>>  querylv123WithSubsById(@PathVariable("cid")Long cid);
//   6.根据categoryId和skuId查询销售类型的搜索类型的规格参数和值
    @GetMapping("pms/skuattrvalue/search/{cid}")
    public ResponseVo<List<SkuAttrValueEntity>> searchQuerySkuAttrValueById(@PathVariable("cid") Long cid,
                                                                            @RequestParam("skuId") Long skuId);
    //根据spuId查询spu下所有销售属性的可取值
    @GetMapping("pms/skuattrvalue/spu/{spuId}")
    public ResponseVo<List<SaleAttrValueVo>> querySaleAttrValuesBySpuId(@PathVariable("spuId") Long spuId);

//    根据skuid查询当前sku的销售属性 后期转换为keyvalue结构
    @GetMapping("pms/skuattrvalue/sku/{skuId}")
    public ResponseVo<List<SkuAttrValueEntity>> querySkuAttrValuesBySkuId(@PathVariable("skuId")Long skuId);

    //根据spuId所有属性的映射关系
    @GetMapping("pms/skuattrvalue/map/{spuId}")
    public ResponseVo<String> queryMappingBySpuId(@PathVariable("spuId")Long spuId);

    //根据分类id,spuid,skuid查询所有的规格参数组及组
    @GetMapping("pms/attrgroup//with/attr/value/{cid}")
    public ResponseVo<List<GroupVo>> queryGroupsWithAttrValuesByCidAndSpuIdAndSkuId(@PathVariable("cid")Long cid,
                                                                                    @RequestParam("spuId")Long spuId,
                                                                                    @RequestParam("skuId")Long skuId);
//    7.根据categoryId和spuId查询基本类型的搜索类型的规格参数和值
    @GetMapping("pms/spuattrvalue/search/{cid}")
    public ResponseVo<List<SpuAttrValueEntity>> searchQuerySpuAttrValueById(@PathVariable("cid") Long cid,
                                                                            @RequestParam("spuId") Long spuId);
}
