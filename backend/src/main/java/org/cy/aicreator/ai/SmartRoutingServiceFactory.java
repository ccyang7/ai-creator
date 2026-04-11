package org.cy.aicreator.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.cy.aicreator.utils.SpringContextUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.swing.*;

/**
 * AI代码生成类型路由服务工厂
 */
@Slf4j
@Configuration
public class SmartRoutingServiceFactory {


    /**
     * 创建AI代码生成类型路由服务实例
     * 多例模式，避免并发问题
     */
    public SmartRoutingService createSmartCodeGenTypeRoutingService() {
        ChatLanguageModel chatModelPrototype = SpringContextUtil.getBean("routingChatModelPrototype", ChatLanguageModel.class);
        return AiServices.builder(SmartRoutingService.class)
                .chatLanguageModel(chatModelPrototype)
                .build();
    }

    @Bean
    public SmartRoutingService smartRoutingService() {
        return createSmartCodeGenTypeRoutingService();
    }
}
