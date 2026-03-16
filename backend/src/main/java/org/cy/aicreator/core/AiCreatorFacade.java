package org.cy.aicreator.core;

import jakarta.annotation.Resource;
import org.cy.aicreator.ai.AiCodeGeneratorService;
import org.cy.aicreator.ai.model.HtmlCodeResult;
import org.cy.aicreator.ai.model.MultiFileCodeResult;
import org.cy.aicreator.exception.BusinessException;
import org.cy.aicreator.exception.ErrorCode;
import org.cy.aicreator.model.enums.GenerTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

/**
 * AI 代码生成服务门面
 */
@Service
public class AiCreatorFacade {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;


    /**
     * 生成代码并保存
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



}
