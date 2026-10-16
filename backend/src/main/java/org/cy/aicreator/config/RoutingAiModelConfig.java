package org.cy.aicreator.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import jakarta.annotation.Resource;
import lombok.Data;
import org.cy.aicreator.aimonitor.AiMonitorListener;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.routing-chat-model")
@Data
public class RoutingAiModelConfig {

    @Resource
    private AiMonitorListener aiMonitorListener;

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private Integer maxTokens;

    private Double temperature;

    private Boolean logRequests = false;

    private Boolean logResponses = false;

    private Boolean thinkingEnabled;

    /**
     * 创建用于路由判断的模型
     */
    @Bean
    @Scope("prototype")
    public ChatModel routingChatModelPrototype() {
        var builder = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .baseUrl(baseUrl)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .logRequests(logRequests)
                .logResponses(logResponses)
                // 路由判断也是一次真实计费的模型调用，一并纳入监控
                .listeners(List.of(aiMonitorListener));
        if (thinkingEnabled != null) {
            builder.defaultRequestParameters(OpenAiChatRequestParameters.builder()
                    .customParameters(Map.of(
                            "thinking", Map.of("type", thinkingEnabled ? "enabled" : "disabled")
                    ))
                    .build());
        }
        return builder.build();
    }
}
