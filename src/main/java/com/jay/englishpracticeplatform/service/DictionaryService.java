package com.jay.englishpracticeplatform.service;

import com.jay.englishpracticeplatform.dto.DictEntry;
import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.entity.WordSource;
import com.jay.englishpracticeplatform.exception.WordCollectionException;
import com.jay.englishpracticeplatform.repository.DictionaryRepository;
import com.jay.englishpracticeplatform.repository.WordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

@Service
public class DictionaryService {

    // 扩展词典最多显示多少个搜索结果
    public static final int SEARCH_LIMIT = 20;

    private final DictionaryRepository dictionaryRepository;
    private final WordRepository wordRepository;
    private final TransactionTemplate transactionTemplate;

    public DictionaryService(DictionaryRepository dictionaryRepository,
                             WordRepository wordRepository,
                             TransactionTemplate transactionTemplate) {
        this.dictionaryRepository = dictionaryRepository;
        this.wordRepository = wordRepository;
        this.transactionTemplate = transactionTemplate;
    }

    // 在扩展词典中搜索（不包括 words 表中已有的词）
    // 输入里有中文就搜释义，否则按英文前缀搜
    @Transactional(readOnly = true)
    public List<DictEntry> search(String keyword) {
        String cleaned = WordService.normalizeKeyword(keyword);
        if (cleaned.isEmpty()) {
            return List.of();
        }
        String escaped = WordService.escapeLike(cleaned);
        return containsNonAscii(cleaned)
                ? dictionaryRepository.searchByMeaning("%" + escaped + "%", SEARCH_LIMIT)
                : dictionaryRepository.searchByPrefix(cleaned, escaped + "%", SEARCH_LIMIT);
    }

    // 把扩展词典中的词条变成一个 Word：words 表里已经有了（比如别人收藏过）就直接用，没有就新建一个 EXTENDED 单词
    //
    // 这个方法故意不加 @Transactional，而是用 TransactionTemplate 手动开一个短事务来插入：
    // 两个人同时收藏同一个新词时，后插入的那个会撞上 words.spelling 的唯一约束。
    // 插入失败的事务回滚后，在【新的事务】里重新查一次，就能查到对方刚插入的那一行。
    // 如果整个方法在同一个事务里，MySQL 的可重复读会让我们一直看到事务开始时的旧快照，查不到对方的数据。
    public Word findOrCreateWord(Long entryId) {
        DictEntry entry = dictionaryRepository.findById(entryId)
                .orElseThrow(() -> new WordCollectionException("扩展词典中没有这个词条"));

        Optional<Word> existing = wordRepository.findBySpelling(entry.word());
        if (existing.isPresent()) {
            return existing.get();
        }

        try {
            return transactionTemplate.execute(status -> wordRepository.saveAndFlush(toWord(entry)));
        } catch (DataIntegrityViolationException e) {
            return wordRepository.findBySpelling(entry.word()).orElseThrow(() -> e);
        }
    }

    private static Word toWord(DictEntry entry) {
        Word word = new Word();
        word.setSpelling(entry.word());
        word.setPhonetic(entry.phonetic());
        word.setMeaning(entry.translation());
        word.setSource(WordSource.EXTENDED);
        // 不设置级别：扩展词不属于任何考试大纲，不会出现在按级别学习和浏览的列表里
        return word;
    }

    static boolean containsNonAscii(String text) {
        return text.chars().anyMatch(c -> c > 127);
    }
}