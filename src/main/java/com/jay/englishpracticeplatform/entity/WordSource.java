package com.jay.englishpracticeplatform.entity;

import lombok.Getter;

//单词来源
@Getter
public enum WordSource {
    CORE("考试词库"),
    EXTENDED("扩展");

    private final String label;

    WordSource(String label) {
        this.label = label;
    }
}