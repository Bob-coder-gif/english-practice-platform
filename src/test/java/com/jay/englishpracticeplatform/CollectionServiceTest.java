package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.entity.AnswerMode;
import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.exception.WordCollectionException;
import com.jay.englishpracticeplatform.repository.WordRepository;
import com.jay.englishpracticeplatform.service.CollectionService;
import com.jay.englishpracticeplatform.service.StudyService;
import com.jay.englishpracticeplatform.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CollectionServiceTest {

    private static final String PASSWORD = "123456";

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private UserService userService;

    @Autowired
    private StudyService studyService;

    @Autowired
    private WordRepository wordRepository;

    // 从词库里取出前 n 个单词
    private List<Word> someWords(int n) {
        return wordRepository.findAll(PageRequest.of(0, n)).getContent();
    }

    @Test
    void addAndRemove() {
        User user = userService.register("col_user01", PASSWORD);
        Word word = someWords(1).get(0);

        collectionService.add(user.getId(), word.getId());
        assertEquals(1, collectionService.count(user.getId()));

        assertTrue(collectionService.remove(user.getId(), word.getId()));
        assertEquals(0, collectionService.count(user.getId()));
        // 再删一次：已经不存在了
        assertFalse(collectionService.remove(user.getId(), word.getId()));
    }

    @Test
    void cannotAddSameWordTwice() {
        User user = userService.register("col_user02", PASSWORD);
        Word word = someWords(1).get(0);
        collectionService.add(user.getId(), word.getId());

        assertThrows(WordCollectionException.class,
                () -> collectionService.add(user.getId(), word.getId()));
        assertEquals(1, collectionService.count(user.getId()));
    }

    @Test
    void cannotAddUnknownWord() {
        User user = userService.register("col_user03", PASSWORD);

        assertThrows(WordCollectionException.class,
                () -> collectionService.add(user.getId(), -1L));
    }

    @Test
    void cannotExceedMaxSize() {
        User user = userService.register("col_user04", PASSWORD);
        List<Word> words = someWords(CollectionService.MAX_SIZE + 1);

        for (int i = 0; i < CollectionService.MAX_SIZE; i++) {
            collectionService.add(user.getId(), words.get(i).getId());
        }

        Long extra = words.get(CollectionService.MAX_SIZE).getId();
        assertThrows(WordCollectionException.class, () -> collectionService.add(user.getId(), extra));
        assertEquals(CollectionService.MAX_SIZE, collectionService.count(user.getId()));
    }

    @Test
    void usersDoNotSeeEachOthersCollections() {
        User alice = userService.register("col_alice", PASSWORD);
        User bob = userService.register("col_bob", PASSWORD);
        Word word = someWords(1).get(0);
        collectionService.add(alice.getId(), word.getId());

        // bob 看不到、删不掉、抽不到 alice 收藏的单词
        assertEquals(0, collectionService.count(bob.getId()));
        assertFalse(collectionService.remove(bob.getId(), word.getId()));
        assertTrue(collectionService.randomWordIds(bob.getId(), 10).isEmpty());
        assertTrue(collectionService.collectedIds(bob.getId(), List.of(word)).isEmpty());
        assertEquals(1, collectionService.count(alice.getId()));
    }

    @Test
    void nextNewWordSkipsLearnedWords() {
        User user = userService.register("col_user05", PASSWORD);
        List<Word> words = someWords(2);
        collectionService.add(user.getId(), words.get(0).getId());
        collectionService.add(user.getId(), words.get(1).getId());

        // 学过第一个之后，下一个新单词就是第二个
        studyService.recordAnswer(user.getId(), words.get(0).getId(), true, AnswerMode.LEARN);

        assertEquals(words.get(1).getId(),
                collectionService.nextNewWord(user.getId()).orElseThrow().getId());
        assertEquals(1, collectionService.countLearned(user.getId()));
    }

    @Test
    void randomWordIdsOnlyFromMyCollection() {
        User user = userService.register("col_user06", PASSWORD);
        List<Word> words = someWords(3);
        for (Word w : words) {
            collectionService.add(user.getId(), w.getId());
        }

        List<Long> ids = collectionService.randomWordIds(user.getId(), 10);

        assertEquals(3, ids.size());
        assertEquals(new HashSet<>(words.stream().map(Word::getId).toList()), new HashSet<>(ids));
    }

    @Test
    void removingKeepsLearningRecords() {
        User user = userService.register("col_user07", PASSWORD);
        Word word = someWords(1).get(0);
        collectionService.add(user.getId(), word.getId());
        studyService.recordAnswer(user.getId(), word.getId(), true, AnswerMode.LEARN);

        collectionService.remove(user.getId(), word.getId());

        // 学习状态还在：这个单词在 CET4 等级别里依然算「学过」，进入复习流程
        // 这里通过「再次收藏后不是新单词」来验证
        collectionService.add(user.getId(), word.getId());
        assertTrue(collectionService.nextNewWord(user.getId()).isEmpty());
    }
}