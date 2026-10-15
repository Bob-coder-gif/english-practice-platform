package com.jay.englishpracticeplatform.exception;

// 收藏、取消收藏时的业务错误：单词不存在、已经收藏过、超过数量上限
// message 直接显示给用户
public class WordCollectionException extends RuntimeException{

    public WordCollectionException(String message){
        super(message);
    }
}
