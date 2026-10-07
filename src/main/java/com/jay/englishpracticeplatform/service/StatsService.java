package com.jay.englishpracticeplatform.service;

import com.jay.englishpracticeplatform.dto.StatsView;
import com.jay.englishpracticeplatform.dto.StatsView.DailyStat;
import com.jay.englishpracticeplatform.dto.StatsView.LevelProgress;
import com.jay.englishpracticeplatform.dto.StatsView.ModeStat;
import com.jay.englishpracticeplatform.entity.AnswerMode;
import com.jay.englishpracticeplatform.entity.WordLevel;
import com.jay.englishpracticeplatform.repository.AnswerRecordRepository;
import com.jay.englishpracticeplatform.repository.UserWordRepository;
import com.jay.englishpracticeplatform.repository.WordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class StatsService {
    //柱状图允许的天数
    public static final Set<Integer> ALLOWED_DAYS = Set.of(7, 30);
    public static final int DEFAULT_DAYS = 7;

    private final AnswerRecordRepository answerRecordRepository;
    private final UserWordRepository userWordRepository;
    private final WordRepository wordRepository;

    public StatsService(AnswerRecordRepository answerRecordRepository,
                        UserWordRepository userWordRepository,
                        WordRepository wordRepository) {
        this.answerRecordRepository = answerRecordRepository;
        this.userWordRepository = userWordRepository;
        this.wordRepository = wordRepository;
    }

    @Transactional(readOnly = true)
    public StatsView getStats(Long userId, int days) {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

        //今天各模式的大体书，献给各模式初始化0 然后再用查询数据覆盖
        Map<AnswerMode, ModeStat> byMode = new EnumMap<>(AnswerMode.class);
        for (AnswerMode mode : AnswerMode.values()) {
            byMode.put(mode, new ModeStat(mode, 0, 0));
        }
        for (var row : answerRecordRepository.countByMode(userId, todayStart, tomorrowStart)) {
            byMode.put(row.getMode(), new ModeStat(row.getMode(), row.getTotal(), row.getCorrect()));
        }
        long todayTotal = 0;
        for (ModeStat stat : byMode.values()) {
            todayTotal += stat.total();
        }

        //最近N天每天的答题数 ， 没有答题的日子，查询结果不会出现，补0
        LocalDate firstDay = today.minusDays(days - 1);
        Map<LocalDate, Long> countsByDay = new HashMap<>();
        for (var row : answerRecordRepository.countByDay(userId, firstDay.atStartOfDay(), tomorrowStart)) {
            countsByDay.put(row.getAnswerDate(), row.getTotal());
        }
        List<DailyStat> daily = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            LocalDate day = firstDay.plusDays(i);
            daily.add(new DailyStat(day, countsByDay.getOrDefault(day, 0L)));
        }

        //累计答题数和答对数 没有任何答题时，sum的结果为null
        var summary = answerRecordRepository.summarize(userId);
        long totalAnswers = summary.getTotal();
        long correctAnswers = summary.getCorrect() == null ? 0 : summary.getCorrect();

        //各级别的学习进度
        List<LevelProgress> levelProgress = new ArrayList<>();
        for (WordLevel level : WordLevel.values()) {
            levelProgress.add(new LevelProgress(
                    level,
                    userWordRepository.countLearnedByLevel(userId, level),
                    wordRepository.countByLevel(level)
            ));
        }

        return new StatsView(
                new ArrayList<>(byMode.values()),
                todayTotal,
                userWordRepository.countByUserId(userId),
                userWordRepository.countByUserIdAndNextReviewAtLessThanEqual(userId, LocalDateTime.now()),
                userWordRepository.countMistakes(userId, StudyService.MASTERED_STREAK),
                totalAnswers,
                correctAnswers,
                levelProgress,
                daily
        );
    }

}
