package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.entity.WordLevel;
import com.jay.englishpracticeplatform.service.WordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WordServiceTest {

    @Autowired
    private WordService wordService;

    // ==================== 列表 ====================

    @Test
    void listReturnsOnePageSortedBySpelling() {
        Page<Word> page = wordService.listWords(WordLevel.CET4, "", 0);

        // 每页数量正确，总数和统计查询一致
        assertEquals(WordService.PAGE_SIZE, page.getContent().size());
        assertEquals(wordService.countByLevel(WordLevel.CET4), page.getTotalElements());

        // 按拼写顺序：每个单词都不应该排在前一个单词前面
        List<Word> words = page.getContent();
        for (int i = 1; i < words.size(); i++) {
            String prev = words.get(i - 1).getSpelling();
            String curr = words.get(i).getSpelling();
            assertTrue(prev.compareToIgnoreCase(curr) <= 0, prev + " 应该排在 " + curr + " 前面");
        }
    }

    @Test
    void negativePageIsTreatedAsFirstPage() {
        assertEquals(0, wordService.listWords(WordLevel.CET4, "", -5).getNumber());
    }

    @Test
    void blankKeywordListsEverything() {
        assertEquals(wordService.countAll(), wordService.listWords(null, "  ", 0).getTotalElements());
        assertEquals(wordService.countByLevel(WordLevel.CET4),
                wordService.listWords(WordLevel.CET4, "", 0).getTotalElements());
    }

    // ==================== 搜索 ====================

    // 计算一个单词在搜索结果中应属于哪一组：0 开头匹配，1 拼写包含，2 只有释义包含
    private int rank(Word word, String keyword) {
        String spelling = word.getSpelling().toLowerCase();
        String key = keyword.toLowerCase();
        if (spelling.startsWith(key)) {
            return 0;
        }
        return spelling.contains(key) ? 1 : 2;
    }

    @Test
    void containsSearchFindsWordFromMiddle() {
        // 取一个单词，去掉第一个字母，用剩下的部分搜索，应该能找到它
        Word word = wordService.listWords(WordLevel.CET4, "", 0).getContent().stream()
                .filter(w -> w.getSpelling().length() >= 5)
                .findFirst().orElseThrow();
        String keyword = word.getSpelling().substring(1);

        Page<Word> result = wordService.listWords(null, keyword, 0);

        assertTrue(result.getContent().stream().anyMatch(w -> w.getId().equals(word.getId())));
    }

    @Test
    void prefixMatchesComeBeforeContainsMatches() {
        List<Word> words = wordService.listWords(null, "ban", 0).getContent();

        assertFalse(words.isEmpty());
        // 每个单词的分组编号都不能比后面的单词大
        for (int i = 0; i + 1 < words.size(); i++) {
            assertTrue(rank(words.get(i), "ban") <= rank(words.get(i + 1), "ban"),
                    words.get(i).getSpelling() + " 不应排在 " + words.get(i + 1).getSpelling() + " 前面");
        }
    }

    @Test
    void chineseKeywordSearchesMeaning() {
        // 从某个单词的释义里取出两个连续的汉字作为搜索词
        Pattern chinese = Pattern.compile("[\\u4e00-\\u9fa5]{2}");
        Word word = wordService.listWords(WordLevel.CET4, "", 0).getContent().stream()
                .filter(w -> chinese.matcher(w.getMeaning()).find())
                .findFirst().orElseThrow();
        Matcher matcher = chinese.matcher(word.getMeaning());
        matcher.find();
        String keyword = matcher.group();

        Page<Word> result = wordService.listWords(null, keyword, 0);

        assertTrue(result.getTotalElements() > 0);
        assertTrue(result.getContent().stream().allMatch(w -> w.getMeaning().contains(keyword)));
    }

    @Test
    void levelFilterStillAppliesWhenSearchingMeaning() {
        // 带级别搜索的结果数，不能超过不带级别的结果数（验证 AND / OR 的括号）
        long inLevel = wordService.listWords(WordLevel.CET4, "放弃", 0).getTotalElements();
        long inAll = wordService.listWords(null, "放弃", 0).getTotalElements();
        assertTrue(inLevel <= inAll);
    }

    @Test
    void searchIsCaseInsensitive() {
        assertEquals(
                wordService.listWords(null, "ban", 0).getTotalElements(),
                wordService.listWords(null, "BAN", 0).getTotalElements());
    }

    @Test
    void wildcardCharactersAreTreatedLiterally() {
        assertEquals(0, wordService.listWords(null, "%", 0).getTotalElements());
        assertEquals(0, wordService.listWords(null, "_", 0).getTotalElements());
    }
}