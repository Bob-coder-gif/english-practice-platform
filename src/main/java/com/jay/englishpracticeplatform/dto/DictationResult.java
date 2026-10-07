package com.jay.englishpracticeplatform.dto;

import com.jay.englishpracticeplatform.entity.AnswerMode;
import com.jay.englishpracticeplatform.entity.WordLevel;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

public record DictationResult(AnswerMode mode, WordLevel level, List<Item> items) implements Serializable {

    public long correctCount() {
        return items.stream().filter(Item::correct).count();
    }

    public int total() {
        return items.size();
    }

    public int percent() {
        return total() == 0 ? 0 : (int) Math.round(correctCount() * 100.0 / total());
    }

    public long wrongCount() {
        return total() - correctCount();
    }

    //是否全部答对
    public boolean perfect() {
        return total() > 0 && correctCount() == total();
    }

    //网址里的模式模式路径 en/cn
    public String modePath() {
        return mode == AnswerMode.DICTATION_EN ? "en" : "cn";
    }

    //[再听写一次] 这组所有单词的 id 用逗号连接
    public String allIds() {
        return items.stream()
                .map(item -> String.valueOf(item.wordId()))
                .collect(Collectors.joining(","));
    }

    // [只练答对的] 打错的单词的id
    public String wrongIds() {
        return items.stream()
                .filter(item -> !item.correct())
                .map(item -> String.valueOf(item.wordId()))
                .collect(Collectors.joining(","));
    }

    //一道题的判卷结果
    public record Item(
            Long wordId,
            String spelling,
            String phonetic,
            String meaning,
            String userAnswer,       // 用户的答案，听写汉语是为选中的释义
            boolean correct
    ) implements Serializable {

    }
}
