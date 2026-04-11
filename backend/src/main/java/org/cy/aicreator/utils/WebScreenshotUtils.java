package org.cy.aicreator.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.cy.aicreator.exception.BusinessException;
import org.cy.aicreator.exception.ErrorCode;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.io.File;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Slf4j
public class WebScreenshotUtils {

    private static final WebDriver webDriver;

    static {
        final int DEFAULT_WIDTH = 1600;
        final int DEFAULT_HEIGHT = 900;
        webDriver = initChromeDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    @PreDestroy
    public void destroy() {
        webDriver.quit();
    }

    /**
     * 初始化 Edge 浏览器驱动
     */
    /**
     * 初始化 Chrome 浏览器驱动
     */
    private static WebDriver initChromeDriver(int width, int height) {
        try {
            // 自动管理 ChromeDriver
            System.setProperty("wdm.chromeDriverMirrorUrl", "https://registry.npmmirror.com/binary.html?path=chromedriver");
            WebDriverManager.chromedriver().useMirror().setup();
            // 配置 Chrome 选项
            ChromeOptions options = new ChromeOptions();
            // 无头模式
            options.addArguments("--headless");
            // 禁用GPU（在某些环境下避免问题）
            options.addArguments("--disable-gpu");
            // 禁用沙盒模式（Docker环境需要）
            options.addArguments("--no-sandbox");
            // 禁用开发者shm使用
            options.addArguments("--disable-dev-shm-usage");
            // 设置窗口大小
            options.addArguments(String.format("--window-size=%d,%d", width, height));
            // 禁用扩展
            options.addArguments("--disable-extensions");
            // 设置用户代理
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            // 创建驱动
            WebDriver driver = new ChromeDriver(options);
            // 设置页面加载超时
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            // 设置隐式等待
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            return driver;
        } catch (Exception e) {
            log.error("初始化 Chrome 浏览器失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化 Chrome 浏览器失败");
        }
    }




    /**
     * 执行网页截图并返回压缩后的文件路径
     *
     * @param pageUrl 目标网页地址
     * @return 压缩图片路径，失败返回 null
     */
    public static String captureAndCompressWebPage(String pageUrl) {
        // 空值校验
        if (StrUtil.isBlank(pageUrl)) {
            log.error("网页地址不能为空，截图终止");
            return null;
        }

        try {
            // 构建临时文件存储目录
            String baseDir = System.getProperty("user.dir") + File.separator + "tmp" + File.separator
                    + "screenshot" + File.separator + UUID.randomUUID().toString().substring(0, 5);
            FileUtil.mkdir(baseDir);

            // 原始图片配置
            final String PIC_SUFFIX = ".png";
            String originalFilePath = baseDir + File.separator + RandomUtil.randomNumbers(5) + PIC_SUFFIX;

            // 访问页面并等待加载完成
            webDriver.get(pageUrl);
            waitPageFullyLoaded(webDriver);

            // 执行截图
            byte[] picBytes = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);

            // 保存原始图片
            saveToFile(picBytes, originalFilePath);
            log.info("原始截图已保存：{}", originalFilePath);

            // 压缩图片
            final String COMPRESSED_SUFFIX = "_compress.jpg";
            String compressedFilePath = baseDir + File.separator + RandomUtil.randomNumbers(5) + COMPRESSED_SUFFIX;
            compressImageQuality(originalFilePath, compressedFilePath);
            log.info("压缩图片已生成：{}", compressedFilePath);

            // 删除原始大图，节省空间
            FileUtil.del(originalFilePath);

            return compressedFilePath;
        } catch (Exception e) {
            log.error("网页截图整体流程异常，URL：{}", pageUrl, e);
            return null;
        }
    }


    /**
     * 将图片字节写入文件
     */
    private static void saveToFile(byte[] imageBytes, String filePath) {
        try {
            // 自动创建目录
            FileUtil.writeBytes(imageBytes, filePath);
        } catch (Exception e) {
            log.error("图片写入文件失败：{}", filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片保存失败");
        }
    }

    /**
     * 压缩图片（可自定义质量）
     */
    private static void compressImageQuality(String sourcePath, String targetPath) {
        final float COMPRESS_QUALITY = 0.3f;
        try {
            ImgUtil.compress(
                    FileUtil.file(sourcePath),
                    FileUtil.file(targetPath),
                    COMPRESS_QUALITY
            );
        } catch (Exception e) {
            log.error("图片压缩失败：源文件={} 目标文件={}", sourcePath, targetPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片压缩失败");
        }
    }

    /**
     * 等待页面完全加载（含动态渲染）
     */
    private static void waitPageFullyLoaded(WebDriver driver) {
        try {
            // 显示等待：直到页面状态为加载完成
            WebDriverWait webDriverWait = new WebDriverWait(driver, Duration.ofSeconds(10));
            webDriverWait.until(waitDriver ->
                    Objects.equals(((JavascriptExecutor) waitDriver).executeScript("return document.readyState"), "complete")
            );

            // 固定等待，保证异步内容渲染完毕
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(2000));
            log.info("页面已加载完成，准备截图");
        } catch (Exception e) {
            log.warn("页面加载等待出错，跳过等待直接截图", e);
        }
    }
}