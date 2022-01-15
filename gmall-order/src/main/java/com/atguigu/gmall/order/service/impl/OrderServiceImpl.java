package com.atguigu.gmall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.OrderException;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.feign.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.pojo.OrderConfirmVo;
import com.atguigu.gmall.oms.vo.OrderItemVo;

import com.atguigu.gmall.order.pojo.UserInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private GmallUmsClient umsClient;
    @Autowired
    private GmallCartClient cartClient;
    @Autowired
    private GmallPmsClient pmsClient;
    @Autowired
    private GmallSmsClient smsClient;
    @Autowired
    private GmallWmsClient wmsClient;
    @Autowired
    private  GmallOmsClient  omsClient;


    private static final  String KEY_PREFIX = "order:token:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;


//    private  static final String key_prefix="order:token:";
    @Override
    public OrderConfirmVo confirm() {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
//        获取登录用户信息
        UserInfo userInfo = LoginInterceptor.userInfo();
        Long userId = userInfo.getUserId();
        //查询收获列表
        ResponseVo<List<UserAddressEntity>> listResponseVo = umsClient.queryAddress(userId);
        List<UserAddressEntity> userAddressEntities = listResponseVo.getData();
        if(!CollectionUtils.isEmpty(userAddressEntities)){
            confirmVo.setAddresses(userAddressEntities);
        }
//        查询送货清单
        ResponseVo<List<Cart>> cartResponseVo = cartClient.queryCheckedCartsByUserId(userId);
        List<Cart> cartList = cartResponseVo.getData();
        if(CollectionUtils.isEmpty(cartList)){
            throw  new OrderException("你没有购买的商品");
        }
//        将购物车集合转换未orderItemVo; 根据购物车的skuId在商品查询sku 防止cart不稳定
        List<OrderItemVo> items = cartList.stream().map(cart -> {
            OrderItemVo orderItemVo = new OrderItemVo();
//            数量
            orderItemVo.setCount(cart.getCount());
//            根据skuId查询sku
            ResponseVo<SkuEntity> skuEntityResponseVo = pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if(skuEntity!=null){
                orderItemVo.setSkuId(skuEntity.getId());
                orderItemVo.setWeight(skuEntity.getWeight());
                orderItemVo.setPrice(skuEntity.getPrice());
                orderItemVo.setTitle(skuEntity.getTitle());
                orderItemVo.setDefaultImage(skuEntity.getDefaultImage());
            }
            //查询营销信息
            ResponseVo<List<ItemSaleVo>> salesResponseVo = smsClient.querySkuBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = salesResponseVo.getData();

            orderItemVo.setSales(itemSaleVos);
            // 销售属性
            ResponseVo<List<SkuAttrValueEntity>> skuAttrValueResponseVo = pmsClient.querySkuAttrValuesBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueResponseVo.getData();
            orderItemVo.setSaleAttrs(skuAttrValueEntities);

            //是否有货
            ResponseVo<List<WareSkuEntity>> wareResponseVo = wmsClient.queryWareSkuBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if(!CollectionUtils.isEmpty(wareSkuEntities)){
                orderItemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity ->wareSkuEntity.getStock()-wareSkuEntity.getStockLocked()>0));
            }
            return orderItemVo;
        }).collect(Collectors.toList());

        confirmVo.setItems(items);
        //获取积分 根据用户id查询用户信息
        ResponseVo<UserEntity> userEntityResponseVo = umsClient.queryUserById(userId);
        UserEntity userEntity = userEntityResponseVo.getData();
       if(userEntity!=null){
           confirmVo.setBounds(userEntity.getIntegration());
       }
        //防重的唯一标识
        String orderToken = IdWorker.getIdStr();
        redisTemplate.opsForValue().set(KEY_PREFIX+orderToken,orderToken,3, TimeUnit.HOURS);
        confirmVo.setOrderToken(orderToken);

        return confirmVo;
    }

    @Override
    public void submit(OrderSubmitVo submitVo) {
        // 1.防重：页面中的orderToken 到redis中查询，查到了说明没有提交过，可以放行
        String orderToken = submitVo.getOrderToken(); // 页面的orderToken
        if (StringUtils.isBlank(orderToken)){
            throw new OrderException("非法提交。。。");
        }
        String script = "if(redis.call('get', KEYS[1]) == ARGV[1]) " +
                "then " +
                "   return redis.call('del', KEYS[1]) " +
                "else " +
                "   return 0 " +
                "end";
        Boolean flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(KEY_PREFIX + orderToken), orderToken);
        if (!flag){
            throw new OrderException("请不要重复提交。。。");
        }

        // 2.验总价：页面中的总价格  和 数据库中实时总价格
        List<OrderItemVo> items = submitVo.getItems();
        if (CollectionUtils.isEmpty(items)){
            throw new OrderException("您没有选中的商品!");
        }
        BigDecimal totalPrice = submitVo.getTotalPrice();// 页面总价格
        BigDecimal currentTotalPrice = items.stream().map(item -> {
            // 查询实时单价
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(item.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) { // 如果skuEntity为空，返回小计0
                return new BigDecimal(0);
            }
            // 计算实时小计
            return skuEntity.getPrice().multiply(item.getCount());
        }).reduce((a, b) -> a.add(b)).get();
        if (totalPrice.compareTo(currentTotalPrice) != 0){
            throw new OrderException("页面已过期，请刷新后重试！");
        }

        // 3.验库存并锁定库存
        ResponseVo<List<SkuLockVo>> wareSkuResponseVo = this.wmsClient.checkLock(items.stream().map(item -> {
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setSkuId(item.getSkuId());
            skuLockVo.setCount(item.getCount().intValue());
            return skuLockVo;
        }).collect(Collectors.toList()), orderToken);

        List<SkuLockVo> skuLockVos = wareSkuResponseVo.getData();

        if (!CollectionUtils.isEmpty(skuLockVos)){
            throw new OrderException(JSON.toJSONString(skuLockVos));
        }

        // 4.创建订单
        UserInfo userInfo = LoginInterceptor.userInfo();
        Long userId = userInfo.getUserId();
        omsClient.saveOrder(submitVo,userId);

//        try {
//        } catch (Exception e) {
//            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.disable", orderToken);
//            throw new OrderException("创建订单失败！");
//        }

        // 5.删除购物车中对应商品的记录 使用mq异步删
        Map<String, Object> msg = new HashMap<>();
        msg.put("userId", userId);
        List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
        msg.put("skuIds", JSON.toJSONString(skuIds));
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "cart.delete", msg);
    }


}
