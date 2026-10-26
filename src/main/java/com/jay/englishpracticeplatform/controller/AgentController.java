package com.jay.englishpracticeplatform.controller;

import com.jay.englishpracticeplatform.agent.AgentService;
import com.jay.englishpracticeplatform.agent.AgentService.AgentReply;
import com.jay.englishpracticeplatform.agent.AgentService.ChatLine;
import com.jay.englishpracticeplatform.agent.AgentUsageService;
import com.jay.englishpracticeplatform.agent.tool.ToolContext;
import com.jay.englishpracticeplatform.exception.AgentException;
import com.jay.englishpracticeplatform.exception.AgentQuotaExceededException;
import com.jay.englishpracticeplatform.security.AuthUser;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

import java.util.List;
import java.util.Map;

/**
 * AI 助手的 JSON 接口，供每个页面右下角的悬浮窗调用。
 * 没有页面了，所以用 @RestController：所有方法的返回值都直接写成 JSON，不再需要逐个加 @ResponseBody。
 * agent.enabled=false 时这个 Controller 不会被创建，/agent 下的地址全部 404。
 */
@Slf4j
@RestController
@RequestMapping("/agent")
@ConditionalOnProperty(prefix = "agent", name = "enabled", havingValue = "true")
public class AgentController {

    // 对话历史存在 Session 里的键
    static final String SESSION_KEY = "agent.history";

    private final AgentService agentService;
    private final AgentUsageService usageService;
    private final JsonMapper mapper;

    public AgentController(AgentService agentService, AgentUsageService usageService, JsonMapper mapper) {
        this.agentService = agentService;
        this.usageService = usageService;
        this.mapper = mapper;
    }

    public record ChatRequest(String message) {}

    /** remaining：这次提问之后，今天还剩几次 */
    public record ChatResponse(String reply, List<AgentService.ToolCall> toolCalls, int remaining) {}

    @PostMapping("/chat")
    public ChatResponse chat(@AuthenticationPrincipal AuthUser loginUser,
                             @RequestBody ChatRequest request,
                             HttpSession session) {
        ArrayNode history = loadHistory(session);
        // 用户身份只从登录状态获取，经 ToolContext 传给工具，不经过模型
        AgentReply reply = agentService.chat(history, request.message(), new ToolContext(loginUser.getId()));
        // 只有成功时才保存：失败时 history 已经被回滚，不保存也一样
        saveHistory(session, history);
        int remaining = usageService.quota(loginUser.getId()).remaining();
        return new ChatResponse(reply.reply(), reply.toolCalls(), remaining);
    }

    @GetMapping("/history")
    public List<ChatLine> history(HttpSession session) {
        return AgentService.visibleLines(loadHistory(session));
    }

    /** 今天的额度：悬浮窗打开时显示「今日剩余 N 次」 */
    @GetMapping("/quota")
    public AgentUsageService.Quota quota(@AuthenticationPrincipal AuthUser loginUser) {
        return usageService.quota(loginUser.getId());
    }

    @PostMapping("/clear")
    public ResponseEntity<Void> clear(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
        return ResponseEntity.noContent().build();
    }

    // ===== Session 中的历史：存成 JSON 字符串 =====

    // 每个请求都从字符串解析出一份新的 ArrayNode，请求之间不共享可变对象
    private ArrayNode loadHistory(HttpSession session) {
        if (session.getAttribute(SESSION_KEY) instanceof String json) {
            try {
                if (mapper.readTree(json) instanceof ArrayNode history) {
                    return history;
                }
            } catch (JacksonException e) {
                log.warn("Session 中的对话历史无法解析，已丢弃", e);
            }
        }
        return mapper.createArrayNode();
    }

    private void saveHistory(HttpSession session, JsonNode history) {
        session.setAttribute(SESSION_KEY, mapper.writeValueAsString(history));
    }

    // ===== 错误：返回 JSON，悬浮窗直接显示 error 字段 =====

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    // 额度用完：429 Too Many Requests。它是 AgentException 的子类，
    // Spring 会选择匹配得最具体的处理方法，所以走这里而不是下面的 503
    @ExceptionHandler(AgentQuotaExceededException.class)
    public ResponseEntity<Map<String, String>> quotaExceeded(AgentQuotaExceededException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(AgentException.class)
    public ResponseEntity<Map<String, String>> unavailable(AgentException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", e.getMessage()));
    }
}