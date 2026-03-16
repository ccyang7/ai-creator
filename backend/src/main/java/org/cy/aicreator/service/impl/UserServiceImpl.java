package org.cy.aicreator.service.impl;

import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.cy.aicreator.exception.BusinessException;
import org.cy.aicreator.exception.ErrorCode;
import org.cy.aicreator.model.dto.UserLoginDTO;
import org.cy.aicreator.model.dto.UserQueryDTO;
import org.cy.aicreator.model.dto.UserRegisterDTO;
import org.cy.aicreator.mapper.UserMapper;
import org.cy.aicreator.model.entity.User;
import org.cy.aicreator.model.enums.UserRoleEnum;
import org.cy.aicreator.model.vo.LoginUserVO;
import org.cy.aicreator.model.vo.UserVO;
import org.cy.aicreator.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.cy.aicreator.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 用户 服务层实现。
 *
 * @author cy
 * @since 2026-02-27
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {


    /**
     * 用户注册
     *
     * @param userRegisterDTO 用户注册信息
     * @return 用户id
     */
    @Override
    public long register(UserRegisterDTO userRegisterDTO) {
        String userAccount = userRegisterDTO.getUserAccount();
        String userPassword = userRegisterDTO.getUserPassword();
        String checkPassword = userRegisterDTO.getCheckPassword();

        // 1.检验输入参数
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "两次输入的密码不一致");
        }
        // 2.检查用户是否存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(User::getUserAccount, userAccount);
        long count = this.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户已存在");
        }

        // 3.加密密码
        String encryptPassword = this.getEncryptPassword(userPassword);

        // 4.创建用户
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("普通用户");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean save = this.save(user);
        if (!save) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "注册失败");
        }
        return user.getId();
    }

    /**
     * 用户信息脱敏
     *
     * @param user 用户信息
     * @return 用户脱敏信息
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 用户登录
     *
     * @param userLoginDTO 用户登录信息
     * @param request      请求
     * @return 用户信息
     */
    @Override
    public LoginUserVO login(UserLoginDTO userLoginDTO, HttpServletRequest request) {
        String userAccount = userLoginDTO.getUserAccount();
        String userPassword = userLoginDTO.getUserPassword();
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户密码过短");
        }

        // 加密密码
        String encryptPassword = this.getEncryptPassword(userPassword);

        // 查询用户
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq(User::getUserAccount, userAccount);
        queryWrapper.eq(User::getUserPassword, encryptPassword);
        User user = this.getOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户不存在或密码错误");
        }

        // 用户存在，记录登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        return this.getLoginUserVO(user);
    }

    /**
     * 获取当前登录用户
     *
     * @param request 请求
     * @return 当前登录用户
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 判断是否已登录
        Object user = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) user;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        // 从数据库查询当前用户信息
        Long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 用户注销
     *
     * @param request 请求
     * @return 是否注销成功
     */
    @Override
    public boolean userLogOut(HttpServletRequest request) {
        // 判断是否已登录
        Object user = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) user;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 移除登录态
        try {
            request.getSession().removeAttribute(USER_LOGIN_STATE);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    /**
     * 获取脱敏用户信息
     *
     * @param user 用户信息
     * @return 脱敏用户信息
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获取脱敏用户信息列表
     *
     * @param userList 用户信息列表
     * @return 脱敏用户信息列表
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollectionUtils.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    /**
     * 获取查询条件
     *
     * @param userQueryDTO 用户查询条件
     * @return 查询条件
     */
    @Override
    public QueryWrapper getQueryWrapper(UserQueryDTO userQueryDTO) {
        if (userQueryDTO == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数为空");
        }
        Long id = userQueryDTO.getId();
        String userAccount = userQueryDTO.getUserAccount();
        String userName = userQueryDTO.getUserName();
        String userProfile = userQueryDTO.getUserProfile();
        String userRole = userQueryDTO.getUserRole();
        String sortField = userQueryDTO.getSortField();
        String sortOrder = userQueryDTO.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .eq("user_role", userRole)
                .like("user_account", userAccount)
                .like("user_name", userName)
                .like("user_profile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    /**
     * 加密密码
     *
     * @param Password 密码
     * @return 加密后的密码
     */
    @Override
    public String getEncryptPassword(String Password) {
        // 密码盐
        final String PASSWORD_SALT = "cyyy";
        return DigestUtils.md5DigestAsHex((Password + PASSWORD_SALT)
                .getBytes(StandardCharsets.UTF_8));
    }

}
