package org.cy.aicreator.manager;

import cn.hutool.core.util.StrUtil;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.cy.aicreator.config.ScreenshotProperties;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 网页截图器
 * <p>
 * 维护一个有界的无头 Chrome 池对外提供截图能力。相比早期实现有三个关键差异：
 * </p>
 * <ul>
 *   <li>浏览器实例池化并有上限：Selenium 的 WebDriver 不是线程安全的，
 *       多个截图任务共用同一个实例会互相打断会话；池中的实例在取用时探活，
 *       失效或超期即销毁重建，单个实例崩溃不会让后续截图永久失败。</li>
 *   <li>等待页面渲染而不是等待加载：{@code document.readyState} 对 Vue 这类
 *       客户端渲染页面没有意义，改为等待"已经画出内容"这一可观测信号，
 *       并以资源加载静默作为辅助判据，最后有兜底超时，到点照常截图。</li>
 *   <li>失败可重试且可分类：瞬时故障重试，失败原因按枚举返回，便于打点与排查。</li>
 * </ul>
 *
 * @author cy
 * @since 2026-10-16
 */
@Component
@Slf4j
public class WebPageCapturer {

    /**
     * 判断页面是否已经渲染出内容。优先看 Vue 的挂载点，退化到 body；
     * 同时要求字体加载结束，避免截到字体回退的中间态
     */
    private static final String RENDER_READY_SCRIPT = """
            return (function () {
              if (document.readyState === 'loading') { return false; }
              var root = document.querySelector('#app') || document.body;
              if (!root || root.children.length === 0) { return false; }
              if (document.body.scrollHeight <= 0) { return false; }
              if (document.fonts && document.fonts.status !== 'loaded') { return false; }
              return true;
            })();
            """;

    /**
     * 页面是否已经画出任何内容。用于渲染等待超时后判断"是慢"还是"真的空白"
     */
    private static final String ANY_CONTENT_SCRIPT = """
            return (function () {
              var body = document.body;
              if (!body) { return false; }
              return body.children.length > 0 && body.scrollHeight > 0;
            })();
            """;

    private static final String CONTENT_HEIGHT_SCRIPT = """
            return Math.max(
              document.body ? document.body.scrollHeight : 0,
              document.documentElement ? document.documentElement.scrollHeight : 0
            );
            """;

    private static final String RESOURCE_COUNT_SCRIPT = """
            return (window.performance && window.performance.getEntriesByType)
              ? window.performance.getEntriesByType('resource').length
              : 0;
            """;

    private static final String SCROLL_TO_TOP_SCRIPT = "window.scrollTo(0, 0);";

    /**
     * 轮询间隔（毫秒）
     */
    private static final long POLL_INTERVAL_MILLIS = 150;

    /**
     * 截图源探活超时（秒）
     */
    private static final int PREFLIGHT_TIMEOUT_SECONDS = 10;

    /**
     * 探活请求用的浏览器标识。默认的 Java UA 会被部分站点拦截，误判成"源不可达"
     */
    private static final String PREFLIGHT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    /**
     * HEAD 请求被拒或未命中时用 GET 复核的状态码：不支持 HEAD（405/501）、
     * 路由只配给 GET 而把 HEAD 当成不存在（404）
     */
    private static final Set<Integer> HEAD_FALLBACK_STATUS = Set.of(404, 405, 501);

    /**
     * 截图源探活用的客户端。只取响应头，不下载正文，开销远小于启动一次浏览器
     */
    private final HttpClient preflightClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * 失败原因分类，同时作为指标标签，取值有限
     */
    public enum FailureCategory {

        BUSY("截图资源繁忙"),
        UNREACHABLE("截图源不可访问"),
        TIMEOUT("页面加载或渲染超时"),
        RENDER("页面未渲染出内容"),
        DRIVER("浏览器实例故障"),
        EMPTY("浏览器返回空图片"),
        INTERRUPTED("线程被中断");

        private final String description;

        FailureCategory(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 一次截图的结果，成功时携带图片字节，失败时携带分类与原因
     */
    public record CaptureOutcome(byte[] image, FailureCategory category, String detail) {

        public static CaptureOutcome success(byte[] image) {
            return new CaptureOutcome(image, null, null);
        }

        public static CaptureOutcome failure(FailureCategory category, String detail) {
            return new CaptureOutcome(null, category, detail);
        }

        public boolean success() {
            return image != null;
        }

        /**
         * 供日志与前端展示的失败描述
         */
        public String describe() {
            return success() ? "" : category.getDescription() + "：" + detail;
        }
    }

    /**
     * 池中的浏览器实例
     */
    private static final class PooledDriver {

        private final WebDriver driver;

        private final Instant createdAt;

        private int uses;

        private PooledDriver(WebDriver driver, Instant createdAt) {
            this.driver = driver;
            this.createdAt = createdAt;
        }
    }

    @Resource
    private ScreenshotProperties properties;

    /**
     * 总闸门：许可数等于池容量，保证同时在世的浏览器实例不超过配置上限
     */
    private Semaphore permits;

    /**
     * 空闲实例。仅在持有许可时访问，因此不需要额外同步
     */
    private final Deque<PooledDriver> idleDrivers = new ArrayDeque<>();

    private final Object driverManagerLock = new Object();

    /**
     * ChromeDriver 是否已就绪。放在首次截图时初始化而不是启动时，
     * 避免下载驱动失败导致整个应用起不来
     */
    private volatile boolean driverManagerReady = false;

    private volatile boolean closed = false;

    @PostConstruct
    public void init() {
        this.permits = new Semaphore(Math.max(1, properties.getPoolSize()));
        log.info("封面截图已初始化：截图源={}, 并发上限={}", properties.getSource(), properties.getPoolSize());
    }

    /**
     * 关闭池中所有实例。这个类由 Spring 管理，销毁钩子才会真正执行，否则 Chrome 进程会泄漏
     */
    @PreDestroy
    public void shutdown() {
        closed = true;
        int count = 0;
        PooledDriver pooled;
        while ((pooled = idleDrivers.pollFirst()) != null) {
            quitQuietly(pooled.driver);
            count++;
        }
        log.info("封面截图已关闭，释放浏览器实例 {} 个", count);
    }

    /**
     * 抓取页面截图，内部按配置重试
     *
     * @param pageUrl 页面地址
     * @return 截图结果，成功时携带 PNG 字节
     */
    public CaptureOutcome capture(String pageUrl) {
        CaptureOutcome preflightFailure = preflight(pageUrl);
        if (preflightFailure != null) {
            log.warn("截图源探活未通过，URL：{}，原因：{}", pageUrl, preflightFailure.describe());
            return preflightFailure;
        }

        int maxAttempts = Math.max(1, properties.getMaxRetries() + 1);
        CaptureOutcome outcome = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            outcome = captureOnce(pageUrl);
            if (outcome.success()) {
                return outcome;
            }
            if (attempt < maxAttempts) {
                long backoff = properties.getRetryBackoffMillis() * (1L << (attempt - 1));
                log.warn("第 {} 次截图失败，{} 毫秒后重试。URL：{}，原因：{}", attempt, backoff, pageUrl, outcome.describe());
                if (!sleepQuietly(backoff)) {
                    return CaptureOutcome.failure(FailureCategory.INTERRUPTED, "重试等待被中断");
                }
            }
        }
        return outcome;
    }

    /**
     * 截图源探活。
     * <p>
     * 之所以要有这一步：Chrome 打不开目标地址时不会留下白页，而是渲染一张自带的"无法访问"
     * 错误页，这张页面有 DOM、有高度、还有字体，任何"页面是否画出内容"的判断都会认为它正常，
     * 于是错误页会被当成封面存下来——既看不出失败原因，也污染了数据。花几十毫秒先自己发一次
     * 请求确认源可达，比启动浏览器后发现拿到的是错误页便宜得多。
     * </p>
     *
     * @return 探活失败的结果，通过时返回 null
     */
    private CaptureOutcome preflight(String pageUrl) {
        if (!StrUtil.startWithAnyIgnoreCase(pageUrl, "http://", "https://")) {
            // 非 http(s) 地址（例如 file://）浏览器自己会处理，不拦截
            return null;
        }
        URI uri;
        try {
            uri = URI.create(pageUrl);
        } catch (IllegalArgumentException e) {
            return CaptureOutcome.failure(FailureCategory.UNREACHABLE, "截图源地址不合法");
        }

        try {
            HttpResponse<Void> response = sendPreflight(uri, "HEAD");
            int statusCode = response.statusCode();
            if (HEAD_FALLBACK_STATUS.contains(statusCode)) {
                response = sendPreflight(uri, "GET");
                statusCode = response.statusCode();
            }
            if (statusCode >= 400) {
                return CaptureOutcome.failure(FailureCategory.UNREACHABLE, "截图源返回 HTTP " + statusCode);
            }
            return null;
        } catch (HttpTimeoutException e) {
            return CaptureOutcome.failure(FailureCategory.TIMEOUT,
                    "截图源 " + PREFLIGHT_TIMEOUT_SECONDS + " 秒内未响应");
        } catch (IOException e) {
            return CaptureOutcome.failure(FailureCategory.UNREACHABLE, rootMessage(e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CaptureOutcome.failure(FailureCategory.INTERRUPTED, "截图源探活被中断");
        }
    }

    private HttpResponse<Void> sendPreflight(URI uri, String method) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .method(method, HttpRequest.BodyPublishers.noBody())
                .timeout(Duration.ofSeconds(PREFLIGHT_TIMEOUT_SECONDS))
                .header("User-Agent", PREFLIGHT_USER_AGENT)
                .build();
        return preflightClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    private CaptureOutcome captureOnce(String pageUrl) {
        PooledDriver pooled;
        try {
            pooled = acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CaptureOutcome.failure(FailureCategory.INTERRUPTED, "等待可用的浏览器实例被中断");
        } catch (Exception e) {
            return CaptureOutcome.failure(FailureCategory.BUSY, rootMessage(e));
        }

        WebDriver driver = pooled.driver;
        boolean reusable = true;
        try {
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(properties.getPageLoadTimeoutSeconds()));
            applyViewport(driver);

            try {
                driver.get(pageUrl);
            } catch (TimeoutException e) {
                // 页面存在轮询或长连接时 pageLoadTimeout 会先触发，此时页面往往已经渲染出来，
                // 继续走渲染等待而不是直接判失败
                log.warn("页面加载超时（{} 秒），按当前状态继续截图：{}", properties.getPageLoadTimeoutSeconds(), pageUrl);
            }

            if (!waitForRender(driver, pageUrl)) {
                return CaptureOutcome.failure(FailureCategory.RENDER, "等待 " + properties.getRenderTimeoutSeconds() + " 秒后仍无可渲染内容");
            }

            expandToFullPage(driver);
            scrollToTop(driver);

            byte[] image = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            if (image == null || image.length == 0) {
                return CaptureOutcome.failure(FailureCategory.EMPTY, "浏览器返回了空图片");
            }
            return CaptureOutcome.success(image);
        } catch (TimeoutException e) {
            return CaptureOutcome.failure(FailureCategory.TIMEOUT, "页面加载超时（" + properties.getPageLoadTimeoutSeconds() + " 秒）");
        } catch (WebDriverException e) {
            reusable = !isSessionDead(e);
            return CaptureOutcome.failure(classify(e), brief(e));
        } catch (Exception e) {
            return CaptureOutcome.failure(FailureCategory.DRIVER, brief(e));
        } finally {
            release(pooled, reusable);
        }
    }

    /**
     * 从池中取一个可用的浏览器实例，没有空闲的就新建。取用时会探活，
     * 失效或超期的实例直接销毁重取，这是"崩一次永久失效"的解药
     */
    private PooledDriver acquire() throws InterruptedException {
        if (!permits.tryAcquire(properties.getAcquireTimeoutSeconds(), TimeUnit.SECONDS)) {
            throw new IllegalStateException("截图并发已满，等待 " + properties.getAcquireTimeoutSeconds() + " 秒仍未获得可用实例");
        }
        boolean handedOff = false;
        try {
            PooledDriver pooled;
            while ((pooled = idleDrivers.pollFirst()) != null) {
                if (isReusable(pooled, false) && isAlive(pooled.driver)) {
                    handedOff = true;
                    return pooled;
                }
                quitQuietly(pooled.driver);
            }
            PooledDriver created = new PooledDriver(createDriver(), Instant.now());
            handedOff = true;
            return created;
        } finally {
            if (!handedOff) {
                permits.release();
            }
        }
    }

    /**
     * 归还实例。不可复用（会话已失效）或超期超次数的直接销毁，其余放回池中
     */
    private void release(PooledDriver pooled, boolean reusable) {
        try {
            if (closed || !reusable || !isReusable(pooled, true)) {
                quitQuietly(pooled.driver);
            } else {
                idleDrivers.addLast(pooled);
            }
        } finally {
            permits.release();
        }
    }

    /**
     * 判断实例是否还值得复用
     *
     * @param countUse 是否把本次使用计入复用次数
     */
    private boolean isReusable(PooledDriver pooled, boolean countUse) {
        if (countUse) {
            pooled.uses++;
        }
        if (pooled.uses >= properties.getMaxUsesPerDriver()) {
            return false;
        }
        long aliveSeconds = Duration.between(pooled.createdAt, Instant.now()).getSeconds();
        return aliveSeconds < properties.getDriverTtlSeconds();
    }

    private boolean isAlive(WebDriver driver) {
        try {
            driver.getWindowHandles();
            return true;
        } catch (Exception e) {
            log.warn("浏览器实例探活失败，将重建：{}", brief(e));
            return false;
        }
    }

    private WebDriver createDriver() {
        ensureDriverManager();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-extensions");
        options.addArguments("--hide-scrollbars");
        // 页面在无头环境下可能被当作后台标签页而降频定时器，会让异步渲染的等待变得不可靠
        options.addArguments("--disable-background-timer-throttling");
        options.addArguments("--disable-renderer-backgrounding");
        options.addArguments(String.format("--window-size=%d,%d", properties.getViewportWidth(), properties.getViewportHeight()));
        return new ChromeDriver(options);
    }

    /**
     * 首次使用时才准备 ChromeDriver，失败可重试，且不会把应用启动拖下水
     */
    private void ensureDriverManager() {
        if (driverManagerReady) {
            return;
        }
        synchronized (driverManagerLock) {
            if (driverManagerReady) {
                return;
            }
            System.setProperty("wdm.chromeDriverMirrorUrl", "https://registry.npmmirror.com/binary.html?path=chromedriver");
            WebDriverManager.chromedriver().useMirror().setup();
            driverManagerReady = true;
        }
    }

    /**
     * 等待页面渲染出内容。主判据是"已经画出东西"，资源静默只作辅助：
     * 生成的应用里可能有轮询请求导致网络永不静默，不能作为必要条件
     *
     * @return 是否等到了渲染信号
     */
    private boolean waitForRender(WebDriver driver, String pageUrl) {
        boolean rendered = false;
        try {
            new WebDriverWait(driver, Duration.ofSeconds(properties.getRenderTimeoutSeconds()))
                    .pollingEvery(Duration.ofMillis(POLL_INTERVAL_MILLIS))
                    .until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(RENDER_READY_SCRIPT)));
            rendered = true;
        } catch (TimeoutException e) {
            log.warn("等待页面渲染超时（{} 秒）：{}", properties.getRenderTimeoutSeconds(), pageUrl);
        } catch (WebDriverException e) {
            log.warn("等待页面渲染时浏览器异常：{}", brief(e));
        }

        waitForNetworkIdle(driver);
        sleepQuietly(properties.getSettleMillis());

        if (rendered) {
            return true;
        }
        // 渲染信号没等到，但页面确实有内容（例如挂了非 Vue 的慢页面）时仍然截图，
        // 只有真的空白才判失败，交给重试
        return hasAnyContent(driver);
    }

    /**
     * 等待资源加载静默，最多 networkIdleTimeoutMillis。必须有上限，
     * 否则页面里的轮询请求会让这里永远等下去
     */
    private void waitForNetworkIdle(WebDriver driver) {
        long timeout = properties.getNetworkIdleTimeoutMillis();
        if (timeout <= 0) {
            return;
        }
        long deadline = System.currentTimeMillis() + timeout;
        long lastCount = resourceCount(driver);
        long lastChangedAt = System.currentTimeMillis();
        while (System.currentTimeMillis() < deadline) {
            if (!sleepQuietly(POLL_INTERVAL_MILLIS)) {
                return;
            }
            long count = resourceCount(driver);
            if (count != lastCount) {
                lastCount = count;
                lastChangedAt = System.currentTimeMillis();
                continue;
            }
            if (System.currentTimeMillis() - lastChangedAt >= properties.getSettleMillis()) {
                return;
            }
        }
    }

    private boolean hasAnyContent(WebDriver driver) {
        try {
            return Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript(ANY_CONTENT_SCRIPT));
        } catch (WebDriverException e) {
            return false;
        }
    }

    private void applyViewport(WebDriver driver) {
        try {
            driver.manage().window().setSize(
                    new Dimension(properties.getViewportWidth(), properties.getViewportHeight()));
        } catch (Exception e) {
            log.warn("设置视口尺寸失败，沿用浏览器当前尺寸：{}", brief(e));
        }
    }

    /**
     * 按页面实际高度扩展视口，截取整页。只改高度不改宽度，
     * 避免触发基于宽度的媒体查询导致布局变化
     */
    private void expandToFullPage(WebDriver driver) {
        if (!properties.isFullPage()) {
            return;
        }
        int contentHeight = measureContentHeight(driver);
        int targetHeight = Math.min(
                Math.max(contentHeight, properties.getViewportHeight()),
                properties.getMaxCaptureHeight());
        if (targetHeight <= properties.getViewportHeight()) {
            return;
        }
        try {
            driver.manage().window().setSize(new Dimension(properties.getViewportWidth(), targetHeight));
            sleepQuietly(properties.getSettleMillis());
        } catch (Exception e) {
            log.warn("扩展视口以截取整页失败，改为只截首屏：{}", brief(e));
        }
    }

    private int measureContentHeight(WebDriver driver) {
        try {
            Object value = ((JavascriptExecutor) driver).executeScript(CONTENT_HEIGHT_SCRIPT);
            if (value instanceof Number number) {
                return number.intValue();
            }
        } catch (WebDriverException e) {
            log.warn("获取页面高度失败，改为只截首屏：{}", brief(e));
        }
        return properties.getViewportHeight();
    }

    private long resourceCount(WebDriver driver) {
        try {
            Object value = ((JavascriptExecutor) driver).executeScript(RESOURCE_COUNT_SCRIPT);
            return value instanceof Number number ? number.longValue() : 0L;
        } catch (WebDriverException e) {
            return 0L;
        }
    }

    private void scrollToTop(WebDriver driver) {
        try {
            ((JavascriptExecutor) driver).executeScript(SCROLL_TO_TOP_SCRIPT);
        } catch (WebDriverException e) {
            log.warn("滚动到页面顶部失败：{}", brief(e));
        }
    }

    /**
     * 按浏览器异常信息分类失败原因。分类会作为指标标签，因此不能直接使用原始文本
     */
    private FailureCategory classify(WebDriverException e) {
        String message = StrUtil.nullToEmpty(e.getMessage());
        if (isSessionDead(e)) {
            return FailureCategory.DRIVER;
        }
        if (message.contains("ERR_NAME_NOT_RESOLVED")
                || message.contains("ERR_CONNECTION_REFUSED")
                || message.contains("ERR_CONNECTION_CLOSED")
                || message.contains("ERR_CONNECTION_RESET")
                || message.contains("ERR_ADDRESS_UNREACHABLE")
                || message.contains("ERR_CERT")) {
            return FailureCategory.UNREACHABLE;
        }
        if (message.contains("ERR_CONNECTION_TIMED_OUT") || message.contains("ERR_TIMED_OUT")) {
            return FailureCategory.TIMEOUT;
        }
        return FailureCategory.DRIVER;
    }

    /**
     * 会话是否已经不可用。这类实例必须销毁，否则会一直失败下去
     */
    private boolean isSessionDead(WebDriverException e) {
        String message = StrUtil.nullToEmpty(e.getMessage());
        return message.contains("invalid session id")
                || message.contains("session deleted")
                || message.contains("chrome not reachable")
                || message.contains("disconnected")
                || message.contains("no such session");
    }

    private void quitQuietly(WebDriver driver) {
        try {
            driver.quit();
        } catch (Exception e) {
            log.debug("释放浏览器实例时异常，已忽略：{}", brief(e));
        }
    }

    /**
     * 休眠，被中断时返回 false 并恢复中断标记
     */
    private boolean sleepQuietly(long millis) {
        if (millis <= 0) {
            return true;
        }
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
        return brief(cause);
    }

    private String brief(Throwable e) {
        String message = StrUtil.nullToEmpty(e.getMessage());
        int newline = message.indexOf('\n');
        if (newline > 0) {
            message = message.substring(0, newline);
        }
        return message.length() > 300 ? message.substring(0, 300) + "..." : message;
    }
}
