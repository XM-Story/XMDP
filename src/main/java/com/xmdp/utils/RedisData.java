package com.xmdp.utils;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RedisData {
    private LocalDateTime expireTime;

    //用于存放设计逻辑过期时间的对象
    private Object data;
}
