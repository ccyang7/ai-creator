package org.cy.aicreator.core.parser;

import org.cy.aicreator.exception.BusinessException;
import org.cy.aicreator.exception.ErrorCode;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用代码提取工具
 */
public final class CodeExtractUtil {

    // 代码块正则
    private static final Map<String, Pattern> CODE_PATTERNS = new HashMap<>();
    static {
        CODE_PATTERNS.put("html",
                Pattern.compile("```html\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE));

        CODE_PATTERNS.put("css",
                Pattern.compile("```css\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE));

        CODE_PATTERNS.put("js",
                Pattern.compile("```(?:js|javascript)\\s*\\n([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE));
    }

    // 防止实例化
    private CodeExtractUtil() {
    }

    /**
     * 通用代码提取方法
     */
    public static String extractCode(String content, String type) {

        Pattern pattern = CODE_PATTERNS.get(type);

        if (pattern == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,
                    "不支持的代码类型：" + type);
        }

        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }
}