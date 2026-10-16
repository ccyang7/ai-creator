package org.cy.aicreator.aimonitor;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * AI 监控平台上报配置（{@code ai-monitor.*}）
 * <p>
 * {@code base-url} 与 {@code api-key} 属于凭据，只能通过环境变量
 * （{@code AI_MONITOR_BASE_URL} / {@code AI_MONITOR_API_KEY}）注入，不要写进任何配置文件。
 * 两者任一为空时整条上报链路静默跳过，模型调用照常进行。
 * </p>
 */
@Component
@ConfigurationProperties(prefix = "ai-monitor")
@Data
public class AiMonitorProperties {

    /**
     * 总开关，配置齐全时才会真正上报
     */
    private boolean enabled = true;

    /**
     * 监控平台根地址，不要带 {@code /api/v1/logs}，例如 http://ai-monitor.internal:8001
     */
    private String baseUrl;

    private String apiKey;

    /**
     * 服务提供方标识，平台按它分组统计，取值自由
     */
    private String provider = "deepseek";

    /**
     * 是否上报提问与回答正文。平台详情抽屉靠它才有内容，但正文会离开本服务落库到平台，
     * 且可能包含用户 prompt 与生成的代码，需要时可关掉
     */
    private boolean reportContent = true;

    private int inputMaxLength = 2000;

    private int outputMaxLength = 4000;

    private int errorMaxLength = 500;

    /**
     * 平台不在业务关键路径上，超时必须给得像"可放弃"的量级
     */
    private Duration connectTimeout = Duration.ofSeconds(2);

    private Duration requestTimeout = Duration.ofSeconds(5);

    /**
     * 配置是否齐备。未配齐时所有上报调用直接返回，不产生任何网络请求
     */
    public boolean configured() {
        return enabled && StrUtil.isNotBlank(baseUrl) && StrUtil.isNotBlank(apiKey);
    }

    /**
     * 上报地址：{@code {baseUrl}/api/v1/logs}
     * <p>平台的路由是复数 logs 且不接受结尾斜杠（会 307 重定向），这里统一裁掉尾斜杠</p>
     */
    public String reportUrl() {
        return StrUtil.removeSuffix(baseUrl.trim(), "/") + "/api/v1/logs";
    }
}
