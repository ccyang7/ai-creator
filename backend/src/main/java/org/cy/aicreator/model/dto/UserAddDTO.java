package org.cy.aicreator.model.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户添加参数
 *
 * @author cy
 * @since 2026-02-28
 */
@Data
public class UserAddDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色: user, admin
     */
    private String userRole;

}
