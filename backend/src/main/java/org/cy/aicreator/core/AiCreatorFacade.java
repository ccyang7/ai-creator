package org.cy.aicreator.core;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.cy.aicreator.ai.AiCodeGeneratorService;
import org.cy.aicreator.ai.model.HtmlCodeResult;
import org.cy.aicreator.ai.model.MultiFileCodeResult;
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
    private AiCodeGeneratorService aiCodeGeneratorService;


    /**
     * 统一入口：生成代码并保存（JSON结构化输出）
     *
     * @param userMessage 用户提示词
     * @param generTypeEnum 生成类型
     * @return 生成的代码文件
     */
    public File generateAndSaveCode(String userMessage, GenerTypeEnum generTypeEnum) {
        if(generTypeEnum == null){
            throw new BusinessException(ErrorCode.PARAM_ERROR,"代码生成类型不能为空");
        }
        return switch (generTypeEnum) {
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield GenerCodeFilesSaver.saveHtmlCode(htmlCodeResult);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield GenerCodeFilesSaver.saveMultiFileCode(multiFileCodeResult);
            }
            default -> {
                String errorMsg = generTypeEnum+"代码生成类型无效";
                throw new BusinessException(ErrorCode.PARAM_ERROR, errorMsg);
            }
        };
    }


    /**
     * 统一入口：生成代码并保存（流式输出）
     *
     * @param userMessage 用户提示词
     * @param generTypeEnum 生成类型
     * @return 生成的代码文件
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, GenerTypeEnum generTypeEnum, Long appId) {
        if(generTypeEnum == null){
            throw new BusinessException(ErrorCode.PARAM_ERROR,"代码生成类型不能为空");
        }
        return switch (generTypeEnum) {
            case HTML -> {
                Flux<String> resultStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield parserAndSaveCodeStream(resultStream, GenerTypeEnum.HTML, appId);
            }
            case MULTI_FILE -> {
                Flux<String> resultStream = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
                yield parserAndSaveCodeStream(resultStream, GenerTypeEnum.MULTI_FILE, appId);
            }
            default -> {
                String errorMsg = generTypeEnum+"代码生成类型无效";
                throw new BusinessException(ErrorCode.PARAM_ERROR, errorMsg);
            }
        };
    }


    /**
     * 解析代码并保存文件（流式输出）
     *
     * @param resultStream 代码流
     * @param outputType 输出类型
     * @return 生成的代码文件
     */
    private Flux<String> parserAndSaveCodeStream(Flux<String>  resultStream, GenerTypeEnum outputType,Long appId) {
        // 创建一个StringBuilder对象，用于实时收集代码片段
        StringBuilder codeBuilder = new StringBuilder();
        // 实时收集代码片段
        return resultStream.doOnNext(codeBuilder::append).doOnComplete(() -> {
            try{
                // 流式输出完成后，解析代码并保存文件
                // 代码解析执行器
                Object execute = CodeParserExecutor.execute(outputType, codeBuilder.toString());
                // 代码保存执行器
                File dir = CodeFileSaverExecutor.executeSaver(execute, outputType,appId);
                log.info("代码保存成功：{}", dir.getAbsolutePath());
            }catch (Exception e){
                log.error("代码保存失败：{}", e.getMessage());
            }

        });
    }





}
