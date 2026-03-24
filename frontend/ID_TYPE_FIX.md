# ID 类型修复说明

## 问题原因

后端返回的 ID 是 string 类型（Long 类型），但前端 TypeScript 类型定义使用的是 number 类型。JavaScript 的 number 类型遵循 IEEE 754 双精度浮点数标准，安全整数范围是 -(2^53 - 1) 到 2^53 - 1。当后端返回的 Long 类型 ID 超过这个范围时，会导致精度丢失。

## 修复内容

### 1. API 类型定义 (src/api/typings.d.ts)

将所有 ID 相关类型从 number 改为 string：

- `App.id` - 应用 ID
- `App.userId` - 用户 ID
- `AppVO.id` - 应用 VO ID
- `AppVO.userId` - 应用 VO 用户 ID
- `User.id` - 用户 ID
- `UserVO.id` - 用户 VO ID
- `LoginUserVO.id` - 登录用户 VO ID
- 所有 DTO 中的 id 字段
- 所有 API 参数中的 id 字段

### 2. 页面组件修复

#### AppChatPage.vue
- `appId` 从 `ref<number>` 改为 `ref<string>`
- 移除 `Number(id)` 转换，直接使用 string
- 修复 SSE 连接中的 appId 使用

#### AppEditPage.vue
- 移除 `Number(id)` 转换，直接使用 string

### 3. 移除不必要的类型断言

由于类型定义已修复，移除了所有 `as any` 类型断言。

## 影响范围

### 修改的文件
1. `src/api/typings.d.ts` - API 类型定义
2. `src/pages/app/AppChatPage.vue` - 应用对话页面
3. `src/pages/app/AppEditPage.vue` - 应用编辑页面
4. `DEBUG_PREVIEW.md` - 调试文档

### 未修改的文件
- `src/api/` 目录下的文件未修改（除了 typings.d.ts）
- 其他页面组件未受影响

## 测试建议

1. **创建应用**
   - 测试创建新应用流程
   - 验证应用 ID 是否正确保存和传递

2. **查看应用**
   - 测试从列表点击进入应用详情
   - 验证应用信息是否正确加载

3. **对话生成**
   - 测试与 AI 对话生成代码
   - 验证 SSE 连接中的 appId 参数

4. **编辑应用**
   - 测试编辑应用功能
   - 验证更新操作

5. **部署应用**
   - 测试部署功能
   - 验证部署请求中的 appId

## 注意事项

1. **禁止使用 Number 转换**
   - 整个项目中不应该使用 `Number(id)` 或 `parseInt(id)` 转换 ID
   - 所有 ID 都应该作为 string 处理

2. **类型安全**
   - 如果 API 接口需要 number 类型，使用 `as any` 临时转换
   - 但优先修改 API 类型定义，避免使用类型断言

3. **后端兼容性**
   - 确保后端接口接受 string 类型的 ID 参数
   - 如果后端需要 number 类型，需要后端配合修改

## 未来建议

1. **代码生成器配置**
   - 修改 Swagger/OpenAPI 代码生成配置
   - 将 Long 类型映射为 string 而不是 number

2. **ESLint 规则**
   - 添加规则禁止对 ID 进行 number 转换
   - 添加规则检测 ID 类型使用

3. **类型检查**
   - 在 CI/CD 流程中添加严格的类型检查
   - 确保所有 ID 类型使用一致
