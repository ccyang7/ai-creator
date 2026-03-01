package org.cy.aicreator.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录参数
 *
 * @author cy
 * @since 2026-02-28
 */
@Data
public class UserLoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;
}
