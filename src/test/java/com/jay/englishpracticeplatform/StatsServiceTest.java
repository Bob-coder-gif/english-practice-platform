package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.dto.StatsView;
import com.jay.englishpracticeplatform.entity.AnswerMode;
import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.entity.WordLevel;
import com.jay.englishpracticeplatform.service.StatsService;
import com.jay.englishpracticeplatform.service.StudyService;
import com.jay.englishpracticeplatform.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class StatsServiceTest {

    @Autowired
    private StatsService statsService;

    @Autowired
    private StudyService studyService;

    @Autowired
    private UserService userService;

    @Test
    void newUserHasEmptyStatsWithoutErrors() {
        User user = userService.register("stats_new", "123456");

        StatsView stats = statsService.getStats(user.getId(), 7);

        // 没有任何答题记录时，各项都是 0，不会因为 sum 返回 null 而报错
        assertEquals(0, stats.todayTotal());
        assertEquals(0, stats.totalAnswers());
        assertEquals(0, stats.accuracyPercent());
        // 四种模式都在，即使都是 0
        assertEquals(AnswerMode.values().length, stats.todayByMode().size());
        // 7 天都在，即使都是 0
        assertEquals(7, stats.daily().size());
    }

    @Test
    void answersAreCountedByModeAndDay() {
        User user = userService.register("stats_tester", "123456");
        Word first = studyService.nextNewWord(user.getId(), WordLevel.CET4).orElseThrow();
        studyService.recordAnswer(user.getId(), first.getId(), true, AnswerMode.LEARN);
        Word second = studyService.nextNewWord(user.getId(), WordLevel.CET4).orElseThrow();
        studyService.recordAnswer(user.getId(), second.getId(), false, AnswerMode.LEARN);
        studyService.recordAnswer(user.getId(), second.getId(), true, AnswerMode.REVIEW);

        StatsView stats = statsService.getStats(user.getId(), 7);

        // 今日：学习 2 题（对 1），复习 1 题（对 1），共 3 题
        assertEquals(3, stats.todayTotal());
        StatsView.ModeStat learn = stats.todayByMode().stream()
                .filter(m -> m.mode() == AnswerMode.LEARN).findFirst().orElseThrow();
        assertEquals(2, learn.total());
        assertEquals(1, learn.correct());

        // 累计：3 题对 2 题，正确率 67%
        assertEquals(3, stats.totalAnswers());
        assertEquals(2, stats.correctAnswers());
        assertEquals(67, stats.accuracyPercent());

        // 学过 2 个单词，其中 1 个在错题本里
        assertEquals(2, stats.learnedCount());
        assertEquals(1, stats.mistakeCount());

        // 柱状图的最后一天是今天，有 3 题
        StatsView.DailyStat last = stats.daily().get(stats.daily().size() - 1);
        assertEquals(LocalDate.now(), last.day());
        assertEquals(3, last.total());
    }
}