package com.jay.englishpracticeplatform.dto;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

public record CheckInView(
        long todayCount,          // 今天答了几题
        int goal,                 // 该用户的每日目标
        boolean checkedInToday,   // 今天是否已打卡
        int currentStreak,        // 连续打卡天数
        long totalDays,           // 累计打卡天数
        YearMonth month,          // 日历显示的月份
        YearMonth prevMonth,      // 上个月（不能再往前时为 null）
        YearMonth nextMonth,      // 下个月（不能再往后时为 null）
        List<List<CalendarDay>> weeks
) {

    // 今日进度百分比，可以超过 100，比如目标 20、答了 40 题就是 200
    public int progressPercent() {
        return goal == 0 ? 0 : (int) (todayCount * 100 / goal);
    }

    // 进度条的宽度，最多 100
    public int progressBarPercent() {
        return Math.min(100, progressPercent());
    }

    // 是否已经达到今日目标
    public boolean goalReached() {
        return todayCount >= goal;
    }

    // 距离目标还差几题
    public long remaining() {
        return Math.max(0, goal - todayCount);
    }

    public record CalendarDay(
            LocalDate date,
            boolean inMonth,
            long count,
            int level,
            boolean checkedIn,
            boolean today,
            boolean future
    ) {
    }
}