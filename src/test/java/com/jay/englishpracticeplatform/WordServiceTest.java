package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.entity.WordLevel;
import com.jay.englishpracticeplatform.repository.WordRepository;
import com.jay.englishpracticeplatform.service.WordService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WordServiceTest {

    @Autowired
    private WordService wordService;

    @Autowired
    private WordRepository wordRepository;

    @Test
    void listByLevelReturnsOnePageSortedBySpelling(){
        Page<Word> page = wordService.listByLevel(WordLevel.CET4,0);

        // 每页数量正确
        assertEquals(WordService.PAGE_SIZE, page.getContent().size());
        //总数和统计查询一致
        assertEquals(wordRepository.countByLevel(WordLevel.CET4) , page.getTotalElements());

        // 按乒协顺序，每个单词都不应该排在前一个单词前面
        List<Word> words = page.getContent();
        for(int i = 1; i < words.size(); i++){
            String prev = words.get(i-1).getSpelling();
            String curr = words.get(i).getSpelling();
            assertTrue(prev.compareToIgnoreCase(curr) <= 0, prev + "应该排在" + curr + "前面");

        }
    }

    @Test
    void negativePageIsTreatedAsFirstPage(){
        Page<Word> page = wordService.listByLevel(WordLevel.CET4, -5);

        assertEquals(0, page.getNumber());
    }
}

