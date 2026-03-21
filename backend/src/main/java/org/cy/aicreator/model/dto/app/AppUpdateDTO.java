package org.cy.aicreator.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用更新参数（用户）
 * <p>
 * 用户更新自己的应用时使用，目前只支持修改应用名称
 * </p>
 *
 * @author cy
 * @since 2026-03-21
 */
@Data
public class AppUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用ID（必填）
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

}
