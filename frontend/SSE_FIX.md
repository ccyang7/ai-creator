# SSE 流式响应修复说明

## 后端接口格式

后端返回的 SSE 数据格式：
```java
@GetMapping(value = "/chat/generate/code", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> chatToGenerateCode(...) {
    // 返回格式：{"d": content}
    Flux<String> contentFlux = appService.chatToGenerateCode(appId, message, loginUser);
    return contentFlux.map(content -> {
        Map<String, String> wrapper = Map.of("d", content);
        String jsonStr = JSONUtil.toJsonStr(wrapper);
        return ServerSentEvent.<String>builder().data(jsonStr).build();
    }).concatWith(Mono.just(ServerSentEvent.<String>builder().event("done").data("").build()));
}
```

## 前端修复内容

### 1. SSE 事件处理

**之前的问题**：
- 没有正确处理后端返回的 JSON 格式 `{"d": content}`
- 没有监听 `done` 事件
- 错误地将所有数据都当作特定格式处理

**修复方案**：
```typescript
let aiContent = ''
let isDone = false

// 监听 done 事件（流式传输完成）
eventSource.addEventListener('done', () => {
  isDone = true
  eventSource.close()
  isLoading.value = false
  
  // 显示预览
  if (appInfo.value.codeGenType && aiContent.trim()) {
    generatedCode.value = aiContent
    showPreview.value = true
    previewUrl.value = `http://localhost:8200/api/static/${appInfo.value.codeGenType}_${appIdParam}/`
    message.success('代码生成完成，已自动展示预览')
  }
})

// 处理普通消息
eventSource.onmessage = (event) => {
  try {
    const parsed = JSON.parse(event.data)
    // 后端返回的格式是 {"d": content}
    const content = parsed.d || parsed.content || ''
    if (content) {
      aiContent += content
      messages.value[aiMessageIndex]!.content = aiContent
      scrollToBottom()
    }
  } catch (e) {
    // 如果不是 JSON，直接作为内容
    aiContent += event.data
    messages.value[aiMessageIndex]!.content = aiContent
    scrollToBottom()
  }
}

// 错误处理
eventSource.onerror = (error) => {
  console.error('SSE 错误:', error)
  if (!isDone) {
    eventSource.close()
    isLoading.value = false
    message.error('对话连接异常中断')
    messages.value.pop()
  }
}
```

### 2. Markdown 代码高亮

**添加代码高亮配置**：
```typescript
const renderMarkdown = (content: string) => {
  md.set({
    highlight: (str: string, lang: string) => {
      return `<pre><code class="hljs language-${lang}">${md.utils.escapeHtml(str)}</code></pre>`
    },
  })
  return md.render(content)
}
```

**添加代码高亮样式**：
```css
/* 代码高亮样式（Monokai 主题） */
.app-chat-page__message-text--ai :deep(.hljs) {
  background: #1a1a1a;
  color: #f8f8f2;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
}

.app-chat-page__message-text--ai :deep(.hljs-keyword) {
  color: #66d9ef;  /* 关键字 */
}

.app-chat-page__message-text--ai :deep(.hljs-function) {
  color: #a6e22e;  /* 函数 */
}

.app-chat-page__message-text--ai :deep(.hljs-string) {
  color: #e6db74;  /* 字符串 */
}

.app-chat-page__message-text--ai :deep(.hljs-number) {
  color: #ae81ff;  /* 数字 */
}

.app-chat-page__message-text--ai :deep(.hljs-comment) {
  color: #75715e;  /* 注释 */
}

.app-chat-page__message-text--ai :deep(.hljs-class) {
  color: #fd971f;  /* 类 */
}
```

## 数据流程

1. **用户发送消息** → 前端添加用户消息到列表
2. **创建 SSE 连接** → 连接到后端 `/api/app/chat/generate/code`
3. **接收流式数据** → 后端持续返回 `{"d": content}` 格式的 JSON
4. **解析并累积内容** → 前端解析 JSON，累积 `content` 字段
5. **实时更新 UI** → 逐字显示 AI 回复内容
6. **接收 done 事件** → 后端发送结束事件
7. **显示预览** → 代码生成完成后自动显示网页预览

## 关键改进

### 1. 正确的 JSON 解析
- 使用 `parsed.d` 获取后端返回的内容
- 兼容旧的 `parsed.content` 格式
- 处理非 JSON 格式的降级方案

### 2. 优雅的错误处理
- 使用 `isDone` 标志区分正常结束和异常中断
- 只在异常中断时显示错误提示
- 正常结束时不触发错误处理

### 3. 代码高亮支持
- 配置 markdown-it 的 highlight 函数
- 添加完整的代码高亮样式
- 支持多种编程语言

### 4. 用户体验优化
- 流式显示内容，无需等待生成完成
- 自动滚动到最新消息
- 生成完成后自动展示预览
- 提供清晰的成功/错误提示

## 测试建议

1. **测试流式显示**
   - 发送代码生成请求
   - 观察内容是否逐字显示
   - 验证代码块是否正确高亮

2. **测试完成事件**
   - 等待生成完成
   - 验证是否触发 `done` 事件
   - 检查预览是否自动显示

3. **测试错误处理**
   - 断开网络连接
   - 验证错误提示是否显示
   - 检查是否能重新发送消息

4. **测试代码高亮**
   - 生成包含多种语言的代码
   - 验证不同语言的语法高亮
   - 检查代码块样式是否正确

## 依赖说明

当前实现使用原生 EventSource API，无需额外依赖。如果需要更好的浏览器兼容性，可以考虑：
- `event-source-polyfill` - 支持 IE 和旧版浏览器
- `@microsoft/fetch-event-source` - 基于 fetch 的 SSE 实现

## 注意事项

1. **跨域问题**：确保后端配置了正确的 CORS
2. **连接复用**：每次发送消息创建新的 EventSource 连接
3. **内存泄漏**：确保在组件卸载时关闭所有连接
4. **错误重试**：可以考虑添加自动重连机制
