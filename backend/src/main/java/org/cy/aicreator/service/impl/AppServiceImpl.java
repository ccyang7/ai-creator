package org.cy.aicreator.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.cy.aicreator.constant.AppPriorityConstant;
import org.cy.aicreator.exception.BusinessException;
import org.cy.aicreator.exception.ErrorCode;
import org.cy.aicreator.mapper.AppMapper;
import org.cy.aicreator.model.dto.app.AppAddDTO;
import org.cy.aicreator.model.dto.app.AppAdminQueryDTO;
import org.cy.aicreator.model.dto.app.AppQueryDTO;
import org.cy.aicreator.model.entity.App;
import org.cy.aicreator.model.entity.User;
import org.cy.aicreator.model.vo.AppVO;
import org.cy.aicreator.model.vo.UserVO;
import org.cy.aicreator.service.AppService;
import org.cy.aicreator.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 应用表 服务层实现
 *
 * @author cy
 * @since 2026-03-21
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Autowired
    private UserService userService;

    /**
     * 创建应用
     *
     * @param appAddDTO 应用添加参数
     * @param userId    创建用户ID
     * @return 新创建的应用ID
     */
    @Override
    public long addApp(AppAddDTO appAddDTO, Long userId) {
        String appName = appAddDTO.getAppName();
        String initPrompt = appAddDTO.getInitPrompt();
        if (StrUtil.isBlank(initPrompt)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "initPrompt 不能为空");
        }
        App app = new App();
        BeanUtil.copyProperties(appAddDTO, app);
        app.setUserId(userId);
        boolean result = this.save(app);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建应用失败");
        }
        return app.getId();
    }

    /**
     * 获取应用视图（脱敏）
     *
     * @param app 应用实体
     * @return 应用视图对象
     */
    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        return appVO;
    }

    /**
     * 获取应用视图（含用户脱敏信息）
     *
     * @param app 应用实体
     * @return 应用视图对象（包含用户信息）
     */
    @Override
    public AppVO getAppVOWithUser(App app) {
        if (app == null) {
            return null;
        }
        AppVO appVO = new AppVO();
        BeanUtil.copyProperties(app, appVO);
        User user = userService.getById(app.getUserId());
        if (user != null) {
            UserVO userVO = userService.getUserVO(user);
            appVO.setUser(userVO);
        }
        return appVO;
    }

    /**
     * 批量获取应用视图列表（脱敏）
     *
     * @param appList 应用实体列表
     * @return 应用视图列表
     */
    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (appList == null || appList.isEmpty()) {
            return new ArrayList<>();
        }
        return appList.stream().map(this::getAppVO).collect(Collectors.toList());
    }

    /**
     * 批量获取应用视图列表（含用户脱敏信息）
     *
     * @param appList 应用实体列表
     * @return 应用视图列表（包含用户信息）
     */
    @Override
    public List<AppVO> getAppVOListWithUser(List<App> appList) {
        if (appList == null || appList.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> userIds = appList.stream()
                .map(App::getUserId)
                .collect(Collectors.toSet());
        List<User> users = userService.listByIds(userIds);
        Map<Long, UserVO> userVOMap = users.stream()
                .collect(Collectors.toMap(User::getId, user -> userService.getUserVO(user)));
        return appList.stream().map(app -> {
            AppVO appVO = new AppVO();
            BeanUtil.copyProperties(app, appVO);
            appVO.setUser(userVOMap.get(app.getUserId()));
            return appVO;
        }).collect(Collectors.toList());
    }

    /**
     * 构建用户查询条件
     * <p>
     * 用于用户查询自己的应用列表，根据用户ID和应用名称进行筛选
     * </p>
     *
     * @param appQueryDTO 用户查询参数
     * @param userId      用户ID
     * @return 查询条件包装器
     */
    @Override
    public QueryWrapper getUserQueryWrapper(AppQueryDTO appQueryDTO, Long userId) {
        if (appQueryDTO == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数为空");
        }
        String appName = appQueryDTO.getAppName();
        String sortField = appQueryDTO.getSortField();
        String sortOrder = appQueryDTO.getSortOrder();
        boolean isAsc = "ascend".equals(sortOrder);
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(App::getUserId, userId)
                .like(App::getAppName, appName);
        if (StrUtil.isBlank(sortField)) {
            queryWrapper.orderBy(App::getCreateTime, true);
        } else {
            queryWrapper.orderBy(sortField, isAsc);
        }
        return queryWrapper;
    }

    /**
     * 构建精选应用查询条件
     * <p>
     * 用于查询精选应用列表，筛选优先级大于等于 {@link AppPriorityConstant#FEATURED_PRIORITY} 的应用，
     * 并按优先级降序排序
     * </p>
     *
     * @param appQueryDTO 查询参数
     * @return 查询条件包装器
     */
    @Override
    public QueryWrapper getFeaturedQueryWrapper(AppQueryDTO appQueryDTO) {
        if (appQueryDTO == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数为空");
        }
        String appName = appQueryDTO.getAppName();
        String sortField = appQueryDTO.getSortField();
        String sortOrder = appQueryDTO.getSortOrder();
        boolean isAsc = "ascend".equals(sortOrder);
        QueryWrapper queryWrapper = QueryWrapper.create()
                .ge(App::getPriority, AppPriorityConstant.FEATURED_PRIORITY)
                .like(App::getAppName, appName)
                .orderBy(App::getPriority, false);
        if (StrUtil.isBlank(sortField)) {
            queryWrapper.orderBy(App::getCreateTime, true);
        } else {
            queryWrapper.orderBy(sortField, isAsc);
        }
        return queryWrapper;
    }

    /**
     * 构建管理员查询条件
     * <p>
     * 用于管理员查询应用列表，支持根据除时间外的任何字段进行筛选
     * </p>
     *
     * @param appAdminQueryDTO 管理员查询参数
     * @return 查询条件包装器
     */
    @Override
    public QueryWrapper getAdminQueryWrapper(AppAdminQueryDTO appAdminQueryDTO) {
        if (appAdminQueryDTO == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请求参数为空");
        }
        Long id = appAdminQueryDTO.getId();
        String appName = appAdminQueryDTO.getAppName();
        String cover = appAdminQueryDTO.getCover();
        String initPrompt = appAdminQueryDTO.getInitPrompt();
        String codeGenType = appAdminQueryDTO.getCodeGenType();
        String deployKey = appAdminQueryDTO.getDeployKey();
        Integer priority = appAdminQueryDTO.getPriority();
        Long userId = appAdminQueryDTO.getUserId();
        Integer isDelete = appAdminQueryDTO.getIsDelete();
        String sortField = appAdminQueryDTO.getSortField();
        String sortOrder = appAdminQueryDTO.getSortOrder();
        boolean isAsc = "ascend".equals(sortOrder);
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq(App::getId, id)
                .like(App::getAppName, appName)
                .like(App::getCover, cover)
                .like(App::getInitPrompt, initPrompt)
                .eq(App::getCodeGenType, codeGenType)
                .eq(App::getDeployKey, deployKey)
                .eq(App::getPriority, priority)
                .eq(App::getUserId, userId)
                .eq(App::getIsDelete, isDelete);
        if (StrUtil.isBlank(sortField)) {
            queryWrapper.orderBy(App::getCreateTime, true);
        } else {
            queryWrapper.orderBy(sortField, isAsc);
        }
        return queryWrapper;
    }

}
