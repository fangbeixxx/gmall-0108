package com.atguigu.gmall.index.aspect;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.index.config.GmallCache;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Component
@Aspect
public class GmallCacheAspect {

    @Autowired
    StringRedisTemplate redisTemplate;
    @Autowired
    RedissonClient redissonClient;

    @Autowired
    RBloomFilter filter;

    @Pointcut("execution(* com.atguigu.gmall.index.service.*.*(..))")
    public void pointcut(){}

//    @Before("pointcut()")
//    public void before(JoinPoint joinPoint){
//        //获取方法签名
//        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
//        System.out.println("前置通知的类---------------------"+joinPoint.getTarget().getClass());
//        System.out.println("前置通知目标方法-----------------"+signature.getMethod());
//        System.out.println("前置通知参数列表-------------------------"+joinPoint.getArgs());
//    }
//    @After("pointcut()")
//    public void  after(){
//        System.out.println("最终通知");
//    }
//    @AfterReturning(value = "pointcut()",returning = "result")
//    public void afterReturning(Object result){
//        System.out.println("后置返回通知----------------"+result);
//    }
//    @AfterThrowing("pointcut()")
//    public void AfterThrowing(){
//        System.out.println("异常通知------------------");
//    }

    @Around("@annotation(com.atguigu.gmall.index.config.GmallCache)")
    public Object around(ProceedingJoinPoint joinPoint) throws  Throwable{
        //获取方法签名
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        //获取目标方法
        Method method = signature.getMethod();
        //获取方法注解
        GmallCache gmallCache = method.getAnnotation(GmallCache.class);
//        获取目标方法形参
        List<Object> args = Arrays.asList(joinPoint.getArgs());
        //获取目标方法返回值类型
        Class returnType = signature.getReturnType();
        // 获取锁前缀
        String prefix = gmallCache.prefix();

        //拼接锁名
        String lock=prefix+args;

        //为了解决缓存穿透，使用了bloom过滤器
        if(!filter.contains(lock)){
            return null;
        }

        //判断缓存中是否存在数据
        String s = redisTemplate.opsForValue().get(lock);
        if(StringUtils.isNotBlank(s)){
//            反序列化把json字符转为对象
            return JSON.parseObject(s,returnType);
        }
        //分布式锁 防止缓存击穿
        RLock fairLock = redissonClient.getFairLock(gmallCache.lock() + args);
        fairLock.lock();

        try {
            //再次查询缓存，在获取分布式锁的可能有其他请求把数据放入缓存了
            String s2 = redisTemplate.opsForValue().get(lock);
            if(StringUtils.isNotBlank(s2)){
//            反序列化把json字符转为对象
                return JSON.parseObject(s2,returnType);
            }
            //执行代理方法
            Object proceed = joinPoint.proceed(joinPoint.getArgs());
            //把数据存入缓存
            if(proceed!=null) {
                int timeOut = gmallCache.timeout() + new Random().nextInt(gmallCache.random());
                redisTemplate.opsForValue().set(lock, JSON.toJSONString(proceed), timeOut, TimeUnit.MINUTES);
            }
            return proceed;
        } finally {
            //释放锁
            fairLock.unlock();
        }
    }
}
