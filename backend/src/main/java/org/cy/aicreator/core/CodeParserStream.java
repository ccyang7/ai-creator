package org.cy.aicreator.core;

import org.cy.aicreator.ai.model.HtmlCodeResult;
import org.cy.aicreator.ai.model.MultiFileCodeResult;
import org.cy.aicreator.exception.BusinessException;
import org.cy.aicreator.exception.ErrorCode;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码解析器（流式输出）
 */
@Deprecated
public final class CodeParserStream {

    /**
     * 代码块正则
     */
    private static final Map<String, Pattern> CODE_PATTERNS = new HashMap<>();

    static {
        CODE_PATTERNS.put("html",
                Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE));

        CODE_PATTERNS.put("css",
                Pattern.compile("```css\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE));

        CODE_PATTERNS.put("js",
                Pattern.compile("```(?:js|javascript)\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE));
    }

    private CodeParserStream() {
        // 防止实例化
    }

    /**
     * 解析 HTML 单文件代码
     */
    public static HtmlCodeResult parseHtmlCode(String content) {

        HtmlCodeResult result = new HtmlCodeResult();

        if (content == null || content.isBlank()) {
            return result;
        }

        String htmlCode = extractCode(content, "html");

        // 如果没有代码块，直接使用原内容
        result.setHtmlCode(htmlCode != null ? htmlCode : content.trim());

        return result;
    }

    /**
     * 解析多文件代码（HTML + CSS + JS）
     */
    public static MultiFileCodeResult parseMultiFileCode(String content) {

        MultiFileCodeResult result = new MultiFileCodeResult();

        if (content == null || content.isBlank()) {
            return result;
        }

        String html = extractCode(content, "html");
        String css = extractCode(content, "css");
        String js = extractCode(content, "js");

        if (html != null) {
            result.setHtmlCode(html);
        }

        if (css != null) {
            result.setCssCode(css);
        }

        if (js != null) {
            result.setJsCode(js);
        }

        return result;
    }

    /**
     * 通用代码提取方法
     */
    private static String extractCode(String content, String type) {

        Pattern pattern = CODE_PATTERNS.get(type);

        if (pattern == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的此代码类型正则表达式：" + type);
        }

        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }
}