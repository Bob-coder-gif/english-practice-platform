package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.security.AuthUser;
import com.jay.englishpracticeplatform.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    private RequestPostProcessor loginAs(String username) {
        User u = userService.register(username, "123456");
        return user(new AuthUser(u.getId(), u.getUsername(), u.getPasswordHash()));
    }

    @Test
    void reviewShowsDueTabByDefault() throws Exception {
        mockMvc.perform(get("/review").with(loginAs("mvcrev01")))
                .andExpect(status().isOk())
                .andExpect(view().name("review"))
                .andExpect(model().attribute("tab", "due"))
                .andExpect(model().attributeExists("dueCount", "mistakeCount"));
    }

    @Test
    void mistakesTabShowsMistakeList() throws Exception {
        mockMvc.perform(get("/review").param("tab", "mistakes").with(loginAs("mvcrev02")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("tab", "mistakes"))
                .andExpect(model().attributeExists("mistakePage", "masteredStreak"));
    }

    @Test
    void unknownTabFallsBackToDue() throws Exception {
        mockMvc.perform(get("/review").param("tab", "whatever").with(loginAs("mvcrev03")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("tab", "due"));
    }

    @Test
    void pageOutOfRangeRedirectsToLastPage() throws Exception {
        mockMvc.perform(get("/review").param("tab", "mistakes").param("page", "99").with(loginAs("mvcrev04")))
                .andExpect(redirectedUrl("/review?tab=mistakes&page=1"));
    }

    @Test
    void oldMistakesUrlRedirectsToReviewTab() throws Exception {
        mockMvc.perform(get("/mistakes").with(loginAs("mvcrev05")))
                .andExpect(redirectedUrl("/review?tab=mistakes"));
    }
}