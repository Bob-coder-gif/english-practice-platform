package com.jay.englishpracticeplatform.agent.tool;

import com.jay.englishpracticeplatform.entity.AnswerMode;
import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.entity.WordLevel;
import com.jay.englishpracticeplatform.service.StudyService;
import com.jay.englishpracticeplatform.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 通过 ToolRegistry 调用真实的工具和 Service（连本地数据库，不调用大模型）。
 * 和其他 Service 测试一样，每个测试结束后自动回滚。
 */
@SpringBootTest
@Transactional
class AgentToolsTest {

    @Autowired
    private ToolRegistry registry;

    @Autowired
    private UserService userService;

    @Autowired
    private StudyService studyService;

    /** 让用户学一个新词并答错：答错的词会进入错题本，并且立即到期需要复习 */
    private Word answerWrong(User user) {
        Word word = studyService.nextNewWord(user.getId(), WordLevel.CET4).orElseThrow();
        studyService.recordAnswer(user.getId(), word.getId(), false, AnswerMode.LEARN);
        return word;
    }

    @Test
    void allReadOnlyToolsAreRegistered() {
        Set<String> names = new HashSet<>();
        for (JsonNode def : registry.definitions()) {
            names.add(def.path("function").path("name").asString(""));
        }
        assertTrue(names.containsAll(Set.of(
                "search_words", "get_study_stats", "get_review_due",
                "get_wrong_words", "get_checkin_status")));
    }

    @Test
    void searchFindsCoreWord() {
        User user = userService.register("agent_search", "123456");
        String result = registry.execute("search_words", "{\"query\":\"abandon\"}", new ToolContext(user.getId()));

        assertTrue(result.contains("abandon"));
        assertTrue(result.contains("考试词库"));
    }

    @Test
    void searchReportsNotFoundAndRejectsBlank() {
        ToolContext ctx = new ToolContext(userService.register("agent_search2", "123456").getId());

        assertTrue(registry.execute("search_words", "{\"query\":\"zzqqxxnotaword\"}", ctx).contains("没有找到"));
        assertTrue(registry.execute("search_words", "{\"query\":\"   \"}", ctx).startsWith("错误"));
        assertTrue(registry.execute("search_words", "{}", ctx).startsWith("错误"));
    }

    @Test
    void wrongAnswerShowsInWrongWordsAndReviewDue() {
        User user = userService.register("agent_wrong", "123456");
        Word word = answerWrong(user);
        ToolContext ctx = new ToolContext(user.getId());

        String wrong = registry.execute("get_wrong_words", "{}", ctx);
        assertTrue(wrong.contains(word.getSpelling()), wrong);
        assertTrue(wrong.contains("答错 1 次"), wrong);

        String due = registry.execute("get_review_due", "{}", ctx);
        assertTrue(due.contains(word.getSpelling()), due);
    }

    @Test
    void newUserHasEmptyLists() {
        ToolContext ctx = new ToolContext(userService.register("agent_empty", "123456").getId());

        assertEquals("错题本为空", registry.execute("get_wrong_words", "{}", ctx));
        assertEquals("当前没有到期需要复习的单词", registry.execute("get_review_due", "{}", ctx));
        assertTrue(registry.execute("get_checkin_status", "{}", ctx).contains("还没打卡"));
    }

    @Test
    void statsCountTodayAnswers() {
        User user = userService.register("agent_stats", "123456");
        answerWrong(user);

        String stats = registry.execute("get_study_stats", "{}", new ToolContext(user.getId()));
        assertTrue(stats.startsWith("今日答题：1 题"), stats);
        assertTrue(stats.contains("错题本：1 个"), stats);
    }

    /**
     * 安全测试：模型可能被用户诱导，在参数里塞进别人的 userId。
     * 工具只认 ToolContext，参数里的 userId 会被直接忽略。
     */
    @Test
    void toolsIgnoreUserIdInArguments() {
        User victim = userService.register("agent_victim", "123456");
        Word secret = answerWrong(victim);
        User attacker = userService.register("agent_attacker", "123456");

        String injected = "{\"userId\":" + victim.getId() + ",\"user_id\":" + victim.getId() + "}";
        ToolContext attackerCtx = new ToolContext(attacker.getId());

        assertEquals("错题本为空", registry.execute("get_wrong_words", injected, attackerCtx));
        assertFalse(registry.execute("get_review_due", injected, attackerCtx).contains(secret.getSpelling()));
        assertTrue(registry.execute("get_study_stats", injected, attackerCtx).startsWith("今日答题：0 题"));
    }
}