package com.jay.englishpracticeplatform.dto;

import com.jay.englishpracticeplatform.entity.AnswerMode;
import com.jay.englishpracticeplatform.entity.WordLevel;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public record StatsView(
        List<ModeStat> todayByMode,
        long todayTotal,
        long learnedCount,
        long dueCount,
        long mistakeCount,
        long totalAnswers,
        long correctAnswers,
        List<LevelProgress> levelProgress,
        List<DailyStat> daily
) {

    private static final DateTimeFormatter LABEL_FORMAT = DateTimeFormatter.ofPattern("MM-dd");

    //累计正确率（百分比，四舍五入）
    public int accuracyPercent() {
        return totalAnswers == 0 ? 0 : (int) Math.round(correctAnswers * 100.0 / totalAnswers);
    }

    //柱状图的横轴标签
    public List<String> dailyLabels() {
        return daily.stream().map(d -> d.day().format(LABEL_FORMAT)).toList();
    }

    //柱状图的数据
    public List<Long> dailyValues() {
        return daily.stream().map(DailyStat::total).toList();
    }

    //==================组成部分================
    public record ModeStat(AnswerMode mode, long total, long correct) {

    }

    public record LevelProgress(WordLevel level, long learned, long total) {
        public double percent() {
            // 学习进度的百分比，保留一位小数
            return total == 0 ? 0 : Math.round(learned * 1000.0 / total) / 10.0;
        }
    }

    public record DailyStat(LocalDate day, long total) {

    }

}
