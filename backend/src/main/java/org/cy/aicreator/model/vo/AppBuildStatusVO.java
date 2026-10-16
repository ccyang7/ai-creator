package org.cy.aicreator.model.vo;

import lombok.Data;
import org.cy.aicreator.model.enums.AppBuildStatusEnum;

import java.io.Serializable;

/**
 * 应用构建状态视图
 * <p>
 * 供前端在生成结束后轮询，决定是展示"构建中"、挂载预览还是提示构建失败。
 * </p>
 *
 * @author cy
 * @since 2026-10-15
 */
@Data
public class AppBuildStatusVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 构建状态（枚举值参考 AppBuildStatusEnum：none / building / success / failed）
     */
    private String status;

    /**
     * 状态说明：构建阶段提示或失败原因
     */
    private String message;

    public static AppBuildStatusVO of(AppBuildStatusEnum statusEnum, String message) {
        AppBuildStatusVO vo = new AppBuildStatusVO();
        vo.setStatus(statusEnum.getValue());
        vo.setMessage(message);
        return vo;
    }
}
