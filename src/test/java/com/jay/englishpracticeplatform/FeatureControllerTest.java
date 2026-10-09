package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.entity.Word;
import com.jay.englishpracticeplatform.entity.WordLevel;
import com.jay.englishpracticeplatform.security.AuthUser;
import com.jay.englishpracticeplatform.service.UserService;
import com.jay.englishpracticeplatform.service.WordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FeatureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private WordService wordService;

    private RequestPostProcessor loginAs(String username) {
        User u = userService.register(username, "123456");
        return user(new AuthUser(u.getId(), u.getUsername(), u.getPasswordHash()));
    }

    // ==================== 单词列表 ====================

    @Test
    void wordsWithoutLevelShowsAllLevels() throws Exception {
        mockMvc.perform(get("/words").with(loginAs("mvcwords01")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentLevel", nullValue()));
    }

    @Test
    void wordsPageOutOfRangeRedirectsAndKeepsKeyword() throws Exception {
        mockMvc.perform(get("/words").param("q", "ban").param("page", "99999")
                        .with(loginAs("mvcwords02")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/words?q=ban&page=*"));
    }

    @Test
    void wordsWithInvalidLevelReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/words").param("level", "ABC").with(loginAs("mvcwords03")))
                .andExpect(status().isBadRequest());
    }

    // ==================== 统计 ====================

    @Test
    void statsWithInvalidDaysFallsBackToSeven() throws Exception {
        mockMvc.perform(get("/stats").param("days", "999").with(loginAs("mvcstats01")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("days", 7));
    }

    // ==================== 打卡 ====================

    @Test
    void dailyGoalOutOfRangeShowsError() throws Exception {
        mockMvc.perform(post("/checkin/goal").param("dailyGoal", "3")
                        .with(loginAs("mvcgoal01")).with(csrf()))
                .andExpect(redirectedUrl("/checkin"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void validDailyGoalShowsMessage() throws Exception {
        mockMvc.perform(post("/checkin/goal").param("dailyGoal", "10")
                        .with(loginAs("mvcgoal02")).with(csrf()))
                .andExpect(redirectedUrl("/checkin"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    void checkinMonthBeforeRegistrationRedirects() throws Exception {
        mockMvc.perform(get("/checkin").param("month", "2000-01").with(loginAs("mvcmonth01")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/checkin?month=*"));
    }

    @Test
    void checkinMalformedMonthReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/checkin").param("month", "abc").with(loginAs("mvcmonth02")))
                .andExpect(status().isBadRequest());
    }

    // ==================== 听写 ====================

    @Test
    void dictationPreviewShowsWords() throws Exception {
        mockMvc.perform(get("/dictation/en")
                        .param("level", "CET4").param("source", "ALL").param("count", "20")
                        .with(loginAs("mvcdict01")))
                .andExpect(status().isOk())
                .andExpect(view().name("dictation-preview"))
                .andExpect(model().attributeExists("words", "ids"));
    }

    @Test
    void dictationFromEmptyMistakesRedirectsBack() throws Exception {
        mockMvc.perform(get("/dictation/cn")
                        .param("level", "CET4").param("source", "MISTAKES").param("count", "20")
                        .with(loginAs("mvcdict02")))
                .andExpect(redirectedUrl("/dictation"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void dictationTestRejectsTooManyIds() throws Exception {
        String ids = IntStream.rangeClosed(1, 31)
                .mapToObj(String::valueOf)
                .collect(Collectors.joining(","));

        mockMvc.perform(get("/dictation/en/test").param("level", "CET4").param("ids", ids)
                        .with(loginAs("mvcdict03")))
                .andExpect(redirectedUrl("/dictation"))
                .andExpect(flash().attributeExists("error"));
    }

    @Test
    void dictationSubmitWithoutCsrfIsRejected() throws Exception {
        mockMvc.perform(post("/dictation/en").param("level", "CET4").param("wordIds[0]", "1")
                        .with(loginAs("mvcdict04")))
                .andExpect(status().isForbidden());
    }

    @Test
    void dictationResultIsShownOnlyOnce() throws Exception {
        RequestPostProcessor login = loginAs("mvcdict05");
        List<Long> ids = wordService.listWords(WordLevel.CET4, "", 0).getContent().stream()
                .limit(3).map(Word::getId).toList();

        // ① 交卷：重定向到结果页，判卷结果放在 Flash 数据里
        MvcResult submit = mockMvc.perform(post("/dictation/en")
                        .param("level", "CET4")
                        .param("wordIds[0]", ids.get(0).toString())
                        .param("wordIds[1]", ids.get(1).toString())
                        .param("wordIds[2]", ids.get(2).toString())
                        .param("answers[0]", "wrong")
                        .with(login).with(csrf()))
                .andExpect(redirectedUrl("/dictation/result"))
                .andExpect(flash().attributeExists("result"))
                .andReturn();
        Object result = submit.getFlashMap().get("result");

        // ② 重定向后的那一次请求带着 Flash 数据：正常显示结果页
        mockMvc.perform(get("/dictation/result").flashAttr("result", result).with(login))
                .andExpect(status().isOk())
                .andExpect(view().name("dictation-result"));

        // ③ 刷新页面（没有 Flash 数据了）：回到设置页，不会重复判卷
        mockMvc.perform(get("/dictation/result").with(login))
                .andExpect(redirectedUrl("/dictation"));
    }
}