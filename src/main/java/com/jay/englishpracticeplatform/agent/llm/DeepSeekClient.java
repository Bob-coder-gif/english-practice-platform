package com.jay.englishpracticeplatform.agent.llm;

import com.jay.englishpracticeplatform.agent.AgentProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

/*
    DeepSeek 实现：从 mini agent 移植，改用 Jackson3 和 Spring 注入
    只有 agent.enabled=true 是在创建这个 Bean
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "agent", name = "enabled", havingValue = "true")
public class DeepSeekClient implements  LlmClient{

    // 重试配置
    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_DELAY_MS = 1500;
    private static final Set<Integer> RETRYABLE = Set.of(429,500,502,503,504);

    private final AgentProperties props;
    private final JsonMapper mapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    //只有一个构造器，Spring 会自动从他注入，不需要@Autowired
    public DeepSeekClient(AgentProperties props,JsonMapper mapper){
        this.props = props;
        this.mapper = mapper;
    }

    @Override
    public ChatResult chat(ArrayNode messages, ArrayNode tools) throws IOException, InterruptedException {
        // 构造请求体
        ObjectNode body = mapper.createObjectNode();
        body.put("model",props.model());
        body.put("max_tokens",props.maxTokens());   //硬约束，单词回答上限
        body.set("messages",messages);
        if(tools != null && !tools.isEmpty()){
            body.set("tools",tools);
        }

        // 构造请求，不可变，重试是直接复用
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(props.baseUrl()))
                .timeout(props.requestTimeout())
                .header("Content-Type","application/json")
                .header("Authorization","Bearer " + props.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)))
                .build();
        
        // 发送，临时错误时重试
        HttpResponse<String> response = sendWithRetry(request);
        
        // 解析： Jackson 3 里 asText 改名为 asString
        JsonNode root = mapper.readTree(response.body());
        JsonNode choice = root.path("choices").path(0);
        JsonNode message = choice.path("message");
        if(message.isMissingNode()){
            throw new IOException("响应中没有 message:" + response.body());
        }
        JsonNode usage = root.path("usage");
        
        return new ChatResult(message,
                choice.path("finish_reason").asString(""),
                usage.path("prompt_tokens").asInt(),
                usage.path("completion_tokens").asInt());
    }

    //遇到临时问题 按 1秒 2秒的间隔重试，最多共尝试 MAX_ATTEMPTS 次
    private HttpResponse<String> sendWithRetry(HttpRequest request) 
            throws IOException, InterruptedException{
        for (int attempt = 1; ; attempt++){
            HttpResponse<String> response;
            try {
                response = httpClient.send(request,HttpResponse.BodyHandlers.ofString());
            }
            catch (IOException e){
                if(attempt >= MAX_ATTEMPTS) throw e;
                backoff(attempt, "网络错误:" + e.getMessage());
                continue;
            }

            int status = response.statusCode();
            if(status == 200){
                return response;
            }

            String location = response.headers().firstValue("location").orElse("");
            //重定向会同一个地址 = 稍后重试， 重定向到别处不更随，防止 key 被带走
            boolean retryableRedirect = (status == 307 || status == 308)
                    && location.equals(props.baseUrl());

            if((RETRYABLE.contains(status) || retryableRedirect) && attempt < MAX_ATTEMPTS){
                backoff(attempt,"状态码" + status);
                continue;
            }

            throw new IOException("DeepSeek 返回状态码 " + status
                    + (location.isEmpty() ? "" : "，重定向地址：" + location)
                    + "（已尝试 " + attempt + " 次）：" + response.body());
        }
    }

    // 指数退避：1 秒、2 秒、4 秒……
    private void backoff(int attempt, String reason) throws InterruptedException{
        long delay = BASE_DELAY_MS * (1L << (attempt -1));
        log.warn("DeepSeek 临时错误({}) , {} 毫秒后第 {} 次重试",reason, delay,attempt + 1);
        Thread.sleep(delay);
    }
}
