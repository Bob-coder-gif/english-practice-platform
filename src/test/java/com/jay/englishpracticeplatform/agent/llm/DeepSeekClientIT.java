package com.jay.englishpracticeplatform.agent.llm;

import com.jay.englishpracticeplatform.agent.AgentProperties;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 联网测试：真的调用 DeepSeek（花钱很少，但需要网络和 key）。
 * 类名以 IT 结尾，mvnw test 默认不会运行它，只在 IDEA 里手动点运行。
 */
class DeepSeekClientIT {

    @Test
    void chatReturnsReply() throws Exception {
        String key = System.getenv("DEEPSEEK_API_KEY");
        assumeTrue(key != null && !key.isBlank(), "未设置 DEEPSEEK_API_KEY，跳过");

        // 不启动 Spring，直接 new：测试更快，也不需要数据库
        AgentProperties props = new AgentProperties(true, key,
                "https://api.deepseek.com/chat/completions", "deepseek-chat",
                50, 5, Duration.ofSeconds(60));
        JsonMapper mapper = JsonMapper.builder().build();
        DeepSeekClient client = new DeepSeekClient(props, mapper);

        ArrayNode messages = mapper.createArrayNode();
        messages.addObject()
                .put("role", "user")
                .put("content", "只回复两个字母：OK");

        LlmClient.ChatResult result = client.chat(messages, null);

        assertEquals("stop", result.finishReason());
        assertFalse(result.message().path("content").asString("").isBlank());
        assertTrue(result.promptTokens() > 0);
    }
}