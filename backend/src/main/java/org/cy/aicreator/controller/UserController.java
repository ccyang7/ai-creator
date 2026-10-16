package org.cy.aicreator.controller;

import cn.hutool.core.bean.BeanUtil;
import com.mybatisflex.core.paginate.Page;
import jakarta.servlet.http.HttpServletRequest;
import org.cy.aicreator.annotation.AuthCheck;
import org.cy.aicreator.common.BaseResponse;
import org.cy.aicreator.common.DeleteRequest;
import org.cy.aicreator.common.ResultUtils;
import org.cy.aicreator.constant.UserConstant;
import org.cy.aicreator.exception.BusinessException;
import org.cy.aicreator.exception.ErrorCode;
import org.cy.aicreator.exception.ThrowUtils;
import org.cy.aicreator.litmer.annotation.RateLimit;
import org.cy.aicreator.litmer.enums.RateLimitType;
import org.cy.aicreator.model.dto.user.UserAddDTO;
import org.cy.aicreator.model.dto.user.UserLoginDTO;
import org.cy.aicreator.model.dto.user.UserQueryDTO;
import org.cy.aicreator.model.dto.user.UserRegisterDTO;
import org.cy.aicreator.model.dto.user.UserUpdateDTO;
import org.cy.aicreator.model.vo.LoginUserVO;
import org.cy.aicreator.model.vo.UserVO;
import org.cy.aicreator.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.cy.aicreator.model.entity.User;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


/**
 * 用户表 控制层。
 *
 * @author cy
 * @since 2026-02-28
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 注册
     *
     * @param userRegisterDTO 用户注册信息
     * @return 用户id
     */
    @PostMapping("/register")
    public BaseResponse<Long> register(@RequestBody UserRegisterDTO userRegisterDTO) {
        ThrowUtils.throwIf(userRegisterDTO == null, ErrorCode.PARAM_ERROR);
        long register = userService.register(userRegisterDTO);
        return ResultUtils.success(register);
    }


    /**
     * 登录
     *
     * @param userLoginDTO 用户登录信息
     * @return 登录用户信息
     */
    @PostMapping("/login")
    @RateLimit(key = "user_login", limitType = RateLimitType.IP, rate = 5, rateInterval = 60,
            message = "登录尝试过于频繁，请 60 秒后重试")
    public BaseResponse<LoginUserVO> login(@RequestBody UserLoginDTO userLoginDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginDTO == null, ErrorCode.PARAM_ERROR);
        LoginUserVO login = userService.login(userLoginDTO, request);
        return ResultUtils.success(login);
    }


    /**
     * 获取当前登录用户
     *
     * @return 当前登录用户(脱敏)
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 退出登录
     *
     * @return 退出登录
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> logout(HttpServletRequest request) {
        return ResultUtils.success(userService.userLogOut(request));
    }

    /**
     * 管理员添加用户
     *
     * @param userAddDTO 用户添加信息
     * @return 新增用户id
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddDTO userAddDTO) {
        ThrowUtils.throwIf(userAddDTO == null, ErrorCode.PARAM_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddDTO, user);
        // 默认密码 12345678
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据 id 获取用户（仅管理员）
     *
     * @param id id
     * @return 用户信息
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAM_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取用户脱敏信息
     *
     * @param id id
     * @return 用户脱敏信息
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户
     *
     * @param deleteRequest 删除请求参数
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户
     *
     * @param userUpdateDTO 更新请求参数
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateDTO userUpdateDTO) {
        if (userUpdateDTO == null || userUpdateDTO.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        User user = new User();
        BeanUtil.copyProperties(userUpdateDTO, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页获取用户脱敏列表（仅管理员）
     *
     * @param userQueryDTO 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryDTO userQueryDTO) {
        ThrowUtils.throwIf(userQueryDTO == null, ErrorCode.PARAM_ERROR);
        long pageNum = userQueryDTO.getPageNum();
        long pageSize = userQueryDTO.getPageSize();
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize),
                userService.getQueryWrapper(userQueryDTO));
        // 数据脱敏
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }


}
