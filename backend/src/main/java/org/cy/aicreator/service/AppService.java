package org.cy.aicreator.service;

import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import org.cy.aicreator.model.dto.app.AppAddDTO;
import org.cy.aicreator.model.dto.app.AppAdminQueryDTO;
import org.cy.aicreator.model.dto.app.AppQueryDTO;
import org.cy.aicreator.model.entity.App;
import org.cy.aicreator.model.entity.User;
import org.cy.aicreator.model.vo.AppVO;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 应用表 服务层
 *
 * @author cy
 * @since 2026-03-21
 */
public interface AppService extends IService<App> {

    /**
     * 创建应用
     *
     * @param appAddDTO 应用添加参数
     * @param userId    创建用户ID
     * @return 新创建的应用ID
     */
    long addApp(AppAddDTO appAddDTO, Long userId);

    /**
     * 获取应用视图（脱敏）
     *
     * @param app 应用实体
     * @return 应用视图对象
     */
    AppVO getAppVO(App app);

    /**
     * 获取应用视图（含用户脱敏信息）
     *
     * @param app 应用实体
     * @return 应用视图对象（包含用户信息）
     */
    AppVO getAppVOWithUser(App app);

    /**
     * 批量获取应用视图列表（脱敏）
     *
     * @param appList 应用实体列表
     * @return 应用视图列表
     */
    List<AppVO> getAppVOList(List<App> appList);

    /**
     * 批量获取应用视图列表（含用户脱敏信息）
     *
     * @param appList 应用实体列表
     * @return 应用视图列表（包含用户信息）
     */
    List<AppVO> getAppVOListWithUser(List<App> appList);

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
    QueryWrapper getUserQueryWrapper(AppQueryDTO appQueryDTO, Long userId);

    /**
     * 构建精选应用查询条件
     * <p>
     * 用于查询精选应用列表，筛选优先级大于0的应用，并按优先级降序排序
     * </p>
     *
     * @param appQueryDTO 查询参数
     * @return 查询条件包装器
     */
    QueryWrapper getFeaturedQueryWrapper(AppQueryDTO appQueryDTO);

    /**
     * 构建管理员查询条件
     * <p>
     * 用于管理员查询应用列表，支持根据除时间外的任何字段进行筛选
     * </p>
     *
     * @param appAdminQueryDTO 管理员查询参数
     * @return 查询条件包装器
     */
    QueryWrapper getAdminQueryWrapper(AppAdminQueryDTO appAdminQueryDTO);

    /**
     * 通过聊天生成代码
     *
     * @param appId    应用ID
     * @param prompt   提示
     * @param loginUser 登录用户
     * @return 生成结果
     */
    Flux<String> chatToGenerateCode(Long appId, String prompt, User loginUser);


    /**
     * 部署应用
     *
     * @param appId 应用ID
     * @param loginUser 登录用户
     * @return 部署结果
     */
    String deployApp(Long appId,User loginUser);

}
