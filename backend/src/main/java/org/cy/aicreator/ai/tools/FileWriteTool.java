package org.cy.aicreator.ai.tools;


import cn.hutool.core.util.StrUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import lombok.extern.slf4j.Slf4j;
import org.cy.aicreator.constant.AppConstant;
import org.cy.aicreator.model.enums.GenerTypeEnum;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * 文件写入工具
 * 支持 AI 通过工具调用的方式写入文件
 * <p>
 * 传入的路径与内容都来自模型，属于不可信输入：所有落盘路径都会规范化并校验仍位于
 * 该应用的项目根目录内，越界一律拒绝并把原因回给模型，而不是抛出异常中断整条流。
 */
@Slf4j
public class FileWriteTool {

    /**
     * 项目根目录名：与部署、下载拼接的目录名保持一致，是跨模块契约
     */
    private static final String PROJECT_DIR_NAME = GenerTypeEnum.VUE_PROJECT.getValue() + "_%d";

    @Tool("写入文件到指定路径")
    public String writeFile(
            @P("文件的相对路径")
            String relativeFilePath,
            @P("要写入文件的内容")
            String content,
            @ToolMemoryId Long appId
    ) {
        if (StrUtil.isBlank(relativeFilePath)) {
            return "文件写入失败: 文件路径不能为空";
        }
        if (appId == null) {
            return "文件写入失败: 缺少应用 ID";
        }
        try {
            // 项目根目录，规范化后作为越界判断的基准
            Path projectRoot = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, String.format(PROJECT_DIR_NAME, appId))
                    .toAbsolutePath()
                    .normalize();
            Path requested = Paths.get(relativeFilePath);
            if (requested.isAbsolute()) {
                return "文件写入失败: 只允许相对路径，收到的是 " + relativeFilePath;
            }
            // resolve 后再 normalize，把 "a/../b" 一类写法收敛掉，再判断是否仍在项目根目录内
            Path path = projectRoot.resolve(requested).normalize();
            if (!path.startsWith(projectRoot)) {
                log.warn("拒绝越界写入: appId={}, 期望根目录={}, 实际路径={}", appId, projectRoot, path);
                return "文件写入失败: 路径必须位于项目目录内，收到的是 " + relativeFilePath;
            }
            // 创建父目录
            Path parentDir = path.getParent();
            if (parentDir != null) {
                Files.createDirectories(parentDir);
            }
            // 写入文件内容
            Files.write(path, StrUtil.nullToEmpty(content).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            log.info("成功写入文件: {}", path);
            // 返回相对路径
            return "文件写入成功: " + relativeFilePath;
        } catch (InvalidPathException e) {
            String errorMessage = "文件写入失败: 路径不合法 " + relativeFilePath;
            log.error(errorMessage, e);
            return errorMessage;
        } catch (IOException e) {
            String errorMessage = "文件写入失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }
}

