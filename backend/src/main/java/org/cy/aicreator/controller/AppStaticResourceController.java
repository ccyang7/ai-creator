package org.cy.aicreator.controller;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.cy.aicreator.constant.AppConstant;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 静态资源访问控制器
 * <p>
 * 访问格式：http://localhost:8200/api/static/{deployKey}[/{fileName}]，
 * 其中 deployKey 实际是 {@code {codeGenType}_{appId}}，指向 tmp/code_output 下的生成目录。
 * 所有路径都会规范化并校验仍位于该应用目录内，越界请求直接拒绝。
 * </p>
 */
@RestController
@RequestMapping("/static")
public class AppStaticResourceController {

    // 应用生成根目录（用于浏览）
    private static final Path PREVIEW_ROOT_DIR = Paths.get(AppConstant.CODE_OUTPUT_ROOT_DIR)
            .toAbsolutePath()
            .normalize();

    /**
     * 构建产物目录名：VUE_PROJECT 生成的是 Vite 源码工程，浏览器跑不起来，
     * 构建出 dist 之后才是可直接访问的产物，因此优先从 dist 提供
     */
    private static final String BUILD_OUTPUT_DIR = "dist";

    /**
     * 提供静态资源访问，支持目录重定向
     * 访问格式：http://localhost:8200/api/static/{deployKey}[/{fileName}]
     */
    @GetMapping("/{deployKey}/**")
    public ResponseEntity<Resource> serveStaticResource(
            @PathVariable String deployKey,
            HttpServletRequest request) {
        try {
            // 获取资源路径
            String resourcePath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            resourcePath = resourcePath.substring(("/static/" + deployKey).length());
            // 如果是目录访问（不带斜杠），重定向到带斜杠的URL
            if (resourcePath.isEmpty()) {
                HttpHeaders headers = new HttpHeaders();
                headers.add("Location", request.getRequestURI() + "/");
                return new ResponseEntity<>(headers, HttpStatus.MOVED_PERMANENTLY);
            }
            // 默认返回 index.html
            if (resourcePath.equals("/")) {
                resourcePath = "/index.html";
            }
            // 应用目录必须在预览根目录内，deployKey 来自 URL，不能直接信任
            Path appRoot = PREVIEW_ROOT_DIR.resolve(deployKey).normalize();
            if (!appRoot.startsWith(PREVIEW_ROOT_DIR)) {
                return ResponseEntity.badRequest().build();
            }
            // 去掉前导斜杠：Path.resolve("/x") 会直接返回绝对路径，绕过后面的边界校验
            String relativePath = StrUtil.removePrefix(resourcePath, "/");
            // dist 优先，其次回退源码目录：Vue 工程构建前也能看到源码中的 index.html
            Path file = resolveExistingFile(appRoot.resolve(BUILD_OUTPUT_DIR), relativePath);
            if (file == null) {
                file = resolveExistingFile(appRoot, relativePath);
            }
            if (file == null) {
                return ResponseEntity.notFound().build();
            }
            // 返回文件资源
            Resource resource = new FileSystemResource(file);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", getContentTypeWithCharset(file.getFileName().toString()));
            // 入口 HTML 不缓存：重新生成后哈希资源名会变，缓存住旧入口会加载不到资源
            if (file.getFileName().toString().endsWith(".html")) {
                headers.add("Cache-Control", "no-store");
            }
            return ResponseEntity.ok().headers(headers).body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 在指定根目录下解析相对路径并校验边界，文件不存在或越界时返回 null
     */
    private Path resolveExistingFile(Path root, String relativePath) {
        Path candidate = root.resolve(relativePath).normalize();
        if (!candidate.startsWith(root) || !Files.isRegularFile(candidate)) {
            return null;
        }
        return candidate;
    }

    /**
     * 根据文件扩展名返回带字符编码的 Content-Type
     */
    private String getContentTypeWithCharset(String fileName) {
        String lowerCaseName = fileName.toLowerCase();
        if (lowerCaseName.endsWith(".html")) return "text/html; charset=UTF-8";
        if (lowerCaseName.endsWith(".css")) return "text/css; charset=UTF-8";
        if (lowerCaseName.endsWith(".js") || lowerCaseName.endsWith(".mjs")) {
            return "application/javascript; charset=UTF-8";
        }
        if (lowerCaseName.endsWith(".json") || lowerCaseName.endsWith(".map")) return "application/json; charset=UTF-8";
        if (lowerCaseName.endsWith(".svg")) return "image/svg+xml";
        if (lowerCaseName.endsWith(".png")) return "image/png";
        if (lowerCaseName.endsWith(".jpg") || lowerCaseName.endsWith(".jpeg")) return "image/jpeg";
        if (lowerCaseName.endsWith(".gif")) return "image/gif";
        if (lowerCaseName.endsWith(".webp")) return "image/webp";
        if (lowerCaseName.endsWith(".ico")) return "image/x-icon";
        if (lowerCaseName.endsWith(".woff")) return "font/woff";
        if (lowerCaseName.endsWith(".woff2")) return "font/woff2";
        if (lowerCaseName.endsWith(".ttf")) return "font/ttf";
        return "application/octet-stream";
    }
}
