package com.atguigu.gmall.index.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

@Component
public class DistributeLock {
    @Autowired
    StringRedisTemplate redisTemplate;

    private Timer timer;

//    获取锁
    public Boolean lock(String lockName,String uuid,Integer expire){
        String script = "if(redis.call('exists', KEYS[1]) == 0 or redis.call('hexists', KEYS[1], ARGV[1]) == 1) " +
                "then " +
                "   redis.call('hincrby', KEYS[1], ARGV[1], 1) " +
                "   redis.call('expire', KEYS[1], ARGV[2]) " +
                "   return 1 " +
                "else " +
                "   return 0 " +
                "end";
        Boolean flag = redisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList(lockName), uuid,expire.toString());
        if(!flag){
            try {
                Thread.sleep(100);
                lock(lockName,uuid,expire);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }else{
            renewExpire(lockName,uuid,expire);
        }
        return true;
    }
    //释放锁
    public void unlock(String lockName,String uuid){
        String script="if(redis.call('hexists',KEYS[1],ARGV[1])==0) " +
                "then " +
                "   return nil " +
                "elseif(redis.call('hincrby',KEYS[1],ARGV[1],-1)==0) " +
                "then " +
                "   return redis.call('del',KEYS[1]) " +
                "else " +
                "   return 0 " +
                "end";
        Long flag = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList(lockName), uuid);
        if(flag==null){
            throw new RuntimeException("释放的锁不属于你");
        }if (flag == 1){
            timer.cancel();
        }
    }

    //定时续期
    public void renewExpire(String lockName,String uuid,Integer expire){
        String script="if(redis.call('hexists',KEYS[1],ARGV[1])==1) " +
                "then" +
                "  redis.call('expire',KEYS[1],ARGV[2]) " +
                "end";
        this.timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                redisTemplate.execute(new DefaultRedisScript<>(script,Boolean.class),Arrays.asList(lockName),expire.toString());
            }
        },expire*3/1000, expire * 1000 / 3);
    }

    public static void main(String[] args) {
        System.out.println(System.currentTimeMillis());
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("这是一个定时任务"+System.currentTimeMillis());
            }
        },5000,10000);
    }
}
