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
}