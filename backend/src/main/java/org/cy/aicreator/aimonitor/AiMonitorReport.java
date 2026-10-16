package org.cy.aicreator.aimonitor;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 上报给 AI 监控平台的一条调用记录，字段对应 {@code POST {base}/api/v1/logs} 的请求体
 * <p>
 * {@code total_tokens} 平台会自行用 input + output 补齐，按规定不上报。
 * 取不到的字段留 null 并整体省略，平台侧都有默认值；但 provider / model / latency / status
 * 是必填，缺任何一个整条记录都会被拒。
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiMonitorReport(

        @JsonProperty("provider")
        String provider,

        @JsonProperty("model")
        String model,

        /**
         * 调用耗时，单位毫秒，必须是 JSON 数字（序列化时不能走全局的 Long 转字符串策略）
         */
        @JsonProperty("latency")
        long latency,

        /**
         * success 或 error，平台按枚举校验，传 failed 会被 422 拒收
         */
        @JsonProperty("status")
        String status,

        @JsonProperty("input_tokens")
        Integer inputTokens,

        /**
         * 命中缓存的输入 Token，是 input_tokens 的一部分，不能大于它
         */
        @JsonProperty("cache_hit_tokens")
        Integer cacheHitTokens,

        @JsonProperty("output_tokens")
        Integer outputTokens,

        @JsonProperty("input")
        String input,

        @JsonProperty("output")
        String output,

        @JsonProperty("error")
        String error,

        /**
         * ISO-8601 带时区偏移的字符串。不带时区的值会被平台按 UTC 解析，本地时间会整体偏移 8 小时
         */
        @JsonProperty("timestamp")
        String timestamp
) {
}
