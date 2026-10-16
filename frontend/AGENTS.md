# AI Creator Frontend Repository Memory

本文件是前端仓库的长期记忆。后续代理应在修改前阅读；若本文与源码冲突，以源码和实际构建结果为准，并同步更新本文中的稳定事实。

## 项目与仓库关系

- 本目录是 AI Creator 的 Vue 前端，位于 monorepo `D:\DevelopProject\ai-creator` 的 `frontend/` 子目录。
- 对应后端在同一仓库的 `backend/`（绝对路径 `D:\DevelopProject\ai-creator\backend`），后端也有自己的 `AGENTS.md`。
- 后端负责用户、应用、聊天记录、AI 生成、预览文件、部署、截图和下载；前端负责页面、会话状态、SSE 展示、iframe 预览及可视化元素选择。
- 仓库使用 `master` 分支。不要假定工作区干净，不要覆盖用户未提交的改动。
- `DEBUG_PREVIEW.md`、`SSE_FIX.md`、`ID_TYPE_FIX.md` 是历史调试记录，存在过期内容，不能代替源码。

## 技术基线

- Vue 3.5、TypeScript 5.9、Vite 7，使用 Composition API 和 `<script setup>`；页面路由均使用动态导入。
- UI：Ant Design Vue 4；状态：Pinia；路由：Vue Router 4；HTTP：Axios。
- Markdown：markdown-it；代码高亮：highlight.js。
- API 客户端由 `@umijs/openapi` 从后端 OpenAPI 生成。
- Node 版本要求：`^20.19.0 || >=22.12.0`。依赖锁文件为 `package-lock.json`，默认使用 npm。

## 常用命令

- 安装：`npm ci`（锁文件变动后开发可用 `npm install`）。
- 开发：`npm run dev`；Vite 将 `/api` 代理到 `http://localhost:8200`。
- 完整构建：`npm run build`，并行执行 `vue-tsc --build` 与 Vite production build。
- 仅类型检查：`npm run type-check`。
- 仅打包：`npm run pure-build`。
- 生成 API：确保后端已启动，再运行 `npm run openapi2ts`；schema 地址是 `http://localhost:8200/api/v3/api-docs`。
- `npm run lint` 带 `--fix`，会修改文件；只做诊断时不要直接运行该脚本。
- 当前没有单元测试或 E2E 测试框架。

## 代码地图

- `src/main.ts`：注册 Pinia、Router、Ant Design Vue 和全局路由权限逻辑。
- `src/router/index.ts`：首页、登录/注册、嵌套管理后台、应用对话页和应用编辑页；路由 meta 还控制全宽内容与 Footer 显示。
- `src/layouts/BasicLayout.vue`：全局 Header/Content/Footer；首页、管理后台和对话工作台使用 full-bleed 内容，管理后台与对话工作台隐藏 Footer；移动端媒体查询不得覆盖 full-bleed 的零内边距。
- `src/layouts/AdminLayout.vue`：管理员侧栏、页面工具栏和三个管理子路由的统一容器。
- `src/access.ts`：首次获取登录用户并保护 `/admin` 路由。
- `src/stores/loginUser.ts`：全局登录用户状态，后端 Session 是真实认证来源。
- `src/request.ts`：Axios 实例、Cookie 会话、40100 跳转和 ZIP 下载。
- `src/api/`：OpenAPI 生成代码；`typings.d.ts` 是全局 `API` namespace。
- `src/pages/HomePage.vue`：以 prompt 创建为首屏主任务；登录用户在“我的作品 / 案例广场”之间切换单一内容区，未登录用户直接查看案例广场。
- `src/components/ParticleField.vue`：首页 Hero 专用的轻量 Canvas 粒子层；无第三方依赖，不参与交互。
- `src/pages/app/AppChatPage.vue`：聊天历史、SSE、预览、部署、下载、删除、工作区模式切换、分屏拖拽及可视化编辑主流程。
- `src/pages/app/AppEditPage.vue`：所有者编辑名称，管理员还能编辑封面/优先级。
- `src/pages/admin/`：用户、应用、聊天记录管理。
- `src/utils/visualEditor.ts`：向同源 iframe 注入选择脚本，以 `postMessage` 传递元素上下文。
- `src/components/`：全局头尾、应用卡片、详情弹窗、代码类型标签、用户信息；`AppCard` 的精选案例支持“基于此创建”，无封面时按生成类型展示稳定的默认封面。

## 后端协议

- Axios base URL 来自 `VITE_API_BASE_URL`，默认 `http://localhost:8200/api`；必须设置 `withCredentials: true` 以携带 Session Cookie。
- 预览 base URL 来自 `VITE_PREVIEW_BASE_URL`，默认 `http://localhost:8200/api/static`。
- 部署访问 base URL 来自 `VITE_DEPLOY_BASE_URL`，默认 `http://localhost:88`；后端 `AppServiceImpl` 的 `code.deploy-host` 未配置时回退 `http://localhost`，本地/生产配置必须让两者指向同一静态部署入口。
- 普通业务响应形如 `{ code, data, message }`，成功码是 `0`，未登录业务码是 `40100`。
- 生成接口是 `GET /app/chat/generate/code?appId=...&message=...`，应用页使用原生 `EventSource(..., { withCredentials: true })`；`src/api/appController.ts` 中同名 Axios 函数是 OpenAPI 生成产物，不能替代命名 `done` 事件监听。
- 普通 SSE 帧的 `data` 是 JSON 字符串 `{"d":"片段"}`；后端最后发送命名事件 `done`。修改任一端时必须同时回归普通帧、完成事件、异常中断和客户端关闭。
- 三种生成类型的 SSE 帧**都是纯文本分片**，没有按类型区分的帧格式。Vue 工程内部虽然传 JSON 消息帧，后端 `JsonMessageStreamHandler` 已经把它们还原成 `[工具调用] 写入文件 xxx` + 代码块再下发，所以前端**不要**再解析 `d` 里的 JSON。
- 构建状态接口 `GET /app/build/status?appId=...` 返回 `{ status, message }`，`status` 为 `none|building|success|failed`，无需登录。`appId` 按 `string` 传（int64 契约）。
- `vue_project` 生成完成后 `dist/` 需要服务端异步构建，**收到 `done` 不等于可以预览**：必须轮询构建状态，`building` 显示进度占位、`success` 才挂 iframe、`failed` 展示后端返回的原因。页面挂载时同样要用该接口恢复上次已构建成功的预览。
- 预览目录 URL 为 `${previewBase}/${codeGenType}_${appId}/`；`codeGenType` 当前只允许 `html`、`multi_file`、`vue_project`。`vue_project` 由后端优先从 `dist/` 解析，未构建时回退源码目录（源码预览下 `.vue` 无法执行，属预期）。
- Java 后端 ID 是 64 位 Long/Snowflake。前端所有 ID 必须按 `string` 处理，禁止 `Number(id)`/`parseInt(id)`，否则可能精度丢失。

## 主要用户流程

1. 首页提交 `initPrompt` 调用 `/app/add`；后端自动选择生成类型和名称，前端只跳转 `/app/chat/:id`，应用页从后端 `app.initPrompt` 读取首轮 prompt。旧的 `?prompt=` 仅作为兼容回退，不应重新把完整 prompt 放入 URL。
2. 首页应用卡片使用稳定 16:9 封面，直接展示生成类型、部署状态和主要操作；不要恢复仅 hover 才能访问的操作层，否则触屏设备无法可靠使用。
3. 首页粒子只用于 Hero 背景，不扩展到对话工作区、管理后台或全页背景。桌面最多 42 个、移动端最多 18 个，DPR 上限 1.5；标签页隐藏时暂停，`prefers-reduced-motion` 开启时只绘制静态帧。
4. 全平台视觉底色统一为雾白蓝灰，并通过 `src/assets/theme.css` 提供蓝色主色、青色辅助色、边框和页面环境背景变量；首页 Hero 与登录/注册页可使用低对比网格纹理，工作区和管理后台只保留静态环境渐变，不扩散粒子动画。
5. 首页模板按“推荐 / 网站 / 产品 / 后台”分类；空作品区和空案例区均提供下一步创建入口，精选案例若存在 `initPrompt` 可直接带入首页输入框。
6. 应用对话页加载应用和游标式聊天历史。所有者的新应用若没有历史，会自动发送初始 prompt。
7. SSE 片段先在内存中缓冲，前台最多每 50ms 合并更新一次纯文本；标签页隐藏时暂停 DOM 刷新，恢复时一次合并。收到 `done` 后才统一执行 Markdown/代码高亮，并构造预览 URL、刷新 iframe。`vue_project` 是例外：`done` 只代表代码写完，还要等服务端构建，因此改为轮询构建状态，成功后才挂 iframe。highlight.js 只注册 HTML/XML、CSS、JS/TS、JSON、Bash、Java、Markdown 和 Vue 所需语言。
8. 对话页提供“对话 / 预览 / 分屏”三种工作区模式，并按应用在 localStorage 保存用户选择。首次生成完成且用户未手动选择时自动进入预览；预览模式保留底部快速修改输入；开启可视化编辑会进入分屏。
9. 编辑模式向预览 iframe 注入脚本，用户选中的 DOM 标签、selector、页面路径和文本会拼入下一轮 prompt。模式切换只用 CSS 隐藏面板，不销毁 iframe；移动端隐藏分屏选项并使用单面板切换。
10. 管理页共享 `/admin` 下的 `AdminLayout`，侧栏负责三个子页面导航；新增管理页时优先作为子路由加入，不要重新把所有入口平铺到全局 Header。
11. 部署返回 URL 并刷新应用信息；下载直接请求后端 ZIP 接口；精选应用通过 `?view=1` 允许查看但不应自动生成。

## 状态与权限约束

- `loginUserStore` 的默认值只表示前端展示状态，不能作为授权依据；所有敏感权限必须由后端再次校验。
- 路由通过 `meta.requiresAuth`、`meta.requiresAdmin`、`meta.guestOnly` 和 `src/access.ts` 统一守卫；管理员页要求管理员，应用编辑页要求登录，登录/注册页会避开已登录用户。
- 应用是否可编辑由登录用户 ID/管理员角色决定，但后端所有权检查才是最终边界。
- Header 菜单按角色过滤；新增管理页时同步路由、菜单、守卫和后端 `@AuthCheck`。

## API 生成与 ID 规则

- `src/api/*.ts` 和 `src/api/typings.d.ts` 视为生成产物，接口变化优先修改后端 OpenAPI 契约并重新生成，不要长期手工维护漂移副本。
- `openapi2ts.config.ts` 通过 `hook.customType` 将 OpenAPI `int64` 统一映射为 `string`，当前生成 typings 的 Long/Snowflake ID 也全部为字符串；重新生成后必须保留该规则。
- 后端 Jackson 已将 Java `Long` 序列化为字符串；前端禁止再把 ID 转为 Number。
- 跨端修改 API 时同时检查后端 DTO/VO、OpenAPI schema、生成客户端、全局 typings、页面调用和 BigInt 序列化策略。

## 已知风险与当前基线

- 截至 2026-07-23，`npm run type-check`、`npm run pure-build` 和首页相关 ESLint 均通过；首页已做 1440px 与 500px 截图检查。
- 路由懒加载、Ant Design Vue 按需注册和 highlight.js 按语言注册后，入口 JS 约 979 KB（gzip 311 KB），AppChatPage 约 204 KB（gzip 78 KB）；入口仍有超过 500 KB 的原始体积警告。
- 生产依赖 `npm audit --omit=dev` 为 0 漏洞；完整开发依赖审计仍有 `@umijs/openapi -> mockjs` 的 2 个高危报告且上游暂无修复，该依赖不进入生产包，只应用于可信本地 OpenAPI 生成。
- `AppChatPage` 的 Markdown 使用单例解析器，禁止原始 HTML，未知语言代码块会先转义；历史 AI 消息只解析一次，流式消息在 `done` 前不执行 Markdown 和 highlight.js。
- `visualEditor.ts` 的父子消息均使用明确 target origin，父页面校验 `event.source`、`event.origin` 和 ElementInfo 结构，iframe 注入脚本也校验父窗口来源。
- AppChatPage 的活动 EventSource、流刷新定时器、`window.message` 和 `visibilitychange` 监听器均在组件级管理，并在完成、错误或卸载时清理。
- 对话页区分真实所有者和管理员管理权限：生成、部署、下载、可视化编辑只向真实所有者开放；管理员编辑/删除使用既有管理员接口。
- 没有自动化测试。SSE、ID 精度、权限跳转、Markdown XSS、postMessage origin 和 iframe 编辑至少需要单元/E2E 覆盖。
- `.env.production` 当前有未提交修改。环境文件只应包含公开的前端 URL，不得放 API key、密码或任何会被打包进浏览器的秘密。
- 调试文档仍引用旧的 `NoCode/LowCode/ProCode` 类型，与后端当前枚举不一致。

## 修改规则

- 先沿页面 -> API client -> 后端 controller/service 阅读完整链路，再修改跨端行为。
- 保持 ID 从路由参数、API 响应、组件 props/emits 到请求 DTO 全程为字符串。
- SSE 连接应有组件级引用；发起新请求、收到 done、发生错误和组件卸载时都要关闭并清理。
- 渲染 AI/用户可控 HTML 前必须 sanitize；不要依赖 TypeScript 类型或 Markdown parser 作为安全边界。
- iframe 消息必须校验 `event.source === previewFrame.contentWindow` 和允许的 origin；发送端也使用明确 targetOrigin。
- 新增页面时同步路由、导航、权限守卫、加载/空/错误状态及响应式布局。
- 完成改动至少运行 `npm run type-check` 和 `npm run pure-build`；完整验收优先 `npm run build`。不要以当前已有类型错误为理由忽略新增错误。
- 稳定的跨端协议发生变化后，同时更新本文件和后端 `D:\DevelopProject\ai-creator\backend\AGENTS.md`。
