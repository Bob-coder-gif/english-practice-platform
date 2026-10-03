package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.entity.UserWord;
import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.entity.WordLevel;
import com.jay.englishpracticeplatform.repository.UserWordRepository;
import com.jay.englishpracticeplatform.service.StudyService;
import com.jay.englishpracticeplatform.service.UserService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class StudyServiceTest {

    @Autowired
    private StudyService studyService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserWordRepository userWordRepository;

    @Test
    void learnedWordIsNotShownAgainAsNewWord() {
        User user = userService.register("study_tester", "123456");

        Word first = studyService.nextNewWord(user.getId(), WordLevel.CET4).orElseThrow();
        studyService.recordAnswer(user.getId(), first.getId(), true);

        Word second = studyService.nextNewWord(user.getId(), WordLevel.CET4).orElseThrow();

        assertNotEquals(first.getId(), second.getId());
        assertEquals(1, studyService.countLearned(user.getId(), WordLevel.CET4));
    }

    @Test
    void answeringSameWordTwiceUpdatesOneRecord() {
        User user = userService.register("study_tester2", "123456");
        Word word = studyService.nextNewWord(user.getId(), WordLevel.CET4).orElseThrow();

        studyService.recordAnswer(user.getId(), word.getId(), true);
        studyService.recordAnswer(user.getId(), word.getId(), false);

        UserWord record = userWordRepository.findByUserIdAndWordId(user.getId(), word.getId()).orElseThrow();
        assertEquals(1, record.getKnownCount());
        assertEquals(1, record.getUnknownCount());
        assertEquals(0, record.getStreak());
    }

    @Test
    void unknownWordIsDueForReviewImmediately() {
        User user = userService.register("review_tester", "123456");
        Word word = studyService.nextNewWord(user.getId(), WordLevel.CET4).orElseThrow();

        studyService.recordAnswer(user.getId(), word.getId(), false);

        UserWord due = studyService.nextDueWord(user.getId()).orElseThrow();
        assertEquals(word.getId(), due.getWord().getId());
        assertEquals(1, studyService.countDue(user.getId()));
    }

    @Test
    void knownWordIsNotDueToday() {
        User user = userService.register("review_tester2", "123456");
        Word word = studyService.nextNewWord(user.getId(), WordLevel.CET4).orElseThrow();

        studyService.recordAnswer(user.getId(), word.getId(), true);

        assertTrue(studyService.nextDueWord(user.getId()).isEmpty());
        assertEquals(0, studyService.countDue(user.getId()));
    }

    @Test
    void onlyUnknownWordsAppearInMistakes() {
        User user = userService.register("mistake_tester", "123456");
        Word first = studyService.nextNewWord(user.getId(), WordLevel.CET4).orElseThrow();
        studyService.recordAnswer(user.getId(), first.getId(), true);
        Word second = studyService.nextNewWord(user.getId(), WordLevel.CET4).orElseThrow();
        studyService.recordAnswer(user.getId(), second.getId(), false);

        var mistakes = studyService.listMistakes(user.getId(), 0);

        assertEquals(1, mistakes.getTotalElements());
        assertEquals(second.getId(), mistakes.getContent().get(0).getWord().getId());
    }

    @Test
    void wordLeavesMistakesAfterConsecutiveKnownAndReturnsWhenForgotten() {
        User user = userService.register("mistake_tester2", "123456");
        Word word = studyService.nextNewWord(user.getId(), WordLevel.CET4).orElseThrow();

        // 不认识 → 进入错题本
        studyService.recordAnswer(user.getId(), word.getId(), false);
        assertEquals(1, studyService.listMistakes(user.getId(), 0).getTotalElements());

        // 连续认识，还差一次时依然在错题本里
        for (int i = 0; i < StudyService.MASTERED_STREAK - 1; i++) {
            studyService.recordAnswer(user.getId(), word.getId(), true);
        }
        assertEquals(1, studyService.listMistakes(user.getId(), 0).getTotalElements());

        // 达到连续认识次数 → 移出错题本
        studyService.recordAnswer(user.getId(), word.getId(), true);
        assertEquals(0, studyService.listMistakes(user.getId(), 0).getTotalElements());

        // 学习记录依然存在，没有被删除
        assertTrue(userWordRepository.findByUserIdAndWordId(user.getId(), word.getId()).isPresent());

        // 又不认识了 → 回到错题本
        studyService.recordAnswer(user.getId(), word.getId(), false);
        assertEquals(1, studyService.listMistakes(user.getId(), 0).getTotalElements());
    }
}