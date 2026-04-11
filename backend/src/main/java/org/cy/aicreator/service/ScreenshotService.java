package org.cy.aicreator.service;


import org.springframework.stereotype.Service;

public interface ScreenshotService {


    /**
     * 截取网页截图
     *
     * @param webUrl 网页地址
     * @return 截图文件路径
     */
    String takeScreenshot(String webUrl);


}
