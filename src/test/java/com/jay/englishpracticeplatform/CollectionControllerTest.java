package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.init.DictionaryImporter;
import com.jay.englishpracticeplatform.repository.DictionaryRepository;
import com.jay.englishpracticeplatform.repository.WordRepository;
import com.jay.englishpracticeplatform.security.AuthUser;
import com.jay.englishpracticeplatform.service.CollectionService;
import com.jay.englishpracticeplatform.service.UserService;
import com.jay.englishpracticeplatform.entity.WordSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CollectionControllerTest {

    private static final String PASSWORD = "123456";

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private CollectionService collectionService;

    @Autowired
    private WordRepository wordRepository;

    private User registered;

    private RequestPostProcessor loginAs(String username) {
        registered = userService.register(username, PASSWORD);
        return user(new AuthUser(registered.getId(), registered.getUsername(), registered.getPasswordHash()));
    }

    private Word anyWord() {
        return wordRepository.findAll(PageRequest.of(0, 1)).getContent().get(0);
    }

    @Test
    void collectionPageRenders() throws Exception {
        mockMvc.perform(get("/collection").with(loginAs("mvccol01")))
                .andExpect(status().isOk())
                .andExpect(view().name("collection"))
                .andExpect(model().attributeExists("collectionPage", "count", "maxSize"));
    }

    @Test
    void searchShowsResults() throws Exception {
        mockMvc.perform(get("/collection").param("q", "ban").with(loginAs("mvccol02")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("searchResults", "collectedIds", "dictResults"));
    }

    @Test
    void addRedirectsBackToSearch() throws Exception {
        Word word = anyWord();

        mockMvc.perform(post("/collection/add")
                        .with(loginAs("mvccol03"))
                        .with(csrf())
                        .param("wordId", word.getId().toString())
                        .param("q", "ban"))
                .andExpect(redirectedUrl("/collection?q=ban"))
                .andExpect(flash().attributeExists("message"));

        assertEquals(1, collectionService.count(registered.getId()));
    }

    @Test
    void addDuplicateShowsError() throws Exception {
        RequestPostProcessor me = loginAs("mvccol04");
        Word word = anyWord();
        collectionService.add(registered.getId(), word.getId());

        mockMvc.perform(post("/collection/add")
                        .with(me)
                        .with(csrf())
                        .param("wordId", word.getId().toString()))
                .andExpect(redirectedUrl("/collection"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void removeWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/collection/remove")
                        .with(loginAs("mvccol05"))
                        .param("wordId", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void studyCollectionModeRenders() throws Exception {
        mockMvc.perform(get("/study").param("collection", "true").with(loginAs("mvccol06")))
                .andExpect(status().isOk())
                .andExpect(view().name("study"))
                .andExpect(model().attribute("collection", true));
    }

    @Test
    void addEntryCollectsExtendedWord() throws Exception {
        RequestPostProcessor me = loginAs("mvccol07");
        Long entryId = jdbcClient.sql("SELECT id FROM dict_entries WHERE word = 'give up'")
                .query(Long.class)
                .single();

        mockMvc.perform(post("/collection/add-entry")
                        .with(me)
                        .with(csrf())
                        .param("entryId", entryId.toString())
                        .param("q", "give up"))
                .andExpect(redirectedUrl("/collection?q=give+up"))
                .andExpect(flash().attribute("message", "已收藏「give up」"));

        // 词条变成了 words 表中的一个扩展词，并且被收藏了
        Word word = wordRepository.findBySpelling("give up").orElseThrow();
        assertEquals(WordSource.EXTENDED, word.getSource());
        assertEquals(1, collectionService.count(registered.getId()));
    }

    @Test
    void addUnknownEntryShowsError() throws Exception {
        mockMvc.perform(post("/collection/add-entry")
                        .with(loginAs("mvccol08"))
                        .with(csrf())
                        .param("entryId", "-1"))
                .andExpect(redirectedUrl("/collection"))
                .andExpect(flash().attributeExists("error"));
    }

    // 解析数据文件的一行：纯 Java 逻辑，不需要启动 Spring
    // 放在 init 包里，才能调用包级私有的 parse 和 unescape
    static class DictionaryImporterTest {

        @Test
        void parsesAllColumns() {
            DictionaryRepository.DictRow row = DictionaryImporter.parse("abandon\tә'bændәn\tvt. 放弃\\nn. 放任\t2057\t0");

            assertEquals("abandon", row.word());
            assertEquals("ә'bændәn", row.phonetic());
            assertEquals("vt. 放弃\nn. 放任", row.translation());
            assertEquals(2057, row.freqRank());
            assertFalse(row.phrase());
        }

        @Test
        void emptyColumnsBecomeNull() {
            DictionaryRepository.DictRow row = DictionaryImporter.parse("give up\t\t放弃\t\t1");

            assertNull(row.phonetic());
            assertNull(row.freqRank());
            assertTrue(row.phrase());
        }

        @Test
        void unescapeHandlesBackslash() {
            assertEquals("a\\b\nc", DictionaryImporter.unescape("a\\\\b\\nc"));
        }

        @Test
        void wrongColumnCountThrows() {
            assertThrows(IllegalStateException.class, () -> DictionaryImporter.parse("only\ttwo"));
        }
    }
}