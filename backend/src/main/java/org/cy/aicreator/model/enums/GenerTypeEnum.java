package org.cy.aicreator.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 代码生成类型枚举
 *
 * @author cy
 * @since 2026-03-21
 */
@Getter
public enum GenerTypeEnum {

    /**
     * HTML模式
     */
    HTML("HTML", "html"),

    /**
     * 多文件模式
     */
    MULTI_FILE("MULTI_FILE", "multi_file"),


    /**
     * Vue 工程模式
     */
    VUE_PROJECT("VUE_PROJECT", "vue_project");
;


    private final String text;
    private final String value;

    GenerTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 枚举值的value
     * @return 枚举值
     */
    public static GenerTypeEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (GenerTypeEnum anEnum : GenerTypeEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
