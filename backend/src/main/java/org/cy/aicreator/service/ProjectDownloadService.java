package org.cy.aicreator.service;

import jakarta.servlet.http.HttpServletResponse;

public interface ProjectDownloadService {


    /**
     * 下载项目
     *
     * @param projectPath      项目路径
     * @param downloadFileName 下载文件名
     * @param response         响应
     */
    void downloadProjectToZip(String projectPath, String downloadFileName, HttpServletResponse response);

}
