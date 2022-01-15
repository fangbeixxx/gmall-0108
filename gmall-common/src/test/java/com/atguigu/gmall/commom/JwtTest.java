package com.atguigu.gmall.commom;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

//public class JwtTest {
//    // 别忘了创建D:\\project\rsa目录
//    private static final String pubKeyPath = "D:\\project\\token\\rsa.pub";
//    private static final String priKeyPath = "D:\\project\\token\\rsa.pri";
//
//    private PublicKey publicKey;
//
//    private PrivateKey privateKey;
//
//    @Test
//    public void testRsa() throws Exception {
//        RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
//    }
//
//    @BeforeEach     // 测试方法之前执行
//    public void testGetRsa() throws Exception {
//        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
//        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
//    }
//
//    @Test
//    public void testGenerateToken() throws Exception {
//        Map<String, Object> map = new HashMap<>();
//        map.put("id", "11");
//        map.put("username", "liuyan");
//        // 生成token
//        String token = JwtUtils.generateToken(map, privateKey, 5);
//        System.out.println("token = " + token);
//    }
//
//
//    @Test
//    public void testParseToken() throws Exception {
//        String token="eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE2MjYyMzk2MDV9.hLXAFLZ_sML_4zRSTOsnROYeqnQRRr1H3tMpr9r8NBxzT8zRiK458rm2DkvI33jlS6yycfqGIWwOpdu8dH4855uh6gA7z7MXpoINMeY0FJZFyixuVvmdOXk5LyjOzCn1CKJhreubrryT6Ja_F1oMsGGPOBpvvMHuunykTLNkmmr6fs5QTcnzmetUXpmBn8VHjfagxIsdOoId19P7LcATOj_BJxKZg3pUuhqxSwnKNI-kZVZ1tJreBiHuD2gCHQrTkz_KpNvCrOTd7Oam4YfhAaHJ8mSjq-sKLkWbZOaZgF08Rq9a6vWL8V9EVl6QbS5iNA_t3gl20G8CrguGEhIUEQ";
//        // 解析token
//        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
//        System.out.println("id: " + map.get("id"));
//        System.out.println("userName: " + map.get("username"));
//    }
//}
