package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.service.CheckInService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CheckInCalculationTest {

    private final LocalDate today = LocalDate.of(2026, 10, 5);

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
        // 今天还没完成，但昨天和前天打卡了：连续天数不应该断
        var dates = List.of(today.minusDays(1), today.minusDays(2));
        assertEquals(2, CheckInService.calculateStreak(dates, today));
    }

    @Test
    void streakStopsAtGap() {
        // 10-05、10-04 连续，10-03 断开，10-02 不算
        var dates = List.of(today, today.minusDays(1), today.minusDays(3));
        assertEquals(2, CheckInService.calculateStreak(dates, today));
    }

    @Test
    void streakIsZeroWhenLastCheckInWasTwoDaysAgo() {
        var dates = List.of(today.minusDays(2), today.minusDays(3));
        assertEquals(0, CheckInService.calculateStreak(dates, today));
    }

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