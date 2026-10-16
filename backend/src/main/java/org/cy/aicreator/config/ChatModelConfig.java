package org.cy.aicreator.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import jakarta.annotation.Resource;
import lombok.Data;
import org.cy.aicreator.monitor.MonitorListener;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * 默认对话模型配置
 * <p>
 * 该 Bean 原先由 LangChain4j 的 OpenAI 自动配置提供，因其强制使用 Spring RestClient 实现
 * （读取 chunked 响应会被截断，原因见 application.yml 中的排除说明）已被排除，
 * 故在此按配置文件的 langchain4j.open-ai.chat-model 手工构建等价模型。
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
@Data
public class ChatModelConfig {

    @Resource
    private MonitorListener monitorListener;

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private Integer maxTokens;

    private Double temperature;

    private Boolean logRequests = false;

    private Boolean logResponses = false;

    private Integer readTimeout;

    /**
     * 默认对话模型（AiServices 的非流式调用）
     * Bean 名称保持与自动配置一致，以便按名称注入
     */
    @Bean(name = "openAiChatModel")
    public ChatModel openAiChatModel() {
        var builder = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .listeners(List.of(monitorListener));
        if (readTimeout != null) {
            builder.timeout(Duration.ofSeconds(readTimeout));
        }
        return builder.build();
    }
}
