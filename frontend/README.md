# AI Creator 前端

> AI Creator 的 Vue 3 前端：首页创建入口、登录注册、应用对话工作台（SSE 流式渲染 + iframe 预览 + 可视化编辑）与管理后台。

本目录是 monorepo `ai-creator` 的 `frontend/` 子目录，对应后端在 [`../backend`](../backend)；跨端协议与开发约定见同目录 [`AGENTS.md`](./AGENTS.md)。

## 项目介绍

前端把后端的 AI 代码生成能力呈现为可交互的产品界面：用户在首页提交需求后跳转到应用对话页，页面以 SSE 流式展示代码生成过程，完成后挂载 iframe 预览生成结果。用户可以在同一个应用上继续对话修改、在预览中点选元素做可视化编辑，也可以部署、下载或删除应用。

三种生成类型 `html`、`multi_file`、`vue_project` 共用同一套流式协议。其中 `vue_project` 产出的是 Vite 源码工程，需由服务端构建，前端要轮询构建状态、成功后才挂载预览。

## 功能模块

| 模块 | 说明 |
| --- | --- |
| 首页 | 以需求输入为主任务，登录用户可切换「我的作品 / 案例广场」，精选案例支持一键带入创建 |
| 登录注册 | 用户注册、登录、退出，会话由后端 Session 维护 |
| 应用对话工作台 | 多轮对话、SSE 流式渲染、代码高亮、iframe 预览、对话/预览/分屏三种模式、可视化编辑、部署与下载 |
| 应用编辑 | 所有者编辑应用名称，管理员可额外编辑封面与优先级 |
| 管理后台 | 用户、应用、对话记录三个管理子页 |

## 技术栈

| 类型 | 技术 |
| --- | --- |
| 框架 | Vue 3.5（Composition API + `<script setup>`）、TypeScript 5.9、Vite 7 |
| 状态与路由 | Pinia、Vue Router 4（路由懒加载与权限守卫） |
| UI | Ant Design Vue 4 |
| 请求 | Axios，`withCredentials` 携带 Session Cookie |
| 内容渲染 | markdown-it、highlight.js（仅注册所需语言） |
| 接口客户端 | `@umijs/openapi` 从后端 OpenAPI 生成，`int64` 统一映射为 `string` |

## 项目结构

```text
src/
├── main.ts        # 应用入口：注册 Pinia、Router、Ant Design Vue 与路由权限逻辑
├── pages/         # 首页、登录注册、应用对话与编辑、admin 管理后台
├── components/    # 全局头尾、应用卡片、详情弹窗、粒子背景等
├── layouts/       # BasicLayout（全局头尾）、AdminLayout（管理后台侧栏）
├── stores/        # Pinia 状态，如登录用户
├── api/           # OpenAPI 生成产物，不要手工维护
├── router/        # 路由与权限 meta
├── utils/         # 可视化编辑注入脚本等
├── assets/        # theme.css 主题变量与静态资源
├── request.ts     # Axios 实例、未登录跳转与 ZIP 下载
└── access.ts      # 登录态获取与 /admin 路由保护
```

## 快速开始

### 环境要求

- Node `^20.19.0 || >=22.12.0`，包管理使用 npm
- 后端服务已启动（默认 `http://localhost:8200`，见 [`../backend/README.md`](../backend/README.md)）

### 安装与开发

```bash
npm ci
npm run dev        # Vite 开发服务器，/api 代理到 http://localhost:8200
```

### 构建与检查

```bash
npm run build      # vue-tsc 类型检查 + Vite 生产构建
npm run type-check # 仅类型检查
npm run pure-build # 仅打包，跳过类型检查
```

### 生成 API 客户端

后端启动后运行，从 `http://localhost:8200/api/v3/api-docs` 生成 `src/api/`：

```bash
npm run openapi2ts
```

### 其他脚本

- `npm run lint`（ESLint，**自带 `--fix` 会直接改写文件**，只想诊断时不要直接运行）
- `npm run format`（Prettier）、`npm run preview`（预览构建产物）

当前没有单元测试或 E2E 测试框架。

## 环境变量

前端只使用公开的 `VITE_*` 变量，它们会打包进浏览器产物，**不得放入任何密钥**：

| 变量 | 默认值 | 用途 |
| --- | --- | --- |
| `VITE_API_BASE_URL` | `http://localhost:8200/api` | 后端接口地址 |
| `VITE_PREVIEW_BASE_URL` | `http://localhost:8200/api/static` | 预览静态资源地址 |
| `VITE_DEPLOY_BASE_URL` | `http://localhost:88` | 部署访问地址，需与后端 `code.deploy-host` 指向同一入口 |

## 相关文档

- [`AGENTS.md`](./AGENTS.md)：跨端协议、代码地图与修改规则，**改代码前先读**
- [`../backend/AGENTS.md`](../backend/AGENTS.md)：后端约定
- [`../README.md`](../README.md)：monorepo 总览与整体架构
