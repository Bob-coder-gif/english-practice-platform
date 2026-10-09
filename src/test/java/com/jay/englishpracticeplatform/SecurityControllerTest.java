package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.security.AuthUser;
import com.jay.englishpracticeplatform.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SecurityControllerTest {

    private static final String PASSWORD = "123456";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    // 注册一个用户，并返回「以这个用户身份发送请求」的处理器
    private RequestPostProcessor loginAs(String username) {
        User u = userService.register(username, PASSWORD);
        return user(new AuthUser(u.getId(), u.getUsername(), u.getPasswordHash()));
    }

    // ==================== 访问权限 ====================

    @ParameterizedTest
    @ValueSource(strings = {"/", "/login", "/register", "/css/app.css", "/js/app.js"})
    void publicResourcesAreAccessibleWithoutLogin(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/study", "/review", "/mistakes", "/words", "/stats",
            "/checkin", "/dictation", "/profile"})
    void protectedPagesRedirectToLogin(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));    }

    // ==================== 登录 ====================

    @Test
    void loginSucceedsWithCorrectPassword() throws Exception {
        userService.register("mvclogin01", PASSWORD);

        mockMvc.perform(post("/login")
                        .param("username", "mvclogin01")
                        .param("password", PASSWORD)
                        .with(csrf()))
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername("mvclogin01"));
    }

    @Test
    void loginFailsWithWrongPassword() throws Exception {
        userService.register("mvclogin02", PASSWORD);

        mockMvc.perform(post("/login")
                        .param("username", "mvclogin02")
                        .param("password", "wrong-password")
                        .with(csrf()))
                .andExpect(redirectedUrl("/login?error"))
                .andExpect(unauthenticated());
    }

    @Test
    void loginWithoutCsrfTokenIsRejected() throws Exception {
        userService.register("mvclogin03", PASSWORD);

        mockMvc.perform(post("/login")
                        .param("username", "mvclogin03")
                        .param("password", PASSWORD))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginReturnsToOriginallyRequestedPage() throws Exception {
        // 未登录时访问 /study，被带到登录页；Spring Security 把原本想访问的地址记在了 Session 里
        MvcResult first = mockMvc.perform(get("/study")).andReturn();
        MockHttpSession session = (MockHttpSession) first.getRequest().getSession(false);
        assertNotNull(session);

        userService.register("mvclogin04", PASSWORD);

        // 用同一个 Session 登录，应该回到 /study，而不是首页
        mockMvc.perform(post("/login")
                        .session(session)
                        .param("username", "mvclogin04")
                        .param("password", PASSWORD)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(result -> assertTrue(
                        result.getResponse().getRedirectedUrl().contains("/study")));
    }

    // ==================== 记住我 ====================

    @Test
    void rememberMeIssuesCookieWhenChecked() throws Exception {
        userService.register("mvcremember01", PASSWORD);

        mockMvc.perform(post("/login")
                        .param("username", "mvcremember01")
                        .param("password", PASSWORD)
                        .param("remember-me", "on")
                        .with(csrf()))
                .andExpect(cookie().exists("remember-me"))
                .andExpect(cookie().httpOnly("remember-me", true));
    }

    @Test
    void noRememberMeCookieWhenNotChecked() throws Exception {
        userService.register("mvcremember02", PASSWORD);

        mockMvc.perform(post("/login")
                        .param("username", "mvcremember02")
                        .param("password", PASSWORD)
                        .with(csrf()))
                .andExpect(cookie().doesNotExist("remember-me"));
    }

    // ==================== 退出 ====================

    @Test
    void logoutWithoutCsrfTokenIsRejected() throws Exception {
        mockMvc.perform(post("/logout").with(loginAs("mvclogout01")))
                .andExpect(status().isForbidden());
    }

    @Test
    void logoutWithCsrfTokenRedirectsHome() throws Exception {
        mockMvc.perform(post("/logout").with(loginAs("mvclogout02")).with(csrf()))
                .andExpect(redirectedUrl("/?logout"));
    }

    // ==================== 注册 ====================

    @Test
    void registerSucceedsAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "mvcreg01")
                        .param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD)
                        .with(csrf()))
                .andExpect(redirectedUrl("/"))
                .andExpect(flash().attributeExists("message"));
    }

    @Test
    void registerRejectsDuplicateUsername() throws Exception {
        userService.register("mvcreg02", PASSWORD);

        mockMvc.perform(post("/register")
                        .param("username", "mvcreg02")
                        .param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "username"));
    }

    @Test
    void registerRejectsMismatchedPasswords() throws Exception {
        mockMvc.perform(post("/register")
                        .param("username", "mvcreg03")
                        .param("password", PASSWORD)
                        .param("confirmPassword", "different")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("registerForm", "confirmPassword"));
    }
}