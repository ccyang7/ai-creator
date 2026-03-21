package org.cy.aicreator.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用更新参数（管理员）
 * <p>
 * 管理员更新任意应用时使用，支持更新应用名称、应用封面、优先级
 * </p>
 *
 * @author cy
 * @since 2026-03-21
 */
@Data
public class AppAdminUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用ID（必填）
     */
    private Long id;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 应用封面（图片URL）
     */
    private String cover;

    /**
     * 优先级（数值越大优先级越高，用于精选应用排序）
     */
    private Integer priority;

}
