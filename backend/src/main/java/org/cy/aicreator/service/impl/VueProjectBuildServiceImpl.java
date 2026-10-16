package org.cy.aicreator.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.cy.aicreator.config.VueBuildProperties;
import org.cy.aicreator.constant.AppConstant;
import org.cy.aicreator.model.enums.AppBuildStatusEnum;
import org.cy.aicreator.model.enums.GenerTypeEnum;
import org.cy.aicreator.model.vo.AppBuildStatusVO;
import org.cy.aicreator.service.VueProjectBuildService;
import org.cy.aicreator.utils.PackageJsonNormalizer;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Vue 工程构建服务实现
 * <p>
 * 流程：等待文件写入静默 → 校验并规范化 package.json → npm install → npm run build
 * → 清理 node_modules → 记录状态。
 * </p>
 *
 * @author cy
 * @since 2026-10-15
 */
@Slf4j
@Service
public class VueProjectBuildServiceImpl implements VueProjectBuildService {

    /**
     * 项目根目录名，与 FileWriteTool、部署、下载拼接的目录名保持一致
     */
    private static final String PROJECT_DIR_NAME = GenerTypeEnum.VUE_PROJECT.getValue() + "_%d";

    private static final String PACKAGE_JSON = "package.json";
    private static final String LOCK_FILE = "package-lock.json";
    private static final String DIST_DIR = "dist";
    private static final String NODE_MODULES = "node_modules";

    /**
     * 允许透传给 npm 子进程的环境变量。
     * <p>
     * 子进程的环境变量用白名单重建，绝不整份继承：JVM 里带着数据库口令、Redis 口令和模型
     * API Key，而 npm install 会执行依赖包的 postinstall 脚本，继承等于把这些凭据交给
     * 任意第三方代码。构建不能加 --ignore-scripts（esbuild 要靠 postinstall 落地二进制），
     * 所以白名单是这里唯一的防线。
     * </p>
     */
    private static final Set<String> SAFE_ENV_KEYS = Set.of(
            "PATH", "PATHEXT", "SYSTEMROOT", "WINDIR", "SYSTEMDRIVE", "COMSPEC", "OS",
            "TEMP", "TMP", "TMPDIR",
            "APPDATA", "LOCALAPPDATA", "PROGRAMDATA", "PROGRAMFILES", "PROGRAMFILES(X86)",
            "USERPROFILE", "HOMEDRIVE", "HOMEPATH", "HOME", "USERNAME", "USER", "SHELL",
            "NUMBER_OF_PROCESSORS", "PROCESSOR_ARCHITECTURE", "LANG", "LC_ALL");

    @Resource
    private VueBuildProperties properties;

    /**
     * appId -> 最近一次构建状态。构建是短生命周期状态，不落库：
     * 成功态可以从 dist 目录自愈，重启后最多丢失一次失败原因
     */
    private final Map<Long, AppBuildStatusVO> statusCache = new ConcurrentHashMap<>();

    /**
     * 正在构建中的应用，用于忽略重复触发
     */
    private final Set<Long> buildingAppIds = ConcurrentHashMap.newKeySet();

    /**
     * 当前活跃构建数，用于限流
     */
    private final AtomicInteger activeBuilds = new AtomicInteger();

    @Override
    public void triggerBuildAsync(long appId) {
        if (!properties.isEnabled()) {
            log.info("服务端构建已关闭，跳过构建，appId={}", appId);
            return;
        }
        if (!buildingAppIds.add(appId)) {
            log.info("appId={} 已在构建中，忽略本次触发", appId);
            return;
        }
        int active = activeBuilds.incrementAndGet();
        if (active > properties.getMaxConcurrentBuilds()) {
            activeBuilds.decrementAndGet();
            buildingAppIds.remove(appId);
            log.warn("构建任务已达上限 {}，拒绝 appId={}", properties.getMaxConcurrentBuilds(), appId);
            setStatus(appId, AppBuildStatusEnum.FAILED, "当前构建任务过多，请稍后重新生成");
            return;
        }
        // 先同步置为构建中再起线程，避免刚结束时前端仍读到上一轮的终态
        setStatus(appId, AppBuildStatusEnum.BUILDING, "正在等待生成结果落盘");
        Thread.startVirtualThread(() -> {
            try {
                doBuild(appId);
            } catch (Exception e) {
                log.error("Vue 工程构建异常，appId={}", appId, e);
                setStatus(appId, AppBuildStatusEnum.FAILED, StrUtil.brief(e.getMessage(), 300));
            } finally {
                buildingAppIds.remove(appId);
                activeBuilds.decrementAndGet();
            }
        });
    }

    @Override
    public AppBuildStatusVO getStatus(long appId) {
        AppBuildStatusVO cached = statusCache.get(appId);
        // 构建中 dist 尚未产出，内存状态是唯一来源
        if (cached != null && AppBuildStatusEnum.BUILDING.getValue().equals(cached.getStatus())) {
            return cached;
        }
        // dist 是构建结果的最终事实，优先级高于内存记录：产物在就算成功，
        // 服务重启后成功态也就天然自愈，不需要启动对账
        if (Files.isRegularFile(distIndex(appId))) {
            return AppBuildStatusVO.of(AppBuildStatusEnum.SUCCESS, "构建成功");
        }
        // 产物已经不在（例如 tmp 被清理过），内存里的 success 就失效了，不能再拿它去通过部署校验
        if (cached != null && AppBuildStatusEnum.FAILED.getValue().equals(cached.getStatus())) {
            return cached;
        }
        return AppBuildStatusVO.of(AppBuildStatusEnum.NONE, "尚未构建");
    }

    /**
     * 实际构建流程
     */
    private void doBuild(long appId) throws Exception {
        Path projectRoot = projectRoot(appId);
        try {
            awaitFileQuiescence(appId, projectRoot);

            // 源码已经被这一轮重新生成，上一轮的 dist 不再对应这份源码。
            // 留着它会让 getStatus 继续报 success，用户看到的是过期页面却毫无提示，
            // 部署出去的新版本也会是旧的；宁可构建失败后暂时没有预览。
            FileUtil.del(projectRoot.resolve(DIST_DIR).toFile());

            Path packageJson = projectRoot.resolve(PACKAGE_JSON);
            if (!Files.isRegularFile(packageJson)) {
                setStatus(appId, AppBuildStatusEnum.FAILED, "未找到 package.json，工程可能未生成完整");
                return;
            }
            // 校验并重建 package.json，模型产出的依赖与脚本都不可信
            String normalized = PackageJsonNormalizer.normalize(
                    Files.readString(packageJson, StandardCharsets.UTF_8));
            Files.writeString(packageJson, normalized, StandardCharsets.UTF_8);
            // 模型生成的 lock 文件与重建后的依赖不一致，删掉让 npm 重新解析
            Files.deleteIfExists(projectRoot.resolve(LOCK_FILE));

            setStatus(appId, AppBuildStatusEnum.BUILDING, "正在安装依赖（npm install）");
            runNpm(appId, projectRoot, List.of("install", "--no-audit", "--no-fund"),
                    properties.getInstallTimeoutSeconds());

            setStatus(appId, AppBuildStatusEnum.BUILDING, "正在构建（npm run build）");
            runNpm(appId, projectRoot, List.of("run", "build"), properties.getBuildTimeoutSeconds());

            Path distIndex = projectRoot.resolve(DIST_DIR).resolve("index.html");
            if (!Files.isRegularFile(distIndex)) {
                setStatus(appId, AppBuildStatusEnum.FAILED, "构建结束但未生成 dist/index.html");
                return;
            }
            log.info("Vue 工程构建成功，appId={}, dist={}", appId, distIndex);
            setStatus(appId, AppBuildStatusEnum.SUCCESS, "构建成功");
        } finally {
            // node_modules 体积大且随时可以重新安装，构建完成后不保留
            cleanNodeModules(projectRoot);
        }
    }

    /**
     * 等待工程目录停止变化。
     * <p>
     * 实测在流已结束甚至客户端已断开之后，模型线程仍会继续调用工具写文件，
     * 因此不能紧跟流结束就启动构建，否则会基于残缺的工程编译。
     * </p>
     */
    private void awaitFileQuiescence(long appId, Path projectRoot) throws InterruptedException {
        long start = System.currentTimeMillis();
        long lastChangeAt = start;
        long lastModified = latestModified(projectRoot);
        while (true) {
            Thread.sleep(properties.getPollIntervalMillis());
            long now = System.currentTimeMillis();
            long current = latestModified(projectRoot);
            if (current != lastModified) {
                lastModified = current;
                lastChangeAt = now;
            }
            if (now - lastChangeAt >= properties.getQuiescenceMillis()) {
                log.info("生成结果已静默，开始构建，appId={}", appId);
                return;
            }
            if (now - start >= properties.getQuiescenceTimeoutMillis()) {
                log.warn("等待文件写入静默超时，仍继续构建，appId={}", appId);
                return;
            }
        }
    }

    /**
     * 取目录树中最新的修改时间，忽略 dist 与 node_modules：
     * 上一轮构建的 dist 会是最新的文件，把它算进来会让静默检测立刻通过
     */
    private long latestModified(Path projectRoot) {
        if (!Files.isDirectory(projectRoot)) {
            return 0L;
        }
        try (Stream<Path> stream = Files.walk(projectRoot)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> {
                        Path rel = projectRoot.relativize(path);
                        return !rel.startsWith(DIST_DIR) && !rel.startsWith(NODE_MODULES);
                    })
                    .mapToLong(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toMillis();
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .max()
                    .orElse(0L);
        } catch (IOException e) {
            log.warn("统计工程目录修改时间失败：{}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 执行 npm 命令，失败或超时抛异常
     */
    private void runNpm(long appId, Path workDir, List<String> args, int timeoutSeconds) throws Exception {
        List<String> command = new ArrayList<>();
        if (isWindows()) {
            // Windows 上 npm 是 npm.cmd，ProcessBuilder 不能直接执行批处理，必须经 cmd.exe 转发
            command.add(System.getenv().getOrDefault("COMSPEC", "cmd.exe"));
            command.add("/c");
        }
        command.add(properties.getNpmCommand());
        command.addAll(args);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workDir.toFile());
        builder.redirectErrorStream(true);
        // 输出重定向到临时文件，避免管道写满导致子进程阻塞，也避免把大量内容读进内存
        Path logFile = Files.createTempFile("vue-build-" + appId + "-", ".log");
        builder.redirectOutput(logFile.toFile());
        builder.environment().clear();
        builder.environment().putAll(safeEnvironment());

        String commandText = String.join(" ", command.subList(isWindows() ? 2 : 0, command.size()));
        Process process = null;
        try {
            process = builder.start();
            if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                destroyTree(process);
                throw new IllegalStateException(
                        "执行 " + commandText + " 超时（超过 " + timeoutSeconds + " 秒）");
            }
            if (process.exitValue() != 0) {
                List<String> tail = readTail(logFile, properties.getMaxOutputLines());
                log.warn("构建命令失败，appId={}, command={}, 输出：\n{}", appId, commandText, String.join("\n", tail));
                throw new IllegalStateException("执行 " + commandText + " 失败：" + summarize(tail));
            }
        } finally {
            if (process != null && process.isAlive()) {
                destroyTree(process);
            }
            FileUtil.del(logFile.toFile());
        }
    }

    /**
     * 用白名单重建子进程环境变量
     */
    private Map<String, String> safeEnvironment() {
        Map<String, String> safe = new HashMap<>();
        System.getenv().forEach((key, value) -> {
            if (SAFE_ENV_KEYS.contains(key.toUpperCase(Locale.ROOT))) {
                safe.put(key, value);
            }
        });
        // 日志去色，否则回传给前端的失败原因里都是 ANSI 转义码
        safe.put("NO_COLOR", "1");
        safe.put("npm_config_color", "false");
        safe.put("npm_config_update_notifier", "false");
        return safe;
    }

    /**
     * 结束整棵进程树：Windows 上只杀 cmd.exe 会留下 node 进程继续占用目录
     */
    private void destroyTree(Process process) {
        process.descendants().forEach(ProcessHandle::destroy);
        process.destroy();
        try {
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 读取日志文件末尾若干行，内存占用与输出总量无关
     */
    private List<String> readTail(Path logFile, int maxLines) {
        Deque<String> tail = new ArrayDeque<>();
        try (BufferedReader reader = Files.newBufferedReader(logFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (StrUtil.isBlank(line)) {
                    continue;
                }
                if (tail.size() == maxLines) {
                    tail.pollFirst();
                }
                tail.addLast(line);
            }
        } catch (IOException e) {
            log.warn("读取构建日志失败：{}", e.getMessage());
            return List.of();
        }
        return new ArrayList<>(tail);
    }

    private String summarize(List<String> tail) {
        if (tail.isEmpty()) {
            return "无输出，请查看服务端日志";
        }
        return StrUtil.brief(String.join(" | ", tail), 300);
    }

    private void cleanNodeModules(Path projectRoot) {
        File nodeModules = projectRoot.resolve(NODE_MODULES).toFile();
        if (nodeModules.exists()) {
            FileUtil.del(nodeModules);
        }
    }

    private void setStatus(long appId, AppBuildStatusEnum statusEnum, String message) {
        statusCache.put(appId, AppBuildStatusVO.of(statusEnum, message));
    }

    /**
     * 工程根目录
     */
    public static Path projectRoot(long appId) {
        return Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR, String.format(PROJECT_DIR_NAME, appId))
                .toAbsolutePath()
                .normalize();
    }

    /**
     * 构建产物入口
     */
    public static Path distIndex(long appId) {
        return projectRoot(appId).resolve(DIST_DIR).resolve("index.html");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }
}
