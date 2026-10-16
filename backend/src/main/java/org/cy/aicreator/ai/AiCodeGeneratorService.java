package org.cy.aicreator.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/**
 * AI 代码生成服务
 */
public interface AiCodeGeneratorService {




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

    /**
     * 生成 Vue 项目代码（流式）
     * <p>
     * 与其他两种类型不同，Vue 工程由模型通过 {@code FileWriteTool} 逐文件写入磁盘，
     * 因此需要 TokenStream 才能拿到工具调用过程与完成回调，供门面组装成 JSON 消息帧。
     *
     * @param appId       应用 ID（同时作为聊天记忆 ID）
     * @param userMessage 用户消息
     * @return 生成过程的 TokenStream
     */
    @SystemMessage(fromResource = "prompt/codegen-vue-project-system-prompt.txt")
    TokenStream generateVueProjectCodeStream(@MemoryId long appId, @UserMessage String userMessage);

}