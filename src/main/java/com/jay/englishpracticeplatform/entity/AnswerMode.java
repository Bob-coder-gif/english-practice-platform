package com.jay.englishpracticeplatform.entity;

public enum AnswerMode {
    LEARN("学习"),
    REVIEW("复习"),
    DICTATION_EN("听写英语"),
    DICTATION_CN("听写汉语");

    private final String label;

    AnswerMode(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
