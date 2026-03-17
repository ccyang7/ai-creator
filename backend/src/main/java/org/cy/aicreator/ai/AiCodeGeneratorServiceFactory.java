package org.cy.aicreator.ai;


import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 代码生成服务 工厂
 */
@Configuration
public class AiCodeGeneratorServiceFactory {

    // JSON结构化输出
    @Resource
    private ChatLanguageModel chatLanguageModel;

    // 流式输出
    @Resource
    private StreamingChatLanguageModel streamingChatLanguageModel;

//    /**
//     * 创建AI代码生成服务（结构化输出）
//     *
//     * @return AI代码生成服务Bean
//     */
//    @Bean
//    public AiCodeGeneratorService getAiCodeGeneratorService() {
//        return AiServices.create(AiCodeGeneratorService.class, chatLanguageModel);
//    }



    /**
     * 创建AI代码生成服务（流式输出）
     *
     * @return AI代码生成服务Bean
     */
    @Bean
    public AiCodeGeneratorService getAiCodeGeneratorService() {
        return AiServices.builder(AiCodeGeneratorService.class)
                .chatLanguageModel(chatLanguageModel)
                .streamingChatLanguageModel(streamingChatLanguageModel)
                .build();
    }


}
