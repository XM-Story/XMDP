package com.xmdp.interception;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.xmdp.constant.UserConstant;
import com.xmdp.dto.UserDTO;
import com.xmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.xmdp.constant.UserConstant.AUTH_TOKEN;

/**
 * @description: 拦截器实现HandlerInterceptor，生效需要实现WebMvcConfig后将实现的拦截器加入。
 * @author: 小明长高高
 * @date: 2022/10/25 22:47
 **/
public class RefreshLoginInterceptor implements HandlerInterceptor {
    //重要，这里无法使用AutoWired,因为这个类没有交给Spring管理
    private StringRedisTemplate stringRedisTemplate;

    public RefreshLoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //防止用户内存泄露
        UserHolder.removeUser();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader(AUTH_TOKEN);
        if (StrUtil.isBlank(token)) {
            //用户访问首页是可以访问的，这个时候我们也应该刷新有效期
            //第二个拦截器必须要登录才可以访问接口，第一个拦截器（这个）不需要所以放行
            //是否登录交给第二个拦截器校验,这个拦截器只负责将用户放入ThreadLocal和刷新有效期
            return true;
        }
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(UserConstant.USER_NAME+token);
        if (map.isEmpty()){
            //用户不存在直接交给第二个拦截器处理
            return true;
        }
        UserDTO userDTO = BeanUtil.fillBeanWithMap(map, new UserDTO(), false);
        //保存到ThreadLocal
        UserHolder.saveUser(userDTO);
        //刷新有效期
        stringRedisTemplate.expire(UserConstant.USER_NAME,30, TimeUnit.MINUTES);
        return true;
    }
}
