package org.cy.aicreator.core.parser;

import org.cy.aicreator.exception.BusinessException;
import org.cy.aicreator.exception.ErrorCode;
import org.cy.aicreator.model.enums.GenerTypeEnum;

import java.util.HashMap;
import java.util.Map;

/**
 * 代码解析执行器
 */
public class CodeParserExecutor {

    private static final Map<GenerTypeEnum, CodeParser<?>> parserMap = new HashMap<>();

    // 构造函数
    static {
        parserMap.put(GenerTypeEnum.HTML, new HtmlCodeParser());
        parserMap.put(GenerTypeEnum.MULTI_FILE, new MultiFileCodeParser());
    }

    /**
     * 执行解析
     */
    @SuppressWarnings("unchecked")
    public static <T> T execute(GenerTypeEnum outputType, String content) {

        CodeParser<?> parser = parserMap.get(outputType);

        if (parser == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的解析类型：" + outputType);
        }

        return (T) parser.parseCode(content);
    }
}