package com.example.ecommerce.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void cacheProduct(Long id, Object product) {
        redisTemplate.opsForValue().set("product:" + id, product, 10, TimeUnit.MINUTES);
    }

    public Object getProduct(Long id) {
        return redisTemplate.opsForValue().get("product:" + id);
    }

    public void invalidateProduct(Long id) {
        redisTemplate.delete("product:" + id);
    }
}
