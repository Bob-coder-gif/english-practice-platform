package com.jay.englishpracticeplatform.entity;

public enum WordLevel {

    GAOKAO("高考"),
    CET4("四级"),
    CET6("六级"),
    KAOYAN("考研"),
    IELTS("雅思");

    private final String label;

    WordLevel(String label){
        this.label = label;
    }

    public String getLabel(){
        return label;
    }
}
