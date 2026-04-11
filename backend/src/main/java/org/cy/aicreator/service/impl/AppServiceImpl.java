package org.cy.aicreator.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.cy.aicreator.ai.SmartRoutingService;
import org.cy.aicreator.ai.SmartRoutingServiceFactory;
import org.cy.aicreator.constant.AppConstant;
import org.cy.aicreator.core.AiCreatorFacade;
import org.cy.aicreator.core.handler.StreamHandlerExecutor;
import org.cy.aicreator.exception.BusinessException;
import org.cy.aicreator.exception.ErrorCode;
import org.cy.aicreator.exception.ThrowUtils;
import org.cy.aicreator.mapper.AppMapper;
import org.cy.aicreator.model.dto.app.AppAddDTO;
import org.cy.aicreator.model.dto.app.AppAdminQueryDTO;
import org.cy.aicreator.model.dto.app.AppQueryDTO;
import org.cy.aicreator.model.entity.App;
import org.cy.aicreator.model.entity.User;
import org.cy.aicreator.model.enums.ChatHistoryMessageTypeEnum;
import org.cy.aicreator.model.enums.GenerTypeEnum;
import org.cy.aicreator.model.vo.AppVO;
import org.cy.aicreator.model.vo.UserVO;
import org.cy.aicreator.service.AppService;
import org.cy.aicreator.service.ChatHistoryService;
import org.cy.aicreator.service.ScreenshotService;
import org.cy.aicreator.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
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

    @Resource
    private UserService userService;

    @Resource
    private AiCreatorFacade aiCreatorFacade;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private SmartRoutingServiceFactory smartRoutingServiceFactory;

    /**
     * 创建应用
     *
     * @param appAddDTO 应用添加参数
     * @param userId    创建用户ID
     * @return 新创建的应用ID
     */
    @Override
    public Long addApp(AppAddDTO appAddDTO, Long userId) {

        String initPrompt = appAddDTO.getInitPrompt();
        if (StrUtil.isBlank(initPrompt)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "initPrompt 不能为空");
        }
        App app = new App();
        BeanUtil.copyProperties(appAddDTO, app);
        app.setUserId(userId);
        // AI智能路由决定生成代码类型（多例模式）
        SmartRoutingService smartRoutingService = smartRoutingServiceFactory.createSmartCodeGenTypeRoutingService();
        GenerTypeEnum codeGenType = smartRoutingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(codeGenType.getValue());
        // AI智能路由决定应用名称
        String appName = smartRoutingService.routeAppName(initPrompt);
        app.setAppName(appName);
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
                .eq(App::getUserId, userId);
        if (StrUtil.isNotBlank(appName)) {
            queryWrapper.like(App::getAppName, appName);
        }
        if (StrUtil.isBlank(sortField)) {
            queryWrapper.orderBy(App::getCreateTime, false);
        } else {
            queryWrapper.orderBy(sortField, isAsc);
        }
        return queryWrapper;
    }

    /**
     * 构建精选应用查询条件
     * <p>
     * 用于查询精选应用列表，筛选优先级大于等于 {@link AppConstant#FEATURED_PRIORITY} 的应用，
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
                .ge(App::getPriority, AppConstant.FEATURED_PRIORITY);
        if (StrUtil.isNotBlank(appName)) {
            queryWrapper.like(App::getAppName, appName);
        }
        queryWrapper.orderBy(App::getPriority, false);
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
        String appName = appAdminQueryDTO.getAppName();
        String codeGenType = appAdminQueryDTO.getCodeGenType();
        Long userId = appAdminQueryDTO.getUserId();
        String sortField = appAdminQueryDTO.getSortField();
        String sortOrder = appAdminQueryDTO.getSortOrder();
        boolean isAsc = "ascend".equals(sortOrder);
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("user_id", userId);
        if (StrUtil.isNotBlank(appName)) {
            queryWrapper.like("app_name", appName);
        }
        if (StrUtil.isNotBlank(codeGenType)) {
            queryWrapper.like("code_gen_type", codeGenType);
        }
        if (StrUtil.isBlank(sortField)) {
            queryWrapper.orderBy("create_time", true);
        } else {
            queryWrapper.orderBy(sortField, isAsc);
        }
        return queryWrapper;
    }

    /**
     * 通过聊天生成代码
     * <p>
     * 用户通过对话方式生成代码，系统会根据应用的初始化提示词和用户输入的需求，
     * 调用 AI 生成对应的代码文件。生成完成后会自动保存到文件系统。
     * </p>
     *
     * @param appId     应用ID
     * @param message   用户输入的提示词/需求描述
     * @param loginUser 当前登录用户
     * @return 流式输出的代码生成结果
     * @throws BusinessException 参数错误、应用不存在、无权限或代码生成类型无效
     */
    @Override
    public Flux<String> chatToGenerateCode(Long appId, String message, User loginUser) {
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "应用ID无效");
        }
        if (StrUtil.isBlank(message)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "提示词不能为空");
        }
        App app = this.getById(appId);
        if (app == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        }
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权操作该应用");
        }
        // 校验应用的代码生成类型
        String codeGenType = app.getCodeGenType();
        GenerTypeEnum generTypeEnum = GenerTypeEnum.getEnumByValue(codeGenType);
        ThrowUtils.throwIf(generTypeEnum == null, ErrorCode.PARAM_ERROR, "代码生成类型无效");

        // 通过校验后，添加用户消息到对话历史
        chatHistoryService.addChatMessage(appId, loginUser.getId(), message, ChatHistoryMessageTypeEnum.USER.getValue());

        // 调用AI生成代码
        String initPrompt = app.getInitPrompt();
        String userMessage = initPrompt + "\n\n用户需求：" + message;
        Flux<String> codeStream = aiCreatorFacade.generateAndSaveCodeStream(userMessage, generTypeEnum, appId);

        // 收集AI响应内容，完成后保存对话历史
        return streamHandlerExecutor.doExecute(codeStream, chatHistoryService, appId, loginUser, generTypeEnum);
    }

    /**
     * 部署应用
     * <p>
     * 将应用生成的代码文件复制到 Nginx 部署目录，并生成可访问的 URL
     * </p>
     *
     * @param appId     应用 ID
     * @param loginUser 登录用户
     * @return 部署后的访问 URL
     * @throws BusinessException 参数错误、应用不存在、无权限或源目录不存在
     */
    @Override
    public String deployApp(Long appId, User loginUser) {
        // 1. 参数校验
        if (appId == null || appId <= 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "应用 ID 无效");
        }

        // 2. 查询应用信息
        App app = this.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");

        // 3. 验证用户是否有权限部署该应用，仅本人可以部署
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权部署该应用");
        }

        // 4. 检查是否已有 deployKey，没有则生成 6 位 deployKey（大小写字母 + 数字）
        String deployKey = app.getDeployKey();
        if (StrUtil.isBlank(deployKey)) {
            deployKey = RandomUtil.randomString(6);
        }

        // 5. 获取代码生成类型，构建源目录路径
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        File sourceDir = new File(sourceDirPath);

        // 6. 检查源目录是否存在
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "该应用不存在，请先生成代码");
        }

        // 7. 复制文件到部署目录（使用 deployKey 作为目录名）
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        File deployDir = new File(deployDirPath);

        // 如果部署目录已存在，先清空
        if (deployDir.exists()) {
            FileUtil.del(deployDir);
        }

        // 创建部署目录并复制文件
        try {
            FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "复制文件失败" + e.getMessage());
        }

        // 8. 更新应用的 deployKey 和部署时间
        App updateApp = new App();
        updateApp.setId(appId);
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        boolean result = this.updateById(updateApp);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "更新部署信息失败");


        // 10. 构建应用访问 URL
        String appDeployUrl = String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
        // 11. 异步生成截图并更新应用封面
        takeScreenshotAsync(appId, appDeployUrl);
        return appDeployUrl;

    }


    @Resource
    private ScreenshotService screenshotService;

    /**
     * 异步生成应用截图并更新封面
     *
     * @param appId  应用ID
     * @param appUrl 应用访问URL
     */
    public void takeScreenshotAsync(Long appId, String appUrl) {
        // 使用虚拟线程异步执行
        Thread.startVirtualThread(() -> {
            // 调用截图服务生成截图并上传
            String screenshotUrl = screenshotService.takeScreenshot(appUrl);
            // 更新应用封面字段
            App updateApp = new App();
            updateApp.setId(appId);
            updateApp.setCover(screenshotUrl);
            boolean updated = this.updateById(updateApp);
            ThrowUtils.throwIf(!updated, ErrorCode.OPERATION_ERROR, "更新应用封面字段失败");
        });
    }


    /**
     * 删除应用时关联删除对话历史、源代码文件和部署文件
     *
     * @param id 应用ID
     * @return 是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        Long appId = Long.parseLong(id.toString());
        if (appId <= 0) {
            return false;
        }

        App app = this.getById(appId);
        if (app == null) {
            return false;
        }

        String codeGenType = app.getCodeGenType();
        String deployKey = app.getDeployKey();

        // 删除源代码文件
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        File sourceDir = new File(sourceDirPath);
        if (sourceDir.exists()) {
            FileUtil.del(sourceDir);
        }

        // 删除部署文件
        if (StrUtil.isNotBlank(deployKey)) {
            String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
            File deployDir = new File(deployDirPath);
            if (deployDir.exists()) {
                FileUtil.del(deployDir);
            }
        }

        chatHistoryService.deleteByAppId(appId);

        return super.removeById(id);
    }


}
