package com.jay.englishpracticeplatform.service;

import com.jay.englishpracticeplatform.entity.UserWord;
import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.entity.WordLevel;
import com.jay.englishpracticeplatform.repository.AnswerRecordRepository;
import com.jay.englishpracticeplatform.repository.UserRepository;
import com.jay.englishpracticeplatform.repository.UserWordRepository;
import com.jay.englishpracticeplatform.repository.WordRepository;
import com.jay.englishpracticeplatform.entity.AnswerMode;
import com.jay.englishpracticeplatform.entity.AnswerRecord;
import com.jay.englishpracticeplatform.entity.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class StudyService {

    public static final int MISTAKES_PAGE_SIZE = 20;

    // 连续认识达到这个次数，就从错题本中移出
    public static final int MASTERED_STREAK = 3;

    private final WordRepository wordRepository;
    private final UserRepository userRepository;
    private final UserWordRepository userWordRepository;
    private final AnswerRecordRepository answerRecordRepository;

    public StudyService(WordRepository wordRepository,
                        UserRepository userRepository,
                        UserWordRepository userWordRepository,
                        AnswerRecordRepository answerRecordRepository) {
        this.wordRepository = wordRepository;
        this.userRepository = userRepository;
        this.userWordRepository = userWordRepository;
        this.answerRecordRepository = answerRecordRepository;
    }

    // 找出该用户在这个级别中，下一个还没学过的单词
    @Transactional(readOnly = true)
    public Optional<Word> nextNewWord(Long userId, WordLevel level) {
        List<Word> words = wordRepository.findNewWords(level, userId, PageRequest.of(0, 1));
        return words.isEmpty() ? Optional.empty() : Optional.of(words.get(0));
    }

    // 该用户在这个级别中，已经学过多少个单词
    @Transactional(readOnly = true)
    public long countLearned(Long userId, WordLevel level) {
        return userWordRepository.countLearnedByLevel(userId, level);
    }

    // 记录一次「认识」或「不认识」
    @Transactional
    public void recordAnswer(Long userId, Long wordId, boolean known, AnswerMode mode) {
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new IllegalArgumentException("单词不存在：" + wordId));
        User userRef = userRepository.getReferenceById(userId);

        //同一次答题只取一次当前时间，保证状态更新和答题记录的时间完全一致
        LocalDateTime now = LocalDateTime.now();

        // 更新单词的学习状态
        UserWord userWord = userWordRepository.findByUserIdAndWordId(userId,wordId)
                .orElseGet( () -> new UserWord(userRef, word));

        if( known){
            userWord.markKnown(now);
        }
        else {
            userWord.markUnknown(now);
        }
        userWordRepository.save(userWord);

        //写入答题记录
        answerRecordRepository.save(new AnswerRecord(userRef,word,mode,known,now));
    }

    // 复习：下一个到期的单词
    @Transactional(readOnly = true)
    public Optional<UserWord> nextDueWord(Long userId) {
        List<UserWord> due = userWordRepository.findDueWords(
                userId, LocalDateTime.now(), PageRequest.of(0, 1));
        return due.isEmpty() ? Optional.empty() : Optional.of(due.get(0));
    }

    // 复习：到期单词的数量
    @Transactional(readOnly = true)
    public long countDue(Long userId) {
        return userWordRepository.countByUserIdAndNextReviewAtLessThanEqual(userId, LocalDateTime.now());
    }

    // 错题本：按不认识的次数从多到少，次数相同的，最近学过的排在前面
    @Transactional(readOnly = true)
    public Page<UserWord> listMistakes(Long userId, int page) {
        int safePage = Math.max(page, 0);
        Sort sort = Sort.by(Sort.Order.desc("unknownCount"), Sort.Order.desc("lastReviewedAt"));
        return userWordRepository.findMistakes(userId, MASTERED_STREAK,
                PageRequest.of(safePage, MISTAKES_PAGE_SIZE, sort));
    }
}