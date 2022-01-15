package com.atguigu.gmall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.feign.GmallPmsClient;
import com.atguigu.gmall.cart.feign.GmallSmsClient;
import com.atguigu.gmall.cart.feign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.mapper.CartMapper;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.cart.service.CartAsyncService;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.CartException;
import com.atguigu.gmall.pms.entity.SkuAttrValueEntity;
import com.atguigu.gmall.pms.entity.SkuEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {
    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    CartMapper cartMapper;
    @Autowired
    GmallPmsClient pmsClient;
    @Autowired
    GmallSmsClient smsClient;
    @Autowired
    GmallWmsClient wmsClient;

    @Autowired
    CartAsyncService cartAsyncService;

    //    userId作为购物车  skuId（商品） ,value内容集合(商品具体内容)
//    购物车前缀key
    private String KEY_PREFIX = "cart:info";

    private static final String PRICE_PREFIX = "cart:price:";

    @Override
    public void addCart(Cart cart) {
        //获取用户的登录信息
        String userId = this.getUserId();
        //根据userid，userKey获取内存数据
        BoundHashOperations<String, Object, Object> boundHashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);

//        判断当前购物车是否包含此商品
        String skuId = cart.getSkuId().toString();
//        获取输入数量内容
        BigDecimal count = cart.getCount();
//      如果包含则改变数量 获取添加的数量，然后添加到数据库`
        if (boundHashOps.hasKey(skuId)) {
//            更新数量,然后存入数据库
//            1、获取购物车中的数据
            String json = boundHashOps.get(skuId).toString();
//            反序列化为购物车对象
            cart = JSON.parseObject(json, Cart.class);
            cart.setCount(cart.getCount().add(count));
//          存入数据库
//            cartMapper.update(cart,new QueryWrapper<Cart>().eq("user_id",userId).eq("sku_id",skuId));
            cartAsyncService.updateCart(userId,cart, skuId);
        } else {
//            购物车不包含 则添加先添加到数据库，然后存入购物车
//            先查找sku
            ResponseVo<SkuEntity> skuEntityResponseVo = pmsClient.querySkuById(cart.getSkuId());
            SkuEntity skuEntity = skuEntityResponseVo.getData();
            if (skuEntity == null) {
                throw new CartException("您添加的商品不存在");
            }
            cart.setDefaultImage(skuEntity.getDefaultImage());
            cart.setTitle(skuEntity.getTitle());
            cart.setPrice(skuEntity.getPrice());
            cart.setUserId(userId);
            // 获取销售属性
            ResponseVo<List<SkuAttrValueEntity>> listResponseVo = pmsClient.querySkuAttrValuesBySkuId(cart.getSkuId());
            List<SkuAttrValueEntity> skuAttrValueEntityList = listResponseVo.getData();
            cart.setSaleAttrs(JSON.toJSONString(skuAttrValueEntityList));
            //营销信息
            ResponseVo<List<ItemSaleVo>> itemResponseVo = smsClient.querySkuBySkuId(cart.getSkuId());
            List<ItemSaleVo> saleVos = itemResponseVo.getData();
            cart.setSales(JSON.toJSONString(saleVos));

            //库存判断  是否有货,无货
            ResponseVo<List<WareSkuEntity>> wareSkuBySkuId = wmsClient.queryWareSkuBySkuId(cart.getSkuId());
            List<WareSkuEntity> wareSkuEntityList = wareSkuBySkuId.getData();
            if (!CollectionUtils.isEmpty(wareSkuEntityList)) {
                cart.setStore(wareSkuEntityList.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() - wareSkuEntity.getStockLocked() > 0));
            }
            //设置状态
            cart.setCheck(true);

            // mysql存入数据库
//         cartMapper.insert(cart);
            cartAsyncService.insertCart(userId,cart);

            //实时加入价格缓存
            redisTemplate.opsForValue().set(PRICE_PREFIX+skuId,skuEntity.getPrice().toString());
        }
        //放入购物车  map数据类型会覆盖原来数据
        boundHashOps.put(skuId, JSON.toJSONString(cart));
    }


    //    记住redis中都是字符串

    @Override
    public Cart queryCartBySkuId(Cart cart) {
        //从购车中取出数据用于展示当前商品添加成功
        //获取用户id
        String userId = this.getUserId();
        BoundHashOperations<String, Object, Object> boundHashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);

        if (!boundHashOps.hasKey(cart.getSkuId().toString())) {
            throw new CartException("当前购物车没有对应用户的记录");
        }
        String cartJson = boundHashOps.get(cart.getSkuId().toString()).toString();
        return JSON.parseObject(cartJson, Cart.class);
    }
//    回显购物车
    @Override
    public List<Cart> queryCart() {
        //获取userkey
        UserInfo userInfo = LoginInterceptor.userInfo();
        String userKey = userInfo.getUserKey();

//        先获取未登录的购物车
        BoundHashOperations<String, Object, Object> boundHashOps = redisTemplate.boundHashOps(KEY_PREFIX + userKey);
//        直接获取值 就是购物车内容
        List<Object> unLoginCartsJsons = boundHashOps.values();
        List<Cart> unLoginCarts = null;

        if (!CollectionUtils.isEmpty(unLoginCartsJsons)) {
//            遍历数据 将json字符串集合转换为购物车对象集合
            unLoginCarts = unLoginCartsJsons.stream().map(o -> {
                Cart cart = JSON.parseObject(o.toString(), Cart.class);
//               设置实时价格
                cart.setCurrentPrice(new BigDecimal(this.redisTemplate.opsForValue().get(PRICE_PREFIX+cart.getSkuId())));
                return cart;
            }).collect(Collectors.toList());
        }

//        获取登录的userId,如果为空则直接返回 未登录的结果
        Long userId = userInfo.getUserId();
        if (userId == null) {
            return unLoginCarts;
        }

        // 4.合并未登录的购物车 到 登录状态的购物车
//        获取登录账号的购物车
        BoundHashOperations<String, Object, Object> loginHashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);
        if (!CollectionUtils.isEmpty(unLoginCarts)) {
//            遍历集合
            unLoginCarts.forEach(cart -> {
                String skuId = cart.getSkuId().toString();

                BigDecimal count = cart.getCount();
//                如果存在则修改数量 未登录数量+登录数量 存入数据库
                if (loginHashOps.hasKey(skuId)) {
                    //用skuid找到登录购物车数据
                    String s = loginHashOps.get(skuId).toString();
//                   反序列化
                    cart = JSON.parseObject(s, Cart.class);
//                    加上未登录数量存入数据库
                    cart.setCount(cart.getCount().add(count));
//                    写入数据库 更新
                    cartAsyncService.updateCart(skuId,cart, skuId);
                } else {
//                    更改下userId存入数据 不存在未登录的数据
                    cart.setUserId(userId.toString());
                    cartAsyncService.insertCart(skuId,cart);
                }
                // 将数据放到购物车
                loginHashOps.put(skuId, JSON.toJSONString(cart));

            });
//                删除未登录的购物车
//                数据库也要删除
            redisTemplate.delete(KEY_PREFIX + userKey);
            cartAsyncService.deleteCartByUserId(userKey);
        }
//      查询合并后的购物车 并给到页面购物车显示
        List<Object> loginCartsJsons = loginHashOps.values();

        if (!CollectionUtils.isEmpty(loginCartsJsons)) {
            return loginCartsJsons.stream().map(o -> {
                Cart cart = JSON.parseObject(o.toString(), Cart.class);
//                实时价格
                cart.setCurrentPrice(new BigDecimal(this.redisTemplate.opsForValue().get(PRICE_PREFIX+cart.getSkuId())));
                return cart;
            }).collect(Collectors.toList());
        }

        return null;
    }

    @Override
    public void updateNum(Cart cart) {
        String userId = getUserId();


        BoundHashOperations<String, Object, Object> boundHashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);
        if(!boundHashOps.hasKey(cart.getSkuId().toString())){
            throw  new  CartException("你没有对应的购物车记录");
        }

        //获取更改数量
        BigDecimal count = cart.getCount();

        String s = boundHashOps.get(cart.getSkuId().toString()).toString();
        cart = JSON.parseObject(s, Cart.class);
        cart.setCount(count);

        //存入缓存
        boundHashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
        //存入数据库
        cartAsyncService.updateCart(userId,cart,cart.getSkuId().toString());
    }

    @Override
    public void deleteCart(Long skuId) {
//        获取用户id
        String userId = getUserId();
        BoundHashOperations<String, Object, Object> boundHashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);
        //删除购物车数据
        boundHashOps.delete(skuId.toString());
        //删除数据库单条数据
        cartAsyncService.deleteCart(userId,skuId);
    }

    //获取用户id
    public String getUserId() {
        UserInfo userInfo = LoginInterceptor.userInfo();
        String userId = userInfo.getUserKey();
        if (userInfo.getUserId() != null) {
            userId = userInfo.getUserId().toString();
        }
        return userId;
    }

    @Override
    public List<Cart> queryCheckCartByUserId(Long userId) {
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(KEY_PREFIX + userId);
        List<Object> values = hashOps.values();
        if(CollectionUtils.isEmpty(values)){
            throw new CartException("你没有购物车记录");
        }
        return values.stream().map(cartJson->JSON.parseObject(cartJson.toString(),Cart.class))
                .filter(Cart::getCheck).collect(Collectors.toList());

    }
}
