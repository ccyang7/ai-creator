package org.cy.aicreator.ai.model;

import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

/**
 * Html 响应结果
 */
@Description("生成HTML响应结果")
@Data
public class HtmlCodeResult {

    /**
     * Html 代码
     */
    @Description("Html代码")
    private String htmlCode;

    /**
     * 描述
     */
    @Description("代码描述")
    private String description;
}
