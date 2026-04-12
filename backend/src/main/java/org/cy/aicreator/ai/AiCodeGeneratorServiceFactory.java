package org.cy.aicreator.ai;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.cy.aicreator.ai.tools.FileWriteTool;
import org.cy.aicreator.exception.BusinessException;
import org.cy.aicreator.exception.ErrorCode;
import org.cy.aicreator.model.enums.GenerTypeEnum;
import org.cy.aicreator.service.ChatHistoryService;
import org.cy.aicreator.utils.SpringContextUtil;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 代码生成服务 工厂
 */
@Configuration
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatLanguageModel;

    @Resource
    private RedisChatMemoryStore redisChatMemoryStore;


    @Resource
    private ChatHistoryService chatHistoryService;



    /**
     * 创建AI代码生成服务（流式输出）
     *
     * @param appId 应用ID
     * @return AI代码生成服务
     */
    public AiCodeGeneratorService crateAiCodeGeneratorService(long appId, GenerTypeEnum codeType) {
        log.info("创建AI代码生成服务，appId：{}", appId);
        // 根据appId构建独立的对话记忆
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .id(appId)
                .maxMessages(20)
                .chatMemoryStore(redisChatMemoryStore)
                .build();
        // 创建对话记忆，数据库->对话记忆
        chatHistoryService.loadChatHistoryMemory(appId, chatMemory, 20);
        // 根据生成类型选择不同的模型配置
        return switch (codeType) {
            // HTML, 多文件模式->默认模型
            case HTML, MULTI_FILE -> {
                // 采用多例模式，避免并发问题
                StreamingChatModel streamingChatModelPrototype = SpringContextUtil
                        .getBean("streamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatLanguageModel)
                        .streamingChatModel(streamingChatModelPrototype)
                        .chatMemory(chatMemory)
                        .build();
            }

            // Vue工程模式->推理流式模型（带工具调用）
            case VUE_PROJECT -> {
                // 采用多例模式，避免并发问题
                StreamingChatModel reasoningStreamingChatModelPrototype = SpringContextUtil
                        .getBean("reasoningStreamingChatModelPrototype", StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .chatModel(chatLanguageModel)
                        .streamingChatModel(reasoningStreamingChatModelPrototype)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .tools(new FileWriteTool())
                        .hallucinatedToolNameStrategy(toolExecutionRequest -> ToolExecutionResultMessage
                                .from(toolExecutionRequest, "没有找到此工具：" + toolExecutionRequest.name()))
                        .build();
            }

            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的生成类型：" + codeType.getValue());
        };
    }


    /**
     * AI 服务缓存
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000) // 最大缓存数量
            .expireAfterWrite(Duration.ofMinutes(30)) // 写入后30分钟过期
            .expireAfterAccess(Duration.ofMinutes(10)) // 访问后10分钟过期
            .removalListener((key, value, cause) -> {
                log.info("AI 服务实例被移除，appId：{}，原因：{}", key, cause);
            })
            .build();

    /**
     * 根据appId获取AI代码生成服务（缓存）->兼容历史逻辑
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, GenerTypeEnum.HTML);
    }

    /**
     * 根据appId获取AI代码生成服务（缓存中的）
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, GenerTypeEnum codeGenType) {
        String cacheKey = getCacheKey(appId, codeGenType);
        // 从Caffeine缓存中获取服务实例
        return serviceCache.get(cacheKey, key -> crateAiCodeGeneratorService(appId, codeGenType));
    }

    /**
     * 获取缓存键
     *
     * @param appId       应用ID
     * @param codeGenType 生成类型
     * @return 缓存键
     */
    private String getCacheKey(long appId, GenerTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }

}
