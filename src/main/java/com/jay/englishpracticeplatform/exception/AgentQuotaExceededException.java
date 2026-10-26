package com.jay.englishpracticeplatform.exception;

// AI 助手今天的提问次数用完了：Controller 返回 429
public class AgentQuotaExceededException extends AgentException{

    public AgentQuotaExceededException(String message){
        super(message);
    }
}
