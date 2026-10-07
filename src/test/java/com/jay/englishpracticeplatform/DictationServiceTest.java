package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.dto.*;
import com.jay.englishpracticeplatform.entity.*;
import com.jay.englishpracticeplatform.repository.AnswerRecordRepository;
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
    @Autowired private AnswerRecordRepository answerRecordRepository;

    private List<Long> idsOf(List<DictationQuestion> questions) {
        return questions.stream().map(DictationQuestion::wordId).toList();
    }

    private DictationSubmission submissionOf(List<DictationQuestion> questions) {
        DictationSubmission submission = new DictationSubmission();
        submission.setLevel(WordLevel.CET4);
        questions.forEach(q -> submission.getWordIds().add(q.wordId()));
        return submission;
    }

    @Test
    void normalizeIgnoresCaseAndExtraSpaces() {
        assertEquals("account for", DictationService.normalize("  Account   FOR  "));
        assertEquals("", DictationService.normalize(null));
    }

    @Test
    void pickWordsReturnsDistinctWordsWithoutOptions() {
        User user = userService.register("dict_pick", "123456");

        List<DictationQuestion> words =
                dictationService.pickWords(user.getId(), WordLevel.CET4, DictationSource.ALL, 20);

        assertEquals(20, words.size());
        assertEquals(20, new HashSet<>(idsOf(words)).size());
        assertTrue(words.get(0).options().isEmpty());
    }

    @Test
    void newUserHasNoMistakesToDictate() {
        User user = userService.register("dict_empty", "123456");

        assertTrue(dictationService
                .pickWords(user.getId(), WordLevel.CET4, DictationSource.MISTAKES, 20)
                .isEmpty());
    }

    @Test
    void buildTestUsesSameWordsAsPreview() {
        User user = userService.register("dict_same", "123456");
        List<DictationQuestion> preview =
                dictationService.pickWords(user.getId(), WordLevel.CET4, DictationSource.ALL, 20);

        List<DictationQuestion> test = dictationService.buildTest(idsOf(preview), WordLevel.CET4, false);

        // 题目是同一组单词（顺序可能被打乱）
        assertEquals(new HashSet<>(idsOf(preview)), new HashSet<>(idsOf(test)));
    }

    @Test
    void chineseTestHasFourDistinctOptionsIncludingAnswer() {
        User user = userService.register("dict_options", "123456");
        List<DictationQuestion> preview =
                dictationService.pickWords(user.getId(), WordLevel.CET4, DictationSource.ALL, 20);

        List<DictationQuestion> test = dictationService.buildTest(idsOf(preview), WordLevel.CET4, true);

        for (DictationQuestion q : test) {
            List<Long> optionIds = q.options().stream().map(DictationQuestion.Option::wordId).toList();
            assertEquals(4, optionIds.size());
            assertEquals(4, new HashSet<>(optionIds).size());
            assertTrue(optionIds.contains(q.wordId()));
        }
    }

    @Test
    void buildTestRejectsInvalidIds() {
        List<Long> tooMany = IntStream.rangeClosed(1, DictationService.MAX_QUESTIONS + 1)
                .mapToObj(i -> (long) i).toList();

        assertThrows(IllegalArgumentException.class,
                () -> dictationService.buildTest(tooMany, WordLevel.CET4, false));
        assertThrows(IllegalArgumentException.class,
                () -> dictationService.buildTest(List.of(-1L), WordLevel.CET4, false));
        assertThrows(IllegalArgumentException.class,
                () -> dictationService.buildTest(List.of(), WordLevel.CET4, false));
    }

    @Test
    void judgeEnglishRecordsEveryAnswerAndSupportsRetry() {
        User user = userService.register("dict_en", "123456");
        List<DictationQuestion> questions = dictationService.buildTest(
                idsOf(dictationService.pickWords(user.getId(), WordLevel.CET4, DictationSource.ALL, 20)),
                WordLevel.CET4, false);

        DictationSubmission submission = submissionOf(questions);
        submission.getAnswers().add("  " + questions.get(0).spelling().toUpperCase() + "  ");
        submission.getAnswers().add("wrong_answer");

        DictationResult result = dictationService.judgeEnglish(user.getId(), submission);

        assertEquals(20, result.total());
        assertEquals(1, result.correctCount());
        assertFalse(result.perfect());
        assertEquals(WordLevel.CET4, result.level());

        // 「只练答错的」：19 个 id，不包含答对的那一个
        List<String> wrongIds = List.of(result.wrongIds().split(","));
        assertEquals(19, wrongIds.size());
        assertFalse(wrongIds.contains(String.valueOf(questions.get(0).wordId())));

        // 每道题都写入了答题记录
        List<AnswerRecord> records = answerRecordRepository.findByUserIdOrderByIdAsc(user.getId());
        assertEquals(20, records.size());
        assertTrue(records.stream().allMatch(r -> r.getMode() == AnswerMode.DICTATION_EN));
        assertEquals(19, studyService.listMistakes(user.getId(), 0).getTotalElements());
    }

    @Test
    void judgeChineseUsesChosenOption() {
        User user = userService.register("dict_cn", "123456");
        List<DictationQuestion> questions = dictationService.buildTest(
                idsOf(dictationService.pickWords(user.getId(), WordLevel.CET4, DictationSource.ALL, 20)),
                WordLevel.CET4, true);

        DictationSubmission submission = submissionOf(questions);
        submission.getAnswers().add(String.valueOf(questions.get(0).wordId()));
        Long wrong = questions.get(1).options().stream()
                .map(DictationQuestion.Option::wordId)
                .filter(id -> !id.equals(questions.get(1).wordId()))
                .findFirst().orElseThrow();
        submission.getAnswers().add(String.valueOf(wrong));

        DictationResult result = dictationService.judgeChinese(user.getId(), submission);

        assertEquals(1, result.correctCount());
        assertTrue(result.items().get(0).correct());
        assertFalse(result.items().get(1).correct());
        assertFalse(result.items().get(1).userAnswer().isEmpty());
    }

    @Test
    void perfectResultHasNoWrongIds() {
        User user = userService.register("dict_perfect", "123456");
        List<DictationQuestion> questions = dictationService.buildTest(
                idsOf(dictationService.pickWords(user.getId(), WordLevel.CET4, DictationSource.ALL, 20)),
                WordLevel.CET4, false);

        DictationSubmission submission = submissionOf(questions);
        questions.forEach(q -> submission.getAnswers().add(q.spelling()));

        DictationResult result = dictationService.judgeEnglish(user.getId(), submission);

        assertTrue(result.perfect());
        assertEquals(100, result.percent());
        assertEquals("", result.wrongIds());
    }
}