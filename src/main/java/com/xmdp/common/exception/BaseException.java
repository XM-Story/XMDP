package com.xmdp.common.exception;

/**
 * @ClassName BaseException
 * @Description  异常封装基础类
 * @Author XM
 * @Date 2022/12/21 11:17
 **/
public class BaseException extends RuntimeException {
    public static final String ERROR_CODE = "-1";
    private String code;

    public BaseException(String msg) {
        super(msg);
        this.code = "-1";
    }

    public BaseException(String msg, String code) {
        super(msg);
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
