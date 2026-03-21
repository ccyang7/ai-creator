package org.cy.aicreator.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用视图（脱敏）
 * <p>
 * 用于返回给前端的应用信息，隐藏了敏感字段
 * </p>
 *
 * @author cy
 * @since 2026-03-21
 */
@Data
public class AppVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用ID
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
     * 应用初始化的 prompt
     */
    private String initPrompt;

    /**
     * 代码生成类型（枚举值参考 GenerTypeEnum）
     */
    private String codeGenType;

    /**
     * 部署标识
     */
    private String deployKey;

    /**
     * 部署时间
     */
    private LocalDateTime deployedTime;

    /**
     * 优先级（数值越大优先级越高，用于精选应用排序）
     */
    private Integer priority;

    /**
     * 创建用户ID
     */
    private Long userId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 创建用户信息（脱敏）
     */
    private UserVO user;

}
