package com.xmdp.common.enums;

import cn.hutool.core.util.StrUtil;

/**
 * @EnumName PreventStrategy
 * @Description
 * @Author XM
 * @Date 2022/12/21 16:33
 **/
public enum PreventStrategy {
    DEFAULT("01", "首次入会");


    final String code;
    final String memo;

    private PreventStrategy(String code, String memo) {
        this.code = code;
        this.memo = memo;
    }

    private String getCode() {
        return code;
    }

    private String getMemo() {
        return memo;
    }

    public static String getMemoByCode(String code) {
        for (PreventStrategy preventStrategy : values()) {
            if (StrUtil.equals(preventStrategy.getCode(), code)) {
                return preventStrategy.getMemo();
            }
        }
        return null;
    }
}
