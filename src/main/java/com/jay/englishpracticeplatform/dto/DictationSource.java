package com.jay.englishpracticeplatform.dto;

public enum DictationSource {

    ALL("全部单词"),
    LEARNED("已学过的单词"),
    MISTAKES("错题本");

    private final String label;

    DictationSource(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
