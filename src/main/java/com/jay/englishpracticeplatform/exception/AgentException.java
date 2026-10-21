package com.jay.englishpracticeplatform.exception;

/*
    AI 助手的业务异常：消息是写给用户看的，Controller会原样返回给页面
    技术细节（状态码，堆栈）只写进日志，不放进消息里
 */
public class AgentException extends RuntimeException{

    public AgentException(String message){
        super(message);
    }

    public AgentException(String message, Throwable cause){
        super(message, cause);
    }
}
