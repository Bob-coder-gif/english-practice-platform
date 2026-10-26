package com.jay.englishpracticeplatform.agent;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * AI 助手的配置，对应 application.properties 中的 agent.* 。
 * 用 record 绑定：不可变，构造时完成校验。
 */
@ConfigurationProperties(prefix = "agent")
public record AgentProperties(
        boolean enabled,
        String apiKey,
        String baseUrl,
        String model,
        int maxTokens,
        int maxSteps,
        Duration requestTimeout,
        int dailyLimit,          // 每人每天最多提问次数
        int globalDailyLimit     // 全站每天最多提问次数，0 表示不限制
) {
    public AgentProperties {
        // 和更新日志的处理思路一样：配置有问题就在启动时失败，而不是等到用户使用时才报错
        if (enabled) {
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException(
                        "agent.enabled=true，但没有配置 agent.api-key（环境变量 DEEPSEEK_API_KEY）");
            }
            if (maxTokens <= 0 || maxSteps <= 0) {
                throw new IllegalStateException("agent.max-tokens 和 agent.max-steps 必须大于 0");
            }
            if (dailyLimit <= 0 || globalDailyLimit < 0) {
                throw new IllegalStateException("agent.daily-limit 必须大于 0，agent.global-daily-limit 不能小于 0");
            }
        }
    }
}