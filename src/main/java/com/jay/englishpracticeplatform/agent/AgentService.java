package com.jay.englishpracticeplatform.agent;

import com.jay.englishpracticeplatform.agent.llm.LlmClient;
import com.jay.englishpracticeplatform.agent.tool.ToolContext;
import com.jay.englishpracticeplatform.agent.tool.ToolRegistry;
import com.jay.englishpracticeplatform.exception.AgentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 助手的核心：循环「请求模型 → 执行工具 → 再请求」，直到模型给出最终回答。
 * 逻辑和 mini agent 第 4 步的 Agent.run 相同。
 *
 * 对话历史由调用方保存和传入（Controller 存在 Session 里），这里不保存任何状态，
 * 所以这个单例 Bean 可以被多个用户的请求同时使用。
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "agent", name = "enabled", havingValue = "true")
public class AgentService {

    // 用户一次最多输入多少字
    public static final int MAX_INPUT_LENGTH = 500;
    // 保存的历史最多多少条消息（不含 system）：历史越长，每次请求的输入 token 越多
    public static final int MAX_HISTORY_MESSAGES = 40;
    // 页面上展示的工具结果最多多少字（发给模型的结果不截断）
    static final int MAX_DISPLAY_OUTPUT = 500;

    // 系统提示词不存进历史，每次请求时临时加在最前面：修改提示词后，已有的对话也立即生效
    static final String SYSTEM_PROMPT = """
            你是「英语练习打卡平台」的学习助手，帮助当前登录的用户学习英语单词。
            规则：
            1. 解释单词含义时，先用 search_words 查询平台词典，以查询结果为准；词典中没有的，要明确告诉用户。
            2. 用户的学习数据（统计、待复习、错题本、打卡）只能通过工具获取，不要猜测或编造数字。
            3. 你只能查看数据，不能修改学习记录、答题记录和打卡；用户要求修改时，告诉他到对应页面操作。
            4. 与英语学习无关、或要求输出大量内容的请求，简短说明后引导回英语学习。
            5. 回答简洁，使用纯文本，不要使用 Markdown 格式（页面不会渲染 Markdown）。
            6. 你能做的只有调用上面提供的工具。没有对应工具的操作（比如清空对话、修改设置），
               绝不能说自己已经做了；用户想清空对话时，告诉他点右上角的「清空」按钮，或者输入 /clear。
            """;

    /** 一次工具调用：工具名、模型给的参数、工具返回的结果（页面展示用，可能被截断） */
    public record ToolCall(String name, String arguments, String output) {}

    /** 一轮对话的结果：最终回答、请求了几次模型、消耗的 token、这一轮的工具调用过程 */
    public record AgentReply(String reply, int steps, int tokens, List<ToolCall> toolCalls) {}

    /** 页面上显示的一条消息；助手的回答附带得出这个回答之前调用过的工具 */
    public record ChatLine(String role, String content, List<ToolCall> toolCalls) {}

    private final LlmClient llm;
    private final ToolRegistry registry;
    private final JsonMapper mapper;
    private final AgentUsageService usageService;
    private final int maxSteps;

    public AgentService(LlmClient llm, ToolRegistry registry, JsonMapper mapper,
                        AgentProperties props, AgentUsageService usageService) {
        this.llm = llm;
        this.registry = registry;
        this.mapper = mapper;
        this.usageService = usageService;
        this.maxSteps = props.maxSteps();
    }

    /**
     * 处理用户的一句话。
     * 成功：这一轮产生的消息都追加到 history 里，并截断过长的历史；
     * 失败：history 恢复到调用前的样子，然后抛出异常。
     */
    public AgentReply chat(ArrayNode history, String input, ToolContext ctx) {
        String text = input == null ? "" : input.strip();
        if (text.isEmpty()) {
            throw new IllegalArgumentException("消息不能为空");
        }
        if (text.length() > MAX_INPUT_LENGTH) {
            throw new IllegalArgumentException("一条消息最多 " + MAX_INPUT_LENGTH + " 个字");
        }

        // 输入合法才扣额度：空消息、超长消息不算次数。额度用完会抛出异常，不会请求模型
        usageService.acquire(ctx.userId());

        int turnStart = history.size();
        history.addObject()
                .put("role", "user")
                .put("content", text);

        try {
            AgentReply reply = loop(history, ctx);
            trimHistory(history, MAX_HISTORY_MESSAGES);
            return reply;
        } catch (RuntimeException e) {
            rollback(history, turnStart);
            throw e;
        }
    }

    // ===== agent 循环 =====
    private AgentReply loop(ArrayNode history, ToolContext ctx) {
        ArrayNode toolDefs = registry.definitions();
        List<ToolCall> toolCalls = new ArrayList<>();
        int tokens = 0;

        for (int step = 1; step <= maxSteps; step++) {
            LlmClient.ChatResult result = callLlm(withSystemPrompt(history), toolDefs);
            history.add(result.message());
            tokens += result.promptTokens() + result.completionTokens();

            // 模型没有要求调用工具：答完了
            if (!"tool_calls".equals(result.finishReason())) {
                // 日志只记录用户 id、步数、token 和工具名，不记录对话内容
                log.info("agent 完成：用户 {}，{} 次请求，{} tokens，工具 {}",
                        ctx.userId(), step, tokens, toolNames(toolCalls));
                return new AgentReply(result.message().path("content").asString(""),
                        step, tokens, List.copyOf(toolCalls));
            }

            // 执行这一步请求的所有工具，结果放回历史，然后进入下一次循环
            for (JsonNode call : result.message().path("tool_calls")) {
                String id = call.path("id").asString("");
                String name = call.path("function").path("name").asString("");
                String arguments = call.path("function").path("arguments").asString("");

                String output = registry.execute(name, arguments, ctx);
                toolCalls.add(new ToolCall(name, arguments, shortenForDisplay(output)));

                // 发给模型的是完整结果
                history.addObject()
                        .put("role", "tool")
                        .put("tool_call_id", id)
                        .put("content", output);
            }
        }

        log.warn("agent 超过最大步数 {}：用户 {}，{} tokens，工具 {}",
                maxSteps, ctx.userId(), tokens, toolNames(toolCalls));
        throw new AgentException("这个问题需要的步骤太多了，请换一个更具体的问法");
    }

    /** 请求模型；技术错误转成给用户看的 AgentException，详细原因写进日志 */
    private LlmClient.ChatResult callLlm(ArrayNode messages, ArrayNode toolDefs) {
        try {
            return llm.chat(messages, toolDefs);
        } catch (IOException e) {
            log.warn("调用大模型失败", e);
            throw new AgentException("AI 服务暂时不可用，请稍后再试", e);
        } catch (InterruptedException e) {
            // 规范做法：恢复中断标志，让线程的管理者（这里是 Tomcat）知道它被中断过
            Thread.currentThread().interrupt();
            throw new AgentException("请求被中断，请重试", e);
        }
    }

    /** 每次请求临时拼上 system 消息；history 里的节点只是被引用，不会被复制 */
    private ArrayNode withSystemPrompt(ArrayNode history) {
        ArrayNode messages = mapper.createArrayNode();
        messages.addObject()
                .put("role", "system")
                .put("content", SYSTEM_PROMPT);
        for (JsonNode message : history) {
            messages.add(message);
        }
        return messages;
    }

    private static void rollback(ArrayNode history, int size) {
        while (history.size() > size) {
            history.remove(history.size() - 1);
        }
    }

    /**
     * 截断历史：从最前面删掉旧消息，直到不超过 max 条。
     * 删完后历史必须从一条 user 消息开始：如果从中间切断，开头可能是一条孤立的 tool 消息
     * （它对应的带 tool_calls 的 assistant 消息被删掉了），API 会因此返回 400。
     */
    static void trimHistory(ArrayNode history, int max) {
        int cut = Math.max(0, history.size() - max);
        if (cut == 0) {
            return;
        }
        while (cut < history.size() && !"user".equals(history.get(cut).path("role").asString(""))) {
            cut++;
        }
        for (int i = 0; i < cut; i++) {
            history.remove(0);
        }
    }

    // ===== 页面展示 =====

    /**
     * 把历史整理成页面上显示的消息。
     * 一轮对话在历史里是：user → assistant(tool_calls) → tool → … → assistant(最终回答)，
     * 这里把中间的工具调用和结果按 tool_call_id 配对，挂到最终回答上。
     */
    public static List<ChatLine> visibleLines(ArrayNode history) {
        List<ChatLine> lines = new ArrayList<>();
        List<ToolCall> pending = new ArrayList<>();          // 本轮已调用、还没挂到回答上的工具
        Map<String, Integer> indexById = new HashMap<>();    // tool_call_id → 在 pending 中的位置

        for (JsonNode message : history) {
            switch (message.path("role").asString("")) {
                case "user" -> {
                    pending.clear();
                    indexById.clear();
                    String text = textOf(message);
                    if (!text.isBlank()) {
                        lines.add(new ChatLine("user", text, List.of()));
                    }
                }
                case "assistant" -> {
                    JsonNode calls = message.path("tool_calls");
                    if (calls.isEmpty()) {
                        // 没有 tool_calls：这是这一轮的最终回答
                        String text = textOf(message);
                        if (!text.isBlank()) {
                            lines.add(new ChatLine("assistant", text, List.copyOf(pending)));
                        }
                        pending.clear();
                        indexById.clear();
                    } else {
                        // 中间步骤：先记下调用了什么，结果等后面的 tool 消息来填
                        for (JsonNode call : calls) {
                            indexById.put(call.path("id").asString(""), pending.size());
                            pending.add(new ToolCall(
                                    call.path("function").path("name").asString(""),
                                    call.path("function").path("arguments").asString(""),
                                    ""));
                        }
                    }
                }
                case "tool" -> {
                    Integer index = indexById.get(message.path("tool_call_id").asString(""));
                    if (index != null) {
                        ToolCall call = pending.get(index);
                        pending.set(index, new ToolCall(call.name(), call.arguments(),
                                shortenForDisplay(textOf(message))));
                    }
                }
                default -> {
                    // 其他角色不显示
                }
            }
        }
        return lines;
    }

    // content 可能是 null（带 tool_calls 的 assistant 消息），这种情况返回空字符串
    private static String textOf(JsonNode message) {
        JsonNode content = message.path("content");
        return content.isString() ? content.asString("") : "";
    }

    static String shortenForDisplay(String output) {
        if (output == null) {
            return "";
        }
        return output.length() > MAX_DISPLAY_OUTPUT
                ? output.substring(0, MAX_DISPLAY_OUTPUT) + "…（共 " + output.length() + " 字）"
                : output;
    }

    private static List<String> toolNames(List<ToolCall> calls) {
        return calls.stream().map(ToolCall::name).toList();
    }
}