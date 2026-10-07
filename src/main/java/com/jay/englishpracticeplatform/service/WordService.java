package com.jay.englishpracticeplatform.service;

import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.entity.WordLevel;
import com.jay.englishpracticeplatform.repository.WordRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WordService {

    public static final int PAGE_SIZE = 20;

    //搜索关键字的最大长度
    public static final int MAX_KEYWORD_LENGTH = 30;

    private final WordRepository wordRepository;

    public WordService(WordRepository wordRepository){
        this.wordRepository = wordRepository;
    }

    //单词列表
    // level 为 null，全部级别，keyword为空，不搜索，按字母列出全部
    @Transactional(readOnly = true)
    public Page<Word> listWords(WordLevel level, String keyword, int page){
        int safePage = Math.max(page,0);
        String cleaned = normalizeKeyword(keyword);

        //不搜索，按字母排序
        if(cleaned.isEmpty()){
            PageRequest sorted = PageRequest.of(safePage, PAGE_SIZE, Sort.by("spelling"));
            return level == null
                    ? wordRepository.findAll(sorted)
                    : wordRepository.findByLevel(level,sorted);
        }

        PageRequest unsorted = PageRequest.of(safePage,PAGE_SIZE);
        String escaped = escapeLike(cleaned);
        String prefix = escaped +"%";
        String contains = "%" + escaped + "%";
        return level == null
                ? wordRepository.search(prefix,contains,unsorted)
                : wordRepository.searchByLevel(level, prefix, contains, unsorted);
    }

    @Transactional(readOnly = true)
    public long countAll(){
        return wordRepository.count();
    }

    @Transactional(readOnly = true)
    public long countByLevel(WordLevel level){
        return wordRepository.countByLevel(level);
    }

    //去掉首尾空格，并限制长度
    public static String normalizeKeyword(String keyword){
        if(keyword == null){
            return "";
        }
        String trimmed = keyword.strip();
        return trimmed.length() > MAX_KEYWORD_LENGTH ? trimmed.substring(0,MAX_KEYWORD_LENGTH) : trimmed;
    }

    //转移 LIKE 中的特殊字符
    static  String escapeLike(String text){
        return text
                .replace("\\","\\\\")
                .replace("%","\\%")
                .replace("_","\\_");
    }
}
