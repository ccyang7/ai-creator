package org.cy.aicreator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 封面截图配置
 * <p>
 * 部署完成后异步截取页面首屏作为应用封面。截图依赖无头浏览器，属于重资源且可失败的操作，
 * 因此并发、超时、重试与等待策略都需要可调。
 * </p>
 *
 * @author cy
 * @since 2026-10-16
 */
@Configuration
@ConfigurationProperties(prefix = "code.screenshot")
@Data
public class ScreenshotProperties {

    /**
     * 截图源
     */
    public enum Source {

        /**
         * 本应用的静态资源接口（{@code /static/{codeGenType}_{appId}/}）。
         * 不经过 nginx 与公网，截图链路只依赖应用自身，是默认值
         */
        PREVIEW,

        /**
         * {@code code.deploy-host} 指向的部署站点。
         * 截图结果与用户实际访问的地址完全一致，但依赖 nginx 与网络可达
         */
        DEPLOY
    }

    /**
     * 是否启用封面截图。关闭后部署不再生成封面，前端会退回按生成类型的占位图
     */
    private boolean enabled = true;

    /**
     * 截图源，默认走应用自身的静态资源接口
     */
    private Source source = Source.PREVIEW;

    /**
     * 截图源根地址，留空时按 {@code server.port} 与 {@code server.servlet.context-path}
     * 推导为 {@code http://localhost:{port}{contextPath}}
     */
    private String previewBaseUrl;

    /**
     * 同时存活的浏览器实例上限，也是截图任务的并发上限。
     * 一个无头 Chrome 约占 150~300MB 内存，不宜放大
     */
    private int poolSize = 2;

    /**
     * 单个浏览器实例最多复用的次数，超过后销毁重建，避免长期复用累积状态
     */
    private int maxUsesPerDriver = 50;

    /**
     * 单个浏览器实例最长存活时间（秒）
     */
    private int driverTtlSeconds = 1800;

    /**
     * 从池中获取浏览器实例的最长等待时间（秒），等待超时视为截图失败
     */
    private int acquireTimeoutSeconds = 30;

    /**
     * 页面加载（含同步资源）超时时间（秒）
     */
    private int pageLoadTimeoutSeconds = 30;

    /**
     * 等待页面渲染完成的最长时间（秒）。到点未渲染完也照常截图，不视为失败
     */
    private int renderTimeoutSeconds = 15;

    /**
     * 等待资源加载静默的最长时间（毫秒）。页面可能有轮询请求而永不静默，因此必须有上限
     */
    private int networkIdleTimeoutMillis = 3000;

    /**
     * 静默判定窗口与渲染完成后的沉降时间（毫秒），用于等待布局与绘制稳定
     */
    private long settleMillis = 300;

    /**
     * 视口宽度
     */
    private int viewportWidth = 1600;

    /**
     * 视口高度
     */
    private int viewportHeight = 900;

    /**
     * 是否截取整页。关闭后只截视口范围内的首屏
     */
    private boolean fullPage = true;

    /**
     * 整页截图的高度上限，防止超长页面产出过大图片
     */
    private int maxCaptureHeight = 3000;

    /**
     * JPEG 压缩质量（0~1），封面不需要原图质量
     */
    private double jpegQuality = 0.7;

    /**
     * 截图失败后的重试次数，只对可重试的失败生效
     */
    private int maxRetries = 2;

    /**
     * 首次重试的等待时间（毫秒），按重试次数倍增
     */
    private long retryBackoffMillis = 1000;
}
