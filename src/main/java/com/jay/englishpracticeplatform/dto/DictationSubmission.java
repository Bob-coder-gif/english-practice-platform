package com.jay.englishpracticeplatform.dto;

import com.jay.englishpracticeplatform.entity.WordLevel;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class DictationSubmission {

    //这次听写的级别，汉语听写[再来一次]是生成干扰项
    private WordLevel level = WordLevel.CET4;

    //第 i 道题单词的 id
    private List<Long> wordIds = new ArrayList<>();

    //第 i 道题的答案，听写英语输入的拼写，听写汉语是选中选项的单词 id
    private List<String> answers = new ArrayList<>();
}
