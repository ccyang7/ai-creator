package org.cy.aicreator.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 聊天生成代码请求参数
 *
 * @author cy
 * @since 2026-03-21
 */
@Data
public class ChatGenerateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用ID
     */
    private Long appId;

    /**
     * 用户输入的提示词/需求描述
     */
    private String prompt;

}
