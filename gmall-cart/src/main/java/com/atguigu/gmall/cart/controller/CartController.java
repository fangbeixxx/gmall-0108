package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class CartController {

    @Autowired
    CartService cartService;
//    用于添加购物车并跳转到成功页面
    @GetMapping
    public String  addCart(Cart cart){
        cartService.addCart(cart);
        return "redirect:http://cart.gmall.com/addcart.html?skuId="+cart.getSkuId()+"&count="+cart.getCount();
    }
    //获取数据并响应给页面
    @GetMapping("addcart.html")
    public String queryCartBySkuId(Cart cart, Model model){
        BigDecimal count = cart.getCount();
        cart=cartService.queryCartBySkuId(cart);
//        要显示页面数量,所以这里改变
        cart.setCount(count);
        model.addAttribute("cart",cart);
        return "addCart";
    }

    //查询购物车
    @GetMapping("cart.html")
    public String queryCart(Model model){
        List<Cart> cartList=cartService.queryCart();
        model.addAttribute("carts",cartList);
        return "cart";
    }
    //更新数量 购物车中
    @PostMapping("updateNum")
    @ResponseBody
    public ResponseVo updateNum(@RequestBody Cart cart){
        cartService.updateNum(cart);
        return ResponseVo.ok();
    }
    //删除购物商品
        @PostMapping("deleteCart")
        @ResponseBody
        public ResponseVo deleteCart(@RequestParam("skuId")Long skuId){
            cartService.deleteCart(skuId);
            return  ResponseVo.ok();
    }

    @GetMapping("user/{userId}")
    @ResponseBody
    public ResponseVo<List<Cart>> queryCheckedCartsByUserId(@PathVariable("userId")Long userId){
        List<Cart> carts=cartService.queryCheckCartByUserId(userId);
        return  ResponseVo.ok(carts);
    }
    @GetMapping("test")
    @ResponseBody
    public String test(){
        UserInfo userInfo = LoginInterceptor.userInfo();
        System.out.println("userKey="+userInfo.getUserKey()+":userId="+userInfo.getUserId());
        return "测试用例";
    }
}
