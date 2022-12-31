package com.xmdp.common.anno;

/**
 * @AnnoName PreventBrush
 * @Description  接口防刷注解
 * @Author XM
 * @Date 2022/12/21 09:26
 **/

import com.xmdp.common.enums.PreventStrategy;

import java.lang.annotation.*;

@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface PreventBrush {

    /**
     * 时间
     * @return
     */
    String value() default "60";

    /**
     * 内容
     * @return
     */
    String message() default "";

    PreventStrategy strategy() default PreventStrategy.DEFAULT;

}