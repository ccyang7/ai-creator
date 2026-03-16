package org.cy.aicreator.ai;

import dev.langchain4j.service.SystemMessage;
import org.cy.aicreator.ai.model.HtmlCodeResult;
import org.cy.aicreator.ai.model.MultiFileCodeResult;

/**
 * AI 代码生成服务
 */
public interface AiCodeGeneratorService {

    /**
     * 生成 Html 代码
     *
     * @param userMessage 用户提示词
     * @return 响应结果
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);


    /**
     * 生成多文件代码
     *
     * @param userMessage 用户提示词
     * @return 响应结果
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);
}