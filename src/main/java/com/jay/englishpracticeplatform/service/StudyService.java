package com.jay.englishpracticeplatform.service;

import com.jay.englishpracticeplatform.entity.UserWord;
import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.entity.UserWord;
import com.jay.englishpracticeplatform.entity.WordLevel;
import com.jay.englishpracticeplatform.repository.UserRepository;
import com.jay.englishpracticeplatform.repository.WordRepository;
import com.jay.englishpracticeplatform.repository.UserWordRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.text.html.Option;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class StudyService {

    private final WordRepository wordRepository;
    private final UserRepository userRepository;
    private final UserWordRepository userWordRepository;

    public StudyService(WordRepository wordRepository,
                        UserRepository userRepository,
                        UserWordRepository userWordRepository){
        this.wordRepository = wordRepository;
        this.userRepository = userRepository;
        this.userWordRepository = userWordRepository;
    }

    //找出该用户在这个级别中，下个还没有学过的单词
    @Transactional(readOnly = true)
    public Optional<Word> nextNewWord(Long userId, WordLevel level){
        List<Word> words = wordRepository.findNewWords(level , userId , PageRequest.of(0,1));
        return words.isEmpty() ? Optional.empty() : Optional.of(words.get(0));

    }

    //在这个等级中，已经学过多少个单词
    @Transactional(readOnly = true)
    public long countLearned(Long userId, WordLevel level){
        return userWordRepository.countLearnedByLevel(userId, level);
    }

    //记录一次 [认识] 或 [不认识]
    @Transactional
    public void recordAnswer(Long userId, Long wordId, boolean known){
        Word word = wordRepository.findById(wordId)
                .orElseThrow(() -> new IllegalArgumentException("单词不存在:" + wordId));

        //有记录就用已有的，没有记录新建一个
        UserWord userWord = userWordRepository.findByUserIdAndWordId(userId, wordId)
                .orElseGet( () -> new UserWord(userRepository.getReferenceById(userId),word));

        LocalDateTime now = LocalDateTime.now();
        if(known){
            userWord.markKnown(now);
        }
        else {
            userWord.markUnknow(now);
        }

        userWordRepository.save(userWord);
    }
}
