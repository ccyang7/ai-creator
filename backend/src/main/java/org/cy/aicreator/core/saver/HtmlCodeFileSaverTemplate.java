package org.cy.aicreator.core.saver;

import cn.hutool.core.util.StrUtil;
import org.cy.aicreator.ai.model.HtmlCodeResult;
import org.cy.aicreator.exception.BusinessException;
import org.cy.aicreator.exception.ErrorCode;
import org.cy.aicreator.model.enums.GenerTypeEnum;

/**
 * HTML代码文件保存器
 */
public class HtmlCodeFileSaverTemplate extends CodeFileSaverTemplate<HtmlCodeResult> {

    /**
     * 获取代码生成类型
     *
     * @return 代码生成类型
     */
    @Override
    protected GenerTypeEnum getOutputType() {
        return GenerTypeEnum.HTML;
    }


    /**
     * 保存文件（HTML）
     *
     * @param result      代码结果对象
     * @param baseDirPath 基础目录路径
     */
    @Override
    protected void saveFiles(HtmlCodeResult result, String baseDirPath) {
        // 保存 HTML 文件
        writeFile(baseDirPath, "index.html", result.getHtmlCode());
    }

    /**
     * 验证输入参数
     *
     * @param result 生成代码结果对象
     */
    @Override
    protected void validateInput(HtmlCodeResult result) {
        super.validateInput(result);
        // HTML 代码不能为空
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码内容不能为空");
        }
    }
}
