package com.jay.englishpracticeplatform.agent.tool;

/**
 * 执行工具时的上下文：当前是哪个用户。
 * M3 中由 Controller 从 Spring Security 的登录状态取出后传入，绝不来自模型生成的参数。
 * 和平台"不信任请求中提交的用户 id"是同一条原则。
 */
public record ToolContext(long userId) {

}
