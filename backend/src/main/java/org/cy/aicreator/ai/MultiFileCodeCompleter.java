package org.cy.aicreator.ai;

import cn.hutool.core.util.StrUtil;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.cy.aicreator.ai.model.MultiFileCodeResult;
import org.cy.aicreator.core.parser.CodeExtractUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 多文件生成补全器：补齐流式生成过程中缺失的 style.css / script.js
 * <p>
 * 多文件的提示词要求模型一次输出 html、css、js 三个代码块，但实测当前模型
 * 只会输出 html 一个代码块，且会照常写上 {@code <link href="style.css">} 与
 * {@code <script src="script.js">}。保存环节只校验 HTML、空内容又是静默跳过，
 * 结果就是预览页里样式与交互全丢，且不产生任何报错。
 * </p>
 * <p>
 * 同一个模型在"只补 CSS/JS 的窄任务"下完全遵守代码块格式（已实测），所以这里
 * 在主流程结束后，仅当确实缺文件时再发一次定向补全请求。之所以用非流式模型：
 * 补全结果不需要逐字展示，且这条链路不带对话记忆，不会污染应用的 Redis 对话窗口。
 * </p>
 */
@Service
@Slf4j
public class MultiFileCodeCompleter {

    /**
     * 回传给模型的 HTML 上下文上限。正常情况下 index.html 只有几万字符，
     * 设上限是为了避免模型异常产出超长页面时把请求撑爆
     */
    private static final int MAX_HTML_CONTEXT_LENGTH = 60000;

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

    /**
     * 这里用全限定名是为了避免与 {@link jakarta.annotation.Resource} 同名冲突
     */
    @Value("classpath:prompt/codegen-multi-file-completion-system-prompt.txt")
    private org.springframework.core.io.Resource completionPrompt;

    private String systemPrompt;

    @PostConstruct
    void loadPrompt() throws IOException {
        this.systemPrompt = completionPrompt.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * 补齐缺失的 CSS / JS，结果直接写回传入的结果对象
     *
     * @param result 多文件解析结果
     * @return 是否真的发起了补全请求；文件齐全、缺少 HTML 上下文或调用异常时均返回 false
     */
    public boolean complete(MultiFileCodeResult result) {
        boolean missingCss = StrUtil.isBlank(result.getCssCode());
        boolean missingJs = StrUtil.isBlank(result.getJsCode());
        if (!missingCss && !missingJs) {
            return false;
        }
        // 没有 HTML 就没有补全的上下文，交给保存环节去报错
        if (StrUtil.isBlank(result.getHtmlCode())) {
            return false;
        }

        List<String> missingFiles = new ArrayList<>(2);
        if (missingCss) {
            missingFiles.add("style.css");
        }
        if (missingJs) {
            missingFiles.add("script.js");
        }
        log.info("多文件生成缺少 {}，发起定向补全", String.join("、", missingFiles));

        long start = System.currentTimeMillis();
        try {
            ChatResponse response = chatModel.chat(List.of(
                    SystemMessage.from(systemPrompt),
                    UserMessage.from(buildUserMessage(result.getHtmlCode(), missingFiles))));
            String text = response.aiMessage().text();
            if (missingCss) {
                String css = CodeExtractUtil.extractCode(text, "css");
                if (StrUtil.isNotBlank(css)) {
                    result.setCssCode(css);
                }
            }
            if (missingJs) {
                String js = CodeExtractUtil.extractCode(text, "js");
                if (StrUtil.isNotBlank(js)) {
                    result.setJsCode(js);
                }
            }
        } catch (Exception e) {
            log.error("多文件定向补全失败：{}", e.getMessage(), e);
            return false;
        }

        boolean cssReady = StrUtil.isNotBlank(result.getCssCode());
        boolean jsReady = StrUtil.isNotBlank(result.getJsCode());
        log.info("多文件定向补全结束，耗时 {} ms，css={}，js={}",
                System.currentTimeMillis() - start, readyText(cssReady), readyText(jsReady));
        if (!cssReady || !jsReady) {
            // 保存环节会静默跳过空内容，这里必须留下明确记录，否则又变成"页面没样式但无报错"
            log.warn("多文件定向补全后仍有文件缺失，生成目录只会保存已产出的部分");
        }
        return true;
    }

    private String buildUserMessage(String htmlCode, List<String> missingFiles) {
        String html = StrUtil.maxLength(htmlCode, MAX_HTML_CONTEXT_LENGTH);
        return """
                这是已经写好的 index.html：

                ```html
                %s
                ```

                请补全缺失的文件：%s
                """.formatted(html, String.join("、", missingFiles));
    }

    private String readyText(boolean ready) {
        return ready ? "已生成" : "仍缺失";
    }
}
