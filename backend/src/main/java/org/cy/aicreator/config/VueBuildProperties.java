package org.cy.aicreator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Vue 工程构建配置
 * <p>
 * 生成完成后由服务端执行 npm install / npm run build，把源码工程编译成可预览、可部署的
 * dist 产物。构建属于重操作，超时与并发都需要可调。
 * </p>
 *
 * @author cy
 * @since 2026-10-15
 */
@Configuration
@ConfigurationProperties(prefix = "code.build")
@Data
public class VueBuildProperties {

    /**
     * 是否启用服务端构建。关闭后 vue_project 只生成源码，不产出 dist
     */
    private boolean enabled = true;

    /**
     * npm 可执行文件名（Windows 下会经 cmd.exe 转发调用）
     */
    private String npmCommand = "npm";

    /**
     * npm install 超时时间（秒），首次安装需要下载依赖，给得宽一些
     */
    private int installTimeoutSeconds = 300;

    /**
     * npm run build 超时时间（秒）
     */
    private int buildTimeoutSeconds = 180;

    /**
     * 文件写入静默期（毫秒）：连续这么久没有新文件写入，才认为生成真正结束。
     * 实测客户端断开后模型仍会继续写文件，因此不能紧跟流结束就构建
     */
    private long quiescenceMillis = 3000;

    /**
     * 等待文件写入静默的最长时间（毫秒），超时后照常构建
     */
    private long quiescenceTimeoutMillis = 120000;

    /**
     * 静默检测的轮询间隔（毫秒）
     */
    private long pollIntervalMillis = 500;

    /**
     * 同时进行的构建上限，构建会打满 CPU 与磁盘，必须限流
     */
    private int maxConcurrentBuilds = 2;

    /**
     * 失败时回传给前端的 npm 输出行数上限
     */
    private int maxOutputLines = 40;
}
