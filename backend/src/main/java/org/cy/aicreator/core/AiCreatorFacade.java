package org.cy.aicreator.core;

import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.cy.aicreator.ai.AiCodeGeneratorService;
import org.cy.aicreator.ai.AiCodeGeneratorServiceFactory;
import org.cy.aicreator.ai.model.HtmlCodeResult;
import org.cy.aicreator.ai.model.MultiFileCodeResult;
import org.cy.aicreator.ai.model.message.AiResponseMessage;
import org.cy.aicreator.ai.model.message.ToolExecutedMessage;
import org.cy.aicreator.ai.model.message.ToolRequestMessage;
import org.cy.aicreator.core.parser.CodeParserExecutor;
import org.cy.aicreator.core.saver.CodeFileSaverExecutor;
import org.cy.aicreator.exception.BusinessException;
import org.cy.aicreator.exception.ErrorCode;
import org.cy.aicreator.model.enums.GenerTypeEnum;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成服务门面
 */
@Service
@Slf4j
public class AiCreatorFacade {


    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;


    /**
     * 统一入口：生成代码并保存（流式输出）
     *
     * @param userMessage   用户提示词
     * @param generTypeEnum 生成类型
     * @return 生成的代码文件
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, GenerTypeEnum generTypeEnum, Long appId) {
        if (generTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "代码生成类型不能为空");
        }
        // 根据 appId 获取对应的 AI 服务实例
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(appId, generTypeEnum);

        return switch (generTypeEnum) {
            case HTML -> {
                Flux<String> resultStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield parserAndSaveCodeStream(resultStream, GenerTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> resultStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield parserAndSaveCodeStream(resultStream, GenerTypeEnum.MULTI_FILE, appId);
            }

            case VUE_PROJECT -> {
                Flux<String> resultStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield parserAndSaveCodeStream(resultStream, GenerTypeEnum.VUE_PROJECT, appId);
            }
            default -> {
                String errorMsg = generTypeEnum + "代码生成类型无效";
                throw new BusinessException(ErrorCode.PARAM_ERROR, errorMsg);
            }
        };
    }


    /**
     * 解析代码并保存文件（流式输出）
     *
     * @param resultStream 代码流
     * @param outputType   输出类型
     * @return 生成的代码文件
     */
    private Flux<String> parserAndSaveCodeStream(Flux<String> resultStream, GenerTypeEnum outputType, Long appId) {
        // 创建一个StringBuilder对象，用于实时收集代码片段
        StringBuilder codeBuilder = new StringBuilder();
        // 实时收集代码片段
        return resultStream.doOnNext(codeBuilder::append)
                .doOnComplete(() -> {
                    try {
                        // 流式输出完成后，解析代码并保存文件
                        // 代码解析执行器
                        Object execute = CodeParserExecutor.execute(outputType, codeBuilder.toString());
                        // 代码保存执行器
                        File dir = CodeFileSaverExecutor.executeSaver(execute, outputType, appId);
                        log.info("代码保存成功：{}", dir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("代码保存失败：{}", e.getMessage());
                    }

                });
    }

    /**
     * 将 TokenStream 转换为 Flux<String>，并传递工具调用信息
     *
     * @param tokenStream TokenStream 对象
     * @return Flux<String> 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) -> {
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onToolExecuted((ToolExecution toolExecution) -> {

                        // 1. 取出工具调用请求（函数名、参数）
                        ToolExecutionRequest request = toolExecution.request();

                        // 2. 推送给前端：告诉前端“AI 正在调用工具”
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(request);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));

                        // 3. 推送给前端：告诉前端“工具执行完成”
                        ToolExecutedMessage toolExecutedMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutedMessage));
                    })

                    .onCompleteResponse((ChatResponse response) -> {
                        sink.complete();
                    })
                    .onError((Throwable error) -> {
                        error.printStackTrace();
                        sink.error(error);
                    })
                    .start();
        });
    }


}
