package com.xmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xmdp.constant.ShopConstant;
import com.xmdp.dto.Result;
import com.xmdp.entity.Shop;
import com.xmdp.mapper.ShopMapper;
import com.xmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Override
    public Result  queryById(Long id) {
        //缓存穿透代码处理(缓存null)
        //return queryWithPassThrough(id);
        //缓存击穿，互斥锁代码处理
        return queryWithMutex(id);
    }

    public Result queryWithMutex(Long id) {
        //查询缓存 查到返回
        String store = stringRedisTemplate.opsForValue().get(ShopConstant.SHOP_KEY+id);
        //isNotBlank 忽略了null ""
        if (StrUtil.isNotBlank(store)) {
            Shop shop = JSONUtil.toBean(store, Shop.class);
            return Result.ok(shop);
        }
        // 如果是isNotBlank确定是空单不是null说明是"",防止缓存穿透查到控制直接报错
        if (store!=null){
            return Result.fail("店铺不存在！");
        }
        Shop shop = null ;
        try{
            //查mysql,存在写入redis，获取互斥锁，获取成功 根据id查询数据库后写入，释放锁失败休眠重试
            if (!tryLock(ShopConstant.LOCK_KEY+id)){
                //每获取到锁，没资格重建缓存，休眠50毫秒后递归
                TimeUnit.MILLISECONDS.sleep(10);
                return queryWithMutex(id);
            }
            shop = getById(id);
            //Jemter压力测试开启 或者apipost
            //TimeUnit.MILLISECONDS.sleep(200);
            if (shop==null) {
                //防止缓存穿透，放入空值，设置过期时间很短防止内存占用过多
                stringRedisTemplate.opsForValue().set(ShopConstant.SHOP_KEY+id,"",ShopConstant.SHOP_TTL_THROUGH, TimeUnit.MINUTES);
                return Result.fail("店铺不存在！");
            }
            //真正的缓存重建  增加一个过期时间兜底
            stringRedisTemplate.opsForValue().set(ShopConstant.SHOP_KEY+id, JSONUtil.toJsonStr(shop),ShopConstant.SHOP_TTL, TimeUnit.MINUTES);
        }catch (Exception e){
            throw new RuntimeException(e);
        }finally {
            //保证异常了也可以释放锁
            unlock(ShopConstant.LOCK_KEY+id);
        }
        return Result.ok(shop);
    }

    private boolean tryLock(String key){
        //互斥锁，setnx 命令如果存在这个值返回false 不存在返回true 为了反正del key失败，设置有效期
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        //返回的是包装类型，会自动拆箱，为了防止null导致空指针，处理
        return BooleanUtil.isTrue(flag);
    }
    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }
    public Result queryWithPassThrough(Long id){
        //查询缓存 查到返回
        String store = stringRedisTemplate.opsForValue().get(ShopConstant.SHOP_KEY+id);
        //isNotBlank 忽略了null ""
        if (StrUtil.isNotBlank(store)) {
            Shop shop = JSONUtil.toBean(store, Shop.class);
            return Result.ok(shop);
        }
        // 如果是isNotBlank确定是空单不是null说明是"",防止缓存穿透查到控制直接报错
        if (store!=null){
            return Result.fail("店铺不存在！");
        }
        //查mysql,存在写入redis，返回，不存在报错
        Shop shop = getById(id);
        if (shop==null) {
            //防止缓存穿透，放入空值，设置过期时间很短防止内存占用过多
            stringRedisTemplate.opsForValue().set(ShopConstant.SHOP_KEY+id,"",ShopConstant.SHOP_TTL_THROUGH, TimeUnit.MINUTES);
            return Result.fail("店铺不存在！");
        }
        //增加一个过期时间兜底
        stringRedisTemplate.opsForValue().set(ShopConstant.SHOP_KEY+id, JSONUtil.toJsonStr(shop),ShopConstant.SHOP_TTL, TimeUnit.MINUTES);
        return Result.ok(shop);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        if (shop.getId()==null){
            return Result.fail("店铺id不可以为空");
        }
        //先更新数据库再删除缓存
        updateById(shop);
        stringRedisTemplate.delete(ShopConstant.SHOP_KEY+shop.getId());
        return Result.ok();
    }
}
