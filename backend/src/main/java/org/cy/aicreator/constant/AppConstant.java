package org.cy.aicreator.constant;

/**
 * 应用优先级常量
 * <p>
 * 用于定义应用的优先级级别，数值越大优先级越高。
 * </p>
 *
 * @author cy
 * @since 2026-03-21
 */
public interface AppConstant {

    /**
     * 普通应用优先级
     */
    int NORMAL_PRIORITY = 0;

    /**
     * 精选应用优先级
     */
    int FEATURED_PRIORITY = 99;



    /**
     * 应用生成目录
     */
    String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    /**
     * 应用部署目录
     */
    String CODE_DEPLOY_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_deploy";

    /**
     * 应用部署域名
     */
    String CODE_DEPLOY_HOST = "http://localhost:88";



}
