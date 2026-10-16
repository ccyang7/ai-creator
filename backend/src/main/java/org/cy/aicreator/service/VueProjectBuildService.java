package org.cy.aicreator.service;

import org.cy.aicreator.model.vo.AppBuildStatusVO;

/**
 * Vue 工程构建服务
 * <p>
 * VUE_PROJECT 生成的是一份 Vite 源码工程，浏览器无法直接运行，必须构建出 dist 产物
 * 才能预览、部署和截图。构建是重操作，统一走异步触发 + 状态轮询。
 * </p>
 *
 * @author cy
 * @since 2026-10-15
 */
public interface VueProjectBuildService {

    /**
     * 异步触发构建，立即返回。
     * <p>
     * 同一个应用重复触发会被忽略；超过并发上限会被拒绝并把状态置为失败。
     * </p>
     *
     * @param appId 应用 ID
     */
    void triggerBuildAsync(long appId);

    /**
     * 查询构建状态
     *
     * @param appId 应用 ID
     * @return 构建状态与说明
     */
    AppBuildStatusVO getStatus(long appId);
}
