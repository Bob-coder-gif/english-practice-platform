package com.jay.englishpracticeplatform.dto;

import javax.swing.text.html.Option;
import java.util.List;

public record DictationQuestion (
        Long wordId,
        String spelling,
        String phonetic,
        String meaning,
        List<Option> options    //听写汉语的四个选项，听写英语时为空列表
){
    //选项。显示的是某个单词的释义，提交的是这个单词的id
    public record Option(Long wordId, String meaning){

    }
}
