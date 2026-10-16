# AI Creator

> 基于 Spring Boot 与 LangChain4j 的 AI 前端代码生成平台：自然语言多轮对话生成代码、SSE 流式输出、在线预览、部署、封面截图与源码下载。

本仓库是 monorepo，前后端同仓管理：

| 目录 | 内容 | 技术栈 |
| --- | --- | --- |
| [`backend/`](./backend) | 用户与应用管理、AI 生成链路、SSE 流式输出、预览/部署/截图/下载、限流与鉴权 | Java 21、Spring Boot 3.5、LangChain4j、MyBatis-Flex、MySQL 8、Redis |
| [`frontend/`](./frontend) | 首页与创建入口、登录注册、应用对话工作台（SSE 渲染 + iframe 预览 + 可视化编辑）、管理后台 | Vue 3.5、TypeScript 5.9、Vite 7、Pinia、Vue Router 4、Ant Design Vue 4 |

两端的长期约定分别写在 [`backend/AGENTS.md`](./backend/AGENTS.md) 与 [`frontend/AGENTS.md`](./frontend/AGENTS.md)，改代码前先读对应那一份。

## 快速开始

### 后端

需要 MySQL 8、Redis，以及可用的 AI 模型配置（本地/生产配置文件被 Git 忽略，放在各自机器上）：

```bash
cd backend
# 初始化数据库：执行 sql/create_table.sql
# 本地配置：src/main/resources/application-local.yml（参考同目录 application.yml）
./mvnw.cmd spring-boot:run        # Windows；Linux/macOS 用 ./mvnw spring-boot:run
```

- 端口 `8200`，context path `/api`；健康检查 `http://localhost:8200/api/actuator/health`
- 仅编译：`./mvnw.cmd -DskipTests compile`；测试：`./mvnw.cmd test`
- 截图/封面功能额外需要 Chrome 与腾讯云 COS

### 前端

Node 版本要求 `^20.19.0 || >=22.12.0`：

```bash
cd frontend
npm ci
npm run dev        # Vite 开发服务器，/api 代理到 http://localhost:8200
npm run build      # vue-tsc 类型检查 + 生产构建
```

- API 客户端由 `@umijs/openapi` 生成：后端启动后运行 `npm run openapi2ts`
- 更多脚本见 `frontend/package.json` 与 `frontend/AGENTS.md`

## 仓库来历

本仓库由原先两个独立仓库合并而来，两段提交历史完整保留（各经 `git filter-repo` 重写前缀，汇合于 merge 提交）：

- 后端：原 `ai-creator` 仓库 → `backend/`
- 前端：原 `ai-creator-frontend` 仓库 → `frontend/`

两个原始仓库在 gitee 上按归档保留，不再更新。
