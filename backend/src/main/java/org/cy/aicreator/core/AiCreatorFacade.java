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
import org.cy.aicreator.ai.MultiFileCodeCompleter;
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
import org.cy.aicreator.service.VueProjectBuildService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.File;

/**
 * AI 代码生成服务门面
 */
@Service
@Slf4j
public class AiCreatorFacade {


    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private MultiFileCodeCompleter multiFileCodeCompleter;

    @Resource
    private VueProjectBuildService vueProjectBuildService;


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
                // Vue 工程没有 parser/saver 策略：文件由模型经 FileWriteTool 直接写盘，
                // 这里只负责把 TokenStream 转成 JSON 消息帧，并在流终止后触发构建。
                TokenStream tokenStream = aiCodeGeneratorService.generateVueProjectCodeStream(appId, userMessage);
                yield processTokenStream(tokenStream, appId)
                        // 用 doFinally 而不是 onCompleteResponse：客户端断开时走的是 CANCEL 分支，
                        // 而且实测断开后模型仍会继续写文件，此时构建依然有意义。
                        .doFinally(signalType -> vueProjectBuildService.triggerBuildAsync(appId));
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
                        if (execute instanceof MultiFileCodeResult multiFileCodeResult) {
                            // 多文件模式下模型时常只给 HTML 代码块，缺的 CSS/JS 在这里定向补全，
                            // 否则会写出一个引用了不存在文件的 index.html（页面没样式，且不报错）
                            multiFileCodeCompleter.complete(multiFileCodeResult);
                        }
                        // 代码保存执行器
                        File dir = CodeFileSaverExecutor.executeSaver(execute, outputType, appId);
                        log.info("代码保存成功：{}", dir.getAbsolutePath());
                    } catch (Exception e) {
                        log.error("代码保存失败：{}", e.getMessage());
                    }

                });
    }

    /**
     * 将 TokenStream 转换为 Flux&lt;String&gt;，每个分片是一条 JSON 消息帧
     * （{@code ai_response} / {@code tool_request} / {@code tool_executed}），由
     * {@code JsonMessageStreamHandler} 负责解析。
     *
     * @param tokenStream TokenStream 对象
     * @param appId       应用 ID（仅用于日志）
     * @return Flux&lt;String&gt; 流式响应
     */
    private Flux<String> processTokenStream(TokenStream tokenStream, Long appId) {
        return Flux.create(sink -> {
            tokenStream.onPartialResponse((String partialResponse) ->
                            emit(sink, new AiResponseMessage(partialResponse)))
                    .onToolExecuted((ToolExecution toolExecution) -> {
                        // 1. 取出工具调用请求（函数名、参数）
                        ToolExecutionRequest request = toolExecution.request();
                        // 2. 推送给前端：告诉前端“AI 正在调用工具”
                        emit(sink, new ToolRequestMessage(request));
                        // 3. 推送给前端：告诉前端“工具执行完成”
                        emit(sink, new ToolExecutedMessage(toolExecution));
                    })
                    .onCompleteResponse((ChatResponse response) -> sink.complete())
                    .onError((Throwable error) -> {
                        log.error("Vue 工程生成失败，appId={}", appId, error);
                        sink.error(error);
                    })
                    .start();
            // LangChain4j 1.3.0 的 TokenStream 没有 cancel() 方法，客户端断开时只能停止向下游转发，
            // 无法中止模型侧的生成：模型线程会继续跑完并把剩余文件写盘，因此构建触发必须放在 doFinally。
            sink.onCancel(() -> log.info("客户端已断开，停止转发 Vue 生成结果，appId={}", appId));
        });
    }

    /**
     * 向下游推送一条 JSON 消息帧，客户端已断开时直接丢弃，避免无谓的序列化开销
     */
    private void emit(FluxSink<String> sink, Object message) {
        if (!sink.isCancelled()) {
            sink.next(JSONUtil.toJsonStr(message));
        }
    }


}
