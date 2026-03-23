package org.cy.aicreator.core.saver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import org.cy.aicreator.constant.AppConstant;
import org.cy.aicreator.exception.BusinessException;
import org.cy.aicreator.exception.ErrorCode;
import org.cy.aicreator.model.enums.GenerTypeEnum;

import java.io.File;



/**
 * 抽象代码文件保存器 - 模板方法模式
 */
public abstract class CodeFileSaverTemplate<T> {

    // 文件保存的根目录
    private static final String ROOT_PATH = AppConstant.CODE_OUTPUT_ROOT_DIR;

    /**
     * 模板方法：保存代码的标准流程
     *
     * @param result 代码结果对象
     * @return 保存的目录
     */
    public final File saveCode(T result, Long appId) {
        // 1. 验证输入
        validateInput(result);
        // 2. 构建唯一目录
        String baseDirPath = buildUniqueFilePath(appId);
        // 3. 保存文件（具体实现由子类提供）
        saveFiles(result, baseDirPath);
        // 4. 返回目录文件对象
        return new File(baseDirPath);
    }

    /**
     * 验证输入参数（可由子类覆盖）
     *
     * @param result 代码结果对象
     */
    protected void validateInput(T result) {
        if (result == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "代码结果对象不能为空");
        }
    }

    /**
     * 构建文件唯一路径：tmp/code_output/outputType_应用ID
     *
     * @param appId 应用ID
     * @return 文件唯一路径
     */
    protected String buildUniqueFilePath(Long appId) {
        String outputType = getOutputType().getValue();
        String uniqueFileName = ROOT_PATH + File.separator + outputType + "_" + appId;
        FileUtil.mkdir(uniqueFileName);
        return uniqueFileName;
    }

    /**
     * 保存文件
     *
     * @param dirPath  目录
     * @param fileName 文件名
     * @param content  文件内容
     */
    protected final void writeFile(String dirPath, String fileName, String content) {
        if (StrUtil.isNotBlank( content)){
            String filePath = dirPath + File.separator + fileName;
            FileUtil.writeUtf8String(content, filePath);
        }
    }

    /**
     * 获取代码类型（由子类实现）
     *
     * @return 代码输出类型
     */
    protected abstract GenerTypeEnum getOutputType();

    /**
     * 保存文件的具体实现（由子类实现）
     *
     * @param result      代码结果对象
     * @param baseDirPath 基础目录路径
     */
    protected abstract void saveFiles(T result, String baseDirPath);
}
