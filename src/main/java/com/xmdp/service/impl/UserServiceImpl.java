package com.xmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.RuntimeUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xmdp.constant.UserConstant;
import com.xmdp.dto.LoginFormDTO;
import com.xmdp.dto.Result;
import com.xmdp.dto.UserDTO;
import com.xmdp.entity.User;
import com.xmdp.mapper.UserMapper;
import com.xmdp.service.IUserService;
import com.xmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.xmdp.constant.UserConstant.USER_CODE;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author XM
 * @since
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        //手机号校验
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号不合法,请重新输入");
        }
        //生成验证码
        String code = RandomUtil.randomNumbers(6);
        //放入session到redis
        stringRedisTemplate.opsForValue().set(USER_CODE+phone,code,5, TimeUnit.MINUTES);
        //没有短信功能,就不发真实的短信了,自己redis看下吧
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //手机号校验
        if(RegexUtils.isPhoneInvalid(loginForm.getPhone())){
            return Result.fail("手机号不合法,请重新输入");
        }
        //验证码校验，redis获取
        String code = stringRedisTemplate.opsForValue().get(USER_CODE + loginForm.getPhone());
        if (code == null || !code.equals(loginForm.getCode())){
            return Result.fail("验证码不正确");
        }
        //该用户是否注册过
        User user = query().eq("phone", loginForm.getPhone()).one();
        if (user == null){
            //没有自动创建账号并登录
            user = createNewUserWithPhone(loginForm.getPhone());
        }
        //用户信息放入redis怎么放呢，这里使用key为一个token令牌，value为用户信息的形式处理，使用hash类型存储更省内存，内存够就用String也行
        String token = UUID.randomUUID().toString(true);//是否忽略掉-
        //防止敏感信息泄露
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //redis的hash结构存储必须转成map这种才行，stringRedisTemplate的key和value必须为String，所以需要使用CopyOptions将userDTO的value类型全部转String，同时保存null类型value
        // 具体请看UserDTO的数据结构。
        Map<String, Object> map = BeanUtil.beanToMap(userDTO, new HashMap<>(), CopyOptions.create().setIgnoreNullValue(true).setFieldValueEditor((k, v) -> v.toString()));
        stringRedisTemplate.opsForHash().putAll(UserConstant.USER_NAME+token,map);
        //设置有效期，不然数据库爆炸
        stringRedisTemplate.expire(UserConstant.USER_NAME+token,30,TimeUnit.MINUTES);
        //注意这里,我们把token返回前端，前端则每次请求都应该带上他，只有这样我们才知道他是不是登录了
        return Result.ok(token);
    }

    private User createNewUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(UserConstant.USER_PRI+RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
