package com.xmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Assert;
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
    public Result queryById(Long id) {
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
