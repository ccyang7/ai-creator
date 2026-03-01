package org.cy.aicreator.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册参数
 *
 * @author cy
 * @since 2026-02-28
 */
@Data
public class UserRegisterDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 校验密码
     */
    private String checkPassword;
}
