package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.security.ChangelogPopupListener;
import com.jay.englishpracticeplatform.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ChangelogControllerTest {

    private static final String PASSWORD = "123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Test
    void changelogPageIsPublic() throws Exception {
        mockMvc.perform(get("/changelog"))
                .andExpect(status().isOk())
                .andExpect(view().name("changelog"))
                .andExpect(model().attributeExists("entries"));
    }

    @Test
    void popupAppearsOnlyOnFirstPageAfterLogin() throws Exception {
        userService.register("mvcchangelog01", PASSWORD);

        // 登录：监听器应该在 Session 里做了标记
        MvcResult login = mockMvc.perform(post("/login")
                        .param("username", "mvcchangelog01")
                        .param("password", PASSWORD)
                        .with(csrf()))
                .andExpect(redirectedUrl("/"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(session);
        assertNotNull(session.getAttribute(ChangelogPopupListener.SESSION_KEY));

        // 登录后的第一个页面：弹出
        mockMvc.perform(get("/").session(session))
                .andExpect(model().attributeExists("popupChangelog"));

        // 再刷新一次：不再弹出（标记已经被取走了）
        mockMvc.perform(get("/").session(session))
                .andExpect(model().attributeDoesNotExist("popupChangelog"));
    }
}