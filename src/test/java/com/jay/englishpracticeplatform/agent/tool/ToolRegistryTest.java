package com.jay.englishpracticeplatform.agent.tool;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 纯单元测试：不启动 Spring、不连数据库、不调用大模型 */
class ToolRegistryTest {

    private final JsonMapper mapper = JsonMapper.builder().build();
    private final ToolContext ctx = new ToolContext(1L);

    /** 测试用工具：原样返回 text 参数和当前用户 id */
    static class EchoTool implements Tool {
        @Override public String name() { return "echo"; }
        @Override public String description() { return "回显"; }
        @Override public ObjectNode parameters() { return Tool.oneStringParam("text", "内容"); }
        @Override public String execute(JsonNode args, ToolContext ctx) {
            return ctx.userId() + ":" + args.path("text").asString("");
        }
    }

    /** 测试用工具：总是抛异常 */
    static class BrokenTool implements Tool {
        @Override public String name() { return "broken"; }
        @Override public String description() { return "坏的"; }
        @Override public ObjectNode parameters() { return Tool.noParams(); }
        @Override public String execute(JsonNode args, ToolContext ctx) {
            throw new IllegalStateException("数据库挂了");
        }
    }

    private ToolRegistry registry() {
        return new ToolRegistry(List.of(new EchoTool(), new BrokenTool()), mapper);
    }

    @Test
    void definitionsMatchOpenAiFormat() {
        ArrayNode defs = registry().definitions();

        assertEquals(2, defs.size());
        JsonNode echo = defs.get(0);
        assertEquals("function", echo.path("type").asString(""));
        assertEquals("echo", echo.path("function").path("name").asString(""));
        assertEquals("text", echo.path("function").path("parameters").path("required").get(0).asString(""));
    }

    @Test
    void definitionsReturnsCopy() {
        ToolRegistry registry = registry();
        registry.definitions().removeAll();          // 调用方误改了拿到的结果
        assertEquals(2, registry.definitions().size()); // 不影响下一次
    }

    @Test
    void executesToolWithContextUser() {
        assertEquals("1:hello", registry().execute("echo", "{\"text\":\"hello\"}", ctx));
    }

    @Test
    void unknownToolReturnsError() {
        assertTrue(registry().execute("nope", "{}", ctx).startsWith("错误"));
    }

    @Test
    void invalidJsonReturnsError() {
        assertTrue(registry().execute("echo", "{text:", ctx).startsWith("错误"));
    }

    @Test
    void blankArgumentsTreatedAsEmptyObject() {
        assertEquals("1:", registry().execute("echo", "", ctx));
    }

    @Test
    void toolExceptionBecomesErrorText() {
        String result = registry().execute("broken", "{}", ctx);
        assertTrue(result.startsWith("错误") && result.contains("数据库挂了"));
    }

    @Test
    void duplicateNamesFailFast() {
        assertThrows(IllegalStateException.class,
                () -> new ToolRegistry(List.of(new EchoTool(), new EchoTool()), mapper));
    }

    @Test
    void currentTimeUsesClock() {
        Clock fixed = Clock.fixed(Instant.parse("2026-10-20T06:00:00Z"), ZoneId.of("Asia/Shanghai"));
        String result = new GetCurrentTimeTool(fixed).execute(mapper.createObjectNode(), ctx);
        assertEquals("2026-10-20 14:00:00 星期二", result);
    }
}