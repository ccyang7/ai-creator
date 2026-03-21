package org.cy.aicreator.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户更新参数
 *
 * @author cy
 * @since 2026-02-28
 */
@Data
public class UserUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

}
