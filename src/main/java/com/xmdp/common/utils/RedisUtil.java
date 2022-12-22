package com.xmdp.common.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @ClassName RedisUtil
 * @Description
 * @Author XM
 * @Date 2022/12/21 11:30
 **/
@Component
public class RedisUtil {
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    public String get(String key){
        return stringRedisTemplate.opsForValue().get(key);
    }

    public void set(String key,String val,Long exp){
         stringRedisTemplate.opsForValue().set(key,val,exp);
    }

}
