package com.jay.englishpracticeplatform.agent.llm;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.lang.reflect.Array;

/*
    大模型客户端接口
    AgentService 只依赖这个接口，测试时换成按剧本返回结果的假实现
 */
public interface LlmClient {

    // 一次调用的结果：模型返回的消息，返回原因，本次用量
    record ChatResult(JsonNode message, String finishReason, int promptTokens, int completionTokens){

    }

    ChatResult chat(ArrayNode message, ArrayNode tools) throws IOException, InterruptedException;
}
