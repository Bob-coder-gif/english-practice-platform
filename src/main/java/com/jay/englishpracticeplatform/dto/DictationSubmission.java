package com.jay.englishpracticeplatform.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DictationSubmission {

    //第 i 道题单词的 id
    private List<Long> wordIds = new ArrayList<>();

    //第 i 道题的答案，听写英语输入的拼写，听写汉语是选中选项的单词 id
    private List<String> answers = new ArrayList<>();
}
