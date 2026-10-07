package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.entity.*;
import com.jay.englishpracticeplatform.repository.AnswerRecordRepository;
import com.jay.englishpracticeplatform.repository.UserWordRepository;
import com.jay.englishpracticeplatform.service.StudyService;
import com.jay.englishpracticeplatform.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Autowired
    private AnswerRecordRepository answerRecordRepository;

    @Test
    void learnedWordIsNotShownAgainAsNewWord() {
        User user = userService.register("study_tester", "123456");

        Word first = studyService.nextNewWord(user.getId(), WordLevel.CET4).orElseThrow();
        studyService.recordAnswer(user.getId(), first.getId(), true, AnswerMode.LEARN);

        Word second = studyService.nextNewWord(user.getId(), WordLevel.CET4).orElseThrow();

        assertNotEquals(first.getId(), second.getId());
        assertEquals(1, studyService.countLearned(user.getId(), WordLevel.CET4));
    }

    @Test
    void answeringSameWordTwiceUpdatesOneRecord() {
        User user = userService.register("study_tester2", "123456");
        Word word = studyService.nextNewWord(user.getId(), WordLevel.CET4).orElseThrow();

        studyService.recordAnswer(user.getId(), word.getId(), true, AnswerMode.LEARN);
        studyService.recordAnswer(user.getId(), word.getId(), false, AnswerMode.REVIEW);

        UserWord record = userWordRepository.findByUserIdAndWordId(user.getId(), word.getId()).orElseThrow();
        assertEquals(1, record.getKnownCount());
        assertEquals(1, record.getUnknownCount());
        assertEquals(0, record.getStreak());
    }

    @Test
    void unknownWordIsDueForReviewImmediately() {
        User user = userService.register("review_tester", "123456");
        Word word = studyService.nextNewWord(user.getId(), WordLevel.CET4).orElseThrow();

        studyService.recordAnswer(user.getId(), word.getId(), false, AnswerMode.LEARN);

        UserWord due = studyService.nextDueWord(user.getId()).orElseThrow();
        assertEquals(word.getId(), due.getWord().getId());
        assertEquals(1, studyService.countDue(user.getId()));
    }

    @Test
    void knownWordIsNotDueToday() {
        User user = userService.register("review_tester2", "123456");
        Word word = studyService.nextNewWord(user.getId(), WordLevel.CET4).orElseThrow();

        studyService.recordAnswer(user.getId(), word.getId(), true, AnswerMode.LEARN);

        assertTrue(studyService.nextDueWord(user.getId()).isEmpty());
        assertEquals(0, studyService.countDue(user.getId()));
    }

    @Test
    void onlyUnknownWordsAppearInMistakes() {
        User user = userService.register("mistake_tester", "123456");
        Word first = studyService.nextNewWord(user.getId(), WordLevel.CET4).orElseThrow();
        studyService.recordAnswer(user.getId(), first.getId(), true, AnswerMode.LEARN);
        Word second = studyService.nextNewWord(user.getId(), WordLevel.CET4).orElseThrow();
        studyService.recordAnswer(user.getId(), second.getId(), false, AnswerMode.LEARN);

        var mistakes = studyService.listMistakes(user.getId(), 0);

        assertEquals(1, mistakes.getTotalElements());
        assertEquals(second.getId(), mistakes.getContent().get(0).getWord().getId());
    }

    @Test
    void wordLeavesMistakesAfterConsecutiveKnownAndReturnsWhenForgotten() {
        User user = userService.register("mistake_tester2", "123456");
        Word word = studyService.nextNewWord(user.getId(), WordLevel.CET4).orElseThrow();

        // 学习时不认识 → 进入错题本
        studyService.recordAnswer(user.getId(), word.getId(), false, AnswerMode.LEARN);
        assertEquals(1, studyService.listMistakes(user.getId(), 0).getTotalElements());

        // 复习时连续认识，还差一次时依然在错题本里
        for (int i = 0; i < StudyService.MASTERED_STREAK - 1; i++) {
            studyService.recordAnswer(user.getId(), word.getId(), true, AnswerMode.REVIEW);
        }
        assertEquals(1, studyService.listMistakes(user.getId(), 0).getTotalElements());

        // 达到连续认识次数 → 移出错题本
        studyService.recordAnswer(user.getId(), word.getId(), true, AnswerMode.REVIEW);
        assertEquals(0, studyService.listMistakes(user.getId(), 0).getTotalElements());

        // 学习记录依然存在，没有被删除
        assertTrue(userWordRepository.findByUserIdAndWordId(user.getId(), word.getId()).isPresent());

        // 复习时又不认识了 → 回到错题本
        studyService.recordAnswer(user.getId(), word.getId(), false, AnswerMode.REVIEW);
        assertEquals(1, studyService.listMistakes(user.getId(), 0).getTotalElements());
    }

    @Test
    void everyAnswerIsRecordedWithModeAndResult() {
        User user = userService.register("record_tester", "123456");
        Word word = studyService.nextNewWord(user.getId(), WordLevel.CET4).orElseThrow();

        studyService.recordAnswer(user.getId(), word.getId(), false, AnswerMode.LEARN);
        studyService.recordAnswer(user.getId(), word.getId(), true, AnswerMode.REVIEW);

        // 同一个单词答了两次：学习状态只有一条，答题记录有两条
        List<AnswerRecord> records = answerRecordRepository.findByUserIdOrderByIdAsc(user.getId());
        assertEquals(2, records.size());

        assertEquals(AnswerMode.LEARN, records.get(0).getMode());
        assertFalse(records.get(0).isCorrect());

        assertEquals(AnswerMode.REVIEW, records.get(1).getMode());
        assertTrue(records.get(1).isCorrect());

        // 答题记录的时间和学习状态的最后复习时间一致
        UserWord userWord = userWordRepository.findByUserIdAndWordId(user.getId(), word.getId()).orElseThrow();
        assertEquals(userWord.getLastReviewedAt(), records.get(1).getAnsweredAt());
    }
}