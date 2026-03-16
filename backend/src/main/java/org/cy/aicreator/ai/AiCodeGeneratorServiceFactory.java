package org.cy.aicreator.ai;


import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 代码生成服务 工厂
 */
@Configuration
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatLanguageModel chatLanguageModel;


    /**
     * 创建AI代码生成服务
     *
     * @return AI代码生成服务Bean
     */
    @Bean
    public AiCodeGeneratorService getAiCodeGeneratorService() {
        return AiServices.create(AiCodeGeneratorService.class, chatLanguageModel);
    }

}
