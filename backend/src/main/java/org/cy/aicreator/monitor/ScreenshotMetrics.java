package org.cy.aicreator.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * 封面截图指标
 * <p>
 * 截图是异步、非关键路径的操作，失败不会影响部署结果，因此只能靠指标观察真实成功率。
 * 失败原因按枚举分类打标签，不做自由文本，避免标签基数失控。
 * </p>
 *
 * @author cy
 * @since 2026-10-16
 */
@Component
@Slf4j
public class ScreenshotMetrics {

    private static final String COUNTER_NAME = "screenshot_total";
    private static final String TIMER_NAME = "screenshot_duration";

    private static final String RESULT_SUCCESS = "success";
    private static final String RESULT_FAILURE = "failure";

    @Resource
    private MeterRegistry meterRegistry;

    private final ConcurrentMap<String, Counter> countersCache = new ConcurrentHashMap<>();

    /**
     * 记录一次成功的截图
     *
     * @param durationMillis 端到端耗时（毫秒）
     */
    public void recordSuccess(long durationMillis) {
        counter(RESULT_SUCCESS, "none").increment();
        recordDuration(durationMillis, RESULT_SUCCESS);
    }

    /**
     * 记录一次失败的截图
     *
     * @param category       失败分类，见 {@code WebPageCapturer.FailureCategory}
     * @param durationMillis 端到端耗时（毫秒）
     */
    public void recordFailure(String category, long durationMillis) {
        counter(RESULT_FAILURE, category).increment();
        recordDuration(durationMillis, RESULT_FAILURE);
    }

    private void recordDuration(long durationMillis, String result) {
        Timer.builder(TIMER_NAME)
                .description("封面截图耗时")
                .tag("result", result)
                .register(meterRegistry)
                .record(durationMillis, TimeUnit.MILLISECONDS);
    }

    private Counter counter(String result, String category) {
        String key = result + ":" + category;
        return countersCache.computeIfAbsent(key, k ->
                Counter.builder(COUNTER_NAME)
                        .description("封面截图次数")
                        .tag("result", result)
                        .tag("category", category)
                        .register(meterRegistry)
        );
    }
}
