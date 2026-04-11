package org.cy.aicreator.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI代码生成类型路由服务工厂
 */
@Slf4j
@Configuration
public class SmartRoutingServiceFactory {

    @Resource
    private ChatLanguageModel chatLanguageModel;

    /**
     * 创建AI代码生成类型路由服务实例
     */
    @Bean
    public SmartRoutingService smartCodeGenTypeRoutingService() {
        return AiServices.builder(SmartRoutingService.class)
                .chatLanguageModel(chatLanguageModel)
                .build();
    }
}
