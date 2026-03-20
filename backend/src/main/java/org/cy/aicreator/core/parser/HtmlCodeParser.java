package org.cy.aicreator.core.parser;

import org.cy.aicreator.ai.model.HtmlCodeResult;

/**
 * HTML 单文件解析器
 */
public class HtmlCodeParser implements CodeParser<HtmlCodeResult> {

    /**
     * 解析 HTML 单文件代码
     *
     * @param content 原始代码内容
     * @return 解析后的结果对象
     */
    @Override
    public HtmlCodeResult parseCode(String content) {

        HtmlCodeResult result = new HtmlCodeResult();

        if (content == null || content.isBlank()) {
            return result;
        }

        String htmlCode = CodeExtractUtil.extractCode(content, "html");

        // 没有代码块就用原内容
        result.setHtmlCode(htmlCode != null ? htmlCode : content.trim());

        return result;
    }
}