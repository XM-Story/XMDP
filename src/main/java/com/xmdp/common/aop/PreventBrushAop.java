package com.xmdp.common.aop;

import cn.hutool.json.JSONUtil;
import com.xmdp.common.anno.PreventBrush;
import com.xmdp.common.enums.PreventStrategy;
import com.xmdp.common.exception.BusinessException;
import com.xmdp.common.utils.RedisUtil;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.Base64;

/**
 * @ClassName PreventBrushAop
 * @Description 防刷切面实现类
 * @Author XM
 * @Date 2022/12/21 11:03
 **/

@Aspect
@Component
public class PreventBrushAop {
    private static Logger log = LoggerFactory.getLogger(PreventBrushAop.class);

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 切入点
     */
    @Pointcut("@annotation(com.xmdp.common.anno.PreventBrush)")
    public void pointcut() {
    }

    /**
     * 处理前
     */
    @Before("pointcut()")
    public void joinPoint(JoinPoint joinPoint) throws Exception {
        String requestStr = JSONUtil.toJsonStr(joinPoint.getArgs()[0]);
        if (StringUtils.isEmpty(requestStr) || requestStr.equalsIgnoreCase("{}")) {
            throw new BusinessException("[防刷]入参不允许为空");
        }

        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = joinPoint.getTarget().getClass().getMethod(methodSignature.getName(),
                methodSignature.getParameterTypes());

        PreventBrush preventAnnotation = method.getAnnotation(PreventBrush.class);
        String methodFullName = method.getDeclaringClass().getName() + method.getName();

        entrance(preventAnnotation, requestStr, methodFullName);
        return;
    }

    /**
     * 入口
     */
    private void entrance(PreventBrush prevent, String requestStr, String methodFullName) throws Exception {
        PreventStrategy strategy = prevent.strategy();
        switch (strategy) {
            case DEFAULT:
                defaultHandle(requestStr, prevent, methodFullName);
                break;
            default:
                throw new BusinessException("无效的策略");
        }
    }

    /**
     * 默认处理方式
     */
    private void defaultHandle(String requestStr, PreventBrush prevent, String methodFullName) throws Exception {
        String base64Str = toBase64String(requestStr);
        long expire = Long.parseLong(prevent.value());

        String resp = redisUtil.get(methodFullName + base64Str);
        if (StringUtils.isEmpty(resp)) {
            redisUtil.set(methodFullName + base64Str, requestStr, expire);
        } else {
            String message = !StringUtils.isEmpty(prevent.message()) ? prevent.message() :
                    expire + "秒内不允许重复请求";
            throw new BusinessException(message);
        }
    }

    /**
     * 对象转换为base64字符串
     *
     * @param obj 对象值
     * @return base64字符串
     */
    private String toBase64String(String obj) throws Exception {
        if (StringUtils.isEmpty(obj)) {
            return null;
        }
        Base64.Encoder encoder = Base64.getEncoder();
        byte[] bytes = obj.getBytes("UTF-8");
        return encoder.encodeToString(bytes);
    }

}
