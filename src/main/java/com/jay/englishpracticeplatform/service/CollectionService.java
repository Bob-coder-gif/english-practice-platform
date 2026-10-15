package com.jay.englishpracticeplatform.service;

import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.entity.UserCollection;
import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.exception.WordCollectionException;
import com.jay.englishpracticeplatform.repository.UserCollectionRepository;
import com.jay.englishpracticeplatform.repository.UserRepository;
import com.jay.englishpracticeplatform.repository.WordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CollectionService {

    //每人最多收藏多少个单词
    public static final int MAX_SIZE = 600;

    public static final int PAGE_SIZE = 20;

    private final UserCollectionRepository collectionRepository;
    private final WordRepository wordRepository;
    private final UserRepository userRepository;
    private final WordService wordService;

    public CollectionService(UserCollectionRepository collectionRepository, WordRepository wordRepository, UserRepository userRepository, WordService wordService) {
        this.collectionRepository = collectionRepository;
        this.wordRepository = wordRepository;
        this.userRepository = userRepository;
        this.wordService = wordService;
    }

    //===============查询==============

    @Transactional(readOnly = true)
    public long count(Long userId) {
        return collectionRepository.countByUserId(userId);
    }

    //我的词库列别，时间顺序
    @Transactional(readOnly = true)
    public Page<UserCollection> listMyWords(Long userId, int page) {
        int safePage = Math.max(page, 0);
        Sort sort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        return collectionRepository.findPageByUserId(userId, PageRequest.of(safePage, PAGE_SIZE));
    }

    //在全部词库中搜索要收藏的单词：直接复用单词列表的搜索
    @Transactional(readOnly = true)
    public List<Word> search(String keyword) {
        return wordService.listWords(null, keyword, 0).getContent();
    }

    //这些单词中，哪些已收藏
    @Transactional(readOnly = true)
    public Set<Long> collectedIds(Long userId, List<Word> words){
        if(words.isEmpty()){
            return Set.of();
        }
        List<Long> ids = words.stream().map(Word::getId).toList();
        return new HashSet<>(collectionRepository.findCollectedWordIds(userId,ids));
    }

    // ===================收藏，取消收藏=====================

    @Transactional
    public Word add(Long userId,Long wordId){
        Word word = wordRepository.findById(wordId)
                .orElseThrow( () -> new WordCollectionException("单词不存在"));

        if (collectionRepository.existsByUserIdAndWordId(userId,wordId)){
            throw new WordCollectionException("[" + word.getSpelling() + "]已经在我的词库中了");
        }
        if(collectionRepository.countByUserId(userId) >= MAX_SIZE){
            throw new WordCollectionException("[我的词库]最多收藏" + MAX_SIZE + "个单词");
        }

        User userRef = userRepository.getReferenceById(userId);
        try{
            //立即执行 INSERT：同一个单词背同时收藏两次时，唯一约束会在这里报错
            collectionRepository.saveAndFlush(new UserCollection(userRef,word, LocalDateTime.now()));
        }
        catch (DataIntegrityViolationException e){
            throw new WordCollectionException("[" + word.getSpelling() + "]已经在我的词库中了");
        }

        return word;
    }

    @Transactional
    public boolean remove(Long userId, Long wordId){
        return collectionRepository.deleteByUserIdAndWordId(userId,wordId) > 0;
    }

    //=============给学习和听写用====================
    //我的词库中下一个没学过的单词
    @Transactional(readOnly = true)
    public Optional<Word> nextNewWord(Long userId){
        List<Word> words = collectionRepository.findNewWords(userId,PageRequest.of(0,1));
        return words.isEmpty() ? Optional.empty() : Optional.of(words.get(0));
    }

    //我的词库中已学过的单词数量
    @Transactional(readOnly = true)
    public long countLearned(Long userId){
        return collectionRepository.countLearned(userId);
    }

    //听写，从我的词库中随机选出若干个单词的id
    public List<Long> randomWordIds(Long userId, int limit){
        return collectionRepository.findRandomWordIds(userId,limit);
    }
}
