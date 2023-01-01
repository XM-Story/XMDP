package com.xmdp.common.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xmdp.constant.ShopConstant;
import com.xmdp.dto.Result;
import com.xmdp.entity.Shop;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

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

    public void set(String key,Object val, Long exp, TimeUnit unit){
         stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(val),exp,unit);
    }

    public boolean tryLock(String key){
        //互斥锁，setnx 命令如果存在这个值返回false 不存在返回true 为了反正del key失败，设置有效期
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        //返回的是包装类型，会自动拆箱，为了防止null导致空指针，以下处理
        return BooleanUtil.isTrue(flag);
    }

    public void unlock(String key){
        stringRedisTemplate.delete(key);
    }

    //缓存穿透 缓存null值处理 T与R是泛型为了统一类型
    public <T,R> R queryWithPassThrough(String keyPreFix, T id, Class<R> type, Function<T,R> dbFunc,Long time,TimeUnit unit){
        //查询缓存 查到返回
        String store = stringRedisTemplate.opsForValue().get(keyPreFix+id);
        //isNotBlank 忽略了null ""
        if (StrUtil.isNotBlank(store)) {
            return JSONUtil.toBean(store, type);
        }
        // 如果是isNotBlank确定是空单不是null说明是"",防止缓存穿透查到控制直接报错
        if (store!=null){
            return null;
        }
        //查mysql,存在写入redis，返回，不存在报错
        R r = dbFunc.apply(id);
        if (r==null) {
            //防止缓存穿透，放入空值，设置过期时间很短防止内存占用过多
            stringRedisTemplate.opsForValue().set(keyPreFix+id,"",ShopConstant.SHOP_TTL_THROUGH, TimeUnit.MINUTES);
            return null;
        }
        //存在写入redis，增加一个过期时间兜底
        this.set(keyPreFix+id,r,time,unit);
        return r;
    }

}
