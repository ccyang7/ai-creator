package org.cy.aicreator.core;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import org.cy.aicreator.ai.model.HtmlCodeResult;
import org.cy.aicreator.ai.model.MultiFileCodeResult;
import org.cy.aicreator.model.enums.GenerTypeEnum;

import java.io.File;

/**
 * 生成代码文件保存器
 */
@Deprecated
public class GenerCodeFilesSaver {

    // 文件保存的根目录
    private static final String ROOT_PATH = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 保存文件
     *
     * @param dirPath  目录
     * @param fileName 文件名
     * @param content  文件内容
     */
    private static void writeFile(String dirPath, String fileName, String content) {
        String filePath = dirPath + File.separator + fileName;
        FileUtil.writeUtf8String(content, filePath);
    }


    /**
     * 构建文件唯一路径：tmp/code_output/outputType_雪花ID
     *
     * @param outputType 输出类型
     * @return 文件唯一路径
     */
    private static String buildUniqueFilePath(String outputType) {
        String uniqueFileName = ROOT_PATH + File.separator + outputType + "_" + IdUtil.getSnowflakeNextIdStr();
        FileUtil.mkdir(uniqueFileName);
        return uniqueFileName;
    }

    /**
     * 保存Html代码
     *
     * @param htmlCodeResult html代码结果
     * @return 保存的文件
     */
    public static File saveHtmlCode(HtmlCodeResult htmlCodeResult) {
        String saveFilePath = buildUniqueFilePath(GenerTypeEnum.HTML.getValue());
        writeFile(saveFilePath, "index.html", htmlCodeResult.getHtmlCode());
        return new File(saveFilePath);
    }


    /**
     * 保存多文件代码
     *
     * @param multiFileCodeResult 多文件代码结果
     * @return 保存的文件
     */
    public static File saveMultiFileCode(MultiFileCodeResult multiFileCodeResult) {
        String saveFilePath = buildUniqueFilePath(GenerTypeEnum.MULTI_FILE.getValue());
        writeFile(saveFilePath, "index.html", multiFileCodeResult.getHtmlCode());
        writeFile(saveFilePath, "style.css", multiFileCodeResult.getCssCode());
        writeFile(saveFilePath, "script.js", multiFileCodeResult.getJsCode());
        return new File(saveFilePath);
    }


}
