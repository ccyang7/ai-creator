package org.cy.aicreator.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用添加参数
 *
 * @author cy
 * @since 2026-03-21
 */
@Data
public class AppAddDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用初始化的 prompt（必填）
     */
    private String initPrompt;


}
