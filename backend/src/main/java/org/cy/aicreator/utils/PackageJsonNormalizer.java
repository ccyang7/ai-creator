package org.cy.aicreator.utils;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * package.json 规范化工具
 * <p>
 * 输入完全来自模型，属于不可信内容，因此不直接使用模型产出的 package.json，
 * 而是校验后重建：依赖必须在白名单内并锁定到已知可构建的版本，scripts 只保留构建命令。
 * 这既避免模型写出无法安装的依赖组合，也避免任意 npm 包在安装期执行脚本
 * （构建不能使用 --ignore-scripts，esbuild 依赖 postinstall 落地二进制）。
 * </p>
 *
 * @author cy
 * @since 2026-10-15
 */
public final class PackageJsonNormalizer {

    private static final String VUE = "vue";
    private static final String VUE_ROUTER = "vue-router";
    private static final String VITE = "vite";
    private static final String PLUGIN_VUE = "@vitejs/plugin-vue";

    /**
     * 构建命令：强制带 --base=./，命令行参数优先级高于 vite.config.js，
     * 保证产物按相对路径引用资源，能挂在 /api/static/vue_project_{appId}/ 这类子路径下
     */
    public static final String BUILD_SCRIPT = "vite build --base=./";

    /**
     * 允许出现的依赖 → 锁定版本。与 codegen-vue-project-system-prompt.txt 声明的技术栈一致：
     * 提示词明确禁止状态管理库、类型校验库和代码格式化库
     */
    private static final Map<String, String> RUNTIME_DEPENDENCIES = ordered(
            VUE, "3.4.38",
            VUE_ROUTER, "4.4.5");

    private static final Map<String, String> BUILD_DEPENDENCIES = ordered(
            VITE, "5.4.11",
            PLUGIN_VUE, "5.2.1");

    /**
     * 白名单全集，用于校验模型声明的依赖（不区分它把依赖放在哪个字段）
     */
    private static final Map<String, String> ALLOWED = orderedAll();

    private PackageJsonNormalizer() {
    }

    /**
     * 校验并重建 package.json
     *
     * @param rawContent 模型产出的 package.json 内容
     * @return 规范化后的 package.json 文本
     * @throws IllegalArgumentException 内容不是合法 JSON，或声明了白名单外的依赖
     */
    public static String normalize(String rawContent) {
        if (StrUtil.isBlank(rawContent)) {
            throw new IllegalArgumentException("package.json 内容为空");
        }
        JSONObject raw;
        try {
            raw = JSONUtil.parseObj(rawContent);
        } catch (Exception e) {
            throw new IllegalArgumentException("package.json 不是合法 JSON：" + e.getMessage());
        }
        checkAllowed(raw, "dependencies");
        checkAllowed(raw, "devDependencies");
        // router 按需装入：模型没用路由就不装，避免多余的安装耗时
        boolean withRouter = isDeclared(raw, VUE_ROUTER);

        JSONObject dependencies = new JSONObject(true);
        dependencies.set(VUE, RUNTIME_DEPENDENCIES.get(VUE));
        if (withRouter) {
            dependencies.set(VUE_ROUTER, RUNTIME_DEPENDENCIES.get(VUE_ROUTER));
        }

        JSONObject devDependencies = new JSONObject(true);
        devDependencies.set(PLUGIN_VUE, BUILD_DEPENDENCIES.get(PLUGIN_VUE));
        devDependencies.set(VITE, BUILD_DEPENDENCIES.get(VITE));

        JSONObject scripts = new JSONObject(true);
        scripts.set("build", BUILD_SCRIPT);

        JSONObject normalized = new JSONObject(true);
        normalized.set("name", sanitizeName(raw.getStr("name")));
        normalized.set("private", true);
        normalized.set("version", "0.0.0");
        // 强制 ESM：vite.config.js 用的是 import 语法，缺少该字段时 Node 会按 CJS 解析并报错
        normalized.set("type", "module");
        normalized.set("scripts", scripts);
        normalized.set("dependencies", dependencies);
        normalized.set("devDependencies", devDependencies);
        return JSONUtil.toJsonPrettyStr(normalized);
    }

    /**
     * 校验某个依赖字段里的包名是否都在白名单内
     */
    private static void checkAllowed(JSONObject raw, String field) {
        Object value = raw.get(field);
        if (value == null) {
            return;
        }
        if (!(value instanceof JSONObject declared)) {
            throw new IllegalArgumentException(field + " 必须是对象");
        }
        for (String name : declared.keySet()) {
            if (!ALLOWED.containsKey(name)) {
                throw new IllegalArgumentException(
                        "不支持的依赖：" + name + "，本项目仅允许 " + ALLOWED.keySet());
            }
        }
    }

    private static boolean isDeclared(JSONObject raw, String name) {
        return declaredIn(raw, "dependencies", name) || declaredIn(raw, "devDependencies", name);
    }

    private static boolean declaredIn(JSONObject raw, String field, String name) {
        Object value = raw.get(field);
        return value instanceof JSONObject declared && declared.containsKey(name);
    }

    /**
     * npm 包名只能是小写、URL 安全的字符，模型可能给出中文或含空格的名称
     */
    private static String sanitizeName(String name) {
        if (StrUtil.isBlank(name)) {
            return "vue-project";
        }
        String sanitized = name.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\-._]", "-")
                .replaceAll("^[._-]+", "");
        if (sanitized.isEmpty()) {
            return "vue-project";
        }
        return sanitized.length() > 64 ? sanitized.substring(0, 64) : sanitized;
    }

    private static Map<String, String> ordered(String k1, String v1, String k2, String v2) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(k1, v1);
        map.put(k2, v2);
        // 不用 Map.copyOf：它不保证迭代顺序，白名单的报错信息需要稳定
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, String> orderedAll() {
        Map<String, String> map = new LinkedHashMap<>(RUNTIME_DEPENDENCIES);
        map.putAll(BUILD_DEPENDENCIES);
        return Collections.unmodifiableMap(map);
    }
}
