package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.dto.CheckInView;
import com.jay.englishpracticeplatform.entity.AnswerMode;
import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.entity.WordLevel;
import com.jay.englishpracticeplatform.repository.CheckInRepository;
import com.jay.englishpracticeplatform.service.CheckInService;
import com.jay.englishpracticeplatform.service.StudyService;
import com.jay.englishpracticeplatform.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CheckInServiceTest {

    @Autowired
    private CheckInService checkInService;

    @Autowired
    private StudyService studyService;

    @Autowired
    private UserService userService;

    @Autowired
    private CheckInRepository checkInRepository;

    // 让某个用户今天答 n 道题
    private void answer(User user, int n) {
        Word word = studyService.nextNewWord(user.getId(), WordLevel.CET4).orElseThrow();
        for (int i = 0; i < n; i++) {
            studyService.recordAnswer(user.getId(), word.getId(), true, AnswerMode.REVIEW);
        }
    }

    private boolean checkedInToday(User user) {
        return checkInRepository.existsByUserIdAndCheckDate(user.getId(), LocalDate.now());
    }

    @Test
    void newUserHasDefaultGoal() {
        User user = userService.register("goal_default", "123456");
        assertEquals(User.DEFAULT_DAILY_GOAL, user.getDailyGoal());
    }

    @Test
    void checkInHappensExactlyWhenGoalIsReached() {
        User user = userService.register("checkin_tester", "123456");

        answer(user, User.DEFAULT_DAILY_GOAL - 1);
        assertFalse(checkedInToday(user));

        answer(user, 1);
        assertTrue(checkedInToday(user));

        // 继续答题：不会重复打卡
        answer(user, 5);
        assertEquals(1, checkInRepository.countByUserId(user.getId()));
    }

    @Test
    void loweringGoalChecksInImmediately() {
        User user = userService.register("goal_lower", "123456");
        answer(user, 10);
        assertFalse(checkedInToday(user));

        // 目标从 20 降到 10，今天已经答了 10 题，应该立即打卡
        checkInService.updateDailyGoal(user.getId(), 10);
        assertTrue(checkedInToday(user));
    }

    @Test
    void raisingGoalKeepsTodaysCheckIn() {
        User user = userService.register("goal_raise", "123456");
        answer(user, User.DEFAULT_DAILY_GOAL);
        assertTrue(checkedInToday(user));

        // 已经打卡后把目标提高：打卡不会被撤销
        checkInService.updateDailyGoal(user.getId(), 50);
        assertTrue(checkedInToday(user));
    }

    @Test
    void progressCanExceedOneHundredPercent() {
        User user = userService.register("goal_exceed", "123456");
        checkInService.updateDailyGoal(user.getId(), 10);
        answer(user, 20);

        CheckInView view = checkInService.getView(user.getId(), YearMonth.now());

        assertEquals(200, view.progressPercent());
        assertEquals(100, view.progressBarPercent());
        assertTrue(view.goalReached());
    }

    @Test
    void goalOutOfRangeIsRejected() {
        User user = userService.register("goal_invalid", "123456");

        assertThrows(IllegalArgumentException.class,
                () -> checkInService.updateDailyGoal(user.getId(), CheckInService.MIN_DAILY_GOAL - 1));
        assertThrows(IllegalArgumentException.class,
                () -> checkInService.updateDailyGoal(user.getId(), CheckInService.MAX_DAILY_GOAL + 1));
    }

    @Test
    void viewShowsTodayProgressAndCalendar() {
        User user = userService.register("checkin_view", "123456");
        answer(user, 5);

        CheckInView view = checkInService.getView(user.getId(), YearMonth.now());

        assertEquals(5, view.todayCount());
        assertFalse(view.checkedInToday());
        assertEquals(0, view.currentStreak());
        assertEquals(User.DEFAULT_DAILY_GOAL - 5, view.remaining());
        view.weeks().forEach(week -> assertEquals(7, week.size()));
        assertNull(view.prevMonth());
        assertNull(view.nextMonth());
    }
}