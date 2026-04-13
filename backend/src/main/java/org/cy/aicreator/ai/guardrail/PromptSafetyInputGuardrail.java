package org.cy.aicreator.ai.guardrail;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrail;
import dev.langchain4j.guardrail.InputGuardrailResult;


import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 输入安全防护拦截器
 */
public class PromptSafetyInputGuardrail implements InputGuardrail {

    // 敏感词库
    private static final List<String> SENSITIVE_WORDS = Arrays.asList(
            "忽略之前指令", "忽略上面内容", "忽略所有指令",
            "ignore previous", "ignore above", "ignore all",
            "forget above", "disregard prior",
            "破解", "黑客", "入侵", "hack", "hacking",
            "绕过", "bypass", "越狱", "jailbreak", "root",
            "指令注入", "prompt injection", "system prompt"
    );

    // 注入攻击正则
    private static final List<Pattern> INJECTION_PATTERNS = Arrays.asList(
            Pattern.compile("(?i)ignore\\s+(previous|above|all|prior)\\s+(instruction|command|prompt)s?"),
            Pattern.compile("(?i)(forget|disregard|cancel)\\s+(everything|all|prior)\\s+(above|before|content)"),
            Pattern.compile("(?i)(pretend|act|behave|roleplay)\\s+(as|like|you are)"),
            Pattern.compile("(?i)system\\s*:?\\s*you\\s+(are|must|should)"),
            Pattern.compile("(?i)new\\s+(rule|instruction|command|prompt)s?\\s*:?"),
            Pattern.compile("(?i)from\\s+now\\s+on\\s+you\\s+are"),
            Pattern.compile("(?i)override\\s+(previous|original)\\s+(instruction|prompt)")
    );

    @Override
    public InputGuardrailResult validate(UserMessage userMessage) {
        String input = userMessage.singleText();

        // 长度校验
        if (input.length() > 1000) {
            return fatal("输入内容过长，请勿超过1000字");
        }

        // 空内容校验
        if (input.trim().isEmpty()) {
            return fatal("输入内容不能为空");
        }

        // 敏感词匹配
        String lowerInput = input.toLowerCase();
        for (String word : SENSITIVE_WORDS) {
            if (lowerInput.contains(word.toLowerCase())) {
                return fatal("输入包含不当内容，请修改后重试");
            }
        }

        // 注入模式匹配
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).find()) {
                return fatal("检测到恶意输入，请求被拒绝");
            }
        }

        return success();
    }
}