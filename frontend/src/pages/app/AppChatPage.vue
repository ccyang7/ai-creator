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
        <a-button class="app-chat-page__detail-btn" @click="showDetailModal = true">
          <InfoCircleOutlined />
          应用详情
        </a-button>
        <a-button
          v-if="!appInfo.deployKey"
          type="primary"
          class="app-chat-page__deploy-btn"
          :loading="deploying"
          @click="handleDeploy"
        >
          <CloudUploadOutlined />
          部署
        </a-button>
        <a-button v-else class="app-chat-page__deployed-btn" disabled>
          <CheckCircleOutlined />
          已部署
        </a-button>
        <a-button v-if="deployedUrl" class="app-chat-page__preview-btn" @click="openDeployedUrl">
          <EyeOutlined />
          访问已部署网站
        </a-button>
      </div>
    </div>

    <!-- 核心内容区域 -->
    <div class="app-chat-page__main" ref="mainContainer">
      <!-- 左侧对话区域 -->
      <div class="app-chat-page__chat" :style="{ width: chatWidth + 'px' }">
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
            <!-- AI 消息：头像在左，内容在右 -->
            <template v-if="message.role === 'ai'">
              <div class="app-chat-page__message-avatar app-chat-page__message-avatar--ai">
                <RobotOutlined />
              </div>
              <div class="app-chat-page__message-content">
                <div
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
            </template>

            <!-- 用户消息：内容在左，头像在右 -->
            <template v-else>
              <div class="app-chat-page__message-content app-chat-page__message-content--user">
                <div class="app-chat-page__message-text app-chat-page__message-text--user">
                  {{ message.content }}
                </div>
              </div>
              <div class="app-chat-page__message-avatar app-chat-page__message-avatar--user">
                <UserOutlined />
              </div>
            </template>
          </div>

          <!-- 加载状态 -->
          <div v-if="isLoading" class="app-chat-page__message app-chat-page__message--ai">
            <div class="app-chat-page__message-avatar app-chat-page__message-avatar--ai">
              <RobotOutlined />
            </div>
            <div class="app-chat-page__message-content">
              <div class="app-chat-page__message-text app-chat-page__message-text--ai">
                <a-spin tip="AI 正在思考中..." />
                <div class="app-chat-page__loading-dots">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 用户消息输入框 -->
        <div class="app-chat-page__input-area">
          <a-tooltip
            v-if="!isOwner && !isViewMode"
            title="无法在别人的作品下对话哦~"
            placement="top"
          >
            <div class="app-chat-page__input-wrapper app-chat-page__input-wrapper--disabled">
              <a-textarea
                v-model:value="userInput"
                class="app-chat-page__input"
                placeholder="描述你的需求，让 AI 帮你完善应用..."
                :auto-size="{ minRows: 2, maxRows: 4 }"
                disabled
              />
            </div>
          </a-tooltip>
          <div v-else class="app-chat-page__input-wrapper">
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
              v-if="isOwner && !isViewMode"
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

      <!-- 可拖动手柄 -->
      <div class="app-chat-page__resizer" @mousedown="startResize">
        <div class="app-chat-page__resizer-line"></div>
      </div>

      <!-- 右侧网页展示区域 -->
      <div class="app-chat-page__preview" :style="{ width: previewWidth + 'px' }">
        <div class="app-chat-page__preview-header">
          <span class="app-chat-page__preview-title">网页预览</span>
          <a-tag v-if="showPreview" color="green" class="app-chat-page__preview-status">
            已加载
          </a-tag>
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

    <!-- 应用详情悬浮窗 -->
    <a-modal
      v-model:open="showDetailModal"
      title="应用详情"
      :footer="null"
      width="480px"
      class="app-chat-page__detail-modal"
    >
      <div class="app-chat-page__detail-content">
        <!-- 应用基础信息 -->
        <div class="app-chat-page__detail-section">
          <h3 class="app-chat-page__detail-section-title">基础信息</h3>
          <div class="app-chat-page__detail-info">
            <div class="app-chat-page__detail-item">
              <span class="app-chat-page__detail-label">创建者</span>
              <div class="app-chat-page__detail-creator">
                <a-avatar
                  :src="appInfo.user?.userAvatar"
                  :size="32"
                  class="app-chat-page__detail-avatar"
                >
                  <UserOutlined />
                </a-avatar>
                <span class="app-chat-page__detail-creator-name">{{
                  appInfo.user?.userName || '未知用户'
                }}</span>
              </div>
            </div>
            <div class="app-chat-page__detail-item">
              <span class="app-chat-page__detail-label">创建时间</span>
              <span class="app-chat-page__detail-value">{{ formatDate(appInfo.createTime) }}</span>
            </div>
          </div>
        </div>

        <!-- 操作栏（仅本人或管理员可见） -->
        <div v-if="isOwner" class="app-chat-page__detail-section">
          <h3 class="app-chat-page__detail-section-title">操作</h3>
          <div class="app-chat-page__detail-actions">
            <a-button
              type="primary"
              class="app-chat-page__detail-action-btn"
              @click="handleEditApp"
            >
              <EditOutlined />
              修改应用
            </a-button>
            <a-popconfirm
              title="确定要删除此应用吗？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="handleDeleteApp"
            >
              <a-button danger class="app-chat-page__detail-action-btn">
                <DeleteOutlined />
                删除应用
              </a-button>
            </a-popconfirm>
          </div>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'
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
  CheckCircleOutlined,
  InfoCircleOutlined,
  DeleteOutlined,
} from '@ant-design/icons-vue'
import { getAppById, deployApp, deleteApp } from '@/api/appController'
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
const deploying = ref(false)

// 权限控制
const isOwner = ref(false)
const isViewMode = ref(false)

// 应用详情悬浮窗
const showDetailModal = ref(false)

// 拖拽相关
const chatWidth = ref(500)
const previewWidth = ref(0)
const isResizing = ref(false)
const startX = ref(0)
const startChatWidth = ref(0)
const mainContainer = ref<HTMLElement | null>(null)

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
      if (lang && hljs.getLanguage(lang)) {
        try {
          return hljs.highlight(str, { language: lang }).value
        } catch (e) {
          console.error('代码高亮失败:', e)
        }
      }
      // 如果没有指定语言或高亮失败，直接转义 HTML
      const escaped = str
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;')
      return escaped
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

  // 检查是否是查看模式（从 URL 参数判断）
  isViewMode.value = !!route.query.view

  try {
    const res = await getAppById({ id: appId.value })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data
      console.log('应用信息加载成功:', appInfo.value)
      console.log('codeGenType:', appInfo.value.codeGenType)

      // 检查是否是当前用户的应用
      if (loginUserStore.loginUser.id && appInfo.value.userId === loginUserStore.loginUser.id) {
        isOwner.value = true
      } else if (loginUserStore.loginUser.userRole === 'admin') {
        isOwner.value = true
      } else {
        isOwner.value = false
      }

      console.log('isOwner:', isOwner.value)
      console.log('isViewMode:', isViewMode.value)
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
    const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8200/api'
    const eventSource = new EventSource(
      `${apiBaseUrl}/app/chat/generate/code?appId=${appIdParam}&message=${encodedMessage}`,
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
        const previewBaseUrl =
          import.meta.env.VITE_PREVIEW_BASE_URL || 'http://localhost:8200/api/static'
        previewUrl.value = `${previewBaseUrl}/${appInfo.value.codeGenType}_${appIdParam}/`
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

  deploying.value = true

  try {
    const res = await deployApp({
      appId: appInfo.value.id,
    })

    if (res.data.code === 0 && res.data.data) {
      deployedUrl.value = res.data.data
      message.success('部署成功！')
      // 更新应用信息
      await loadAppInfo()
    } else {
      message.error(res.data.message || '部署失败')
    }
  } catch (error) {
    console.error(error)
    message.error('部署失败，请重试')
  } finally {
    deploying.value = false
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

// 格式化日期
const formatDate = (timestamp?: number) => {
  if (!timestamp) return '未知'
  return new Date(timestamp).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// 修改应用
const handleEditApp = () => {
  if (appInfo.value.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

// 删除应用
const handleDeleteApp = async () => {
  if (!appInfo.value.id) {
    message.error('应用 ID 无效')
    return
  }

  try {
    const res = await deleteApp({ id: appInfo.value.id })
    if (res.data.code === 0 && res.data.data) {
      message.success('删除成功')
      showDetailModal.value = false
      // 删除成功后返回上一页
      setTimeout(() => {
        router.back()
      }, 500)
    } else {
      message.error(res.data.message || '删除失败')
    }
  } catch (error) {
    console.error(error)
    message.error('删除失败，请重试')
  }
}

// 开始拖拽
const startResize = (e: MouseEvent) => {
  isResizing.value = true
  startX.value = e.clientX
  startChatWidth.value = chatWidth.value
  document.addEventListener('mousemove', resize)
  document.addEventListener('mouseup', stopResize)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
  document.body.style.overflow = 'hidden'
}

// 拖拽中
const resize = (e: MouseEvent) => {
  if (!isResizing.value || !mainContainer.value) return

  const diff = e.clientX - startX.value
  const newChatWidth = startChatWidth.value + diff
  const containerWidth = mainContainer.value.offsetWidth
  const maxChatWidth = containerWidth * 0.6
  const minPreviewWidth = 900
  const minChatWidth = 400

  // 限制最小和最大宽度
  if (newChatWidth >= minChatWidth && newChatWidth <= maxChatWidth) {
    const newPreviewWidth = containerWidth - newChatWidth - 8 // 8px 是 gap
    if (newPreviewWidth >= minPreviewWidth) {
      chatWidth.value = newChatWidth
      previewWidth.value = newPreviewWidth
    }
  }
}

// 停止拖拽
const stopResize = () => {
  isResizing.value = false
  document.removeEventListener('mousemove', resize)
  document.removeEventListener('mouseup', stopResize)
  document.body.style.cursor = ''
  document.body.style.userSelect = ''
  document.body.style.overflow = ''
}

// 自动发送初始消息
const sendInitialMessage = async () => {
  // 如果是查看模式（从别人的作品进入），不自动发送消息
  if (isViewMode.value) {
    console.log('查看模式，不自动发送消息')
    return
  }

  const initialPrompt = route.query.prompt as string
  if (initialPrompt) {
    userInput.value = initialPrompt
    await handleSendMessage()
  }
}

onMounted(async () => {
  await loadAppInfo()
  // 初始化宽度
  nextTick(() => {
    if (mainContainer.value) {
      const containerWidth = mainContainer.value.offsetWidth
      chatWidth.value = 500
      previewWidth.value = containerWidth - 500 - 8
    }
  })
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
  border-radius: 0 0 24px 24px;
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

.app-chat-page__detail-btn {
  background: rgba(102, 126, 234, 0.08);
  border: 2px solid rgba(102, 126, 234, 0.3);
  color: #667eea;
  border-radius: 20px;
  font-weight: 500;
  transition: all 0.3s;
}

.app-chat-page__detail-btn:hover {
  background: rgba(102, 126, 234, 0.15);
  border-color: #667eea;
  transform: translateY(-2px);
}

.app-chat-page__deploy-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  border-radius: 20px;
}

.app-chat-page__deployed-btn {
  background: #52c41a;
  border: none;
  color: #fff;
  border-radius: 20px;
}

.app-chat-page__preview-btn {
  margin-left: 8px;
  border-radius: 20px;
}

.app-chat-page__main {
  flex: 1;
  display: flex;
  gap: 12px;
  padding: 20px;
  overflow: hidden;
  height: 100%;
}

.app-chat-page__chat {
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.12);
  flex-shrink: 0;
  min-width: 400px;
  height: 100%;
}

.app-chat-page__messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
  height: 100%;
}

.app-chat-page__message {
  display: flex;
  gap: 12px;
  animation: fadeIn 0.3s ease;
  align-items: flex-start;
}

.app-chat-page__message--ai {
  flex-direction: row;
}

.app-chat-page__message--user {
  flex-direction: row;
  justify-content: flex-end;
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
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 100%;
  min-width: 0;
}

.app-chat-page__message--ai .app-chat-page__message-content {
  flex: 1;
}

.app-chat-page__message-content--user {
  align-items: flex-end;
  max-width: calc(100% - 48px);
}

.app-chat-page__message-text {
  padding: 16px 20px;
  border-radius: 20px;
  font-size: 14px;
  line-height: 1.6;
  word-wrap: break-word;
  word-break: break-word;
  white-space: pre-wrap;
  max-width: 100%;
  overflow-wrap: break-word;
}

.app-chat-page__message-text--user {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-bottom-right-radius: 6px;
  border-bottom-left-radius: 20px;
}

.app-chat-page__message-text--ai {
  background: #f5f7fa;
  color: #333;
  border-bottom-left-radius: 6px;
  border-bottom-right-radius: 20px;
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
  background: #282c34;
  color: #abb2bf;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  overflow-y: hidden;
  margin: 8px 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  word-break: break-all;
  max-width: 100%;
}

.app-chat-page__message-text--ai :deep(pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
  white-space: pre-wrap;
  word-wrap: break-word;
  word-break: break-all;
  display: block;
  max-width: 100%;
}

/* 代码高亮样式 - 使用 highlight.js 的 atom-one-dark 主题 */
.app-chat-page__message-text--ai :deep(.hljs) {
  background: #282c34;
  color: #abb2bf;
  padding: 12px;
  border-radius: 8px;
  overflow-x: auto;
  overflow-y: hidden;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-wrap: break-word;
  word-break: break-all;
  max-width: 100%;
  display: block;
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
  font-size: 13px;
  line-height: 1.5;
}

/* 确保代码块中的长行自动换行 */
.app-chat-page__message-text--ai :deep(.hljs-string),
.app-chat-page__message-text--ai :deep(.hljs-attr),
.app-chat-page__message-text--ai :deep(.hljs-value) {
  word-wrap: break-word;
  word-break: break-all;
  white-space: pre-wrap;
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
  padding: 10px 14px;
  background: #f5f7fa;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s;
}

.app-chat-page__message-file:hover {
  background: rgba(102, 126, 234, 0.1);
  transform: translateY(-2px);
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

/* 加载动画 */
.app-chat-page__loading-dots {
  display: inline-flex;
  gap: 4px;
  margin-left: 8px;
}

.app-chat-page__loading-dots span {
  width: 8px;
  height: 8px;
  background: #667eea;
  border-radius: 50%;
  animation: bounce 1.4s infinite ease-in-out both;
}

.app-chat-page__loading-dots span:nth-child(1) {
  animation-delay: -0.32s;
}

.app-chat-page__loading-dots span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes bounce {
  0%,
  80%,
  100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.app-chat-page__input-area {
  padding: 20px 24px 24px;
  background: #fff;
  border-top: 1px solid #e8e8e8;
  flex-shrink: 0;
  border-radius: 0 0 24px 24px;
}

.app-chat-page__input-wrapper {
  margin-bottom: 12px;
}

.app-chat-page__input-wrapper--disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.app-chat-page__input {
  border: 2px solid rgba(102, 126, 234, 0.2);
  border-radius: 16px;
  padding: 14px;
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

/* 拖动手柄样式 */
.app-chat-page__resizer {
  width: 12px;
  cursor: col-resize;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  z-index: 10;
  transition: all 0.2s ease;
  border-radius: 6px;
}

.app-chat-page__resizer:hover {
  background: rgba(102, 126, 234, 0.15);
}

.app-chat-page__resizer-line {
  width: 4px;
  height: 40px;
  background: rgba(102, 126, 234, 0.3);
  border-radius: 2px;
  transition: all 0.2s ease;
}

.app-chat-page__resizer:hover .app-chat-page__resizer-line {
  background: rgba(102, 126, 234, 0.8);
  height: 60px;
}

/* 预览区域样式 */
.app-chat-page__preview {
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 24px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.12);
  flex-shrink: 0;
  min-width: 900px;
  padding-right: 60px;
  height: 100%;
}

.app-chat-page__preview-header {
  height: 48px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.02) 0%, rgba(118, 75, 162, 0.02) 100%);
  border-bottom: 1px solid rgba(102, 126, 234, 0.1);
  display: flex;
  align-items: center;
  padding: 0 32px 0 16px;
  gap: 12px;
  border-radius: 24px 24px 0 0;
}

.app-chat-page__preview-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.app-chat-page__preview-status {
  margin-left: 0;
  border-radius: 12px;
}

.app-chat-page__refresh-btn {
  margin-left: auto;
  padding: 4px 12px;
  font-size: 13px;
  color: #667eea;
  border-radius: 12px;
  transition: all 0.3s;
}

.app-chat-page__refresh-btn:hover {
  background: rgba(102, 126, 234, 0.1);
}

.app-chat-page__preview-content {
  flex: 1;
  position: relative;
  background: #fafafa;
  overflow: hidden;
  padding: 0 16px 0 0;
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

/* 应用详情悬浮窗样式 */
.app-chat-page__detail-modal :deep(.ant-modal-header) {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}

.app-chat-page__detail-modal :deep(.ant-modal-title) {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.app-chat-page__detail-modal :deep(.ant-modal-body) {
  padding: 24px;
}

.app-chat-page__detail-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.app-chat-page__detail-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.app-chat-page__detail-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #666;
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.app-chat-page__detail-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  background: #f5f7fa;
  border-radius: 12px;
}

.app-chat-page__detail-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-chat-page__detail-label {
  font-size: 13px;
  color: #666;
  min-width: 70px;
}

.app-chat-page__detail-creator {
  display: flex;
  align-items: center;
  gap: 10px;
}

.app-chat-page__detail-avatar {
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.app-chat-page__detail-creator-name {
  font-size: 14px;
  font-weight: 500;
  color: #1a1a1a;
}

.app-chat-page__detail-value {
  font-size: 14px;
  color: #1a1a1a;
}

.app-chat-page__detail-actions {
  display: flex;
  gap: 12px;
}

.app-chat-page__detail-action-btn {
  flex: 1;
  height: 40px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: all 0.3s;
}

.app-chat-page__detail-action-btn:hover {
  transform: translateY(-2px);
}
</style>
