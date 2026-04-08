package org.cy.aicreator.model.dto.app;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.cy.aicreator.common.PageRequest;

import java.io.Serializable;

/**
 * 应用(分页)查询参数（用户）
 * <p>
 * 用于用户查询自己的应用列表或查询精选应用列表
 * </p>
 *
 * @author cy
 * @since 2026-03-21
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AppQueryDTO extends PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;


    /**
     * 应用名称（支持模糊查询）
     */
    private String appName;


}
