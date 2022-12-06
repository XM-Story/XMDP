package com.xmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
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

import javax.annotation.Resource;

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
        if (StrUtil.isNotBlank(store)) {
            Shop shop = JSONUtil.toBean(store, Shop.class);
            return Result.ok(shop);
        }
        //查mysql,存在写入redis，返回，不存在报错
        Shop shop = getById(id);
        if (shop==null) {
            return Result.fail("店铺不存在！");
        }
        stringRedisTemplate.opsForValue().set(ShopConstant.SHOP_KEY+id, JSONUtil.toJsonStr(shop));
        return Result.ok(shop);
    }
}
