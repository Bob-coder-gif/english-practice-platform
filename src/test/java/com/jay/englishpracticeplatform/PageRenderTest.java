package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.security.AuthUser;
import com.jay.englishpracticeplatform.service.UserService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 登录后，每个页面都能正常渲染（模板里的表达式写错时，这里会失败）
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PageRenderTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    private RequestPostProcessor loginAs(String username) {
        User u = userService.register(username, "123456");
        return user(new AuthUser(u.getId(), u.getUsername(), u.getPasswordHash()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/",
            "/study",
            "/review",
            "/mistakes",
            "/words",
            "/words?q=ban",
            "/words?level=CET6&q=ab",
            "/stats",
            "/stats?days=30",
            "/checkin",
            "/dictation",
            "/profile"
    })
    void pageRendersForLoggedInUser(String url) throws Exception {
        mockMvc.perform(get(url).with(loginAs("mvcpage")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML));
    }
}