package com.jay.englishpracticeplatform.exception;

public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException(String username) {
        super("用户名存在：" + username);
    }
}
