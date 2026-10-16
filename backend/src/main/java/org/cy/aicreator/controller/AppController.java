package org.cy.aicreator.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.cy.aicreator.annotation.AuthCheck;
import org.cy.aicreator.common.BaseResponse;
import org.cy.aicreator.common.DeleteRequest;
import org.cy.aicreator.common.ResultUtils;
import org.cy.aicreator.constant.AppConstant;
import org.cy.aicreator.constant.UserConstant;
import org.cy.aicreator.exception.BusinessException;
import org.cy.aicreator.exception.ErrorCode;
import org.cy.aicreator.exception.ThrowUtils;
import org.cy.aicreator.litmer.annotation.RateLimit;
import org.cy.aicreator.litmer.enums.RateLimitType;
import org.cy.aicreator.model.dto.app.*;
import org.cy.aicreator.model.entity.App;
import org.cy.aicreator.model.entity.User;
import org.cy.aicreator.model.vo.AppBuildStatusVO;
import org.cy.aicreator.model.vo.AppVO;
import org.cy.aicreator.service.AppService;
import org.cy.aicreator.service.ProjectDownloadService;
import org.cy.aicreator.service.UserService;
import org.cy.aicreator.service.VueProjectBuildService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.List;
import java.util.Map;

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

    @Resource
    private AppService appService;

    @Resource
    private UserService userService;

    @Resource
    private ProjectDownloadService projectDownloadService;

    @Resource
    private VueProjectBuildService vueProjectBuildService;


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
     * @param id 应用ID
     * @return 应用详情（含用户脱敏信息）
     */
    @GetMapping("/get")
    public BaseResponse<AppVO> getAppById(Long id) {
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
        List<AppVO> appVOList = appService.getAppVOListWithUser(appPage.getRecords());
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
        pageSize = Math.min(pageSize, 20);
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

    /**
     * 用户通过聊天生成代码
     * <p>
     * 用户通过对话方式生成代码，系统会根据应用的初始化提示词和用户输入的需求，
     * 调用 AI 生成对应的代码文件。生成完成后会自动保存到文件系统。
     * 采用 SSE（Server-Sent Events）方式流式返回生成结果。
     * </p>
     *
     * @param appId   应用ID
     * @param message 用户输入的生成需求
     * @param request HTTP 请求对象
     * @return 流式输出的代码生成结果
     */
    @GetMapping(value = "/chat/generate/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(limitType = RateLimitType.USER,rate = 5,rateInterval = 60,message = "请求过于频繁，稍后再试！")
    public Flux<ServerSentEvent<String>> chatToGenerateCode(
            @RequestParam Long appId,
            @RequestParam String message,
            HttpServletRequest request) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAM_ERROR, "应用ID无效");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAM_ERROR, "用户消息不能为空");
        User loginUser = userService.getLoginUser(request);
        Flux<String> contentFlux = appService.chatToGenerateCode(appId, message, loginUser);
        return contentFlux.map(content -> {
            Map<String, String> wrapper = Map.of("d", content);
            String jsonStr = JSONUtil.toJsonStr(wrapper);
            return ServerSentEvent.<String>builder().data(jsonStr).build();
        }).concatWith(Mono.just(ServerSentEvent.<String>builder().event("done").data("").build()));
    }

    /**
     * 查询应用构建状态
     * <p>
     * VUE_PROJECT 生成的是 Vite 源码工程，需要服务端构建出 dist 才能预览和部署。
     * 前端在生成结束后轮询本接口，据此展示"构建中"、挂载预览或提示失败原因。
     * 与应用详情接口一致不限制调用者：返回的只是构建状态，不含任何敏感信息。
     * </p>
     *
     * @param appId 应用ID
     * @return 构建状态
     */
    @GetMapping("/build/status")
    public BaseResponse<AppBuildStatusVO> getBuildStatus(@RequestParam Long appId) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAM_ERROR, "应用ID无效");
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        return ResultUtils.success(vueProjectBuildService.getStatus(appId));
    }

    /**
     * 部署应用
     * <p>
     * 将应用生成的代码文件部署到 Nginx 服务器，部署后可通过浏览器访问。
     * 系统会将生成的代码文件复制到 Nginx 部署目录，并生成唯一的访问 URL。
     * </p>
     *
     * @param appDeployDTO 部署参数
     * @param request      HTTP 请求对象
     * @return 部署后的访问 URL
     */
    @PostMapping("/deploy")
    public BaseResponse<String> deployApp(
            @RequestBody AppDeployDTO appDeployDTO,
            HttpServletRequest request) {
        ThrowUtils.throwIf(appDeployDTO == null, ErrorCode.PARAM_ERROR);
        Long appId = appDeployDTO.getAppId();
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAM_ERROR);
        User loginUser = userService.getLoginUser(request);
        String deployUrl = appService.deployApp(appId, loginUser);
        return ResultUtils.success(deployUrl);
    }


    /**
     * 下载应用代码
     *
     * @param appId    应用ID
     * @param request  请求
     * @param response 响应
     */
    @GetMapping("/download/{appId}")
    public void downloadAppCode(@PathVariable Long appId,
                                HttpServletRequest request,
                                HttpServletResponse response) {
        // 1. 基础校验
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAM_ERROR, "应用ID无效");
        // 2. 查询应用信息
        App app = appService.getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 3. 权限校验：只有应用创建者可以下载代码
        User loginUser = userService.getLoginUser(request);
        if (!app.getUserId().equals(loginUser.getId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限下载该应用代码");
        }
        // 4. 构建应用代码目录路径（生成目录，非部署目录）
        String codeGenType = app.getCodeGenType();
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        // 5. 检查代码目录是否存在
        File sourceDir = new File(sourceDirPath);
        ThrowUtils.throwIf(!sourceDir.exists() || !sourceDir.isDirectory(),
                ErrorCode.NOT_FOUND_ERROR, "应用代码不存在，请先生成代码");
        // 6. 生成下载文件名
        String downloadFileName = String.valueOf(appId);
        // 7. 调用通用下载服务
        projectDownloadService.downloadProjectToZip(sourceDirPath, downloadFileName, response);
    }


}
