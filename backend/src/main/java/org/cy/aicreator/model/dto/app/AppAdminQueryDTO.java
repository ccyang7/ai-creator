package org.cy.aicreator.model.dto.app;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.cy.aicreator.common.PageRequest;

import java.io.Serializable;

/**
 * 应用(分页)查询参数（管理员）
 * <p>
 * 管理员查询应用列表时使用，支持根据除时间外的任何字段查询
 * </p>
 *
 * @author cy
 * @since 2026-03-21
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AppAdminQueryDTO extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用ID
     */
    private Long id;

    /**
     * 应用名称（支持模糊查询）
     */
    private String appName;

    /**
     * 代码生成类型（枚举值参考 GenerTypeEnum）
     */
    private String codeGenType;

    /**
     * 创建用户ID
     */
    private Long userId;

}
