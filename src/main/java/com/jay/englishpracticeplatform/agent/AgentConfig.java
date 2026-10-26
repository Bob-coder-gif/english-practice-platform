package com.jay.englishpracticeplatform.agent;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

// 让 Spring 把 agent.* 配置绑定到 AgentProperties
@Configuration
@EnableConfigurationProperties(AgentProperties.class)
public class AgentConfig {
}
