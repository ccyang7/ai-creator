package org.cy.aicreator.ai;

import dev.langchain4j.service.SystemMessage;

import org.cy.aicreator.model.enums.GenerTypeEnum;

/**
 * AI 代码生成类型智能路由服务
 * 使用结构化 JSON 输出返回路由决策
 */
public interface SmartRoutingService {

    /**
     * 根据用户需求智能选择代码生成类型
     *
     * @param userPrompt 用户输入的需求描述
     * @return 路由决策结果，包含类型字段
     */
    @SystemMessage(fromResource = "prompt/smart-router-prompt.txt")
    GenerTypeEnum routeCodeGenType(String userPrompt);


    /**
     * 根据用户需求智能生成应用名称
     *
     * @param userPrompt 用户输入的需求描述
     * @return 智能生成的应用名称
     */
    @SystemMessage(fromResource = "prompt/smart-router-app-name-prompt.txt")
    String routeAppName(String userPrompt);
}
