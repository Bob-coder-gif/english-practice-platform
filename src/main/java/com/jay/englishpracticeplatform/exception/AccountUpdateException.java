package com.jay.englishpracticeplatform.exception;

import lombok.Getter;

// 修改账号信息（用户名、密码）时的业务校验失败
// field 是出错的表单字段名，Controller 用它把错误提示显示在对应的输入框下面
@Getter
public class AccountUpdateException extends RuntimeException {

    private final String field;

    public AccountUpdateException(String field, String message) {
        super(message);
        this.field = field;
    }
}