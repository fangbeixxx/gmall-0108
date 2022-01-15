package com.atguigu.gmall.index.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.config.GmallCache;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.index.utils.DistributeLock;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class IndexServiceImpl implements IndexService {
    @Autowired
    GmallPmsClient gmallPmsClient;
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Autowired
    DistributeLock lock;
    @Autowired
    RedissonClient redissonClient;

    public final static String KEY_PREFIX = "index:cates:";
    public final static String LOCK_PREFIX="index:cates:lock";
    @Override
    public List<CategoryEntity> queryLvllCategories() {
        ResponseVo<List<CategoryEntity>> listResponseVo = gmallPmsClient.queryCategoriesByPid(0l);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        return categoryEntities;
    }


//    aop和注解改造
    @Override
    @GmallCache(prefix = KEY_PREFIX,timeout = 259200,random = 14400,lock = LOCK_PREFIX)
    public List<CategoryEntity> queryLvl2WithSubsByPid(Long pid) {
        ResponseVo<List<CategoryEntity>> listResponseVo = gmallPmsClient.queryLvl2WithSubsById(pid);
        List<CategoryEntity> data = listResponseVo.getData();
        return data;
    }



    public List<CategoryEntity> queryLvl2WithSubsByPid2(Long pid) {
        // 判断redis是否存在数据 则从redis取数据并返回
        String s = stringRedisTemplate.opsForValue().get(KEY_PREFIX + pid);
        if (StringUtils.isNotBlank(s)) {
            return JSON.parseArray(s, CategoryEntity.class);
        }
            //为了防止缓存击穿添加分布式锁
            RLock lock = redissonClient.getFairLock(LOCK_PREFIX+pid);
            lock.lock();
        try {
            //不存在则到数据库取数据，然后存入缓存数据库
            // 在当前请求获取锁的过程中，可能已经有其他线程获取到锁，并把数据放入缓存，此时最好再次确认缓存中是否已有
            String s2 = stringRedisTemplate.opsForValue().get(KEY_PREFIX + pid);
            if (StringUtils.isNotBlank(s2)) {
                return JSON.parseArray(s2, CategoryEntity.class);
            }
            ResponseVo<List<CategoryEntity>> listResponseVo = gmallPmsClient.queryLvl2WithSubsById(pid);
            List<CategoryEntity> categoryEntities = listResponseVo.getData();
            if (CollectionUtils.isEmpty(categoryEntities)) {
//                防止缓存穿透 把不存在的值也保存 并设置过期时间(相对较短的)
                stringRedisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities), 5, TimeUnit.MINUTES);
            }else {
                //防止缓存雪崩，给过期时间加上随机值
                stringRedisTemplate.opsForValue().set(KEY_PREFIX + pid, JSON.toJSONString(categoryEntities), 180 + new Random().nextInt(10), TimeUnit.DAYS);
            }

            return categoryEntities;
        } finally {
            lock.unlock();
        }

    }


    public void testLock1() {
        //先查询数据库中的数据 获取锁：setnx 利用是否存在的特性
        String uuid = UUID.randomUUID().toString();
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid, 3, TimeUnit.SECONDS);
//       没有获取锁的重新去抢
        if (!flag) {
            try {
                Thread.sleep(100);
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } else {
//            不具备原子性 同一个指令具备原子性
//          stringRedisTemplate.expire("lock",10,TimeUnit.SECONDS);
            //执行业务操作
            //不存在数据 获取锁成功的线程
            String numString = this.stringRedisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(numString)) {
                this.stringRedisTemplate.opsForValue().set("num", "1");
            }
            int num = Integer.parseInt(numString);
            this.stringRedisTemplate.opsForValue().set("num", String.valueOf(++num));
            //释放锁 删除锁key
//            if(StringUtils.equals(uuid,stringRedisTemplate.opsForValue().get("lock"))){
//                stringRedisTemplate.delete("lock");
//            }
            String script = "if(redis.call('get', KEYS[1]) == ARGV[1]) then return redis.call('del', KEYS[1]) else return 0 end";
            stringRedisTemplate.execute(new DefaultRedisScript<>(script, Boolean.class), Arrays.asList("lock"), uuid);

        }
    }


    public void testLock2() {
        //先查询数据库中的数据 获取锁：setnx 利用是否存在的特性
        String uuid = UUID.randomUUID().toString();
        Boolean lock = this.lock.lock("lock", uuid, 30);
        if (lock) {
            String numString = this.stringRedisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(numString)) {
                this.stringRedisTemplate.opsForValue().set("num", "1");
            }

            int num = Integer.parseInt(numString);
            this.stringRedisTemplate.opsForValue().set("num", String.valueOf(++num));

            subLock("lock", uuid);
            //释放锁 删除锁key
            this.lock.unlock("lock", uuid);
        }
    }

    public void subLock(String lockName, String uuid) {
        lock.lock(lockName, uuid, 30);
        System.out.println("重入锁测试");
        lock.unlock(lockName, uuid);
    }


    public void testLock() {
        RLock lock = redissonClient.getLock("lock");
        lock.lock();
        try {
            String numString = this.stringRedisTemplate.opsForValue().get("num");
            if (StringUtils.isBlank(numString)) {
                this.stringRedisTemplate.opsForValue().set("num", "1");
            }
            int num = Integer.parseInt(numString);
            this.stringRedisTemplate.opsForValue().set("num", String.valueOf(++num));
//            try {
//                TimeUnit.SECONDS.sleep(1000);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void testRead() {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock("rwLock");
        rwLock.readLock().lock(10,TimeUnit.SECONDS);
        System.out.println("-------测试读锁--------");
//        rwLock.readLock().unlock();
    }

    @Override
    public void testWrite() {
        RReadWriteLock rwLock = redissonClient.getReadWriteLock("rwLock");
        rwLock.writeLock().lock(10,TimeUnit.SECONDS);
        System.out.println("测试写锁");
        // rwLock.writeLock().unlock();

    }

    @Override
    public void testLatch() {
        try {
            RCountDownLatch cdl = redissonClient.getCountDownLatch("cdl");
            cdl.trySetCount(6);
            cdl.await();
            System.out.println("班长准备锁门了");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void testCountDown() {
        RCountDownLatch cdl = redissonClient.getCountDownLatch("cdl");
        cdl.countDown();
        System.out.println("出来了一位同学");
    }
}
