package org.cy.aicreator.ai;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.cy.aicreator.service.ChatHistoryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * AI 代码生成服务 工厂
 */
@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    // JSON结构化输出
    @Resource
    private ChatLanguageModel chatLanguageModel;

    // 流式输出
    @Resource
    private StreamingChatLanguageModel streamingChatLanguageModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;


    @Resource
    private ChatHistoryService chatHistoryService;



    /**
     * 创建AI代码生成服务（流式输出）
     *
     * @param appId 应用ID
     * @return
     */
    public AiCodeGeneratorService crateAiCodeGeneratorService(long appId) {
        log.info("创建AI代码生成服务，appId：{}", appId);
        // 根据appId构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .maxMessages(20)
                .chatMemoryStore(redisChatMemoryStore)
                .build();
        // 创建对话记忆，数据库->对话记忆
        chatHistoryService.loadChatHistoryMemory(appId, chatMemory, 20);
        // 创建AI代码生成服务
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatLanguageModel(chatLanguageModel)
                .streamingChatLanguageModel(streamingChatLanguageModel)
                .chatMemory(chatMemory)
                .build();
    }

    /**
     * AI 服务缓存
     */
    private final Cache<Long, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000) // 最大缓存数量
            .expireAfterWrite(Duration.ofMinutes(30)) // 写入后30分钟过期
            .expireAfterAccess(Duration.ofMinutes(10)) // 访问后10分钟过期
            .removalListener((key, value, cause) -> {
                log.info("AI 服务实例被移除，appId：{}，原因：{}", key, cause);
            })
            .build();

    /**
     * 根据appId获取AI代码生成服务（缓存）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return serviceCache.get(appId, this::crateAiCodeGeneratorService);
    }


}
