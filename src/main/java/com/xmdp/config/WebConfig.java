package com.xmdp.config;

import com.xmdp.interception.LoginInterceptor;
import com.xmdp.interception.RefreshLoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @description: 配置拦截器，并使其生效
 * @author: 小明长高高
 * @date: 2022/10/23 21:02
 **/
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        //添加拦截器，使其生效，并排除一些不需要拦截的路径
        registry.addInterceptor(new LoginInterceptor()).excludePathPatterns(
                "/shop/**",
                "/voucher/**",
                "/shop-type/**",
                "/upload/**",
                "/blog/hot",
                "/user/code",
                "/user/login"
        ).order(1);
        //优先生效，存用户，刷新有效期，对所有接口都生效
        registry.addInterceptor(new RefreshLoginInterceptor(stringRedisTemplate)).order(0);
    }
}
