package com.xmdp.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.xmdp.constant.ShopConstant;
import com.xmdp.dto.Result;
import com.xmdp.entity.ShopType;
import com.xmdp.mapper.ShopTypeMapper;
import com.xmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryTypeList() {
        //和shopControl做法一样
        String shop = stringRedisTemplate.opsForValue().get(ShopConstant.SHOP_TYPE_List);
        if (StrUtil.isNotBlank(shop)) {
            List<ShopType> shopTypes = JSONUtil.toList(shop, ShopType.class);
            return Result.ok(shopTypes);
        }
        List<ShopType> typeList = query().orderByAsc("sort").list();
        if (CollUtil.isEmpty(typeList)){
            return Result.fail("首页数据不存在");
        }
        stringRedisTemplate.opsForValue().set(ShopConstant.SHOP_TYPE_List,JSONUtil.toJsonStr(typeList));
        return Result.ok(typeList);
    }
}
