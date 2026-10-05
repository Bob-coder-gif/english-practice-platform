package com.jay.englishpracticeplatform.dto;

import com.jay.englishpracticeplatform.entity.AnswerMode;


import java.io.Serializable;
import java.util.List;

public record DictationResult(AnswerMode mode, List<Item> items) implements Serializable {

    public long correctCount(){
        return items.stream().filter(Item::correct).count();
    }

    public int total(){
        return items.size();
    }

    public int percent(){
        return total() == 0 ? 0 : (int) Math.round(correctCount() * 100.0 / total());
    }

    //一道题的判卷结果
    public record Item(
        String spelling,
        String phonetic,
        String meaning,
        String userAnswer,       // 用户的答案，听写汉语是为选中的释义
        boolean correct
    ) implements Serializable{

    }
}
