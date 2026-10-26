package com.jay.englishpracticeplatform.agent;

import com.jay.englishpracticeplatform.agent.llm.LlmClient;
import com.jay.englishpracticeplatform.agent.llm.LlmClient.ChatResult;
import com.jay.englishpracticeplatform.agent.tool.Tool;
import com.jay.englishpracticeplatform.agent.tool.ToolContext;
import com.jay.englishpracticeplatform.agent.tool.ToolRegistry;
import com.jay.englishpracticeplatform.exception.AgentException;
import com.jay.englishpracticeplatform.exception.AgentQuotaExceededException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * agent 循环的单元测试：用按剧本返回结果的假模型，不联网、不花钱、每次结果都一样。
 * 这就是 M1 抽出 LlmClient 接口的原因。
 */
class AgentServiceTest {

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final ToolContext ctx = new ToolContext(7L);
    // 额度服务用 Mockito 的假对象：默认 acquire 什么也不做（表示额度充足），需要时再让它抛异常
    private final AgentUsageService usage = mock(AgentUsageService.class);

    // ===== 测试替身 =====

    /** 假模型：按顺序返回预先写好的结果，并记下每次收到的 messages */
    static class FakeLlmClient implements LlmClient {
        private final Deque<Object> script = new ArrayDeque<>();
        final List<ArrayNode> requests = new ArrayList<>();

        FakeLlmClient then(ChatResult result) {
            script.add(result);
            return this;
        }

        FakeLlmClient thenFail(IOException e) {
            script.add(e);
            return this;
        }

        @Override
        public ChatResult chat(ArrayNode messages, ArrayNode tools) throws IOException {
            requests.add(messages.deepCopy());
            Object next = script.poll();
            if (next == null) {
                throw new AssertionError("剧本里没有更多结果了：agent 多请求了一次模型");
            }
            if (next instanceof IOException e) {
                throw e;
            }
            return (ChatResult) next;
        }
    }

    /** 测试工具：返回「当前用户 id:text」，用来验证工具拿到的是 ctx 里的用户 */
    static class EchoTool implements Tool {
        @Override public String name() { return "echo"; }
        @Override public String description() { return "回显"; }
        @Override public ObjectNode parameters() { return Tool.oneStringParam("text", "内容"); }
        @Override public String execute(JsonNode args, ToolContext ctx) {
            return ctx.userId() + ":" + args.path("text").asString("");
        }
    }

    private AgentService service(FakeLlmClient llm, int maxSteps) {
        AgentProperties props = new AgentProperties(true, "test-key", "http://localhost",
                "test-model", 100, maxSteps, Duration.ofSeconds(5), 30, 0);
        ToolRegistry registry = new ToolRegistry(List.of(new EchoTool()), mapper);
        return new AgentService(llm, registry, mapper, props, usage);
    }

    /** 模型直接回答 */
    private ChatResult answer(String text) {
        ObjectNode message = mapper.createObjectNode()
                .put("role", "assistant")
                .put("content", text);
        return new ChatResult(message, "stop", 10, 5);
    }

    /** 模型要求调用一个工具 */
    private ChatResult callTool(String id, String name, String arguments) {
        ObjectNode message = mapper.createObjectNode().put("role", "assistant");
        message.putNull("content");
        ObjectNode call = message.putArray("tool_calls").addObject()
                .put("id", id)
                .put("type", "function");
        call.putObject("function")
                .put("name", name)
                .put("arguments", arguments);
        return new ChatResult(message, "tool_calls", 10, 5);
    }

    private static String role(JsonNode message) {
        return message.path("role").asString("");
    }

    // ===== 测试 =====

    @Test
    void directAnswer() {
        FakeLlmClient llm = new FakeLlmClient().then(answer("你好！"));
        ArrayNode history = mapper.createArrayNode();

        AgentService.AgentReply reply = service(llm, 5).chat(history, "  你好  ", ctx);

        assertEquals("你好！", reply.reply());
        assertEquals(1, reply.steps());
        assertEquals(15, reply.tokens());
        assertTrue(reply.toolCalls().isEmpty());

        // 历史里是 user + assistant，输入的首尾空格已去掉
        assertEquals(2, history.size());
        assertEquals("你好", history.get(0).path("content").asString(""));
        // system 消息只在请求里，不存进历史
        assertEquals("system", role(llm.requests.get(0).get(0)));
        assertEquals("user", role(history.get(0)));
    }

    @Test
    void toolCallThenAnswer() {
        FakeLlmClient llm = new FakeLlmClient()
                .then(callTool("call_1", "echo", "{\"text\":\"hi\"}"))
                .then(answer("完成"));
        ArrayNode history = mapper.createArrayNode();

        AgentService.AgentReply reply = service(llm, 5).chat(history, "测试", ctx);

        assertEquals("完成", reply.reply());
        assertEquals(2, reply.steps());
        // 工具调用过程：名字、模型给的参数、工具返回的结果
        assertEquals(List.of(new AgentService.ToolCall("echo", "{\"text\":\"hi\"}", "7:hi")),
                reply.toolCalls());
        // user → assistant(tool_calls) → tool → assistant
        assertEquals(4, history.size());
        JsonNode toolMessage = history.get(2);
        assertEquals("tool", role(toolMessage));
        assertEquals("call_1", toolMessage.path("tool_call_id").asString(""));
        // 工具拿到的是 ctx 里的用户 7
        assertEquals("7:hi", toolMessage.path("content").asString(""));

        // 第二次请求时，模型能看到工具结果
        ArrayNode second = llm.requests.get(1);
        assertEquals("tool", role(second.get(second.size() - 1)));
    }

    @Test
    void tooManyStepsRollsBack() {
        FakeLlmClient llm = new FakeLlmClient()
                .then(callTool("c1", "echo", "{\"text\":\"1\"}"))
                .then(callTool("c2", "echo", "{\"text\":\"2\"}"));
        ArrayNode history = mapper.createArrayNode();

        assertThrows(AgentException.class, () -> service(llm, 2).chat(history, "一直调用工具", ctx));
        // 整轮回滚：不留下半截的工具调用
        assertEquals(0, history.size());
    }

    @Test
    void llmFailureRollsBackAndKeepsOldHistory() {
        FakeLlmClient llm = new FakeLlmClient()
                .then(answer("第一轮回答"))
                .thenFail(new IOException("状态码 503"));
        AgentService service = service(llm, 5);
        ArrayNode history = mapper.createArrayNode();

        service.chat(history, "第一轮", ctx);
        AgentException e = assertThrows(AgentException.class, () -> service.chat(history, "第二轮", ctx));

        // 给用户看的消息不包含技术细节
        assertEquals("AI 服务暂时不可用，请稍后再试", e.getMessage());
        // 第一轮还在，第二轮的 user 消息被撤回
        assertEquals(2, history.size());
    }

    @Test
    void invalidInputDoesNotCallModel() {
        FakeLlmClient llm = new FakeLlmClient();   // 空剧本：一旦请求模型就会失败
        AgentService service = service(llm, 5);
        ArrayNode history = mapper.createArrayNode();

        assertThrows(IllegalArgumentException.class, () -> service.chat(history, "   ", ctx));
        assertThrows(IllegalArgumentException.class, () -> service.chat(history, null, ctx));
        assertThrows(IllegalArgumentException.class,
                () -> service.chat(history, "a".repeat(AgentService.MAX_INPUT_LENGTH + 1), ctx));
        assertTrue(llm.requests.isEmpty());
        assertEquals(0, history.size());
        // 输入不合法时不扣额度
        verify(usage, never()).acquire(anyLong());
    }

    @Test
    void trimHistoryStartsFromUserMessage() {
        ArrayNode history = mapper.createArrayNode();
        history.addObject().put("role", "user").put("content", "旧问题");
        history.add(callTool("c1", "echo", "{}").message());
        history.addObject().put("role", "tool").put("tool_call_id", "c1").put("content", "结果");
        history.addObject().put("role", "assistant").put("content", "旧回答");
        history.addObject().put("role", "user").put("content", "新问题");
        history.addObject().put("role", "assistant").put("content", "新回答");

        // 保留 4 条的话，开头会是一条孤立的 tool 消息；所以继续往后删，直到 user 消息
        AgentService.trimHistory(history, 4);

        assertEquals(2, history.size());
        assertEquals("新问题", history.get(0).path("content").asString(""));
    }

    @Test
    void visibleLinesAttachToolCallsToAnswer() {
        ArrayNode history = mapper.createArrayNode();
        history.addObject().put("role", "user").put("content", "问题");
        history.add(callTool("c1", "echo", "{\"text\":\"x\"}").message());   // content 为 null
        history.addObject().put("role", "tool").put("tool_call_id", "c1").put("content", "结果");
        history.addObject().put("role", "assistant").put("content", "回答");
        history.addObject().put("role", "user").put("content", "第二个问题");
        history.addObject().put("role", "assistant").put("content", "直接回答");

        List<AgentService.ChatLine> lines = AgentService.visibleLines(history);

        assertEquals(List.of(
                new AgentService.ChatLine("user", "问题", List.of()),
                new AgentService.ChatLine("assistant", "回答",
                        List.of(new AgentService.ToolCall("echo", "{\"text\":\"x\"}", "结果"))),
                new AgentService.ChatLine("user", "第二个问题", List.of()),
                // 新的一轮没有调用工具，不能带上上一轮的工具
                new AgentService.ChatLine("assistant", "直接回答", List.of())
        ), lines);
    }

    @Test
    void quotaExceededDoesNotCallModel() {
        doThrow(new AgentQuotaExceededException("今天的次数用完了")).when(usage).acquire(7L);
        FakeLlmClient llm = new FakeLlmClient();   // 空剧本：一旦请求模型就会失败
        ArrayNode history = mapper.createArrayNode();

        assertThrows(AgentQuotaExceededException.class, () -> service(llm, 5).chat(history, "你好", ctx));
        assertTrue(llm.requests.isEmpty());
        assertEquals(0, history.size());
    }
}