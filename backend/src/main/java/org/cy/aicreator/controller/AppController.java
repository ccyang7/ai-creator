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
import org.cy.aicreator.model.dto.app.AppAddDTO;
import org.cy.aicreator.model.dto.app.AppAdminQueryDTO;
import org.cy.aicreator.model.dto.app.AppAdminUpdateDTO;
import org.cy.aicreator.model.dto.app.AppQueryDTO;
import org.cy.aicreator.model.dto.app.AppUpdateDTO;
import org.cy.aicreator.model.entity.App;
import org.cy.aicreator.model.entity.User;
import org.cy.aicreator.model.vo.AppVO;
import org.cy.aicreator.service.AppService;
import org.cy.aicreator.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 应用表 控制层
 * <p>
 * 提供应用的增删改查接口，分为用户接口和管理员接口
 * </p>
 *
 * @author cy
 * @since 2026-03-21
 */
@RestController
@RequestMapping("/app")
public class AppController {

    @Autowired
    private AppService appService;

    @Autowired
    private UserService userService;

    /**
     * 用户创建应用
     * <p>
     * 用户创建新应用，须填写 initPrompt 参数
     * </p>
     *
     * @param appAddDTO 应用添加参数
     * @param request   HTTP请求对象
     * @return 新创建的应用ID
     */
    @PostMapping("/add")
    public BaseResponse<Long> addApp(@RequestBody AppAddDTO appAddDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(appAddDTO == null, ErrorCode.PARAM_ERROR);
        User loginUser = userService.getLoginUser(request);
        long appId = appService.addApp(appAddDTO, loginUser.getId());
        return ResultUtils.success(appId);
    }

    /**
     * 用户修改自己的应用
     * <p>
     * 用户根据应用ID修改自己的应用，目前只支持修改应用名称
     * </p>
     *
     * @param appUpdateDTO 应用更新参数
     * @param request      HTTP请求对象
     * @return 是否更新成功
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateApp(@RequestBody AppUpdateDTO appUpdateDTO, HttpServletRequest request) {
        if (appUpdateDTO == null || appUpdateDTO.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long appId = appUpdateDTO.getId();
        App oldApp = appService.getById(appId);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!oldApp.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR);
        App app = new App();
        app.setId(appId);
        app.setAppName(appUpdateDTO.getAppName());
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 用户删除自己的应用
     * <p>
     * 用户根据应用ID删除自己的应用，只能删除自己创建的应用
     * </p>
     *
     * @param deleteRequest 删除请求参数
     * @param request       HTTP请求对象
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteApp(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long appId = deleteRequest.getId();
        App oldApp = appService.getById(appId);
        ThrowUtils.throwIf(oldApp == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!oldApp.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR);
        boolean result = appService.removeById(appId);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 用户查看应用详情
     * <p>
     * 用户根据应用ID查看任意应用详情，返回应用信息和创建用户的脱敏信息
     * </p>
     *
     * @param id      应用ID
     * @param request HTTP请求对象
     * @return 应用详情（含用户脱敏信息）
     */
    @GetMapping("/get")
    public BaseResponse<AppVO> getAppById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAM_ERROR);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(appService.getAppVOWithUser(app));
    }

    /**
     * 用户分页查询自己的应用列表
     * <p>
     * 用户分页查询自己创建的应用列表，支持根据应用名称模糊查询，每页最多返回20条记录
     * </p>
     *
     * @param appQueryDTO 查询参数
     * @param request     HTTP请求对象
     * @return 应用分页列表
     */
    @PostMapping("/my/list/page")
    public BaseResponse<Page<AppVO>> listMyAppByPage(@RequestBody AppQueryDTO appQueryDTO, HttpServletRequest request) {
        ThrowUtils.throwIf(appQueryDTO == null, ErrorCode.PARAM_ERROR);
        User loginUser = userService.getLoginUser(request);
        long pageNum = appQueryDTO.getPageNum();
        long pageSize = appQueryDTO.getPageSize();
        pageSize = Math.min(pageSize, 20);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize),
                appService.getUserQueryWrapper(appQueryDTO, loginUser.getId()));
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 用户分页查询精选应用列表
     * <p>
     * 用户分页查询精选应用列表，精选应用为优先级大于0的应用，按优先级降序排序。
     * 支持根据应用名称模糊查询，每页最多返回20条记录。返回结果包含创建用户的脱敏信息
     * </p>
     *
     * @param appQueryDTO 查询参数
     * @return 精选应用分页列表（含用户脱敏信息）
     */
    @PostMapping("/featured/list/page")
    public BaseResponse<Page<AppVO>> listFeaturedAppByPage(@RequestBody AppQueryDTO appQueryDTO) {
        ThrowUtils.throwIf(appQueryDTO == null, ErrorCode.PARAM_ERROR);
        long pageNum = appQueryDTO.getPageNum();
        long pageSize = appQueryDTO.getPageSize();
        pageSize = Math.min(pageSize, 20);
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize),
                appService.getFeaturedQueryWrapper(appQueryDTO));
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOListWithUser(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 管理员删除任意应用
     * <p>
     * 管理员根据应用ID删除任意应用，需要管理员权限
     * </p>
     *
     * @param deleteRequest 删除请求参数
     * @return 是否删除成功
     */
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminDeleteApp(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        boolean result = appService.removeById(deleteRequest.getId());
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 管理员更新任意应用
     * <p>
     * 管理员根据应用ID更新任意应用，支持更新应用名称、应用封面、优先级，需要管理员权限
     * </p>
     *
     * @param appAdminUpdateDTO 管理员更新参数
     * @return 是否更新成功
     */
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminUpdateApp(@RequestBody AppAdminUpdateDTO appAdminUpdateDTO) {
        if (appAdminUpdateDTO == null || appAdminUpdateDTO.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR);
        }
        App app = new App();
        BeanUtil.copyProperties(appAdminUpdateDTO, app);
        boolean result = appService.updateById(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 管理员分页查询应用列表
     * <p>
     * 管理员分页查询应用列表，支持根据除时间外的任何字段查询，每页数量不限，需要管理员权限
     * </p>
     *
     * @param appAdminQueryDTO 管理员查询参数
     * @return 应用分页列表
     */
    @PostMapping("/admin/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<AppVO>> adminListAppByPage(@RequestBody AppAdminQueryDTO appAdminQueryDTO) {
        ThrowUtils.throwIf(appAdminQueryDTO == null, ErrorCode.PARAM_ERROR);
        long pageNum = appAdminQueryDTO.getPageNum();
        long pageSize = appAdminQueryDTO.getPageSize();
        Page<App> appPage = appService.page(Page.of(pageNum, pageSize),
                appService.getAdminQueryWrapper(appAdminQueryDTO));
        Page<AppVO> appVOPage = new Page<>(pageNum, pageSize, appPage.getTotalRow());
        List<AppVO> appVOList = appService.getAppVOList(appPage.getRecords());
        appVOPage.setRecords(appVOList);
        return ResultUtils.success(appVOPage);
    }

    /**
     * 管理员查看应用详情
     * <p>
     * 管理员根据应用ID查看应用详情，返回完整的应用信息（包括敏感字段），需要管理员权限
     * </p>
     *
     * @param id 应用ID
     * @return 应用详情（完整信息）
     */
    @GetMapping("/admin/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<App> adminGetAppById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAM_ERROR);
        App app = appService.getById(id);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(app);
    }

}
