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
//        ????????????????????????
        UserInfo userInfo = LoginInterceptor.userInfo();
        Long userId = userInfo.getUserId();
        //??????????????????
        ResponseVo<List<UserAddressEntity>> listResponseVo = umsClient.queryAddress(userId);
        List<UserAddressEntity> userAddressEntities = listResponseVo.getData();
        if(!CollectionUtils.isEmpty(userAddressEntities)){
            confirmVo.setAddresses(userAddressEntities);
        }
//        ??????????????????
        ResponseVo<List<Cart>> cartResponseVo = cartClient.queryCheckedCartsByUserId(userId);
        List<Cart> cartList = cartResponseVo.getData();
        if(CollectionUtils.isEmpty(cartList)){
            throw  new OrderException("????????????????????????");
        }
//        ???????????????????????????orderItemVo; ??????????????????skuId???????????????sku ??????cart?????????
        List<OrderItemVo> items = cartList.stream().map(cart -> {
            OrderItemVo orderItemVo = new OrderItemVo();
//            ??????
            orderItemVo.setCount(cart.getCount());
//            ??????skuId??????sku
            ResponseVo<SkuEntity> skuEntityResponseVo = pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if(skuEntity!=null){
                orderItemVo.setSkuId(skuEntity.getId());
                orderItemVo.setWeight(skuEntity.getWeight());
                orderItemVo.setPrice(skuEntity.getPrice());
                orderItemVo.setTitle(skuEntity.getTitle());
                orderItemVo.setDefaultImage(skuEntity.getDefaultImage());
            }
            //??????????????????
            ResponseVo<List<ItemSaleVo>> salesResponseVo = smsClient.querySkuBySkuId(cart.getSkuId());
            List<ItemSaleVo> itemSaleVos = salesResponseVo.getData();

            orderItemVo.setSales(itemSaleVos);
            // ????????????
            ResponseVo<List<SkuAttrValueEntity>> skuAttrValueResponseVo = pmsClient.querySkuAttrValuesBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntities = skuAttrValueResponseVo.getData();
            orderItemVo.setSaleAttrs(skuAttrValueEntities);

            //????????????
            ResponseVo<List<WareSkuEntity>> wareResponseVo = wmsClient.queryWareSkuBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntities = wareResponseVo.getData();
            if(!CollectionUtils.isEmpty(wareSkuEntities)){
                orderItemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity ->wareSkuEntity.getStock()-wareSkuEntity.getStockLocked()>0));
            }
            return orderItemVo;
        }).collect(Collectors.toList());

        confirmVo.setItems(items);
        //???????????? ????????????id??????????????????
        ResponseVo<UserEntity> userEntityResponseVo = umsClient.queryUserById(userId);
        UserEntity userEntity = userEntityResponseVo.getData();
       if(userEntity!=null){
           confirmVo.setBounds(userEntity.getIntegration());
       }
        //?????????????????????
        String orderToken = IdWorker.getIdStr();
        redisTemplate.opsForValue().set(KEY_PREFIX+orderToken,orderToken,3, TimeUnit.HOURS);
        confirmVo.setOrderToken(orderToken);

        return confirmVo;
    }

    @Override
    public void submit(OrderSubmitVo submitVo) {
        // 1.?????????????????????orderToken ???redis?????????????????????????????????????????????????????????
        String orderToken = submitVo.getOrderToken(); // ?????????orderToken
        if (StringUtils.isBlank(orderToken)){
            throw new OrderException("?????????????????????");
        }
        String script = "if(redis.call('get', KEYS[1]) == ARGV[1]) " +
                "then " +
                "   return redis.call('del', KEYS[1]) " +
                "else " +
                "   return 0 " +
                "end";
        Boolean flag = this.redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(KEY_PREFIX + orderToken), orderToken);
        if (!flag){
            throw new OrderException("??????????????????????????????");
        }

        // 2.?????????????????????????????????  ??? ???????????????????????????
        List<OrderItemVo> items = submitVo.getItems();
        if (CollectionUtils.isEmpty(items)){
            throw new OrderException("????????????????????????!");
        }
        BigDecimal totalPrice = submitVo.getTotalPrice();// ???????????????
        BigDecimal currentTotalPrice = items.stream().map(item -> {
            // ??????????????????
            ResponseVo<SkuEntity> skuEntityResponseVo = this.pmsClient.querySkuById(item.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) { // ??????skuEntity?????????????????????0
                return new BigDecimal(0);
            }
            // ??????????????????
            return skuEntity.getPrice().multiply(item.getCount());
        }).reduce((a, b) -> a.add(b)).get();
        if (totalPrice.compareTo(currentTotalPrice) != 0){
            throw new OrderException("???????????????????????????????????????");
        }

        // 3.????????????????????????
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

        // 4.????????????
        UserInfo userInfo = LoginInterceptor.userInfo();
        Long userId = userInfo.getUserId();
        omsClient.saveOrder(submitVo,userId);

//        try {
//        } catch (Exception e) {
//            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "order.disable", orderToken);
//            throw new OrderException("?????????????????????");
//        }

        // 5.??????????????????????????????????????? ??????mq?????????
        Map<String, Object> msg = new HashMap<>();
        msg.put("userId", userId);
        List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
        msg.put("skuIds", JSON.toJSONString(skuIds));
        this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "cart.delete", msg);
    }


}
