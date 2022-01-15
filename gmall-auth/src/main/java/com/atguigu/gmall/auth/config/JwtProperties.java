package com.atguigu.gmall.auth.config;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

@ConfigurationProperties(prefix = "jwt")
@Component
@Data
public class JwtProperties {
    private  String pubFilePath;
    private  String  priFilePath;
    private  String  secret;
    private  Integer expire;
    private  String cookieName;
    private  String unick;

    private PublicKey publicKey;
    private PrivateKey privateKey;

//    被@PostConstruct修饰的方法会在服务器加载Servlet的时候运行
    @PostConstruct
    public void init(){
        try {
//            为了判断密钥文件是否存在
            File pubFile = new File(pubFilePath);
            File priFile = new File(priFilePath);
            if(!priFile.exists()|| !pubFile.exists()){
//          根据密文，生存rsa公钥和私钥,并写入指定文件
                RsaUtils.generateKey(pubFilePath,priFilePath,secret);
            }
//            从文件中读取公钥,密钥
            this.publicKey = RsaUtils.getPublicKey(pubFilePath);
            this.privateKey=RsaUtils.getPrivateKey(priFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
