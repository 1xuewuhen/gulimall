package com.xwh.common.exception;

public enum BizCodeEnum {
    UN_KNOW_EXCEPTION(10000,"系统未知异常"),
    VAILD_EXCEPTION(10001,"参数格式校验失败"),
    SMS_CODE_EXCEPTION(10002,"验证码获取频率太高，请稍候再试"),
    PRODUCT_UP_EXCEPTION(11000,"商品上架异常"),
    USER_EXIST_EXCEPTION(15001,"用户已存在"),
    PHONE_CODE_EXCEPTION(15002,"手机号已存在"),
    LOGINACCT_PASSWORD_INVAILD_EXCEPTION(15003,"用户名或密码错误");

    private Integer code;
    private String message;

    BizCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
