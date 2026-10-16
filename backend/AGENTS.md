# AI Creator Repository Memory

本文件是仓库级长期记忆。后续代理在修改代码前应先阅读本文件；若源码与本文冲突，以源码和可执行验证结果为准，并同步更新本文中的稳定事实。

## 项目定位

- AI Creator 是一个 AI 驱动的前端代码生成平台后端。用户创建应用、以自然语言多轮对话生成代码、预览、部署、截图并下载项目。
- 本仓库是 monorepo：Java 后端在 `backend/`（本文件所在目录），Vue 3 前端在 `frontend/`，两端各有一份 `AGENTS.md`。本目录的 `front-end.md` 只是前端技术栈和功能说明。
- 主分支为 `master`。不要假定工作区干净，也不要覆盖用户未提交的修改。
- `docs/feature-proposals.md` 和 `docs/upgrade-plan.md` 是提案/改进计划，不代表功能已经实现。

## 技术基线

- Java 21、Spring Boot 3.5.9、Maven Wrapper。
- Web：Spring MVC；流式输出使用 Reactor `Flux` + SSE。
- ORM：MyBatis-Flex 1.11.6；数据库：MySQL 8。
- 会话、AI 记忆、限流：Spring Session Redis、LangChain4j RedisChatMemoryStore、Redisson；进程内 AI 服务缓存使用 Caffeine。
- AI：LangChain4j 依赖由 `langchain4j-bom` 1.3.0 和配套的 `langchain4j-community-bom` 1.3.0-beta9 统一管理，使用 OpenAI-compatible 模型端点。模型角色由环境配置决定，不要在代码中固化供应商密钥或模型地址。
- AI 的 HTTP 客户端固定为 `langchain4j-http-client-jdk`：`langchain4j-http-client-spring-restclient` 在 `pom.xml` 中被排除，LangChain4j 的 OpenAI 自动配置 `dev.langchain4j.openai.spring.AutoConfig` 在 `application.yml` 中被排除。原因是 Spring RestClient 经 HttpComponents 读取 chunked 响应（无 Content-Length）时只能得到 1 字节，会让所有 AI 调用抛 `JsonEOFException` 或流式挂起。`config/` 下的四个配置类（含提供 `openAiChatModel` 的 `ChatModelConfig`）已覆盖自动配置的全部能力，恢复自动配置会重新引入该故障。
- 其他基础设施：腾讯云 COS、Selenium/Chrome、Micrometer/Prometheus/Grafana。
- API 文档：SpringDoc + Knife4j。

## 运行约定

- 默认端口 `8200`，context path 为 `/api`。
- 默认激活 `local` profile。公共配置在 `src/main/resources/application.yml`；本地和生产配置文件应保持 Git 忽略。
- 启动前需要 MySQL、Redis 和有效的 AI 模型配置。截图/封面还需要 Chrome 与 COS；部署访问通常还需要 Nginx 或等效静态文件服务。
- 初始化数据库：执行 `sql/create_table.sql`。项目尚未使用 Flyway/Liquibase。
- Windows 构建命令（均在 `backend/` 目录下执行）：`./mvnw.cmd test`；仅编译：`./mvnw.cmd -DskipTests compile`。
- 启动命令：`./mvnw.cmd spring-boot:run`。
- 健康检查：`http://localhost:8200/api/actuator/health`；Knife4j：`http://localhost:8200/api/doc.html`；Prometheus：`http://localhost:8200/api/actuator/prometheus`。
- 运行时文件位于 `backend/tmp/`，该目录不应提交。

## 代码地图

- `controller/`：用户、应用、聊天记录与生成结果静态资源 API。
- `service/impl/`：业务校验、所有权检查、数据库读写、部署、截图和下载编排；`VueProjectBuildServiceImpl` 负责 `vue_project` 的异步构建；`ScreenshotServiceImpl` 只负责截图链路的编排（抓取 → 压缩 → 上传 → 清理），浏览器操作在 `manager/` 下。
- `manager/WebPageCapturer.java`：无头 Chrome 截图引擎，池化实例 + 渲染信号等待 + 失败分类 + 有限重试；截图源配置见 `config/ScreenshotProperties`（`code.screenshot.*`）。
- `core/AiCreatorFacade.java`：统一的 AI 生成入口。
- `core/parser/`、`core/saver/`：HTML 和多文件输出的解析/保存策略（Vue 无策略，文件由工具直接写盘）。
- `core/handler/`：按生成类型整理 SSE 数据并落库 AI 回复。
- `ai/`：LangChain4j AiService、智能路由、模型服务工厂、输入护栏、Vue 文件写入工具和多文件补全器 `MultiFileCodeCompleter`。
- `utils/PackageJsonNormalizer.java`：把模型产出的 package.json 重建为可信版本（依赖白名单 + 版本锁定 + 只保留构建脚本）。
- `config/`：模型、Redis、Session、CORS、COS、JSON 和 MyBatis-Flex 配置；`VueBuildProperties` 是 `code.build.*` 构建参数，`ScreenshotProperties` 是 `code.screenshot.*` 截图参数。
- `litmer/`：拼写如此，包含基于 Redisson 的注解式限流。
- `monitor/`：AI 请求、错误、Token 和响应时长指标，以及截图结果与耗时指标（`ScreenshotMetrics`）。
- `aimonitor/`：把模型调用上报到自建的 AI 监控平台（`POST {base}/api/v1/logs`）。`AiMonitorProperties` 读 `ai-monitor.*`，`AiMonitorListener` 挂到 `config/` 下的模型 Bean 上拼字段，实际的 HTTP 上报在 `AiMonitorReporter` 里异步发出。与 `monitor/` 是两套互不影响的东西：`monitor/` 走 Micrometer 给 Prometheus，`aimonitor/` 走 HTTP 给外部平台。
- `model/entity`：`User`、`App`、`ChatHistory`；DTO 用作请求，VO 用作对外响应。
- `src/main/resources/prompt/`：生成和智能路由提示词；修改提示词属于行为变更，应做针对性验证。

## 核心业务流

1. `AppServiceImpl.addApp` 调用 `SmartRoutingService`，根据初始 prompt 决定 `codeGenType` 并生成应用名，然后保存 `App`。
   路由模型可通过 profile 配置 `thinking-enabled`；配置后会向 OpenAI-compatible 请求添加 `thinking.type=enabled|disabled`，并同时作用于类型判断和应用命名。非 DeepSeek 模型应省略该配置。
2. `GET /api/app/chat/generate/code` 校验登录和限流，返回 SSE。每个普通数据帧包装为 JSON `{"d":"..."}`，结束时发送 `event: done`。
3. `AppServiceImpl.chatToGenerateCode` 校验应用所有权，先保存用户消息，设置监控上下文，再把 `initPrompt` 与本轮消息交给 `AiCreatorFacade`。
4. `AiCodeGeneratorServiceFactory` 以 `{appId}_{codeGenType}` 缓存服务；每个应用使用最多 20 条消息的 Redis 对话窗口，并从 `chat_history` 加载历史。
5. HTML 与 MULTI_FILE 由流式模型输出文本，完成后解析代码块并写文件；VUE_PROJECT 依赖模型调用 `FileWriteTool` 逐文件写入。MULTI_FILE 的提示词要求模型一次输出 html/css/js 三个代码块，但当前模型只会给 html 一个块（且照常写上指向 `style.css`/`script.js` 的引用），因此 `AiCreatorFacade` 在解析后会调 `MultiFileCodeCompleter` 做一次定向补全：只把缺失的文件名和已生成的 HTML 回传给非流式模型，要求它只输出缺的那几个代码块。补全是同步阻塞的，会让 SSE 的 `done` 晚到十几秒。
6. `StreamHandlerExecutor` 收集完整 AI 输出并写入 `chat_history`。三种模式**对外**的 SSE 格式一致（纯文本分片）；Vue 内部虽然传 JSON 帧，但 `JsonMessageStreamHandler` 会在下发前把它们还原成 `[工具调用] 写入文件 xxx` + 代码块的人读文本。
7. VUE_PROJECT 在流结束的 `doFinally` 上触发 `VueProjectBuildService.triggerBuildAsync`：等文件静默 → 删旧 `dist` → 规范化 package.json → `npm install` → `npm run build` → 落 `dist/`，最后清理 `node_modules`。用 `doFinally` 而非完成回调，因为客户端断开时完成回调不执行而模型线程仍在写文件。HTML/多文件无此步骤。
8. 部署把生成目录复制到 `tmp/code_deploy/{deployKey}`，更新数据库，再通过虚拟线程异步截图并上传 COS。Vue 工程复制的是 `{appId}/dist` 且要求构建状态为 `success`。
9. 截图源由 `code.screenshot.source` 决定：默认 `preview` 走本应用 `GET /static/{codeGenType}_{appId}/`，容器内自洽、不依赖 nginx 与公网；`deploy` 才走 `code.deploy-host`。启动浏览器前先用 JDK HttpClient 探活截图源，因为 Chrome 连不上时会渲染一张"无法访问"错误页，它有 DOM 也有高度，任何"页面是否画出内容"的判断都会把它当正常页面存成封面。等待渲染的主判据同样是"已画出内容"而非 `document.readyState`（Vue 挂载前它就已经是 `complete`）。截图失败只记日志和指标，不影响部署结果。

## 生成类型与文件布局

- 枚举持久化值严格为 `html`、`multi_file`、`vue_project`，不要改成枚举名或大写值。
- HTML：`tmp/code_output/html_{appId}/index.html`。
- MULTI_FILE：`tmp/code_output/multi_file_{appId}/index.html|style.css|script.js`。
- VUE_PROJECT：`tmp/code_output/vue_project_{appId}/...`，由 `FileWriteTool` 创建完整工程；构建产物在 `tmp/code_output/vue_project_{appId}/dist/`。
- 部署目录：`tmp/code_deploy/{deployKey}/...`。
- 下载和部署代码根据数据库中的 `codeGenType` 拼接源目录，因此目录命名是跨模块契约。
- Vite 的 `base` 由构建脚本强制为 `--base=./`（CLI 优先级高于 `vite.config.js`），保证预览与部署在子路径下都能取到资源。

## API 与安全约束

- 业务 JSON 响应使用 `BaseResponse`/`ResultUtils`；业务失败使用 `BusinessException`、`ErrorCode`、`ThrowUtils`，不要返回临时 Map 或随意异常。
- 登录状态保存在 HttpSession；用户 Session 键与角色常量在 `UserConstant`。
- 管理接口必须使用 `@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)`。
- 应用生成、修改、删除、部署和下载等私有操作必须校验 `app.userId == loginUser.id`，不能只验证已登录。
- `GET /api/app/build/status?appId=` 与 `GET /app/get` 一致，不要求登录也不校验所有权（精选应用只读预览需要读取它）。**不要**往这里加需要登录的校验，否则未登录预览会挂。
- AI 生成接口当前为用户级限流：60 秒 5 次。改变它会直接改变成本和滥用风险。
- 登录接口使用 Redisson 按客户端 IP 分布式限流：60 秒 5 次，独立 Key 前缀为 `user_login`，超限返回业务码 `42900`。
- MyBatis-Flex 使用逻辑删除语义；删除应用还会清理聊天记录、生成目录和部署目录。调整删除流程时注意数据库事务不能回滚文件系统操作。
- 不要把实体直接作为公共 API 响应；优先复用 DTO/VO 转换方法。
- Java Long/Snowflake ID 发给浏览器时必须按字符串处理。后端 `JsonConfig` 将 Long 序列化为字符串，前端 `openapi2ts.config.ts` 通过 `int64 -> string` 生成 typings；跨端改动必须保持这条契约，禁止在前端转回 `number`。
- SSE 跨端契约是普通帧 `data: {"d":"片段"}` 加最终命名事件 `done`；前端使用带 Cookie 的原生 EventSource。

## 跨仓库接口矩阵

- 前端 `HomePage.vue` 创建应用调用 `POST /api/app/add`，成功后只跳转 `/app/chat/:id`；完整初始化 prompt 保存在后端 `app.initPrompt`，由应用页在“本人且没有历史记录”时自动发送。精选应用通过 `?view=1` 只读查看，不应自动生成。
- 前端 `AppChatPage.vue` 的真实生成连接是原生 `EventSource`，不是 `src/api/appController.ts` 中生成的 Axios 函数：需要监听普通 `message` 帧和命名 `done` 事件，并携带 `AI_CREATOR_SESSION` Cookie。修改 SSE 时两端必须一起验证。
- 前端对 `vue_project` 的 `done` 之后不立即挂 iframe，而是轮询 `GET /api/app/build/status`，按 `building`/`success`/`failed` 渲染；页面挂载时会用同一个接口恢复上次已构建成功的预览。`getBuildStatus` 通过 `npm run openapi2ts` 生成。
- 预览使用 `GET /api/static/{codeGenType}_{appId}/...`，源目录来自 `tmp/code_output`；`vue_project` 优先在 `dist/` 下解析，未构建时回退源码目录。部署访问 URL 分别受后端 `code.deploy-host` 和前端 `VITE_DEPLOY_BASE_URL` 控制。当前后端未配置时回退 `http://localhost`，前端默认 `http://localhost:88`，本地环境必须显式对齐；`AppConstant.CODE_DEPLOY_HOST` 当前未被部署服务使用。部署、预览和下载不要混用 `deployKey` 与 `codeGenType_appId`。
- 业务接口的 OpenAPI 客户端位于前端 `src/api/`，响应统一解包为 `{ code, data, message }`；接口签名变化应先改后端 DTO/VO 和 OpenAPI，再重新生成前端客户端与 typings。

## 配置与秘密

- 任何数据库、Redis、模型 API、COS 凭据都视为秘密。不要在回答、日志、测试夹具、文档或补丁中复述真实值。
- 当前公共配置和部分文档存在硬编码/示例化凭据，这是已知 P0 技术债，详见 `docs/upgrade-plan.md`；后续相关改动应优先改为环境变量或外部配置并轮换凭据。
- `application-local.yml`、`application-prod.yml` 已在 `.gitignore` 中，但工作区可能仍有已暂存文件；不要擅自删除或提交它们。
- AI 请求/响应日志可能包含用户 prompt、生成代码或敏感数据；生产环境应关闭。
- AI 监控平台的接入凭据（`ai-monitor.base-url` 和 `ai-monitor.api-key`）放在 `application-local.yml` / `application-prod.yml` 里，这两个文件被 `.gitignore` 排除。**不要**把它们写进受版本控制的 `application.yml`——那样等于把 Key 提交进 Git 历史，事后删文件也挽不回，只能吊销重发。`application.yml` 里只保留 `${AI_MONITOR_BASE_URL:}` / `${AI_MONITOR_API_KEY:}` 占位作为兜底，两者取不到值时整条上报链路静默跳过。
- `ai-monitor.report-content` 默认开启，会把用户提问和模型回答正文一并上报到平台。这些正文包含用户 prompt 与生成代码，会离开本服务落库到平台；不需要时关掉它，其余字段照常上报。

## 已知风险与未完成项

- 测试覆盖仍然很薄：只有 `@SpringBootTest contextLoads` 和若干隔离单测（`PackageJsonNormalizer`、`FileWriteTool` 路径边界、`VueProjectBuildServiceImpl` 状态合成、限流切面、路由配置、`WebPageCapturer` 的延迟渲染等待与池化并发、`MultiFileCodeCompleter` 的补全决策、`AiMonitorListener` 的上报字段契约、`thinking.type` 下发）。多文件是唯一有端到端用例的生成类型（`MultiFileGenerationEndToEndTest`，会真实调用模型），HTML 与 Vue 生成链路、部署、静态资源控制器仍无自动化覆盖，改动这些必须手工端到端验证。注意 `WebPageCapturerTest` 需要本机有 Chrome，且当前 `src/test/` 整体被 `.gitignore` 排除，这些测试不会进版本库，换机器即丢失。
- `MonitorContext` 目前在线程池回调里拿不到：`MonitorContextHolder` 用纯 `ThreadLocal`，而 LangChain4j 的 chat model listener 在 `onPool-worker-*` 线程执行，于是每次调用都会抛 `NullPointerException: ... "context" is null` 并被 LangChain4j 忽略。后果是 AI 请求/Token/耗时指标实际上从未被记录（日志里可见大量该 NPE）。需要改成可跨线程传递的上下文（如 `TransmittableThreadLocal` 或在订阅时显式传递）才能让 `monitor/` 生效。
- `VueProjectBuildService` 的状态是**进程内**的：失败原因在服务重启后丢失（降级为 `none`），只在 `dist/index.html` 存在时自愈为 `success`。构建结果未落库，多实例部署时各实例状态不一致——当前是单实例假设。
- 构建会执行 `npm install`，即**在应用服务器上运行模型生成的工程**。已做的防线是依赖白名单、环境变量白名单（不透传 JVM 里的数据库/模型凭据）和路径边界；但 `postinstall` 脚本本身仍会执行（esbuild 必需），没有容器级隔离。
- VUE_PROJECT 的 `index.html` 会在浏览器请求 `/favicon.ico` 时 404（context path 外），属正常现象，不影响页面。
- 模型角色由 `application-local.yml` / `application-prod.yml` 决定，当前所有角色指向同一个推理型模型。推理型模型默认开启思考，思考内容走 `reasoning_content` **不计入正文**，而 `max-tokens` 是"思考 + 正文"的总预算：预算被思考吃光时接口照样返回 HTTP 200 和 `finish_reason: length`，只是 `content` 全程为空，调用方拿到的是一条没有任何分片的正常流，最后只能看到 `代码保存失败：HTML代码内容不能为空`。因此该模型下 `max-tokens` 必须给足（当前各角色 `65536`），或者显式关掉思考（`thinking-enabled: false`，实测能省约 60% 的 completion tokens）。`ChatModelConfig`、`StreamingChatModelConfig`、`ReasoningStreamingChatModelConfig`、`RoutingAiModelConfig` 都支持把 `thinking-enabled` 作为 `thinking.type` 下发给 OpenAI-compatible 接口，不配置则不发该参数、由模型自行决定。排查"流式无输出"时，先确认响应里有没有 `reasoning_content`。
- 流式请求的超时门槛比看上去松：LangChain4j 把 `readTimeout` 落到 JDK 的 `HttpRequest.timeout()`，而后者对 `ofInputStream` 语义只覆盖到"响应头到达"为止。实测把 `timeout` 设为 3 秒、让 SSE 中途静默 8 秒，调用仍完整收完（耗时 8.3 秒）。所以长思考、长生成不会被超时掐断，不必为它调大超时——反而不要调，`OpenAiStreamingChatModel` 的 connect 与 read 共用同一个 `timeout` 值，调大会让连不上的地址挂更久。`StreamingChatModelConfig` / `ReasoningStreamingChatModelConfig` 因此没有 `readTimeout` 字段，只保留 LangChain4j 默认的 connect 15 秒 / read 60 秒。
- 生成结果为空时对外是**静默失败**：`AiCreatorFacade.parserAndSaveCodeStream` 在 `doOnComplete` 里解析并保存，异常只写日志；SSE 侧随后仍照常发 `done`。于是"一个文件都没写出来"在用户看来仍是一次成功的生成，只有翻日志才能发现。改动生成链路时应考虑把保存失败传播给前端。
- 多文件的残缺产物同样静默：`MultiFileCodeFileSaverTemplate.validateInput` 只要求 HTML 非空（注释明写"CSS 和 JS 可以为空"），`CodeFileSaverTemplate.writeFile` 对空内容直接跳过不写。所以模型少给代码块时，`tmp/code_output/multi_file_{appId}/` 里只会有一个引用了不存在资源的 `index.html`——页面样式与交互全丢，但没有任何报错。`MultiFileCodeCompleter` 是对当前模型的专项补丁：**换模型后必须重新验证**模型是否仍只输出 HTML 块，如果新模型本身遵守三块格式，这个补全会自己跳过（不产生额外调用），可以保留。
- 定向补全本身依赖一个经验事实：同一个模型在"只补 CSS/JS 的窄任务"下完全遵守代码块格式，而在"生成整个三文件站点"的宽任务下只会输出 HTML。改多文件提示词、换模型、或调整补全提示词时，都要重新验证这条前提，因为它不是协议保证，只是实测行为。
- 上报给 AI 监控平台的记录里 `model` 必须取**请求里的配置名**（`chatRequest().modelName()`），不能取响应回显名（`chatResponse().modelName()`）：实测服务端回显的是另一个别名，而平台的成本按价格表现有模型名匹配，用回显名会让成本整列显示为空且不报任何错。
- AI 监控平台按 API Key 隔离数据，且一个账号最多 3 个活跃项目，所以整个后端只能作为一个平台项目接入（一把 Key）。平台的上报体没有自定义字段，`appId` / `userId` 这条维度在平台上表达不出来，只能在本机日志和 `monitor/` 的指标里看。
- AI 监控上报全程在旁路：`AiMonitorReporter` 用 `sendAsync` 发、超时 2 秒连接 / 5 秒请求、`whenComplete` 里吞掉所有异常只写日志。模型 HTTP 客户端的回调线程不会被它占住，平台挂掉也不影响生成。异步发送意味着"测试里发完请求立刻断言"必然失败，等待上报落地要轮询。
- AI 监控上报的 HTTP 客户端**必须钉死 `HttpClient.Version.HTTP_1_1`**：JDK 客户端在明文连接上默认尝试 HTTP/2（h2c 升级），平台的 uvicorn/h11 解析不了这种请求，一律回 `400 Invalid HTTP request received.`。这个坑用 JDK 自带的 `HttpServer` 搭桩测不出来——它容忍 h2c 升级，桩上一切正常、真平台全 400。任何新增的对该平台的调用都要注意这点。
- 本项目配置的模型名（`application-local.yml` 里的 `deepseek-flash`）**不在平台内置价格表里**（表里是 `deepseek-chat` / `deepseek-reasoner` / `deepseek-v4-flash`）。上报本身不受影响、记录照常入库，但成本列会显示「—」，且平台模型管理页会出现橙色提示。要么在平台的模型管理里补一条 `deepseek-flash` 的价格，要么把配置里的 `model-name` 改成价格表里已有的名字。
- LangChain4j 对监听器是逐个 try/catch（`ChatModelListenerUtils`），一个监听器抛异常不影响其他监听器，所以 `MonitorListener` 那个每次都抛的 NPE 不会挡住 `AiMonitorListener` 的上报。监听器之间靠 `attributes()`（同一次请求的同一个 map）传递数据，不依赖 ThreadLocal，因此不受回调线程切换影响——这也是 `aimonitor/` 能正常工作而 `monitor/` 不行的原因。
- 用户密码当前使用静态盐 MD5，安全强度不足；升级目标是 BCrypt/Argon2，并需要兼容存量密码迁移。
- CORS、验证码增强、硬编码凭据、缺少迁移工具均在 `docs/upgrade-plan.md` 中列为技术债。
- LangChain4j 使用 BOM 维护稳定版核心模块与配套 beta 扩展的兼容矩阵；后续升级仍需回归流式响应、Redis memory、tool call 和监控 listener。
- 测试启动会报告两份 `org.json.JSONObject` 实现（`android-json` 与 `org.json`）；应通过依赖排除统一实现，避免运行时行为不确定。
- 文档存在少量过期信息，例如部署指南中的生成目录大小写；路径和行为以源码为准。

## 修改与验证规则

- 先读相关 controller -> service -> core/ai -> persistence 的完整调用链，再动代码。
- 保持已有分层和枚举驱动策略；新增生成类型时必须同时检查路由 prompt、枚举、AI service、parser/saver 或 tool、stream handler、目录拼接、部署、下载、前端协议和测试。
- SSE 改动必须验证数据帧包装、错误传播、完成事件、客户端断开以及聊天记录只保存一次。
- 改动 `vue_project` 时必须同时想到构建：源码改了但没重新构建，预览和部署拿到的还是旧 `dist`；因此构建会在开始时删掉上一轮产物，宁可失败也不展示过期页面。改依赖白名单（`PackageJsonNormalizer`）等于改构建成功率，要做针对性验证。
- API、ID、认证 Cookie、SSE、预览路径或部署 URL 变更时，同步检查并验证 `D:\DevelopProject\ai-creator\frontend`；稳定协议变化后更新两端各自的 `AGENTS.md`。
- 文件操作必须使用规范化后的 `Path`，验证目标仍位于预期应用根目录内；不得信任 appId、deployKey、文件名或 AI tool 参数。
- 数据库结构变化要同步 `sql/create_table.sql`、实体/DTO/VO、查询、索引说明和 `docs/database-design.md`。在引入迁移工具前，明确给出现有库的升级 SQL。
- 优先运行针对性测试，再运行 `./mvnw.cmd test`。若因 MySQL、Redis、模型或 COS 不可用而无法验证，应明确报告，不能声称测试通过。
- 改 `aimonitor/` 时必须验证"真的发出去了"，不能只看编译通过：`MonitorListener` 的教训是每个回调都抛异常、指标一条都没记下来，却没有任何地方报错。端到端做法是把 `ai-monitor.base-url` 指向一个本地桩服务跑一次真实生成，`AiMonitorEndToEndTest` 就是这个用例。
- 文档入口为 `docs/README.md`；架构/API/配置/部署细节分别见同目录对应文档。稳定行为变化后同步相关文档和本文件。
