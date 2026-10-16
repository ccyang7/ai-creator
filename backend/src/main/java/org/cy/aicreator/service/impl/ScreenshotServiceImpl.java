package org.cy.aicreator.service.impl;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.cy.aicreator.config.ScreenshotProperties;
import org.cy.aicreator.exception.BusinessException;
import org.cy.aicreator.exception.ErrorCode;
import org.cy.aicreator.exception.ThrowUtils;
import org.cy.aicreator.manager.CosManager;
import org.cy.aicreator.manager.WebPageCapturer;
import org.cy.aicreator.monitor.ScreenshotMetrics;
import org.cy.aicreator.service.ScreenshotService;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 封面截图服务
 * <p>
 * 负责截图链路的编排：抓取 → 压缩 → 上传对象存储 → 清理临时文件。
 * 具体的浏览器操作与失败重试在 {@link WebPageCapturer}，这里只处理产物与失败上报。
 * 截图属于部署的非关键路径，任何失败都不应影响应用本身的可用性。
 * </p>
 *
 * @author cy
 * @since 2026-10-16
 */
@Service
@Slf4j
public class ScreenshotServiceImpl implements ScreenshotService {

    /**
     * 上传对象存储的重试次数。截图已经抓到了，因为一次网络抖动丢掉整张封面不划算
     */
    private static final int UPLOAD_MAX_ATTEMPTS = 3;

    /**
     * 上传重试的退避基数（毫秒）
     */
    private static final long UPLOAD_RETRY_BACKOFF_MILLIS = 500;

    private static final String UPLOAD_FAILURE_CATEGORY = "upload";

    private static final String UNKNOWN_FAILURE_CATEGORY = "unknown";

    @Resource
    private WebPageCapturer webPageCapturer;

    @Resource
    private CosManager cosManager;

    @Resource
    private ScreenshotProperties screenshotProperties;

    @Resource
    private ScreenshotMetrics screenshotMetrics;

    @Override
    public String takeScreenshot(String webUrl) {
        ThrowUtils.throwIf(StrUtil.isBlank(webUrl), ErrorCode.PARAM_ERROR, "网页URL不能为空");
        log.info("开始生成网页截图，URL: {}", webUrl);

        long startedAt = System.currentTimeMillis();
        File workDir = createWorkDir();
        try {
            WebPageCapturer.CaptureOutcome outcome = webPageCapturer.capture(webUrl);
            if (!outcome.success()) {
                String reason = outcome.describe();
                log.error("网页截图失败，URL：{}，原因：{}", webUrl, reason);
                screenshotMetrics.recordFailure(categoryOf(outcome), elapsed(startedAt));
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "网页截图失败：" + reason);
            }

            File compressedFile = compressToJpeg(outcome.image(), workDir);
            String cosUrl = uploadToCos(compressedFile);
            if (StrUtil.isBlank(cosUrl)) {
                screenshotMetrics.recordFailure(UPLOAD_FAILURE_CATEGORY, elapsed(startedAt));
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "截图上传对象存储失败");
            }

            screenshotMetrics.recordSuccess(elapsed(startedAt));
            log.info("网页截图生成并上传成功: {} -> {}", webUrl, cosUrl);
            return cosUrl;
        } catch (BusinessException e) {
            // 上面的失败分支已经打过点，这里不再重复计数
            throw e;
        } catch (Exception e) {
            screenshotMetrics.recordFailure(UNKNOWN_FAILURE_CATEGORY, elapsed(startedAt));
            log.error("网页截图流程异常，URL：{}", webUrl, e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "网页截图失败：" + StrUtil.brief(rootMessage(e), 200));
        } finally {
            cleanup(workDir);
        }
    }

    /**
     * 创建本次截图的工作目录，临时文件都放在这个目录下，结束时整体删除
     */
    private File createWorkDir() {
        String baseDir = System.getProperty("user.dir") + File.separator + "tmp" + File.separator
                + "screenshot" + File.separator + UUID.randomUUID().toString().substring(0, 5);
        return FileUtil.mkdir(baseDir);
    }

    /**
     * 把截图压缩成固定质量的 JPEG，封面不需要原图质量
     */
    private File compressToJpeg(byte[] image, File workDir) {
        File rawFile = FileUtil.file(workDir, RandomUtil.randomNumbers(5) + ".png");
        File compressedFile = FileUtil.file(workDir, RandomUtil.randomNumbers(5) + "_compress.jpg");
        try {
            FileUtil.writeBytes(image, rawFile);
            ImgUtil.compress(rawFile, compressedFile, (float) screenshotProperties.getJpegQuality());
        } finally {
            // 原图体积大且不再使用，压缩后立即删除
            FileUtil.del(rawFile);
        }
        return compressedFile;
    }

    /**
     * 上传截图到对象存储，失败时按次数退避重试
     *
     * @return 对象存储访问URL，重试后仍失败返回 null
     */
    private String uploadToCos(File screenshotFile) {
        // 对象键只生成一次，重试覆盖同一个对象，避免留下无人引用的残留文件
        String fileName = UUID.randomUUID().toString().substring(0, 8) + "_compressed.jpg";
        String cosKey = generateScreenshotKey(fileName);
        String lastError = "未知原因";
        for (int attempt = 1; attempt <= UPLOAD_MAX_ATTEMPTS; attempt++) {
            try {
                String cosUrl = cosManager.uploadFile(cosKey, screenshotFile);
                if (StrUtil.isNotBlank(cosUrl)) {
                    return cosUrl;
                }
                lastError = "上传接口返回空结果";
            } catch (Exception e) {
                lastError = StrUtil.brief(rootMessage(e), 200);
            }
            log.warn("截图上传对象存储失败，第 {}/{} 次尝试：{}", attempt, UPLOAD_MAX_ATTEMPTS, lastError);
            if (attempt < UPLOAD_MAX_ATTEMPTS && !sleepQuietly(UPLOAD_RETRY_BACKOFF_MILLIS * attempt)) {
                break;
            }
        }
        log.error("截图上传对象存储最终失败：{}", lastError);
        return null;
    }

    /**
     * 生成截图的对象存储键
     * 格式：/screenshots/2025/07/31/filename.jpg
     */
    private String generateScreenshotKey(String fileName) {
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return String.format("/screenshots/%s/%s", datePath, fileName);
    }

    /**
     * 清理本次截图产生的所有本地文件
     */
    private void cleanup(File workDir) {
        try {
            FileUtil.del(workDir);
        } catch (Exception e) {
            log.warn("清理截图临时目录失败：{}", workDir, e);
        }
    }

    private String categoryOf(WebPageCapturer.CaptureOutcome outcome) {
        return outcome.category().name().toLowerCase(Locale.ROOT);
    }

    private long elapsed(long startedAt) {
        return System.currentTimeMillis() - startedAt;
    }

    private boolean sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private String rootMessage(Throwable e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return StrUtil.nullToEmpty(cause.getMessage());
    }
}
