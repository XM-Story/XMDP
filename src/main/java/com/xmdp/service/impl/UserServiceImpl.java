package com.xmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xmdp.constant.UserConstant;
import com.xmdp.dto.LoginFormDTO;
import com.xmdp.dto.Result;
import com.xmdp.entity.User;
import com.xmdp.mapper.UserMapper;
import com.xmdp.service.IUserService;
import com.xmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;

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
    @Override
    public Result sendCode(String phone, HttpSession session) {
        //手机号校验
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号不合法,请重新输入");
        }
        //生成验证码
        String code = RandomUtil.randomNumbers(6);
        //放入session
        session.setAttribute(UserConstant.USER_CODE,code);
        //没有短信功能，就控制台输出自己看下把。
        log.debug(phone+"的验证码为{}",code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //手机号校验
        if(RegexUtils.isPhoneInvalid(loginForm.getPhone())){
            return Result.fail("手机号不合法,请重新输入");
        }
        //验证码校验
        Object code = session.getAttribute(UserConstant.USER_CODE);
        if (code == null || !code.toString().equals(loginForm.getCode())){
            return Result.fail("验证码不正确");
        }
        //该用户是否注册过
        User user = query().eq("phone", loginForm.getPhone()).one();
        if (user == null){
            //没有自动创建账号并登录
            user = createNewUserWithPhone(loginForm.getPhone());
        }
        //放入session
        session.setAttribute(UserConstant.USER_NAME,user);
        //这里不需要返回用户信息给前端，因为session信息默认会被cookie带上
        return Result.ok();
    }

    private User createNewUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(UserConstant.USER_PRI+RandomUtil.randomString(10));
        save(user);
        return user;
    }
}
