package com.xmdp.interception;

import com.xmdp.constant.UserConstant;
import com.xmdp.dto.UserDTO;
import com.xmdp.entity.User;
import com.xmdp.utils.UserHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @description:  拦截器实现HandlerInterceptor，生效需要实现WebMvcConfig后将实现的拦截器加入。
 * @author: 小明长高高
 * @date: 2022/10/23 20:55
 **/
public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Object user = request.getSession().getAttribute(UserConstant.USER_NAME);
        if (user == null){
            //直接返回401，401表示没有权限访问的意思  false表示拦截，true表示放行
            response.setStatus(401);
            return false;
        }
        UserHolder.saveUser((UserDTO)user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //登录请求处理完毕后立即清理数据，防止内存泄露
        UserHolder.removeUser();
    }
}
