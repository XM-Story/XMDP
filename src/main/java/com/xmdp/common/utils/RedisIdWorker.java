package com.xmdp.common.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * @description: 分布式全局ID生成器
 * @author: 小明长高高
 * @date: 2023/1/1 20:29
 **/
@Component
public class RedisIdWorker {
    /**
     *开始时间戳
     */
    private static final long BEGIN_TIMESTAMP = 1640995200L;

    /**
     * 序列号位数
     */
    private static final int COUNT_BITS = 32;
    StringRedisTemplate stringRedisTemplate;

    public RedisIdWorker(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public long workId(String keyPreFix){
        //当前时间戳  ZoneOffset.UTC 表示转成秒，使用国际标准时间
        LocalDateTime now = LocalDateTime.now();
        long l = now.toEpochSecond(ZoneOffset.UTC);
        //获取时间差值 单位秒
        long p = l - BEGIN_TIMESTAMP;
        //获取时间yyyy：MM：dd 每天一个key，这样每天的数据都使用单独的key可以防止数据量过大，超过redis增长的最大长度，而且方便数据统计
        String format = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        Long increment = stringRedisTemplate.opsForValue().increment("icr:" + keyPreFix + ":" + format);
        //时间差值左移32位，这样低位的32位都是0 再或 | 上increment 这样increment就到了最低位上
        //最后的结果是 0加上31位时间戳 加上 32位序列号数 一共64位
        return p << COUNT_BITS | increment;
    }
}
