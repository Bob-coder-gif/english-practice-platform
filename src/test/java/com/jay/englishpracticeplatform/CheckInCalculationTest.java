package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.service.CheckInService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckInCalculationTest {

    private final LocalDate today = LocalDate.of(2026, 10, 5);

    // ==================== 当前连续天数 ====================

    @Test
    void noCheckInMeansZeroStreak() {
        assertEquals(0, CheckInService.calculateStreak(List.of(), today));
    }

    @Test
    void streakCountsBackFromToday() {
        var dates = List.of(today, today.minusDays(1), today.minusDays(2));
        assertEquals(3, CheckInService.calculateStreak(dates, today));
    }

    @Test
    void streakStartsFromYesterdayWhenTodayNotCheckedIn() {
        var dates = List.of(today.minusDays(1), today.minusDays(2));
        assertEquals(2, CheckInService.calculateStreak(dates, today));
    }

    @Test
    void streakStopsAtGap() {
        var dates = List.of(today, today.minusDays(1), today.minusDays(3));
        assertEquals(2, CheckInService.calculateStreak(dates, today));
    }

    @Test
    void streakIsZeroWhenLastCheckInWasTwoDaysAgo() {
        var dates = List.of(today.minusDays(2), today.minusDays(3));
        assertEquals(0, CheckInService.calculateStreak(dates, today));
    }

    // ==================== 最长连续天数 ====================

    @Test
    void longestStreakOfEmptyIsZero() {
        assertEquals(0, CheckInService.calculateLongestStreak(List.of()));
    }

    @Test
    void longestStreakFindsTheLongestRun() {
        // 10-01～10-03 连续 3 天，10-05～10-06 连续 2 天
        var dates = List.of(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 3),
                LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 6));
        assertEquals(3, CheckInService.calculateLongestStreak(dates));
    }

    @Test
    void longestStreakIgnoresOrder() {
        // 顺序打乱，结果应该一样
        var dates = List.of(
                LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 3),
                LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 5));
        assertEquals(3, CheckInService.calculateLongestStreak(dates));
    }

    @Test
    void longestStreakAcrossMonthBoundary() {
        // 跨月：9-30 和 10-01 是连续的
        var dates = List.of(LocalDate.of(2026, 9, 29), LocalDate.of(2026, 9, 30), LocalDate.of(2026, 10, 1));
        assertEquals(3, CheckInService.calculateLongestStreak(dates));
    }

    // ==================== 颜色深浅 ====================

    @Test
    void heatLevelBoundaries() {
        assertEquals(0, CheckInService.heatLevel(0));
        assertEquals(1, CheckInService.heatLevel(1));
        assertEquals(1, CheckInService.heatLevel(19));
        assertEquals(2, CheckInService.heatLevel(20));
        assertEquals(2, CheckInService.heatLevel(39));
        assertEquals(3, CheckInService.heatLevel(40));
        assertEquals(3, CheckInService.heatLevel(79));
        assertEquals(4, CheckInService.heatLevel(80));
        assertEquals(4, CheckInService.heatLevel(500));
    }
}