package com.atguigu.gmall.item.service.impl;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.ItemException;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.GroupVo;
import com.atguigu.gmall.pms.vo.SaleAttrValueVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {
    @Autowired
    GmallPmsClient pmsClient;

    @Autowired
    GmallSmsClient smsClient;

    @Autowired
    GmallWmsClient wmsClient;

    @Autowired
    ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private TemplateEngine templateEngine;
    @Override
    public ItemVo loadData(Long skuId) {
        ItemVo itemVo = new ItemVo();

        // 1、根据skuid查询sku
        CompletableFuture<SkuEntity> skuCompletableFuture = CompletableFuture.supplyAsync(() -> {
            ResponseVo<SkuEntity> skuEntityResponseVo = pmsClient.querySkuById(skuId);
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                throw new ItemException("该skuId对应的商品不存在！");
            }
            //    中间详情信息
            itemVo.setSkuId(skuEntity.getId());
            itemVo.setTitle(skuEntity.getTitle());
            itemVo.setSubTitle(skuEntity.getSubtitle());
            itemVo.setPrice(skuEntity.getPrice());
            itemVo.setWeight(skuEntity.getWeight());
            itemVo.setDefaultImage(skuEntity.getDefaultImage());
            return skuEntity;
        });

        // 2、一二三级分类
        CompletableFuture<Void> categoryCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {

            ResponseVo<List<CategoryEntity>> listResponseVo = pmsClient.querylv123WithSubsById(skuEntity.getCategoryId());
            List<CategoryEntity> categoryEntities = listResponseVo.getData();
            itemVo.setCategoryEntities(categoryEntities);
        },threadPoolExecutor);

//        3.根据品牌id查询品牌
        CompletableFuture<Void> brandCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {

            ResponseVo<BrandEntity> brandEntityResponseVo = pmsClient.queryBrandById(skuEntity.getBrandId());
            BrandEntity brandEntity = brandEntityResponseVo.getData();
            if(brandEntity!=null) {
                itemVo.setBrandId(brandEntity.getId());
                itemVo.setBrandName(brandEntity.getName());
            }
        },threadPoolExecutor);

//        4.根据spuId查询SPU
        CompletableFuture<Void> spuEntityCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<SpuEntity> spuEntityResponseVo = pmsClient.querySpuById(skuEntity.getSpuId());
            SpuEntity spuEntity = spuEntityResponseVo.getData();
            if(skuEntity!=null) {
                itemVo.setSpuId(spuEntity.getId());
                itemVo.setSpuName(spuEntity.getName());
            }
        },threadPoolExecutor);

//        5.根据skuId查询营销信息
        CompletableFuture<Void> saleCompletableFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<ItemSaleVo>> saleResponseVo = smsClient.querySkuBySkuId(skuId);
            List<ItemSaleVo> itemSaleVos = saleResponseVo.getData();
            itemVo.setSales(itemSaleVos);
        }, threadPoolExecutor);

//        6.根据skuId查询库存列表
        CompletableFuture<Void> wareCompletableFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<WareSkuEntity>> wareSkuBySkuId = wmsClient.queryWareSkuBySkuId(skuId);
            List<WareSkuEntity> wareSkuEntities = wareSkuBySkuId.getData();
            if(!CollectionUtils.isEmpty(wareSkuEntities)){
                itemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock()-wareSkuEntity.getStockLocked()>0));
            }
        }, threadPoolExecutor);

//        7.根据skuId查询sku的图片列表
        CompletableFuture<Void> skuImageCompletableFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<SkuImagesEntity>> skuImageResponseVo1 = pmsClient.querySkuImageBySkuId(skuId);
            List<SkuImagesEntity> skuImagesEntityList = skuImageResponseVo1.getData();
            if(!CollectionUtils.isEmpty(skuImagesEntityList)){
                itemVo.setImages(skuImagesEntityList);
            }
        }, threadPoolExecutor);

//        8.根据spuId查询spu下所有销售属性的可取值
        CompletableFuture<Void> saleAttrCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<SaleAttrValueVo>> saleAttrResponseVo1 = pmsClient.querySaleAttrValuesBySpuId(skuEntity.getSpuId());
            List<SaleAttrValueVo> saleAttrValueVos = saleAttrResponseVo1.getData();
            if(!CollectionUtils.isEmpty(saleAttrValueVos)){
                itemVo.setSaleAttrs(saleAttrValueVos);
            }
        }, threadPoolExecutor);

//        9.根据skuId查询当前sku的销售属性
        CompletableFuture<Void> skuAttrCompletableFuture = CompletableFuture.runAsync(() -> {
            ResponseVo<List<SkuAttrValueEntity>> skuAttrValuesBySkuId = pmsClient.querySkuAttrValuesBySkuId(skuId);
            List<SkuAttrValueEntity> skuAttrValuesBySkuIdData = skuAttrValuesBySkuId.getData();
            if(!CollectionUtils.isEmpty(skuAttrValuesBySkuIdData)){
                //转换为kv结构 key-value
                Map<Long, String> map = skuAttrValuesBySkuIdData.stream().collect(Collectors.toMap(SkuAttrValueEntity::getAttrId, SkuAttrValueEntity::getAttrValue));
                itemVo.setSaleAttr(map);
            }
        }, threadPoolExecutor);

//        10.根据spuId所有销售属性组合和skuId的映射关系
        CompletableFuture<Void> stringCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<String> stringResponseVo = pmsClient.queryMappingBySpuId(skuEntity.getSpuId());
            String json = stringResponseVo.getData();
            itemVo.setSkuJsons(json);
        }, threadPoolExecutor);


//        11.根据spuId查询spu的描述信息
        CompletableFuture<Void> spuDescCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<SpuDescEntity> spuDescEntityResponseVo = pmsClient.querySpuDescById(skuEntity.getSpuId());
            SpuDescEntity spuDescEntity = spuDescEntityResponseVo.getData();
            if(spuDescEntity!=null){
                System.out.println(Arrays.asList(StringUtils.split(spuDescEntity.getDecript(), ",")));

                itemVo.setSpuImages(Arrays.asList(StringUtils.split(spuDescEntity.getDecript(), ",")));
            }
        }, threadPoolExecutor);

//        12.根据分类id、spuId、skuId查询出所有的规格参数组及组下的规格参数和值
        CompletableFuture<Void> queryGroupsWithAttrValuesCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuEntity -> {
            ResponseVo<List<GroupVo>> responseVo = pmsClient.queryGroupsWithAttrValuesByCidAndSpuIdAndSkuId(skuEntity.getCategoryId(), skuEntity.getSpuId(), skuId);
            List<GroupVo> groupVoList = responseVo.getData();
            if(!CollectionUtils.isEmpty(groupVoList)){
                itemVo.setGroups(groupVoList);
            }
        }, threadPoolExecutor);
//        多任务组合
        CompletableFuture.allOf(categoryCompletableFuture,brandCompletableFuture,spuEntityCompletableFuture,saleCompletableFuture,
                wareCompletableFuture,skuImageCompletableFuture,saleAttrCompletableFuture,skuAttrCompletableFuture,stringCompletableFuture,
                spuDescCompletableFuture,queryGroupsWithAttrValuesCompletableFuture).join();

        return itemVo;
    }

//    页面静态化
    public void createHtml(Long skuId){
        ItemVo itemVo = this.loadData(skuId);
        // 上下文对象的初始化
        Context context = new Context();
        // 页面静态化所需要的数据模型
        context.setVariable("itemVo", itemVo);

        // 初始化文件流，输出静态页面到硬盘的某个目录下。注意需要提前创建该html目录
        try (PrintWriter printWriter = new PrintWriter(new File("D:\\project\\html\\" + skuId + ".html"))) {
            //  通过thymeleaf提供的模板引擎进行模板的静态化
            // 1-模板的视图名称 2-thymeleaf的上下文对象 3-文件流
            templateEngine.process("item", context, printWriter);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

//    异步生成静态页面
    public void asyncExecute(Long skuId){
        threadPoolExecutor.execute(() -> createHtml(skuId));
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
//        CompletableFuture<Object> completableFuture = new CompletableFuture<>();
//        CompletableFuture completableFuture=CompletableFuture.runAsync(() -> {
//            System.out.println("执行子线程");
////            return "hello completableFuture"; runAsync不支持返回值
//        });
//
//        System.out.println("主线程main---------------");
        CompletableFuture completableFuture=CompletableFuture.supplyAsync(() -> {
            System.out.println("异步任务----------"+Thread.currentThread().getName());
            System.out.println("使用supplyAsync初始化");
            return "hello CompletableFuture";
        }).whenCompleteAsync((t,u)->{
            System.out.println("任务--------------"+Thread.currentThread().getName());
            System.out.println("=---------------whenComplete----------");
            System.out.println(t);  //返回结果集
           // System.out.println(u);  //异常
        });


       // completableFuture.get();    //get方法会阻塞线程
        System.out.println("main线程------------------"+Thread.currentThread().getName());
    }
}
