package org.cy.aicreator.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * Vue 工程构建状态枚举
 * <p>
 * 只有 VUE_PROJECT 类型需要构建：模型产出的是 Vite 源码工程，必须经 npm build
 * 才能得到浏览器可直接运行的 dist 产物。其余生成类型始终是 {@link #NONE}。
 * </p>
 *
 * @author cy
 * @since 2026-10-15
 */
@Getter
public enum AppBuildStatusEnum {

    /**
     * 尚未构建
     */
    NONE("none", "尚未构建"),

    /**
     * 构建中
     */
    BUILDING("building", "构建中"),

    /**
     * 构建成功，dist 产物可用
     */
    SUCCESS("success", "构建成功"),

    /**
     * 构建失败
     */
    FAILED("failed", "构建失败");

    private final String value;
    private final String text;

    AppBuildStatusEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static AppBuildStatusEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (AppBuildStatusEnum anEnum : AppBuildStatusEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
