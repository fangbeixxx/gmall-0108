package com.atguigu.gmall.cart.exception.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Component
@Slf4j
public class AsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final  String Exception_key="cart:exception";


    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {

        // 记录到数据库 或者 log日志
//        log.error("异步任务出现异常。方法：{}，参数：{}，异常信息：{}", method.getName(), Arrays.asList(objects), throwable.getMessage());
//        获取可变形参列表第一个参数也就是userId 存入redis中
        this.redisTemplate.boundSetOps(Exception_key).add(objects[0].toString());
    }
}
