package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.dto.DictEntry;
import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.entity.WordSource;
import com.jay.englishpracticeplatform.exception.WordCollectionException;
import com.jay.englishpracticeplatform.repository.DictionaryRepository;
import com.jay.englishpracticeplatform.repository.WordRepository;
import com.jay.englishpracticeplatform.service.DictionaryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class DictionaryServiceTest {

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private JdbcClient jdbcClient;

    // 按拼写找到词条的 id
    private Long entryId(String word) {
        return jdbcClient.sql("SELECT id FROM dict_entries WHERE word = ?")
                .param(word)
                .query(Long.class)
                .single();
    }

    @Test
    void dictionaryIsImportedOnStartup() {
        // 数据文件约 9.6 万条，这里只检查数量级，以后重新生成数据文件不用改测试
        assertTrue(dictionaryRepository.count() > 90_000);
    }

    @Test
    void englishSearchMatchesPrefixAndPutsExactMatchFirst() {
        List<DictEntry> results = dictionaryService.search("give up");

        assertFalse(results.isEmpty());
        assertEquals("give up", results.get(0).word());
        assertTrue(results.get(0).phrase());
        assertTrue(results.stream().allMatch(e -> e.word().toLowerCase().startsWith("give up")));
        assertTrue(results.size() <= DictionaryService.SEARCH_LIMIT);
    }

    @Test
    void searchExcludesWordsAlreadyInWordsTable() {
        // abandon 在考试词库中，应该出现在「词库中的单词」里，而不是扩展词典的结果里
        assertTrue(wordRepository.findBySpelling("abandon").isPresent());
        assertTrue(dictionaryService.search("abandon").stream()
                .noneMatch(e -> e.word().equalsIgnoreCase("abandon")));
    }

    @Test
    void chineseSearchLooksInTranslation() {
        List<DictEntry> results = dictionaryService.search("放弃");

        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(e -> e.translation().contains("放弃")));
    }

    @Test
    void blankKeywordReturnsNothing() {
        assertTrue(dictionaryService.search("   ").isEmpty());
    }

    @Test
    void findOrCreateWordCreatesExtendedWordOnlyOnce() {
        Long id = entryId("give up");
        assertTrue(wordRepository.findBySpelling("give up").isEmpty());

        Word first = dictionaryService.findOrCreateWord(id);
        Word second = dictionaryService.findOrCreateWord(id);

        assertEquals(first.getId(), second.getId());
        assertEquals("give up", first.getSpelling());
        assertEquals(WordSource.EXTENDED, first.getSource());
        assertTrue(first.isExtended());
        // 扩展词不属于任何级别
        assertTrue(first.getLevels().isEmpty());
    }

    @Test
    void findOrCreateWordReusesExistingCoreWord() {
        Long id = entryId("abandon");
        Word core = wordRepository.findBySpelling("abandon").orElseThrow();

        Word word = dictionaryService.findOrCreateWord(id);

        assertEquals(core.getId(), word.getId());
        assertEquals(WordSource.CORE, word.getSource());
    }

    @Test
    void unknownEntryThrows() {
        assertThrows(WordCollectionException.class, () -> dictionaryService.findOrCreateWord(-1L));
    }
}