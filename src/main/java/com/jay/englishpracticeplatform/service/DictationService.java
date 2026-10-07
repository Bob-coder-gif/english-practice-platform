package com.jay.englishpracticeplatform.service;


import com.jay.englishpracticeplatform.dto.DictationQuestion;
import com.jay.englishpracticeplatform.dto.DictationQuestion.Option;
import com.jay.englishpracticeplatform.dto.DictationResult;
import com.jay.englishpracticeplatform.dto.DictationSource;
import com.jay.englishpracticeplatform.dto.DictationSubmission;
import com.jay.englishpracticeplatform.entity.AnswerMode;
import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.entity.WordLevel;
import com.jay.englishpracticeplatform.repository.UserWordRepository;
import com.jay.englishpracticeplatform.repository.WordRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class DictationService {

    //每次听写可选的单词数量
    public static final List<Integer> COUNT_OPTIONS = List.of(20, 25, 30);
    public static final int DEFAULT_COUNT = 20;

    //一次提交最多允许多少道题
    public static final int MAX_QUESTIONS = 30;

    //听写汉语的选项数量
    private static final int OPTION_COUNT = 4;

    private final WordRepository wordRepository;
    private final UserWordRepository userWordRepository;
    private final StudyService studyService;

    public DictationService(WordRepository wordRepository,
                            UserWordRepository userWordRepository,
                            StudyService studyService) {
        this.wordRepository = wordRepository;
        this.userWordRepository = userWordRepository;
        this.studyService = studyService;
    }

    //====================预习，随机选出一组单词============

    @Transactional(readOnly = true)
    public List<DictationQuestion> pickWords(Long userId, WordLevel level, DictationSource source, int count) {
        List<Long> ids = switch (source) {
            case ALL -> wordRepository.findRandomIdsByLevel(level.name(), count);
            case LEARNED -> wordRepository.findRandomLearnedIds(userId, level.name(), count);
            case MISTAKES -> userWordRepository.findRandomMistakeWordIds(userId, StudyService.MASTERED_STREAK, count);
        };

        List<DictationQuestion> words = new ArrayList<>();
        for (Word word : findInOrder(ids)) {
            words.add(toQuestion(word, List.of()));
        }
        return words;
    }

    // ====================出题==========================

    public List<DictationQuestion> buildTest(List<Long> wordIds, WordLevel level, boolean withOptions) {
        // 去掉重复的 id ，保留原来的顺序
        List<Long> distinctIds = new ArrayList<>(new LinkedHashSet<>(wordIds));

        // 打乱题目顺序
        List<Word> words = new ArrayList<>(loadWords(distinctIds));
        Collections.shuffle(words);

        List<Word> pool = withOptions
                ? findInOrder(wordRepository.findRandomIdsByLevel(level.name(), words.size() * 3 + 10))
                : List.of();

        List<DictationQuestion> questions = new ArrayList<>();
        for (Word word : words) {
            questions.add(toQuestion(word, withOptions ? buildOptions(word, pool) : List.of()));
        }
        return questions;
    }

    private DictationQuestion toQuestion(Word word, List<Option> options) {
        return new DictationQuestion(word.getId(), word.getSpelling(), word.getPhonetic(), word.getMeaning(), options);
    }

    //生成四个选项，一个正确答案，三个干扰选项，再打乱顺序
    private List<Option> buildOptions(Word answer, List<Word> pool) {
        List<Word> candidates = new ArrayList<>(pool);
        Collections.shuffle(candidates);

        List<Option> options = new ArrayList<>();
        options.add(new Option(answer.getId(), answer.getMeaning()));

        for (Word candidate : candidates) {
            if (options.size() == OPTION_COUNT) {
                break;
            }
            //跳过题目单词本身，以及释义和已有选项完全相同的单词
            boolean duplicate = options.stream().anyMatch(o ->
                    o.wordId().equals(candidate.getId()) || o.meaning().equals(candidate.getMeaning()));
            if (!duplicate) {
                options.add(new Option(candidate.getId(), candidate.getMeaning()));
            }
        }

        Collections.shuffle(options);
        return options;
    }

    //===============判卷==============

    @Transactional
    public DictationResult judgeEnglish(Long userId, DictationSubmission submission) {
        List<Word> words = loadSubmittedWords(submission);

        List<DictationResult.Item> items = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            Word word = words.get(i);
            String answer = answerAt(submission, i);
            boolean correct = answer != null && normalize(answer).equals(normalize(word.getSpelling()));

            studyService.recordAnswer(userId, word.getId(), correct, AnswerMode.DICTATION_EN);
            items.add(new DictationResult.Item(word.getId(), word.getSpelling(), word.getPhonetic(), word.getMeaning(),
                    answer == null ? "" : answer.strip(), correct));
        }
        return new DictationResult(AnswerMode.DICTATION_EN, levelOf(submission), items);
    }

    @Transactional
    public DictationResult judgeChinese(Long userId, DictationSubmission submission) {
        List<Word> words = loadWords(submission.getWordIds());

        List<Long> chosenIds = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            Long chosen = parseId(answerAt(submission, i));
            if (chosen != null) {
                chosenIds.add(chosen);
            }
        }
        Map<Long, String> meaningById = new HashMap<>();
        for (Word w : wordRepository.findAllById(chosenIds)) {
            meaningById.put(w.getId(), w.getMeaning());
        }

        List<DictationResult.Item> items = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            Word word = words.get(i);
            Long chosen = parseId(answerAt(submission, i));
            boolean correct = word.getId().equals(chosen);

            studyService.recordAnswer(userId, word.getId(), correct, AnswerMode.DICTATION_CN);
            items.add(new DictationResult.Item(word.getId(), word.getSpelling(), word.getPhonetic(),
                    word.getMeaning(), chosen == null ? "" : meaningById.getOrDefault(chosen, ""), correct));
        }
        return new DictationResult(AnswerMode.DICTATION_CN, levelOf(submission), items);
    }

    //==============工具方法===================

    private List<Word> loadWords(List<Long> ids) {
        if (ids == null || ids.isEmpty() || ids.size() > MAX_QUESTIONS) {
            throw new IllegalArgumentException("题目数量不合法");
        }
        List<Word> words = findInOrder(ids);
        if (words.size() != ids.size()) {
            throw new IllegalArgumentException("题目中包含不存在的单词");
        }
        return words;
    }

    private WordLevel levelOf(DictationSubmission submission) {
        return submission.getLevel() == null ? WordLevel.CET4 : submission.getLevel();
    }

    //统一格式，起吊首尾空格，多个空格合并为一个，转为小写
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    //校验提交的题目，并按提交的顺序查出单词
    private List<Word> loadSubmittedWords(DictationSubmission submission) {
        int size = submission.getWordIds().size();
        if (size == 0 || size > MAX_QUESTIONS) {
            throw new IllegalArgumentException("题目数量不合法");
        }
        List<Word> words = findInOrder(submission.getWordIds());
        if (words.size() != size) {
            throw new IllegalArgumentException("提交的题目中包含不存在的单词");
        }

        return words;
    }

    //第 i 题的答案，没作答时可能没有提交这一项
    private String answerAt(DictationSubmission submission, int index) {
        List<String> answers = submission.getAnswers();
        return index < answers.size() ? answers.get(index) : null;
    }

    // findAllById 不保证返回顺序，这里按照ids 的顺序重新排序
    private List<Word> findInOrder(List<Long> ids) {
        Map<Long, Word> byId = new HashMap<>();
        for (Word word : wordRepository.findAllById(ids)) {
            byId.put(word.getId(), word);
        }
        List<Word> result = new ArrayList<>();
        for (Long id : ids) {
            Word word = byId.get(id);
            if (word != null) {
                result.add(word);
            }
        }
        return result;
    }

    //把选项的值转换为单词 id ，格式不对就当作没选
    private Long parseId(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(text.strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
