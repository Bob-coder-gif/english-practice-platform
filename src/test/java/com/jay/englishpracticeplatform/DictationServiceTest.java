package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.dto.*;
import com.jay.englishpracticeplatform.entity.*;
import com.jay.englishpracticeplatform.repository.AnswerRecordRepository;
import com.jay.englishpracticeplatform.repository.UserWordRepository;
import com.jay.englishpracticeplatform.service.DictationService;
import com.jay.englishpracticeplatform.service.StudyService;
import com.jay.englishpracticeplatform.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DictationServiceTest {

    @Autowired private DictationService dictationService;
    @Autowired private StudyService studyService;
    @Autowired private UserService userService;
    @Autowired private UserWordRepository userWordRepository;
    @Autowired private AnswerRecordRepository answerRecordRepository;

    // 把题目的单词 id 放进提交对象
    private DictationSubmission submissionOf(List<DictationQuestion> questions) {
        DictationSubmission submission = new DictationSubmission();
        questions.forEach(q -> submission.getWordIds().add(q.wordId()));
        return submission;
    }

    @Test
    void normalizeIgnoresCaseAndExtraSpaces() {
        assertEquals("account for", DictationService.normalize("  Account   FOR  "));
        assertEquals("", DictationService.normalize(null));
    }

    @Test
    void generateReturnsRequestedNumberOfDistinctWords() {
        User user = userService.register("dict_gen", "123456");

        List<DictationQuestion> questions =
                dictationService.generate(user.getId(), WordLevel.CET4, DictationSource.ALL, 20, false);

        assertEquals(20, questions.size());
        assertEquals(20, new HashSet<>(questions.stream().map(DictationQuestion::wordId).toList()).size());
        assertTrue(questions.get(0).options().isEmpty());
    }

    @Test
    void chineseQuestionsHaveFourDistinctOptionsIncludingAnswer() {
        User user = userService.register("dict_options", "123456");

        List<DictationQuestion> questions =
                dictationService.generate(user.getId(), WordLevel.CET4, DictationSource.ALL, 20, true);

        for (DictationQuestion q : questions) {
            List<Long> optionIds = q.options().stream().map(DictationQuestion.Option::wordId).toList();
            assertEquals(4, optionIds.size());
            assertEquals(4, new HashSet<>(optionIds).size());
            assertTrue(optionIds.contains(q.wordId()));
        }
    }

    @Test
    void newUserHasNoMistakesToDictate() {
        User user = userService.register("dict_empty", "123456");

        assertTrue(dictationService
                .generate(user.getId(), WordLevel.CET4, DictationSource.MISTAKES, 20, false)
                .isEmpty());
    }

    @Test
    void judgeEnglishRecordsEveryAnswer() {
        User user = userService.register("dict_en", "123456");
        List<DictationQuestion> questions =
                dictationService.generate(user.getId(), WordLevel.CET4, DictationSource.ALL, 20, false);

        DictationSubmission submission = submissionOf(questions);
        submission.getAnswers().add("  " + questions.get(0).spelling().toUpperCase() + "  ");  // 大小写、空格不同：算对
        submission.getAnswers().add("wrong_answer");                                           // 写错了
        // 其余 18 题没有作答

        DictationResult result = dictationService.judgeEnglish(user.getId(), submission);

        assertEquals(20, result.total());
        assertEquals(1, result.correctCount());

        // 每道题都写入了答题记录，模式是听写英语
        List<AnswerRecord> records = answerRecordRepository.findByUserIdOrderByIdAsc(user.getId());
        assertEquals(20, records.size());
        assertTrue(records.stream().allMatch(r -> r.getMode() == AnswerMode.DICTATION_EN));

        // 答错和没作答的 19 个单词，都进入了错题本
        assertEquals(19, studyService.listMistakes(user.getId(), 0).getTotalElements());
    }

    @Test
    void judgeChineseUsesChosenOption() {
        User user = userService.register("dict_cn", "123456");
        List<DictationQuestion> questions =
                dictationService.generate(user.getId(), WordLevel.CET4, DictationSource.ALL, 20, true);

        DictationSubmission submission = submissionOf(questions);
        // 第 1 题选正确答案
        submission.getAnswers().add(String.valueOf(questions.get(0).wordId()));
        // 第 2 题选一个错误的选项
        Long wrong = questions.get(1).options().stream()
                .map(DictationQuestion.Option::wordId)
                .filter(id -> !id.equals(questions.get(1).wordId()))
                .findFirst().orElseThrow();
        submission.getAnswers().add(String.valueOf(wrong));

        DictationResult result = dictationService.judgeChinese(user.getId(), submission);

        assertEquals(1, result.correctCount());
        assertTrue(result.items().get(0).correct());
        assertFalse(result.items().get(1).correct());
        // 错误的那题，结果里显示的是用户选中的那个释义
        assertFalse(result.items().get(1).userAnswer().isEmpty());
    }

    @Test
    void tooManyQuestionsAreRejected() {
        User user = userService.register("dict_limit", "123456");
        DictationSubmission submission = new DictationSubmission();
        IntStream.rangeClosed(1, DictationService.MAX_QUESTIONS + 1)
                .forEach(i -> submission.getWordIds().add((long) i));

        assertThrows(IllegalArgumentException.class,
                () -> dictationService.judgeEnglish(user.getId(), submission));
    }
}