package org.cy.aicreator.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * 多文件代码结果
 */
@Description("生成多文件代码结果")
@Data
public class MultiFileCodeResult {

    /**
     * Html 代码
     */
    @Description("Html样式代码")
    private String htmlCode;

    /**
     * Css 样式代码
     */
    @Description("Css样式代码")
    private String cssCode;

    /**
     * Js 脚本代码
     */
    @Description("Js脚本代码")
    private String jsCode;

    /**
     * 描述
     */
    @Description("代码描述")
    private String description;
}
