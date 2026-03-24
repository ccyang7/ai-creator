<template>
  <div class="app-chat-page">
    <!-- 顶部栏 -->
    <div class="app-chat-page__header">
      <div class="app-chat-page__header-left">
        <a-button type="text" class="app-chat-page__back-btn" @click="goBack">
          <LeftOutlined />
          返回
        </a-button>
        <div class="app-chat-page__app-info">
          <a-avatar
            :src="appInfo.cover"
            :size="32"
            shape="square"
            class="app-chat-page__app-avatar"
          >
            {{ appInfo.appName?.[0] || 'A' }}
          </a-avatar>
          <span class="app-chat-page__app-name">{{ appInfo.appName || '未命名应用' }}</span>
        </div>
      </div>
      <div class="app-chat-page__header-right">
        <a-button
          v-if="appInfo.deployKey"
          type="primary"
          class="app-chat-page__deploy-btn"
          @click="handleDeploy"
        >
          <CloudUploadOutlined />
          部署
        </a-button>
        <a-button v-if="deployedUrl" class="app-chat-page__preview-btn" @click="openDeployedUrl">
          <EyeOutlined />
          访问已部署网站
        </a-button>
      </div>
    </div>

    <!-- 核心内容区域 -->
    <div class="app-chat-page__main">
      <!-- 左侧对话区域 -->
      <div class="app-chat-page__chat">
        <!-- 消息区域 -->
        <div class="app-chat-page__messages" ref="messagesContainer">
          <div
            v-for="(message, index) in messages"
            :key="index"
            :class="[
              'app-chat-page__message',
              message.role === 'user'
                ? 'app-chat-page__message--user'
                : 'app-chat-page__message--ai',
            ]"
          >
            <div
              v-if="message.role === 'ai'"
              class="app-chat-page__message-avatar app-chat-page__message-avatar--ai"
            >
              <RobotOutlined />
            </div>
            <div class="app-chat-page__message-content">
              <div
                v-if="message.role === 'user'"
                class="app-chat-page__message-text app-chat-page__message-text--user"
              >
                {{ message.content }}
              </div>
              <div
                v-else
                class="app-chat-page__message-text app-chat-page__message-text--ai"
                v-html="renderMarkdown(message.content)"
              ></div>
              <!-- 代码文件列表 -->
              <div
                v-if="message.files && message.files.length > 0"
                class="app-chat-page__message-files"
              >
                <div
                  v-for="file in message.files"
                  :key="file.path"
                  class="app-chat-page__message-file"
                  @click="selectFile(file)"
                >
                  <FileOutlined />
                  <span class="app-chat-page__message-file-name">{{ file.name }}</span>
                  <span class="app-chat-page__message-file-path">{{ file.path }}</span>
                </div>
              </div>
            </div>
            <div
              v-if="message.role === 'user'"
              class="app-chat-page__message-avatar app-chat-page__message-avatar--user"
            >
              <UserOutlined />
            </div>
          </div>

          <!-- 加载状态 -->
          <div v-if="isLoading" class="app-chat-page__message app-chat-page__message--ai">
            <div class="app-chat-page__message-avatar app-chat-page__message-avatar--ai">
              <RobotOutlined />
            </div>
            <div class="app-chat-page__message-content">
              <a-spin tip="AI 正在思考中..." />
            </div>
          </div>
        </div>

        <!-- 用户消息输入框 -->
        <div class="app-chat-page__input-area">
          <div class="app-chat-page__input-wrapper">
            <a-textarea
              v-model:value="userInput"
              class="app-chat-page__input"
              placeholder="描述你的需求，让 AI 帮你完善应用..."
              :auto-size="{ minRows: 2, maxRows: 4 }"
              :disabled="isLoading || !appInfo.id"
              @press-enter="handleSendMessage"
            />
          </div>
          <div class="app-chat-page__input-actions">
            <div class="app-chat-page__input-left">
              <a-button class="app-chat-page__action-btn" disabled>
                <PaperClipOutlined />
                上传
              </a-button>
              <a-button class="app-chat-page__action-btn" disabled>
                <EditOutlined />
                编辑
              </a-button>
              <a-button class="app-chat-page__action-btn" disabled>
                <EditOutlined />
                优化
              </a-button>
            </div>
            <a-button
              type="primary"
              class="app-chat-page__send-btn"
              :loading="isLoading"
              @click="handleSendMessage"
            >
              <SendOutlined />
              发送
            </a-button>
          </div>
        </div>
      </div>

      <!-- 右侧网页展示区域 -->
      <div class="app-chat-page__preview">
        <div class="app-chat-page__preview-header">
          <span class="app-chat-page__preview-title">网页预览</span>
          <a-tag v-if="showPreview" color="green" style="margin-left: 12px"> 已加载 </a-tag>
          <a-button
            v-if="generatedCode && showPreview"
            type="link"
            class="app-chat-page__refresh-btn"
            @click="refreshPreview"
          >
            <ReloadOutlined />
            刷新
          </a-button>
        </div>
        <div class="app-chat-page__preview-content">
          <div v-if="!showPreview" class="app-chat-page__preview-empty">
            <Empty description="等待代码生成完成后自动展示" />
          </div>
          <iframe
            v-else
            ref="previewFrame"
            class="app-chat-page__preview-frame"
            :src="previewUrl"
            sandbox="allow-scripts allow-same-origin allow-modals"
            @load="handleFrameLoad"
            @error="handleFrameError"
          ></iframe>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import MarkdownIt from 'markdown-it'
import {
  LeftOutlined,
  CloudUploadOutlined,
  EyeOutlined,
  RobotOutlined,
  UserOutlined,
  FileOutlined,
  SendOutlined,
  PaperClipOutlined,
  EditOutlined,
  ReloadOutlined,
} from '@ant-design/icons-vue'
import { getAppById, deployApp } from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'


const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const md = new MarkdownIt()

// 应用信息
const appInfo = ref<API.AppVO>({})
const appId = ref<string>('')

// 消息列表
interface ChatMessage {
  role: 'user' | 'ai'
  content: string
  files?: Array<{
    name: string
    path: string
    content?: string
  }>
}

const messages = ref<ChatMessage[]>([])
const userInput = ref('')
const isLoading = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)

// 代码生成相关
const generatedCode = ref('')
const showPreview = ref(false)
const previewUrl = ref('')
const previewFrame = ref<HTMLIFrameElement | null>(null)

// 部署相关
const deployedUrl = ref('')

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 渲染 Markdown
const renderMarkdown = (content: string) => {
  // 配置 markdown-it 支持代码高亮
  md.set({
    highlight: (str: string, lang: string) => {
      return `<pre><code class="hljs language-${lang}">${md.utils.escapeHtml(str)}</code></pre>`
    },
  })
  return md.render(content)
}

// 加载应用信息
const loadAppInfo = async () => {
  const id = route.params.id as string
  appId.value = id

  if (!appId.value) {
    message.error('应用 ID 无效')
    router.push('/')
    return
  }

  try {
    const res = await getAppById({ id: appId.value })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data
      console.log('应用信息加载成功:', appInfo.value)
      console.log('codeGenType:', appInfo.value.codeGenType)

      // 检查是否是当前用户的应用
      if (
        loginUserStore.loginUser.id &&
        appInfo.value.userId !== loginUserStore.loginUser.id &&
        loginUserStore.loginUser.userRole !== 'admin'
      ) {
        message.error('无权查看此应用')
        router.push('/')
        return
      }
    } else {
      message.error('应用不存在')
      router.push('/')
    }
  } catch (error) {
    console.error(error)
    message.error('加载应用信息失败')
  }
}

// 发送消息
const handleSendMessage = async () => {
  if (!userInput.value.trim()) {
    message.warning('请输入消息内容')
    return
  }

  if (!appInfo.value.id) {
    message.error('应用信息加载失败')
    return
  }

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: userInput.value,
  })

  const userMessage = userInput.value
  userInput.value = ''
  isLoading.value = true
  scrollToBottom()

  try {
    // 添加 AI 消息占位
    const aiMessageIndex = messages.value.length
    messages.value.push({
      role: 'ai',
      content: '',
      files: [],
    })

    // 使用 EventSource 处理 SSE
    const appIdParam = appInfo.value.id
    const encodedMessage = encodeURIComponent(userMessage)
    const eventSource = new EventSource(
      `http://localhost:8200/api/app/chat/generate/code?appId=${appIdParam}&message=${encodedMessage}`,
      {
        withCredentials: true,
      },
    )

    let aiContent = ''
    const hasFiles = false
    let isDone = false

    // 处理自定义事件（包括 done 事件）
    eventSource.addEventListener('done', () => {
      isDone = true
      eventSource.close()
      isLoading.value = false
      console.log('SSE 流式传输完成，aiContent:', aiContent)
      console.log('appInfo.codeGenType:', appInfo.value.codeGenType)
      console.log('appId:', appIdParam)

      // 生成完成后显示预览
      if (appInfo.value.codeGenType && aiContent.trim()) {
        generatedCode.value = aiContent
        showPreview.value = true
        previewUrl.value = `http://localhost:8200/api/static/${appInfo.value.codeGenType}_${appIdParam}/`
        console.log('预览 URL:', previewUrl.value)
        message.success('代码生成完成，已自动展示预览')
        refreshPreview()
      } else if (!aiContent.trim()) {
        message.warning('AI 未生成任何内容')
      } else if (!appInfo.value.codeGenType) {
        message.warning('应用缺少 codeGenType，无法预览')
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

    eventSource.onerror = (error) => {
      console.error('SSE 错误:', error)
      if (!isDone) {
        eventSource.close()
        isLoading.value = false
        message.error('对话连接异常中断')
        // 移除失败的消息
        messages.value.pop()
      }
    }
  } catch (error) {
    console.error('对话错误:', error)
    message.error('对话失败，请重试')
    // 移除失败的 AI 消息
    messages.value.pop()
    isLoading.value = false
  }
}

// 选择文件
const selectFile = (file: any) => {
  console.log('选择文件:', file)
  message.info(`查看文件：${file.name}`)
}

// 部署应用
const handleDeploy = async () => {
  if (!appInfo.value.id) {
    message.error('应用信息加载失败')
    return
  }

  try {
    const res = await deployApp({
      appId: appInfo.value.id,
    })

    if (res.data.code === 0 && res.data.data) {
      deployedUrl.value = res.data.data
      message.success('部署成功')
      // 更新应用信息
      await loadAppInfo()
    } else {
      message.error(res.data.message || '部署失败')
    }
  } catch (error) {
    console.error(error)
    message.error('部署失败，请重试')
  }
}

// 打开已部署的 URL
const openDeployedUrl = () => {
  if (deployedUrl.value) {
    window.open(deployedUrl.value, '_blank')
  }
}

// 刷新预览
const refreshPreview = () => {
  if (previewFrame.value) {
    previewFrame.value.src = previewUrl.value
  }
}

// iframe 加载完成
const handleFrameLoad = () => {
  console.log('iframe 加载完成')
}

// iframe 加载错误
const handleFrameError = () => {
  console.error('iframe 加载失败')
  message.error('预览加载失败，请检查后端服务是否正常运行')
}

// 监听预览状态变化
watch(showPreview, (newVal) => {
  if (newVal) {
    console.log('预览已启用，URL:', previewUrl.value)
  }
})

// 返回
const goBack = () => {
  router.back()
}

// 自动发送初始消息
const sendInitialMessage = async () => {
  const initialPrompt = route.query.prompt as string
  if (initialPrompt) {
    userInput.value = initialPrompt
    await handleSendMessage()
  }
}

onMounted(async () => {
  await loadAppInfo()
  // 等待应用信息加载完后发送初始消息
  setTimeout(() => {
    sendInitialMessage()
  }, 500)
})
</script>

<style scoped>
.app-chat-page {
  height: calc(100vh - 64px);
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.app-chat-page__header {
  height: 64px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.app-chat-page__header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.app-chat-page__back-btn {
  font-size: 14px;
  color: #666;
}

.app-chat-page__app-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-chat-page__app-avatar {
  border: 2px solid rgba(102, 126, 234, 0.2);
}

.app-chat-page__app-name {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.app-chat-page__header-right {
  display: flex;
  gap: 12px;
}

.app-chat-page__deploy-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
}

.app-chat-page__main {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 16px;
  overflow: hidden;
}

.app-chat-page__chat {
  width: 500px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.app-chat-page__messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.app-chat-page__message {
  display: flex;
  gap: 12px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.app-chat-page__message--user {
  flex-direction: row-reverse;
}

.app-chat-page__message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.app-chat-page__message-avatar--ai {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.app-chat-page__message-avatar--user {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: #fff;
}

.app-chat-page__message-content {
  max-width: 400px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.app-chat-page__message-text {
  padding: 14px 18px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.6;
  word-wrap: break-word;
}

.app-chat-page__message-text--user {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.app-chat-page__message-text--ai {
  background: #f5f7fa;
  color: #333;
  border-bottom-left-radius: 4px;
}

.app-chat-page__message-text--ai :deep(p) {
  margin: 0 0 8px 0;
}

.app-chat-page__message-text--ai :deep(p:last-child) {
  margin: 0;
}

.app-chat-page__message-text--ai :deep(code) {
  background: rgba(0, 0, 0, 0.06);
  padding: 2px 6px;
  border-radius: 4px;
  font-family: 'Courier New', monospace;
  font-size: 13px;
}

.app-chat-page__message-text--ai :deep(pre) {
  background: #1a1a1a;
  color: #f8f8f2;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 8px 0;
}

.app-chat-page__message-text--ai :deep(pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}

/* 代码高亮样式 */
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
  color: #66d9ef;
}

.app-chat-page__message-text--ai :deep(.hljs-function) {
  color: #a6e22e;
}

.app-chat-page__message-text--ai :deep(.hljs-string) {
  color: #e6db74;
}

.app-chat-page__message-text--ai :deep(.hljs-number) {
  color: #ae81ff;
}

.app-chat-page__message-text--ai :deep(.hljs-comment) {
  color: #75715e;
}

.app-chat-page__message-text--ai :deep(.hljs-class) {
  color: #fd971f;
}

.app-chat-page__message-text--ai :deep(.hljs-title) {
  color: #fd971f;
}

.app-chat-page__message-text--ai :deep(.hljs-variable) {
  color: #f8f8f2;
}

.app-chat-page__message-text--ai :deep(.hljs-built_in) {
  color: #66d9ef;
}

.app-chat-page__message-text--ai :deep(.language-html),
.app-chat-page__message-text--ai :deep(.language-css),
.app-chat-page__message-text--ai :deep(.language-javascript) {
  font-family: 'Courier New', monospace;
}

.app-chat-page__message-files {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.app-chat-page__message-file {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #f5f7fa;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.app-chat-page__message-file:hover {
  background: rgba(102, 126, 234, 0.1);
}

.app-chat-page__message-file-name {
  font-size: 13px;
  font-weight: 600;
  color: #333;
}

.app-chat-page__message-file-path {
  font-size: 12px;
  color: #999;
  margin-left: auto;
}

.app-chat-page__input-area {
  padding: 16px 24px 24px;
  background: #fff;
  border-top: 1px solid #e8e8e8;
}

.app-chat-page__input-wrapper {
  margin-bottom: 12px;
}

.app-chat-page__input {
  border: 2px solid rgba(102, 126, 234, 0.2);
  border-radius: 12px;
  padding: 12px;
  font-size: 14px;
  resize: none;
  transition: all 0.3s;
}

.app-chat-page__input:hover {
  border-color: rgba(102, 126, 234, 0.4);
}

.app-chat-page__input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.app-chat-page__input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.app-chat-page__input-left {
  display: flex;
  gap: 8px;
}

.app-chat-page__action-btn {
  border-radius: 20px;
  background: rgba(102, 126, 234, 0.05);
  border: 1px solid rgba(102, 126, 234, 0.2);
  color: #667eea;
  font-size: 13px;
  transition: all 0.3s;
}

.app-chat-page__action-btn:hover:not(:disabled) {
  background: rgba(102, 126, 234, 0.1);
  border-color: #667eea;
}

.app-chat-page__send-btn {
  height: 36px;
  padding: 0 24px;
  border-radius: 18px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 2px 12px rgba(102, 126, 234, 0.3);
}

.app-chat-page__preview {
  flex: 1;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
}

.app-chat-page__preview-header {
  height: 48px;
  background: #f5f7fa;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}

.app-chat-page__preview-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.app-chat-page__preview-content {
  flex: 1;
  position: relative;
  background: #fafafa;
}

.app-chat-page__preview-empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.app-chat-page__preview-frame {
  width: 100%;
  height: 100%;
  border: none;
  background: #fff;
}

@media (max-width: 1200px) {
  .app-chat-page__chat {
    width: 400px;
  }
}
</style>
