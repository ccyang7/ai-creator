package org.cy.aicreator.core.parser;

import org.cy.aicreator.ai.model.MultiFileCodeResult;

/**
 * 多文件代码解析器（HTML + CSS + JS）
 */
public class MultiFileCodeParser implements CodeParser<MultiFileCodeResult> {



    /**
     * 解析多文件代码
     *
     * @param content 多文件代码内容
     * @return 解析结果
     */
    @Override
    public MultiFileCodeResult parseCode(String content) {

        MultiFileCodeResult result = new MultiFileCodeResult();

        if (content == null || content.isBlank()) {
            return result;
        }

        String html = CodeExtractUtil.extractCode(content, "html");
        String css = CodeExtractUtil.extractCode(content, "css");
        String js = CodeExtractUtil.extractCode(content, "js");

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
}