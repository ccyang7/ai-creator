# AI Creator

> 基于 AI 大语言模型的智能代码生成平台 — 用自然语言描述需求，自动生成完整前端代码。

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.9-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

---

## 项目简介

**AI Creator** 允许用户通过自然语言描述一个网站，系统自动调用多种 LLM（DeepSeek、Qwen）生成完整的前端代码。支持 **两种生成模式**：HTML 单文件、HTML+CSS+JS 多文件。提供从代码生成、流式预览、一键部署、ZIP 下载、自动截图到多轮对话优化的全生命周期管理。

## 核心能力

| 能力 | 说明 |
|------|------|
| 🧠 **智能代码生成** | 基于 LangChain4j 集成 DeepSeek-Chat / DeepSeek-Reasoner / Qwen3.5-Plus 多种 LLM |
| 🎯 **两种生成模式** | HTML 单文件、多文件（HTML+CSS+JS） |
| 🚦 **AI 智能路由** | 轻量 Qwen-Turbo 自动判断最佳生成模式并生成应用名称 |
| 📡 **流式响应** | 基于 SSE（Server-Sent Events）实时推送代码生成进度 |
| 🚀 **一键部署** | 生成代码自动部署到 Nginx，生成唯一 6 位 `deployKey` 访问 URL |
| 📸 **自动截图** | Selenium 无头浏览器截图 + 压缩 + 上传腾讯云 COS 作为封面 |
| 📦 **代码下载** | 支持将项目源码打包为 ZIP 下载 |
| 💬 **多轮对话** | Redis 持久化对话记忆（窗口 20 条），支持迭代优化代码 |
| 🔒 **安全限流** | Redisson 分布式限流（API / 用户 / IP 三级）+ Prompt 注入防护 |
| 👮 **权限控制** | AOP + `@AuthCheck` 注解驱动的角色权限校验 |
| 📊 **全面监控** | Micrometer + Prometheus + Grafana 监控请求量、Token 消耗、响应时间 |
| 📖 **API 文档** | Knife4j / Swagger UI 在线接口文档 |

## 技术栈

| 层级 | 技术 |
|------|------|
| **语言** | Java 21 |
| **框架** | Spring Boot 3.5.9 |
| **构建** | Maven |
| **ORM** | MyBatis-Flex 1.11.6 |
| **数据库** | MySQL 8.x |
| **缓存** | Redis + Caffeine（内存级 AI 服务缓存） |
| **AI 框架** | LangChain4j 1.1.0 |
| **AI 模型** | DeepSeek-Chat / DeepSeek-Reasoner / Qwen3.5-Plus / Qwen-Turbo |
| **流式** | Reactor (Flux) + SSE |
| **会话** | Spring Session + Redis |
| **限流** | Redisson |
| **API 文档** | SpringDoc OpenAPI 3 + Knife4j 4.4.0 |
| **对象存储** | 腾讯云 COS |
| **截图** | Selenium 4.33.0 + ChromeDriver (Headless) |
| **工具库** | Hutool 5.8.38 |
| **前端** | Vue 3.5 + TypeScript + Vite 7 + Ant Design Vue 4 + Pinia |

## 架构概览

```
┌─────────────────────────────────────────────────────────┐
│                    Controller Layer                      │
│   AppController │ UserController │ ChatHistoryController │
│   AppStaticResourceController                           │
│   AOP: AuthInterceptor + RateLimitAspect                 │
├─────────────────────────────────────────────────────────┤
│                     Service Layer                        │
│   AppService │ UserService │ ChatHistoryService           │
│   ScreenshotService │ ProjectDownloadService             │
├─────────────────────────────────────────────────────────┤
│                   Core Layer (Facade)                    │
│   AiCreatorFacade — 统一入口：生成 → 解析 → 保存        │
│   CodeParserExecutor (策略) │ CodeFileSaverExecutor      │
│   StreamHandlerExecutor                                   │
├─────────────────────────────────────────────────────────┤
│                       AI Layer                           │
│   AiCodeGeneratorServiceFactory (Caffeine 缓存)          │
│   SmartRoutingServiceFactory                              │
│   LangChain4j AiServices + @SystemMessage                      │
│   PromptSafetyInputGuardrail                                     │
├─────────────────────────────────────────────────────────┤
│                    Infrastructure                        │
│   MySQL │ Redis │ Redisson │ Caffeine │ COS │ Selenium   │
│   Prometheus │ Grafana                                    │
└─────────────────────────────────────────────────────────┘
```

### AI 模型分工

| 模型 | 用途 |
|------|------|
| **Qwen-Turbo** | 智能路由：判断生成模式 + 生成应用名称 |
| **DeepSeek-Chat** | HTML 单文件 / 多文件生成（非流式结构输出） |
| **Qwen3.5-Plus** | HTML / 多文件生成（流式 SSE 推送） |

## 快速开始

### 环境要求

- **JDK 21+**
- **Maven 3.8+**
- **MySQL 8.0+**
- **Redis 6.0+**
- **Chrome/Chromium**（用于截图功能，可选）

### 1. 初始化数据库

```bash
mysql -u root -p < sql/create_table.sql
```

### 2. 配置文件

项目的敏感配置（数据库密码、Redis 密码、API Key 等）已从 `application.yml` 中移除，改为 Spring 占位符（如 `${DB_PASSWORD:}`）。实际值存放在 `application-local.yml`（本地开发）和 `application-prod.yml`（生产环境）中，这两个文件已加入 `.gitignore`，需要自行创建。

**`src/main/resources/application-local.yml`** 示例：

```yaml
# 数据库与 Redis 配置（application.yml 中对应占位符的实际值）
spring:
  datasource:
    password: your-db-password
  data:
    redis:
      host: your-redis-host
      password: your-redis-password

# AI 模型配置
langchain4j:
  open-ai:
    chat-model:
      api-key: your-deepseek-api-key
      base-url: https://api.deepseek.com
      model-name: deepseek-chat
      max-tokens: 8192
      read-timeout: 600
    streaming-chat-model:
      api-key: your-qwen-api-key
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      model-name: qwen3.5-plus
      max-tokens: 8192
      read-timeout: 600
      temperature: 0.1
    routing-chat-model:
      api-key: your-qwen-api-key
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      model-name: qwen-turbo
      max-tokens: 150
      temperature: 0.1
    reasoning-streaming-chat-model:
      api-key: your-deepseek-api-key
      base-url: https://api.deepseek.com
      model-name: deepseek-reasoner
      max-tokens: 32768
      temperature: 0.1

# 腾讯云 COS
cos:
  client:
    host: https://your-bucket.cos.ap-guangzhou.myqcloud.com
    secret-id: your-secret-id
    secret-key: your-secret-key
    region: ap-guangzhou
    bucket: your-bucket-name
```

### 3. 启动 Redis

确保 Redis 服务正在运行。

### 4. 启动项目

```bash
# 开发环境
./mvnw spring-boot:run

# 生产环境
./mvnw clean package -DskipTests
java -jar target/ai-creator-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

服务启动后：
- 服务地址：`http://localhost:8200/api`
- API 文档：`http://localhost:8200/api/doc.html`
- 部署的应用：`http://localhost:8200/api/static/{deployKey}/`

## 项目结构

```
ai-creator/
├── sql/                          # 数据库建表脚本
├── docs/                         # 详细文档
│   ├── architecture.md           # 架构设计
│   ├── api-reference.md          # API 接口文档
│   ├── configuration-guide.md    # 配置指南
│   ├── database-design.md        # 数据库设计
│   ├── deployment-guide.md       # 部署指南
│   ├── development-guide.md      # 开发指南
│   ├── monitoring-guide.md       # 监控指南
│   └── ...
├── src/main/java/org/cy/aicreator/
│   ├── AiCreatorApplication.java        # 启动入口
│   ├── ai/                               # AI 层
│   │   ├── AiCodeGeneratorService.java   # AI 服务接口
│   │   ├── AiCodeGeneratorServiceFactory.java
│   │   ├── SmartRoutingService.java      # 智能路由接口
│   │   ├── SmartRoutingServiceFactory.java
│   │   └── guardrail/PromptSafetyInputGuardrail.java
│   ├── controller/                       # REST 控制器
│   │   ├── AppController.java           # 应用 CRUD + 生成 + 部署
│   │   ├── UserController.java          # 用户注册/登录/管理
│   │   ├── ChatHistoryController.java   # 对话历史
│   │   └── AppStaticResourceController.java
│   ├── core/                             # 核心门面层
│   │   ├── AiCreatorFacade.java         # 统一编排入口
│   │   ├── parser/                       # 代码解析器（策略模式）
│   │   ├── saver/                        # 代码保存器（模板方法）
│   │   └── handler/                      # 流式处理器
│   ├── service/                          # 业务服务层
│   ├── aop/                              # AOP 切面
│   │   ├── AuthInterceptor.java         # 权限校验
│   │   └── litmer/aspect/RateLimitAspect.java  # 限流
│   ├── config/                           # 配置类
│   ├── model/                            # 实体 / DTO
│   ├── utils/                            # 工具类
│   │   └── WebScreenshotUtils.java      # 网页截图
│   └── monitor/                          # 监控指标采集
├── src/main/resources/
│   ├── application.yml                   # 主配置
│   ├── application-local.yml             # 本地环境（gitignore）
│   ├── application-prod.yml              # 生产环境（gitignore）
│   └── prompt/                           # AI Prompt 模板
├── front-end.md                          # 前端技术栈说明
├── prometheus.yml                        # Prometheus 配置
└── pom.xml                               # Maven 构建
```

## API 概览

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/user/register` | 用户注册 |
| `POST` | `/api/user/login` | 用户登录 |
| `GET` | `/api/user/logout` | 用户登出 |
| `POST` | `/api/app/generate` | 创建应用并生成代码（SSE 流式） |
| `POST` | `/api/app/chat` | 多轮对话优化（SSE 流式） |
| `POST` | `/api/app/deploy/{appId}` | 部署应用 |
| `GET` | `/api/app/download/{appId}` | 下载应用 ZIP |
| `GET` | `/api/app/page` | 分页查询应用列表 |
| `DELETE` | `/api/app/delete/{appId}` | 删除应用 |
| `GET` | `/api/chat-history/page` | 分页查询对话历史 |
| `GET` | `/api/static/{deployKey}/` | 访问已部署的应用 |

> 完整接口文档请查看 `http://localhost:8200/api/doc.html` 或 [docs/api-reference.md](docs/api-reference.md)

## 设计模式

项目中应用了多种设计模式：

| 模式 | 应用场景 |
|------|----------|
| **Facade（门面）** | `AiCreatorFacade` 统一编排代码生成、解析、保存流程 |
| **Factory（工厂）** | `AiCodeGeneratorServiceFactory` 按 appId 创建独立 AI 服务实例 |
| **Template Method（模板方法）** | `CodeFileSaverTemplate` 定义保存流程，子类实现具体逻辑 |
| **Strategy（策略）** | `CodeParserExecutor` / `CodeFileSaverExecutor` 枚举驱动选择解析器 |
| **Prototype（原型）** | 流式聊天模型 Bean 作用域为 prototype，避免并发冲突 |



## License

个人学习项目
