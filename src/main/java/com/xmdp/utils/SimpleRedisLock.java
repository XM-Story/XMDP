package com.xmdp.utils;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * @description:  简易版redis分布式锁
 * @author: 小明长高高
 * @date: 2023/1/3 20:32
 **/
public class SimpleRedisLock implements ILock{
    //name是本次抢锁的key
    private String name;
    private StringRedisTemplate stringRedisTemplate;
    private static final String KEY_PREFIX = "lock:";

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutOfSeconds) {
        //获取线程的值 这里的value可以设置其他值，主要是key的逻辑使用了nx加ex
        long id = Thread.currentThread().getId();
        Boolean ifFlag = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, String.valueOf(id), timeoutOfSeconds, TimeUnit.SECONDS);
        //防止自动拆箱导致的空指针
        return Boolean.TRUE.equals(ifFlag);
    }

    @Override
    public void unlock() {
        stringRedisTemplate.delete(KEY_PREFIX+name);
    }
}
