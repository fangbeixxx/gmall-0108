package com.atguigu.gmall.index.config;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class BloomFilterConfig {
    @Autowired
    GmallPmsClient gmallPmsClient;
    @Autowired
    RedissonClient redissonClient;

    private static final  String KEY_PREFIX="index:cates:";

    @Bean
    public RBloomFilter rBloomFilter(){
        //初始化布隆过滤器
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("index:bloom:filter");
        bloomFilter.tryInit(1000,0.03);
        //给布隆过滤器加入数据
        ResponseVo<List<CategoryEntity>> listResponseVo = gmallPmsClient.queryCategoriesByPid(0l);
        List<CategoryEntity> categoryEntities = listResponseVo.getData();
        categoryEntities.forEach(categoryEntity -> {
            bloomFilter.add(KEY_PREFIX + "[" +categoryEntity.getId() + "]");
        });

        return bloomFilter;
    }
}
