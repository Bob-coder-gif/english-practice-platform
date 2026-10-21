package com.jay.englishpracticeplatform;

import com.jay.englishpracticeplatform.agent.llm.LlmClient;
import com.jay.englishpracticeplatform.entity.User;
import com.jay.englishpracticeplatform.security.AuthUser;
import com.jay.englishpracticeplatform.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AI 助手接口和悬浮窗测试。
 * @MockitoBean 把容器里的 LlmClient（DeepSeekClient）换成 Mockito 的假对象，测试不会真的调用 DeepSeek。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;

    @Autowired
    private JsonMapper mapper;

    @MockitoBean
    private LlmClient llmClient;

    private RequestPostProcessor loginAs(String username) {
        User registered = userService.register(username, "123456");
        return user(new AuthUser(registered.getId(), registered.getUsername(), registered.getPasswordHash()));
    }

    private void modelAnswers(String text) throws Exception {
        ObjectNode message = mapper.createObjectNode()
                .put("role", "assistant")
                .put("content", text);
        when(llmClient.chat(any(), any())).thenReturn(new LlmClient.ChatResult(message, "stop", 1, 1));
    }

    private static String body(String message) {
        return "{\"message\":\"" + message + "\"}";
    }

    // ===== 悬浮窗 =====

    @Test
    void widgetShownOnPagesForLoggedInUser() throws Exception {
        mockMvc.perform(get("/stats").with(loginAs("agent_widget")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"agentFab\"")))
                .andExpect(content().string(containsString("/css/agent.css")));
    }

    @Test
    void widgetHiddenForAnonymousUser() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("id=\"agentFab\""))));
    }

    // ===== 接口 =====

    @Test
    void chatRequiresLogin() throws Exception {
        mockMvc.perform(post("/agent/chat").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("你好")))
                .andExpect(status().is3xxRedirection());

        verify(llmClient, never()).chat(any(), any());
    }

    @Test
    void chatRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/agent/chat").with(loginAs("agent_csrf"))
                        .contentType(MediaType.APPLICATION_JSON).content(body("你好")))
                .andExpect(status().isForbidden());
    }

    @Test
    void chatKeepsHistoryInSessionUntilCleared() throws Exception {
        modelAnswers("Hello!");
        RequestPostProcessor login = loginAs("agent_chat");
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/agent/chat").with(login).with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(body("你好")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Hello!"));

        // 同一个 Session 再取历史：一问一答
        mockMvc.perform(get("/agent/history").with(login).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].role").value("user"))
                .andExpect(jsonPath("$[1].content").value("Hello!"));

        mockMvc.perform(post("/agent/clear").with(login).with(csrf()).session(session))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/agent/history").with(login).session(session))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void blankMessageIsRejectedWithoutCallingModel() throws Exception {
        mockMvc.perform(post("/agent/chat").with(loginAs("agent_blank")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("消息不能为空"));

        verify(llmClient, never()).chat(any(), any());
    }

    @Test
    void modelFailureReturns503() throws Exception {
        when(llmClient.chat(any(), any())).thenThrow(new IOException("状态码 503"));

        mockMvc.perform(post("/agent/chat").with(loginAs("agent_fail")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body("你好")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("AI 服务暂时不可用，请稍后再试"));
    }
}