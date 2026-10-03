package com.jay.englishpracticeplatform.service;

import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.entity.WordLevel;
import com.jay.englishpracticeplatform.repository.WordRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WordService {

    public static final int PAGE_SIZE = 20;

    private final WordRepository wordRepository;

    public WordService(WordRepository wordRepository){
        this.wordRepository = wordRepository;
    }

    @Transactional(readOnly = true)
    public Page<Word> listByLevel(WordLevel level, int page){
        //页码不能小于 0
        int safePage = Math.max(page, 0);
        PageRequest pageRequest = PageRequest.of(safePage, PAGE_SIZE, Sort.by("spelling"));
        return wordRepository.findByLevel(level, pageRequest);
    }

    @Transactional(readOnly = true)
    public long countAll(){
        return wordRepository.count();
    }

    @Transactional(readOnly = true)
    public long countByLevel(WordLevel level){
        return wordRepository.countByLevel(level);
    }
}
