package com.xmdp.common.exception;

/**
 * @ClassName BusinessException
 * @Description
 * @Author XM
 * @Date 2022/12/21 11:19
 **/
public class BusinessException extends BaseException{
    public BusinessException(String msg) {
        super(msg);
    }

    public BusinessException(String msg, String code) {
        super(msg, code);
    }
}
