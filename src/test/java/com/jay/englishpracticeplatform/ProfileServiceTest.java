package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.dto.ProfileView;
import com.jay.englishpracticeplatform.entity.AnswerMode;
import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.entity.WordLevel;
import com.jay.englishpracticeplatform.service.CheckInService;
import com.jay.englishpracticeplatform.service.ProfileService;
import com.jay.englishpracticeplatform.service.StudyService;
import com.jay.englishpracticeplatform.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ProfileServiceTest {

    @Autowired private ProfileService profileService;
    @Autowired private StudyService studyService;
    @Autowired private CheckInService checkInService;
    @Autowired private UserService userService;

    @Test
    void newUserProfileIsEmpty() {
        User user = userService.register("profile_new", "123456");

        ProfileView profile = profileService.getProfile(user.getId());

        assertEquals("profile_new", profile.username());
        assertEquals("P", profile.avatarText());
        assertEquals(LocalDate.now(), profile.registeredOn());
        assertEquals(1, profile.daysSinceRegistered());
        assertEquals(0, profile.learnedCount());
        assertEquals(0, profile.totalAnswers());
        assertEquals(0, profile.accuracyPercent());
        assertEquals(0, profile.currentStreak());
        assertEquals(0, profile.longestStreak());
        assertEquals(User.DEFAULT_DAILY_GOAL, profile.dailyGoal());
    }

    @Test
    void profileReflectsStudyAndCheckIn() {
        User user = userService.register("profile_active", "123456");
        checkInService.updateDailyGoal(user.getId(), CheckInService.MIN_DAILY_GOAL);

        Word word = studyService.nextNewWord(user.getId(), WordLevel.CET4).orElseThrow();
        studyService.recordAnswer(user.getId(), word.getId(), false, AnswerMode.LEARN);
        for (int i = 1; i < CheckInService.MIN_DAILY_GOAL; i++) {
            studyService.recordAnswer(user.getId(), word.getId(), true, AnswerMode.REVIEW);
        }

        ProfileView profile = profileService.getProfile(user.getId());

        assertEquals(1, profile.learnedCount());
        assertEquals(CheckInService.MIN_DAILY_GOAL, profile.totalAnswers());
        assertEquals(1, profile.checkInDays());
        assertEquals(1, profile.currentStreak());
        assertEquals(1, profile.longestStreak());
        assertEquals(CheckInService.MIN_DAILY_GOAL, profile.dailyGoal());
    }
}