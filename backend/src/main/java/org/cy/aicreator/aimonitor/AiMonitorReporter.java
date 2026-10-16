package org.cy.aicreator.aimonitor;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * 把模型调用记录异步发给 AI 监控平台
 * <p>
 * 平台是旁路系统，此处遵守它的四条硬性要求：异步发送、超时给短、吞掉所有异常、失败调用也要报。
 * 任何一步出问题都只写日志，绝不影响生成业务。
 * </p>
 */
@Component
@Slf4j
public class AiMonitorReporter {

    /**
     * 专用序列化器。全局的 ObjectMapper 会把所有 Long 序列化成字符串，
     * 那样 latency 会变成 {@code "123"}，平台按 int 校验直接 422
     */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AiMonitorProperties properties;

    private final HttpClient httpClient;

    public AiMonitorReporter(AiMonitorProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                // 必须钉死 HTTP/1.1：JDK 客户端在明文连接上默认尝试 HTTP/2（h2c 升级），
                // 平台用的 uvicorn/h11 解析不了这种请求，一律回 400 Invalid HTTP request received.
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        if (properties.configured()) {
            log.info("AI 监控上报已开启：url={}，provider={}，上报正文={}",
                    properties.reportUrl(), properties.getProvider(), properties.isReportContent() ? "是" : "否");
        } else {
            log.info("AI 监控上报未开启（ai-monitor.enabled=false，或未配置 base-url / api-key），模型调用不上报");
        }
    }

    /**
     * 异步上报一条记录。方法本身不做网络等待，监听器线程不会被占用
     */
    public void report(AiMonitorReport report) {
        if (!properties.configured()) {
            return;
        }

        HttpRequest request;
        try {
            request = HttpRequest.newBuilder(URI.create(properties.reportUrl()))
                    .timeout(properties.getRequestTimeout())
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", properties.getApiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(
                            MAPPER.writeValueAsString(report), StandardCharsets.UTF_8))
                    .build();
        } catch (Exception e) {
            // 地址写错、序列化失败都走这里，上报是旁路，出问题不能影响调用方
            log.warn("AI 监控上报请求构造失败（已忽略）：{}", e.getMessage());
            return;
        }

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .whenComplete(this::afterSend);
    }

    private void afterSend(HttpResponse<String> response, Throwable error) {
        try {
            if (error != null) {
                log.warn("AI 监控上报失败（已忽略，不影响业务）：{}", error.getMessage());
                return;
            }
            int statusCode = response.statusCode();
            if (statusCode >= 200 && statusCode < 300) {
                return;
            }
            // 5xx 按平台文档只记日志、不重试，避免把故障放大
            log.warn("AI 监控上报未被接受：HTTP {}，响应={}",
                    statusCode, StrUtil.maxLength(response.body(), 500));
        } catch (Exception e) {
            log.warn("AI 监控上报响应处理异常（已忽略）：{}", e.getMessage());
        }
    }

    @PreDestroy
    void shutdown() {
        httpClient.close();
    }
}
