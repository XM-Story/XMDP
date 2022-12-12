package com.xmdp.constant;

/**
 * @description:
 * @author: 小明长高高
 * @date: 2022/12/3 15:49
 **/
public abstract class ShopConstant {
    public static final String SHOP_KEY = "cache:shop:";
    public static final String SHOP_TYPE_List = "cache:shoptype:list";

    public static final String LOCK_KEY = "lock:shop:";

    public static final Long SHOP_TTL = 30L;
    public static final Long SHOP_TTL_THROUGH = 2L;
}
