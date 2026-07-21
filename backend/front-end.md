## 📊 项目技术栈分析
### 🎯 核心技术栈 1. 前端框架
- Vue 3.5.26 - 使用 Composition API（组合式 API）
- TypeScript 5.9.3 - 类型安全的 JavaScript 超集
- Vite 7.3.1 - 下一代前端构建工具 2. 状态管理
- Pinia 3.0.4 - Vue 3 官方推荐的状态管理库 3. 路由管理
- Vue Router 4.6.4 - Vue.js 官方路由管理器 4. UI 组件库
- Ant Design Vue 4.2.6 - 基于 Ant Design 的 Vue 3 组件库
  - 使用 ant-design-vue/dist/reset.css 重置样式
  - 全局注册使用 app.use(Antd) 5. HTTP 请求
- Axios 1.13.2 - 基于 Promise 的 HTTP 客户端
  - 配置了请求拦截器和响应拦截器
  - 支持文件下载功能
  - 配置了跨域请求（withCredentials: true） 6. 代码高亮
- Highlight.js 11.11.1 - JavaScript 语法高亮库
  - 使用 atom-one-dark 主题
  - 用于 Markdown 渲染中的代码块高亮 7. Markdown 渲染
- Markdown-it 14.1.1 - Markdown 解析器
  - 用于将 Markdown 格式转换为 HTML 8. 构建工具和开发工具
  构建工具：

- @vitejs/plugin-vue 6.0.3 - Vue 3 的 Vite 插件
- vite-plugin-vue-devtools 8.0.5 - Vue DevTools 支持
代码质量工具：

- ESLint 9.39.2 - JavaScript/TypeScript 代码检查工具
  - eslint-plugin-vue 10.7.0 - Vue 专用的 ESLint 插件
  - @vue/eslint-config-typescript 14.6.0 - TypeScript 配置
  - eslint-config-prettier 10.1.8 - 与 Prettier 兼容
  代码格式化：

- Prettier 3.8.1 - 代码格式化工具
类型检查：

- vue-tsc 3.2.2 - Vue 的 TypeScript 类型检查工具
OpenAPI 工具：

- @umijs/openapi 1.14.1 - 从 OpenAPI 生成 TypeScript 类型和 API 客户端 9. 类型定义
- @types/node 24.10.9 - Node.js 类型定义
- @types/markdown-it 14.1.2 - Markdown-it 类型定义
- tslib 2.8.1 - TypeScript 辅助库 10. 其他工具库
- Day.js - 轻量级日期处理库（用于日期格式化）
### 🏗️ 项目架构特点
1. 现代化技术栈 - 使用最新的 Vue 3 + Vite + TypeScript 组合
2. 类型安全 - 全面使用 TypeScript，提供完整的类型提示
3. 组件化开发 - 基于 Ant Design Vue 的组件化开发模式
4. 状态管理 - 使用 Pinia 进行全局状态管理（如登录用户信息）
5. API 集成 - 通过 Axios 与后端 API 通信，支持拦截器和错误处理
6. 代码规范 - 配置了 ESLint 和 Prettier 保证代码质量
7. 开发体验 - 支持 Vue DevTools 和热更新
### 🎨 主要功能模块
- 用户系统 - 登录、注册、用户信息管理
- 应用管理 - 应用创建、编辑、部署、删除
- 对话系统 - AI 对话、历史记录、消息渲染
- 可视化编辑 - 基于 iframe 的可视化编辑功能
- 代码生成 - 支持 HTML、Multi-file、Vue 等多种代码生成类型
- 管理后台 - 用户管理、应用管理、对话管理
### 📦 项目配置
- 别名配置 - @ 指向 src 目录
- 代理配置 - /api 代理到 http://localhost:8200
- 环境配置 - 支持开发环境和生产环境的环境变量
- Node 版本 - 要求 Node.js ^20.19.0 或 >=22.12.0