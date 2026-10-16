package org.cy.aicreator.aimonitor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiTokenUsage;
import dev.langchain4j.model.output.TokenUsage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 把 LangChain4j 的模型调用事件翻译成监控平台的上报记录
 * <p>
 * 注册在 {@code config/} 下的四个模型 Bean 上，流式与非流式调用都会走到这里。
 * 监听器由 LangChain4j 在模型 HTTP 客户端的线程上回调，而平台是旁路系统：
 * 这里只做字段拼装，实际的网络请求交给 {@link AiMonitorReporter} 异步发出；
 * 且每个回调整体包在 try/catch 里 —— LangChain4j 虽然会吞掉监听器抛出的异常，
 * 但那会在每次调用上留下一条告警日志，不如自己处理干净。
 * </p>
 * <p>
 * 请求与响应之间靠 {@code attributes()} 传递数据（同一个 map 会从 onRequest 传到
 * onResponse / onError），不依赖任何 ThreadLocal，因此不受回调线程切换影响。
 * </p>
 */
@Component
@Slf4j
public class AiMonitorListener implements ChatModelListener {

    private static final String START_TIME_KEY = "ai_monitor_start_time";
    private static final String MODEL_NAME_KEY = "ai_monitor_model_name";
    private static final String INPUT_TEXT_KEY = "ai_monitor_input_text";

    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_ERROR = "error";

    /**
     * model 是必填且长度 1~100，取不到时给占位值；宁可成本算不出来，也不能让整条记录被平台拒收
     */
    private static final String UNKNOWN_MODEL = "unknown";
    private static final int MAX_MODEL_NAME_LENGTH = 100;

    private final AiMonitorProperties properties;

    private final AiMonitorReporter reporter;

    public AiMonitorListener(AiMonitorProperties properties, AiMonitorReporter reporter) {
        this.properties = properties;
        this.reporter = reporter;
    }

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        try {
            Map<Object, Object> attributes = requestContext.attributes();
            attributes.put(START_TIME_KEY, System.currentTimeMillis());
            ChatRequest request = requestContext.chatRequest();
            // 用请求里的模型名（就是配置里的 model-name），不要用响应回显的那个：
            // 实测服务端回显的名字和配置名不一致，而平台的成本按价格表里的模型名匹配，
            // 回显名对不上会让成本整列显示为空
            attributes.put(MODEL_NAME_KEY, request.modelName());
            if (properties.isReportContent()) {
                attributes.put(INPUT_TEXT_KEY, renderInput(request.messages()));
            }
        } catch (Exception e) {
            log.warn("AI 监控记录请求信息失败（已忽略）：{}", e.getMessage());
        }
    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        try {
            ChatResponse response = responseContext.chatResponse();
            reporter.report(build(
                    responseContext.attributes(),
                    STATUS_SUCCESS,
                    properties.isReportContent() && response.aiMessage() != null
                            ? response.aiMessage().text() : null,
                    null,
                    response.metadata() == null ? null : response.metadata().tokenUsage()));
        } catch (Exception e) {
            log.warn("AI 监控上报成功记录失败（已忽略）：{}", e.getMessage());
        }
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        try {
            Throwable error = errorContext.error();
            // 失败调用也要报，否则平台统计里只剩成功的那些，成本与错误率都是错的
            reporter.report(build(
                    errorContext.attributes(),
                    STATUS_ERROR,
                    null,
                    error == null ? null : error.getClass().getSimpleName() + ": " + error.getMessage(),
                    null));
        } catch (Exception e) {
            log.warn("AI 监控上报失败记录失败（已忽略）：{}", e.getMessage());
        }
    }

    private AiMonitorReport build(Map<Object, Object> attributes, String status, String output,
                                  String error, TokenUsage usage) {
        Integer inputTokens = usage == null ? null : usage.inputTokenCount();
        Integer outputTokens = usage == null ? null : usage.outputTokenCount();
        Integer cacheHitTokens = cachedTokensOf(usage);
        // 平台对 cache_hit_tokens > input_tokens 会整条 422 拒收，这里先夹紧
        if (cacheHitTokens != null && inputTokens != null && cacheHitTokens > inputTokens) {
            cacheHitTokens = inputTokens;
        }

        return new AiMonitorReport(
                properties.getProvider(),
                modelNameOf(attributes),
                latencyOf(attributes),
                status,
                inputTokens,
                cacheHitTokens,
                outputTokens,
                textOf(attributeOf(attributes, INPUT_TEXT_KEY), properties.getInputMaxLength()),
                textOf(output, properties.getOutputMaxLength()),
                textOf(error, properties.getErrorMaxLength()),
                Instant.now().toString());
    }

    /**
     * 调用耗时。起始时间由 onRequest 写进 attributes，取不到时给 0（平台要求 ≥ 0）
     */
    private long latencyOf(Map<Object, Object> attributes) {
        Object start = attributeOf(attributes, START_TIME_KEY);
        if (!(start instanceof Long startMillis)) {
            return 0L;
        }
        return Math.max(0L, System.currentTimeMillis() - startMillis);
    }

    private String modelNameOf(Map<Object, Object> attributes) {
        Object name = attributeOf(attributes, MODEL_NAME_KEY);
        if (!(name instanceof String modelName) || StrUtil.isBlank(modelName)) {
            return UNKNOWN_MODEL;
        }
        return StrUtil.maxLength(modelName, MAX_MODEL_NAME_LENGTH);
    }

    /**
     * 从流式响应的用量里取缓存命中数。不同实现可能给不出这个明细，取不到就留空
     */
    private Integer cachedTokensOf(TokenUsage usage) {
        if (usage instanceof OpenAiTokenUsage openAiUsage && openAiUsage.inputTokensDetails() != null) {
            return openAiUsage.inputTokensDetails().cachedTokens();
        }
        return null;
    }

    /**
     * 把本次请求的消息拼成一段文本，超过上限就不再继续拼。
     * 生成链路的系统提示词加上历史对话可能上万字符，平台虽不限长度，但没必要全量发过去
     */
    private String renderInput(List<ChatMessage> messages) {
        if (CollUtil.isEmpty(messages)) {
            return null;
        }
        int limit = Math.max(0, properties.getInputMaxLength());
        StringBuilder text = new StringBuilder();
        for (ChatMessage message : messages) {
            if (text.length() >= limit) {
                break;
            }
            String content = textOfMessage(message);
            if (StrUtil.isBlank(content)) {
                continue;
            }
            text.append(message.type().name()).append(": ").append(content).append('\n');
        }
        if (text.isEmpty()) {
            return null;
        }
        return StrUtil.maxLength(text.toString(), limit);
    }

    /**
     * {@link ChatMessage} 是个只有 {@code type()} 的接口，取正文只能按实现类判断
     */
    private static String textOfMessage(ChatMessage message) {
        if (message instanceof UserMessage userMessage) {
            return userMessage.hasSingleText() ? userMessage.singleText() : userMessage.contents().toString();
        }
        if (message instanceof SystemMessage systemMessage) {
            return systemMessage.text();
        }
        if (message instanceof AiMessage aiMessage) {
            return aiMessage.text();
        }
        if (message instanceof ToolExecutionResultMessage toolMessage) {
            return toolMessage.text();
        }
        return null;
    }

    private static Object attributeOf(Map<Object, Object> attributes, String key) {
        return attributes == null ? null : attributes.get(key);
    }

    private static String textOf(Object value, int maxLength) {
        if (!(value instanceof String text) || StrUtil.isBlank(text)) {
            return null;
        }
        return StrUtil.maxLength(text, Math.max(0, maxLength));
    }
}
