package org.cy.aicreator.core.saver;

import cn.hutool.core.util.StrUtil;
import org.cy.aicreator.ai.model.MultiFileCodeResult;
import org.cy.aicreator.exception.BusinessException;
import org.cy.aicreator.exception.ErrorCode;
import org.cy.aicreator.model.enums.GenerTypeEnum;

/**
 * 多文件代码保存器
 */
public class MultiFileCodeFileSaverTemplate extends CodeFileSaverTemplate<MultiFileCodeResult> {

    /**
     * 获取代码类型
     *
     * @return 代码类型
     */
    @Override
    public GenerTypeEnum getOutputType() {
        return GenerTypeEnum.MULTI_FILE;
    }

    /**
     * 保存多文件代码
     *
     * @param result 生成代码结果对象
     */
    @Override
    protected void saveFiles(MultiFileCodeResult result, String baseDirPath) {
        // 保存 HTML 文件
        writeFile(baseDirPath, "index.html", result.getHtmlCode());
        // 保存 CSS 文件
        writeFile(baseDirPath, "style.css", result.getCssCode());
        // 保存 JavaScript 文件
        writeFile(baseDirPath, "script.js", result.getJsCode());
    }

    /**
     * 验证输入参数
     *
     * @param result 生成代码结果对象
     */
    @Override
    protected void validateInput(MultiFileCodeResult result) {
        super.validateInput(result);
        // 至少要有 HTML 代码，CSS 和 JS 可以为空
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HTML代码内容不能为空");
        }
    }
}
