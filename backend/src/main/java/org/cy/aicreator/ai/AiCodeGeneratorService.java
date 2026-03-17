package org.cy.aicreator.ai;

import dev.langchain4j.service.SystemMessage;
import org.cy.aicreator.ai.model.HtmlCodeResult;
import org.cy.aicreator.ai.model.MultiFileCodeResult;
import reactor.core.publisher.Flux;

/**
 * AI 代码生成服务
 */
public interface AiCodeGeneratorService {

    /**
     * 生成 Html 代码（JSON结构化输出）
     *
     * @param userMessage 用户提示词
     * @return 响应结果
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);


    /**
     * 生成多文件代码（JSON结构化输出）
     *
     * @param userMessage 用户提示词
     * @return 响应结果
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);



    /**
     * 生成 Html 代码（流式输出）
     *
     * @param userMessage 用户提示词
     * @return 响应结果
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(String userMessage);


    /**
     * 生成多文件代码（流式输出）
     *
     * @param userMessage 用户提示词
     * @return 响应结果
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    Flux<String> generateMultiFileCodeStream(String userMessage);
}