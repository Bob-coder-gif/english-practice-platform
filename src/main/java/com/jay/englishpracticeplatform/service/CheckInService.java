package com.jay.englishpracticeplatform.service;

import com.jay.englishpracticeplatform.dto.CheckInView;
import com.jay.englishpracticeplatform.dto.CheckInView.CalendarDay;
import com.jay.englishpracticeplatform.entity.CheckIn;
import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.repository.AnswerRecordRepository;
import com.jay.englishpracticeplatform.repository.CheckInRepository;
import com.jay.englishpracticeplatform.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;

@Service
public class CheckInService {

    // 每日目标的允许范围
    public static final int MIN_DAILY_GOAL = 5;
    public static final int MAX_DAILY_GOAL = 200;

    // 日历颜色的固定档位：达到 1、20、40、80 题，分别进入第 1～4 档
    private static final int[] HEAT_THRESHOLDS = {1, 20, 40, 80};

    private final CheckInRepository checkInRepository;
    private final AnswerRecordRepository answerRecordRepository;
    private final UserRepository userRepository;

    public CheckInService(CheckInRepository checkInRepository,
                          AnswerRecordRepository answerRecordRepository,
                          UserRepository userRepository) {
        this.checkInRepository = checkInRepository;
        this.answerRecordRepository = answerRecordRepository;
        this.userRepository = userRepository;
    }

    // ==================== 自动打卡 ====================

    // 当天答题数达到该用户的目标、且还没打卡，就写入打卡记录
    @Transactional
    public void checkInIfGoalReached(Long userId, LocalDateTime at) {
        LocalDate day = at.toLocalDate();

        if (checkInRepository.existsByUserIdAndCheckDate(userId, day)) {
            return;
        }

        User user = userRepository.findById(userId).orElseThrow();
        long count = answerRecordRepository.countBetween(
                userId, day.atStartOfDay(), day.plusDays(1).atStartOfDay());

        if (count >= user.getDailyGoal()) {
            checkInRepository.save(new CheckIn(user, day, at));
        }
    }

    // ==================== 修改每日目标 ====================

    @Transactional
    public void updateDailyGoal(Long userId, int goal) {
        if (goal < MIN_DAILY_GOAL || goal > MAX_DAILY_GOAL) {
            throw new IllegalArgumentException(
                    "每日目标需要在 " + MIN_DAILY_GOAL + " 到 " + MAX_DAILY_GOAL + " 题之间");
        }

        User user = userRepository.findById(userId).orElseThrow();
        user.setDailyGoal(goal);

        // 目标降低后，今天可能已经达标了，立即重新检查一次
        checkInIfGoalReached(userId, LocalDateTime.now());
    }

    // ==================== 打卡页面 ====================

    @Transactional(readOnly = true)
    public YearMonth clampMonth(Long userId, YearMonth requested) {
        YearMonth current = YearMonth.now();
        YearMonth earliest = YearMonth.from(userRepository.findById(userId).orElseThrow().getCreatedAt());

        if (requested == null || requested.isAfter(current)) {
            return current;
        }
        if (requested.isBefore(earliest)) {
            return earliest;
        }
        return requested;
    }

    @Transactional(readOnly = true)
    public CheckInView getView(Long userId, YearMonth month) {
        LocalDate today = LocalDate.now();
        User user = userRepository.findById(userId).orElseThrow();

        // 今日进度
        long todayCount = answerRecordRepository.countBetween(
                userId, today.atStartOfDay(), today.plusDays(1).atStartOfDay());

        // 连续天数和累计天数
        List<LocalDate> allDates = checkInRepository.findAllDates(userId);
        boolean checkedInToday = allDates.contains(today);
        int streak = calculateStreak(allDates, today);

        // 这个月每天的答题数和打卡情况
        LocalDate firstDay = month.atDay(1);
        LocalDate nextMonthFirstDay = month.plusMonths(1).atDay(1);

        Map<LocalDate, Long> countsByDay = new HashMap<>();
        for (var row : answerRecordRepository.countByDay(
                userId, firstDay.atStartOfDay(), nextMonthFirstDay.atStartOfDay())) {
            countsByDay.put(row.getAnswerDate(), row.getTotal());
        }
        Set<LocalDate> checkDates = new HashSet<>(
                checkInRepository.findDatesBetween(userId, firstDay, nextMonthFirstDay));

        //  生成日历：从本月 1 号所在那一周的星期一开始，一周一行
        LocalDate cursor = firstDay.minusDays(firstDay.getDayOfWeek().getValue() - 1);
        List<List<CalendarDay>> weeks = new ArrayList<>();
        while (cursor.isBefore(nextMonthFirstDay)) {
            List<CalendarDay> week = new ArrayList<>(7);
            for (int i = 0; i < 7; i++) {
                long count = countsByDay.getOrDefault(cursor, 0L);
                week.add(new CalendarDay(
                        cursor,
                        YearMonth.from(cursor).equals(month),
                        count,
                        heatLevel(count),
                        checkDates.contains(cursor),
                        cursor.equals(today),
                        cursor.isAfter(today)));
                cursor = cursor.plusDays(1);
            }
            weeks.add(week);
        }

        // 上个月、下个月能不能翻
        YearMonth earliest = YearMonth.from(user.getCreatedAt());
        YearMonth prev = month.isAfter(earliest) ? month.minusMonths(1) : null;
        YearMonth next = month.isBefore(YearMonth.now()) ? month.plusMonths(1) : null;

        return new CheckInView(todayCount, user.getDailyGoal(), checkedInToday, streak,
                allDates.size(), month, prev, next, weeks);
    }

    // ==================== 计算 ====================

    // 连续打卡天数：今天打卡了就从今天往前数，否则从昨天往前数
    public static int calculateStreak(Collection<LocalDate> checkDates, LocalDate today) {
        Set<LocalDate> dates = new HashSet<>(checkDates);
        LocalDate cursor = dates.contains(today) ? today : today.minusDays(1);

        int streak = 0;
        while (dates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    // 颜色深浅：答题数每达到一个档位，等级加一，最高 4 级
    public static int heatLevel(long count) {
        int level = 0;
        for (int threshold : HEAT_THRESHOLDS) {
            if (count >= threshold) {
                level++;
            }
        }
        return level;
    }

    // 历史上最长的连续打卡天数
    public static int calculateLongestStreak(Collection<LocalDate> checkDates) {
        int longest = 0;
        int current = 0;
        LocalDate previous = null;

        // TreeSet：自动去重，并按日期从早到晚排序
        for (LocalDate date : new TreeSet<>(checkDates)) {
            if (previous != null && date.equals(previous.plusDays(1))) {
                current++;          // 和前一天相连，连续天数加一
            } else {
                current = 1;        // 断开了，从这一天重新开始计数
            }
            longest = Math.max(longest, current);
            previous = date;
        }
        return longest;
    }
}