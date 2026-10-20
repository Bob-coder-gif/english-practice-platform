package com.jay.englishpracticeplatform.agent;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.core.parameters.P;

import java.time.Duration;

/**
 * AI 助手的配置，对应 application.properties 中的 agent.* 。
 * 用 record 绑定：不可变，构造时完成校验。
 */
@ConfigurationProperties(prefix = "agent")
public record AgentProperties (
        boolean enabled,
        String apiKey,
        String baseUrl,
        String model,
        int maxTokens,
        int maxSteps,
        Duration requestTimeout
){
    public AgentProperties{
        // 和更新日志的处理思路一样：配置有问题就在启动时失败，而不是等到用户使用时才报错
        if(enabled) {
            if(apiKey == null || apiKey.isBlank()){
                throw new IllegalStateException(
                        "agent.enabled=true,但没有配置 agent.api-key(环境变量 DEEPSEEK_API_KEY)"
                );
            }
            if(maxTokens <= 0 || maxSteps <= 0){
                throw new IllegalStateException("agent.max-tokens 和 agent.max-steps 必须大于0");
            }
        }
    }
}
