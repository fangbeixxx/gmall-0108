package com.atguigu.gmall.auth.service.impl;

import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.feign.UmsFeignClient;
import com.atguigu.gmall.auth.service.AuthService;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.exception.AuthException;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.IpUtil;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;

@Service
@EnableConfigurationProperties(JwtProperties.class)
public class AuthServiceImpl implements AuthService {
    @Autowired
    UmsFeignClient umsFeignClient;
    @Autowired
    JwtProperties jwtProperties;
    @Override
    public void login(String loginName, String password, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // 1. 调用ums接口校验登录名和密码（查询）
        ResponseVo<UserEntity> userEntityResponseVo = umsFeignClient.query(loginName, password);
        UserEntity userEntity = userEntityResponseVo.getData();
        // 2.判断用户信息是否为空
        if (userEntity==null){
            throw new AuthException("登录名或者密码错误！请重新输入");
        }
        //3.组装载荷
        HashMap<String, Object> map = new HashMap<>();
        map.put("userId",userEntity.getId());
        map.put("username",userEntity.getUsername());
        // 4.防止jwt的盗用，加入登录用户的ip地址
        String ip = IpUtil.getIpAddressAtService(request);
        map.put("ip",ip);

        // 5.制作jwt
        String s = JwtUtils.generateToken(map, jwtProperties.getPrivateKey(), jwtProperties.getExpire());
        // 6.把jwt类型的token放入cookie中
        CookieUtils.setCookie(request,response,jwtProperties.getCookieName(),s,jwtProperties.getExpire()*60);
        // 7.把昵称放入cookie
        CookieUtils.setCookie(request,response,jwtProperties.getUnick(),userEntity.getNickname(),jwtProperties.getExpire()*60);

    }
}
