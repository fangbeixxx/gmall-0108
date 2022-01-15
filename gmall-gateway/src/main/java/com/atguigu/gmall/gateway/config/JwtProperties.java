package com.atguigu.gmall.gateway.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.security.PublicKey;

@Component
@Data
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String pubFilePath;
    private String cookieName;
    private String token;

    private PublicKey publicKey;

    //    被@PostConstruct修饰的方法会在服务器加载Servlet的时候运行
    @PostConstruct
    public void init(){
        try {
            this.publicKey = RsaUtils.getPublicKey(pubFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
