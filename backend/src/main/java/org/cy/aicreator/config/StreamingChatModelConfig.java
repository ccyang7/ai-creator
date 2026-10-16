package org.cy.aicreator.config;


import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatRequestParameters;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import jakarta.annotation.Resource;
import lombok.Data;
import org.cy.aicreator.aimonitor.AiMonitorListener;
import org.cy.aicreator.monitor.MonitorListener;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.List;
import java.util.Map;


/**
 * 流式模型配置
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.streaming-chat-model")
@Data
public class StreamingChatModelConfig {

    @Resource
    private MonitorListener monitorListener;

    @Resource
    private AiMonitorListener aiMonitorListener;

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private Integer maxTokens;

    private Double temperature;

    private boolean logRequests;

    private boolean logResponses;

    /**
     * 是否开启思考，不配置则不发送该参数，由模型自行决定。取值会作为 thinking.type 传给 OpenAI-compatible 接口
     */
    private Boolean thinkingEnabled;

    @Bean
    @Scope("prototype")
    public StreamingChatModel streamingChatModelPrototype() {
        var builder = OpenAiStreamingChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .listeners(List.of(monitorListener, aiMonitorListener));
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


