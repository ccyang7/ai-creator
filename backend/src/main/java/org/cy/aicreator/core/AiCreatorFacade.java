package org.cy.aicreator.core;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.cy.aicreator.ai.AiCodeGeneratorService;
import org.cy.aicreator.ai.model.HtmlCodeResult;
import org.cy.aicreator.ai.model.MultiFileCodeResult;
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
    public Flux<String> generateAndSaveCodeStream(String userMessage, GenerTypeEnum generTypeEnum) {
        if(generTypeEnum == null){
            throw new BusinessException(ErrorCode.PARAM_ERROR,"代码生成类型不能为空");
        }
        return switch (generTypeEnum) {
            case HTML -> generateAndSaveHtmlCodeStream(userMessage);
            case MULTI_FILE -> generateAndSaveMultiFileCodeStream(userMessage);
            default -> {
                String errorMsg = generTypeEnum+"代码生成类型无效";
                throw new BusinessException(ErrorCode.PARAM_ERROR, errorMsg);
            }
        };
    }


    /**
     * 多文件代码生成并保存（流式输出）
     *
     * @param userMessage 用户提示词
     * @return 生成的代码文件
     */
    private Flux<String> generateAndSaveMultiFileCodeStream(String userMessage) {
        Flux<String> result = aiCodeGeneratorService.generateMultiFileCodeStream(userMessage);
        StringBuilder codeBuilder = new StringBuilder();
        return result.doOnNext(code -> {
            // 实时收集代码片段
            codeBuilder.append(code);
        }).doOnComplete(() -> {
            try{
                // 流式输出完成后，解析代码并保存文件
                MultiFileCodeResult multiFileCodeResult = CodeParserStream.parseMultiFileCode(codeBuilder.toString());
                File dir = GenerCodeFilesSaver.saveMultiFileCode(multiFileCodeResult);
                log.info("代码保存成功：{}", dir.getAbsolutePath());
            }catch (Exception e){
                log.error("代码保存失败：{}", e.getMessage());
            }

        });
    }

    /**
     * HTML代码生成并保存（流式输出）
     *
     * @param userMessage 用户提示词
     * @return 生成的代码文件
     */
    private Flux<String> generateAndSaveHtmlCodeStream(String userMessage) {
        Flux<String> result = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
        StringBuilder codeBuilder = new StringBuilder();
        return result.doOnNext(code -> {
            // 实时收集代码片段
            codeBuilder.append(code);
        }).doOnComplete(() -> {
            try{
                // 流式输出完成后，解析代码并保存文件
                HtmlCodeResult htmlCodeResult = CodeParserStream.parseHtmlCode(codeBuilder.toString());
                File dir = GenerCodeFilesSaver.saveHtmlCode(htmlCodeResult);
                log.info("代码保存成功：{}", dir.getAbsolutePath());
            }catch (Exception e){
                log.error("代码保存失败：{}", e.getMessage());
            }

        });
    }


}
