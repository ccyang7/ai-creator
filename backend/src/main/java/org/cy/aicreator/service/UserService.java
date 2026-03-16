package org.cy.aicreator.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import jakarta.servlet.http.HttpServletRequest;
import org.cy.aicreator.model.dto.UserLoginDTO;
import org.cy.aicreator.model.dto.UserQueryDTO;
import org.cy.aicreator.model.dto.UserRegisterDTO;
import org.cy.aicreator.model.entity.User;
import org.cy.aicreator.model.vo.LoginUserVO;
import org.cy.aicreator.model.vo.UserVO;

import java.util.List;

/**
 * 用户 服务层。
 *
 * @author cy
 * @since 2026-02-27
 */
public interface UserService extends IService<User> {

    /**
     * 注册用户。
     *
     * @param userRegisterDTO 注册信息
     * @return 用户id
     */
    long register(UserRegisterDTO userRegisterDTO);

    /**
     * 获取脱敏登录用户信息
     *
     * @param user 用户信息
     * @return 登录用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 登录
     *
     * @param userLoginDTO 登录信息
     * @return 脱敏后的用户信息
     */
    LoginUserVO login(UserLoginDTO userLoginDTO, HttpServletRequest request);


    /**
     * 获取当前登录用户
     *
     * @return 用户
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 退出登录
     *
     * @return 是否退出成功
     */
    boolean userLogOut(HttpServletRequest request);

    /**
     * 获取脱敏用户信息
     *
     * @param user 用户信息
     * @return 脱敏后的用户信息
     */
    UserVO getUserVO(User user);


    /**
     * 获取脱敏用户信息列表
     *
     * @param userList 用户列表
     * @return 脱敏后的用户列表
     */
    List<UserVO> getUserVOList(List<User> userList);


    /**
     * 获取查询条件
     *
     * @param userQueryDTO 查询参数
     * @return 查询条件
     */
    QueryWrapper getQueryWrapper(UserQueryDTO userQueryDTO);

    /**
     * 获取加密密码
     *
     * @return 默认密码
     */
    String getEncryptPassword(String Password);
}
