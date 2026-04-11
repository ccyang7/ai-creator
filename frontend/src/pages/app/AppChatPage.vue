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
          <CodeGenTypeTag v-if="appInfo.codeGenType" :type="appInfo.codeGenType" />
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
        <a-button
          class="app-chat-page__download-btn"
          :loading="downloading"
          @click="handleDownloadCode"
        >
          <DownloadOutlined />
          下载代码
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
          <!-- 加载更多按钮 -->
          <div v-if="hasMoreHistory && messages.length > 0" class="app-chat-page__load-more">
            <a-button type="link" :loading="loadingHistory" @click="loadChatHistory(true)">
              {{ loadingHistory ? '加载中...' : '加载更多历史消息' }}
            </a-button>
          </div>

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
          <!-- 选中元素信息展示 -->
          <a-alert
            v-if="selectedElementInfo"
            class="app-chat-page__element-alert"
            type="info"
            closable
            @close="clearSelectedElement"
          >
            <template #message>
              <div class="selected-element-info">
                <div class="element-header">
                  <span class="element-tag">
                    选中元素：{{ selectedElementInfo.tagName.toLowerCase() }}
                  </span>
                  <span v-if="selectedElementInfo.id" class="element-id">
                    #{{ selectedElementInfo.id }}
                  </span>
                  <span v-if="selectedElementInfo.className" class="element-class">
                    .{{ selectedElementInfo.className.split(' ').join('.') }}
                  </span>
                </div>
                <div class="element-details">
                  <div v-if="selectedElementInfo.textContent" class="element-item">
                    内容: {{ selectedElementInfo.textContent.substring(0, 50) }}
                    {{ selectedElementInfo.textContent.length > 50 ? '...' : '' }}
                  </div>
                  <div v-if="selectedElementInfo.pagePath" class="element-item">
                    页面路径: {{ selectedElementInfo.pagePath }}
                  </div>
                  <div class="element-item">
                    选择器:
                    <code class="element-selector-code">{{ selectedElementInfo.selector }}</code>
                  </div>
                </div>
              </div>
            </template>
          </a-alert>

          <a-tooltip v-if="!isOwner" title="无法在别人的作品下对话哦~" placement="top">
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
                优化
              </a-button>
            </div>
            <a-button
              v-if="isOwner"
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
          <div class="app-chat-page__preview-actions">
            <a-button
              v-if="isOwner && showPreview"
              type="link"
              :danger="isEditMode"
              @click="toggleEditMode"
              :class="{ 'edit-mode-active': isEditMode }"
            >
              <EditOutlined />
              {{ isEditMode ? '退出编辑' : '可视化编辑' }}
            </a-button>
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
    <AppDetailModal
      v-model:visible="showDetailModal"
      :app="appInfo"
      :is-owner="isOwner"
      :show-actions="isOwner"
      @edit="handleEditApp"
      @delete="handleDeleteApp"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, watch, computed, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-dark.css'
import AppDetailModal from '@/components/AppDetailModal.vue'
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
  DownloadOutlined,
} from '@ant-design/icons-vue'
import { getAppById, deployApp, deleteApp } from '@/api/appController'
import { downloadFile } from '@/request'
import { listAppChatHistory } from '@/api/chatHistoryController'
import { useLoginUserStore } from '@/stores/loginUser'
import CodeGenTypeTag from '@/components/CodeGenTypeTag.vue'
import { VisualEditor, type ElementInfo } from '@/utils/visualEditor'

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

// 下载相关
const downloading = ref(false)

// 可视化编辑相关
const isEditMode = ref(false)
const selectedElementInfo = ref<ElementInfo | null>(null)
const visualEditor = new VisualEditor({
  onElementSelected: (elementInfo: ElementInfo) => {
    selectedElementInfo.value = elementInfo
    message.success(`已选中元素：${elementInfo.tagName.toLowerCase()}`)
  },
})

// ==================== 权限控制 ====================
const isOwner = ref(false) // 是否是应用的所有者（本人或管理员）

// ==================== 对话历史相关 ====================
const hasMoreHistory = ref(true) // 是否还有更多历史消息可加载
const loadingHistory = ref(false) // 是否正在加载历史消息
const lastCreateTime = ref<string>('') // 游标：最后一条消息的创建时间，用于分页加载

// ==================== 应用详情悬浮窗 ====================
const showDetailModal = ref(false) // 是否显示应用详情弹窗

// ==================== 拖拽相关 ====================
const chatWidth = ref(500) // 对话区域宽度
const previewWidth = ref(0) // 预览区域宽度
const isResizing = ref(false) // 是否正在拖拽调整大小
const startX = ref(0) // 拖拽开始时的鼠标 X 坐标
const startChatWidth = ref(0) // 拖拽开始时的对话区域宽度
const mainContainer = ref<HTMLElement | null>(null) // 主容器引用

// ==================== 滚动控制 ====================
/**
 * 滚动到消息列表底部
 * 用于新消息到达时自动滚动
 */
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// ==================== 可视化编辑相关 ====================
/**
 * 切换编辑模式
 */
const toggleEditMode = () => {
  if (!previewFrame.value) {
    message.warning('请等待页面加载完成')
    return
  }

  const newEditMode = visualEditor.toggleEditMode()
  isEditMode.value = newEditMode

  if (newEditMode) {
    message.info('已进入编辑模式，鼠标悬浮在元素上可查看，点击选中元素')
  } else {
    clearSelectedElement()
  }
}

/**
 * 清除选中的元素
 */
const clearSelectedElement = () => {
  selectedElementInfo.value = null
  visualEditor.clearSelection()
}

// ==================== Markdown 渲染 ====================
/**
 * 渲染 Markdown 内容
 * @param content Markdown 格式的内容
 * @returns 渲染后的 HTML
 *
 * 功能说明：
 * 1. 支持代码高亮（使用 highlight.js）
 * 2. 自动转义 HTML 特殊字符
 * 3. 支持多种编程语言的语法高亮
 */
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

// ==================== 加载应用信息 ====================
/**
 * 加载应用详细信息
 * 1. 获取应用 ID
 * 2. 调用后端接口获取应用信息
 * 3. 判断当前用户是否是应用的所有者
 */
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
      if (loginUserStore.loginUser.id && appInfo.value.userId === loginUserStore.loginUser.id) {
        isOwner.value = true
      } else if (loginUserStore.loginUser.userRole === 'admin') {
        isOwner.value = true
      } else {
        isOwner.value = false
      }

      console.log('isOwner:', isOwner.value)
    } else {
      message.error('应用不存在')
      router.push('/')
    }
  } catch (error) {
    console.error(error)
    message.error('加载应用信息失败')
  }
}

// ==================== 加载对话历史 ====================
/**
 * 加载对话历史消息
 * @param loadMore 是否是加载更多（true：加载下一页；false：首次加载）
 *
 * 功能说明：
 * 1. 首次加载：获取最近 10 条消息
 * 2. 加载更多：使用游标（lastCreateTime）获取更早的消息
 * 3. 消息按创建时间升序排列
 * 4. 自动判断是否还有更多历史
 */
const loadChatHistory = async (loadMore: boolean = false) => {
  if (!appId.value) {
    return
  }

  loadingHistory.value = true
  try {
    const params: API.listAppChatHistoryParams = {
      appId: appId.value,
      pageSize: 10,
    }

    // 如果是加载更多，传入上一页的最后一条消息的创建时间
    if (loadMore && lastCreateTime.value) {
      params.lastCreateTime = lastCreateTime.value
    }

    const res = await listAppChatHistory(params)
    if (res.data.code === 0 && res.data.data) {
      const historyData = res.data.data
      const records = historyData.records || []

      if (records.length > 0) {
        // 转换历史消息格式
        const historyMessages: ChatMessage[] = records
          .map((record) => ({
            role: record.messageType === 'user' ? 'user' : 'ai',
            content: record.message || '',
          }))
          .reverse() // 因为后端返回的是倒序，需要反转

        if (loadMore) {
          // 加载更多：插入到消息列表前面
          messages.value = [...historyMessages, ...messages.value]
        } else {
          // 首次加载：直接设置消息列表
          messages.value = historyMessages
        }

        // 更新游标（最早的一条消息的创建时间）
        lastCreateTime.value = records[records.length - 1].createTime || ''

        // 判断是否还有更多历史
        hasMoreHistory.value = records.length === 10

        // 滚动到底部（仅首次加载）
        if (!loadMore) {
          scrollToBottom()
        }
      } else {
        hasMoreHistory.value = false
      }

      console.log('对话历史加载成功，消息数量:', records.length)
    }
  } catch (error) {
    console.error('加载对话历史失败:', error)
    message.error('加载对话历史失败')
  } finally {
    loadingHistory.value = false
  }
}

// ==================== 发送消息 ====================
/**
 * 发送用户消息并获取 AI 响应
 *
 * 功能说明：
 * 1. 添加用户消息到消息列表
 * 2. 使用 SSE（Server-Sent Events）流式传输获取 AI 响应
 * 3. 实时更新 AI 消息内容
 * 4. 生成完成后自动显示预览
 */
const handleSendMessage = async () => {
  if (!userInput.value.trim()) {
    message.warning('请输入消息内容')
    return
  }

  if (!appInfo.value.id) {
    message.error('应用信息加载失败')
    return
  }

  let finalMessage = userInput.value
  if (selectedElementInfo.value) {
    let elementContext = `\n\n选中元素信息：`
    if (selectedElementInfo.value.pagePath) {
      elementContext += `\n- 页面路径: ${selectedElementInfo.value.pagePath}`
    }
    elementContext += `\n- 标签: ${selectedElementInfo.value.tagName.toLowerCase()}\n- 选择器: ${selectedElementInfo.value.selector}`
    if (selectedElementInfo.value.textContent) {
      elementContext += `\n- 当前内容: ${selectedElementInfo.value.textContent.substring(0, 100)}`
    }
    finalMessage += elementContext
  }

  // 添加用户消息
  messages.value.push({
    role: 'user',
    content: userInput.value,
  })

  const userMessage = finalMessage
  userInput.value = ''

  // 发送消息后，清除选中元素并退出编辑模式
  if (selectedElementInfo.value) {
    clearSelectedElement()
    if (isEditMode.value) {
      toggleEditMode()
    }
  }

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
        console.log('SSE 原始数据:', event.data)
        const parsed = JSON.parse(event.data)
        console.log('SSE 解析后的数据:', parsed)

        // 后端返回的格式可能包含多个字段
        // {"d": content} 或 {"content": content, "explanation": explanation}
        const content = parsed.d || parsed.content || ''

        // 如果有中文说明字段（假设字段名为 explanation 或 description）
        const explanation = parsed.explanation || parsed.description || parsed.cn || ''

        console.log('提取的内容:', content)
        console.log('提取的说明:', explanation)

        if (content || explanation) {
          // 拼接内容和中文说明
          let fullContent = aiContent
          if (explanation && !fullContent.includes(explanation)) {
            // 如果说明存在且未包含在内容中，添加到前面
            fullContent = `${explanation}\n\n${content}`
          } else {
            fullContent += content
          }

          aiContent = fullContent
          messages.value[aiMessageIndex]!.content = aiContent
          scrollToBottom()
        }
      } catch (e) {
        console.error('JSON 解析失败:', e)
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

// ==================== 文件选择 ====================
/**
 * 选择文件
 * @param file 选中的文件对象
 */
const selectFile = (file: any) => {
  console.log('选择文件:', file)
  message.info(`查看文件：${file.name}`)
}

// ==================== 部署应用 ====================
/**
 * 部署应用到线上环境
 *
 * 功能说明：
 * 1. 调用后端部署接口
 * 2. 获取部署后的 URL
 * 3. 更新应用信息
 */
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

// ==================== 打开已部署的 URL ====================
/**
 * 在新标签页中打开已部署的应用 URL
 */
const openDeployedUrl = () => {
  if (deployedUrl.value) {
    window.open(deployedUrl.value, '_blank')
  }
}

// ==================== 下载代码 ====================
/**
 * 下载应用代码（ZIP 格式）
 *
 * 功能说明：
 * 1. 调用后端下载接口
 * 2. 从响应头获取文件名
 * 3. 创建 Blob 对象并触发浏览器下载
 */
const handleDownloadCode = async () => {
  if (!appInfo.value.id) {
    message.error('应用信息加载失败')
    return
  }

  downloading.value = true

  try {
    // 使用 downloadFile 工具函数下载
    const result = await downloadFile(
      `/app/download/${appInfo.value.id}`,
      {},
      `${appInfo.value.appName || 'app'}.zip`,
    )

    if (result.success) {
      message.success(`代码下载成功：${result.filename}`)
    }
  } catch (error) {
    console.error('下载失败:', error)
    message.error('下载失败，请重试')
  } finally {
    downloading.value = false
  }
}

// ==================== 刷新预览 ====================
/**
 * 刷新预览 iframe
 * 用于重新加载生成的网页
 */
const refreshPreview = () => {
  if (previewFrame.value) {
    previewFrame.value.src = previewUrl.value
  }
}

// ==================== iframe 事件处理 ====================
/**
 * iframe 加载完成回调
 */
const handleFrameLoad = () => {
  console.log('iframe 加载完成')
  // 初始化可视化编辑器
  if (previewFrame.value) {
    visualEditor.init(previewFrame.value)
    visualEditor.onIframeLoad()
  }
}

/**
 * iframe 加载错误回调
 */
const handleFrameError = () => {
  console.error('iframe 加载失败')
  message.error('预览加载失败，请检查后端服务是否正常运行')
}

// ==================== 监听预览状态 ====================
/**
 * 监听预览状态变化
 */
watch(showPreview, (newVal) => {
  if (newVal) {
    console.log('预览已启用，URL:', previewUrl.value)
  }
})

// ==================== 导航操作 ====================
/**
 * 返回上一页
 */
const goBack = () => {
  router.back()
}

/**
 * 跳转到应用编辑页面
 */
const handleEditApp = () => {
  if (appInfo.value.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

/**
 * 删除应用
 * 删除成功后返回上一页
 */
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

// ==================== 拖拽调整面板大小 ====================
/**
 * 开始拖拽调整面板大小
 * @param e 鼠标事件
 *
 * 功能说明：
 * 1. 记录初始状态
 * 2. 添加鼠标移动和释放事件监听
 * 3. 设置拖拽样式
 */
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

/**
 * 拖拽过程中调整面板大小
 * @param e 鼠标事件
 *
 * 约束条件：
 * 1. 左侧对话区域：最小宽度 400px，最大宽度 60%
 * 2. 右侧预览区域：最小宽度 900px
 */
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

/**
 * 停止拖拽
 * 清理事件监听和样式
 */
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
  // 如果不是自己的应用，不自动发送
  if (!isOwner.value) {
    console.log('不是自己的应用，不自动发送消息')
    return
  }

  // 如果有对话历史，不自动发送
  if (messages.value.length > 0) {
    console.log('已有对话历史，不自动发送消息')
    return
  }

  // 如果有初始提示词，自动发送
  const initialPrompt = appInfo.value.initPrompt || (route.query.prompt as string)
  if (initialPrompt) {
    console.log('自动发送初始消息:', initialPrompt)
    userInput.value = initialPrompt
    await handleSendMessage()
  }
}

onMounted(async () => {
  await loadAppInfo()

  // 加载对话历史
  await loadChatHistory()

  // 初始化宽度
  nextTick(() => {
    if (mainContainer.value) {
      const containerWidth = mainContainer.value.offsetWidth
      chatWidth.value = 500
      previewWidth.value = containerWidth - 500 - 8
    }
  })

  // 如果有至少 2 条对话记录，展示网站预览
  if (messages.value.length >= 2 && appInfo.value.codeGenType) {
    showPreview.value = true
    const previewBaseUrl =
      import.meta.env.VITE_PREVIEW_BASE_URL || 'http://localhost:8200/api/static'
    previewUrl.value = `${previewBaseUrl}/${appInfo.value.codeGenType}_${appId.value}/`
    console.log('已有对话历史，自动展示预览:', previewUrl.value)
  }

  // 监听 iframe 消息
  window.addEventListener('message', (event) => {
    visualEditor.handleIframeMessage(event)
  })

  // 等待应用信息加载完后发送初始消息
  setTimeout(() => {
    sendInitialMessage()
  }, 500)
})

// 组件卸载时清理监听器
onUnmounted(() => {
  // 清理拖拽相关事件监听
  document.removeEventListener('mousemove', resize)
  document.removeEventListener('mouseup', stopResize)
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

/* 代码生成类型标签样式 */
.app-chat-page__app-info .code-gen-type-tag {
  margin-left: 4px;
  transform: translateY(-1px);
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

.app-chat-page__download-btn {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  border: none;
  color: #fff;
  border-radius: 20px;
  margin-left: 8px;
}

.app-chat-page__download-btn:hover {
  background: linear-gradient(135deg, #f5576c 0%, #f093fb 100%);
  transform: translateY(-2px);
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

.app-chat-page__load-more {
  text-align: center;
  padding: 12px 0;
}

.app-chat-page__load-more .ant-btn-link {
  color: #667eea;
  font-size: 14px;
}

.app-chat-page__load-more .ant-btn-link:hover {
  color: #764ba2;
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

/* 选中元素信息样式 */
.app-chat-page__element-alert {
  margin-bottom: 12px;
  border-radius: 8px;
}

.selected-element-info {
  line-height: 1.4;
}

.element-header {
  margin-bottom: 8px;
}

.element-details {
  margin-top: 8px;
}

.element-item {
  margin-bottom: 4px;
  font-size: 13px;
}

.element-item:last-child {
  margin-bottom: 0;
}

.element-tag {
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 14px;
  font-weight: 600;
  color: #007bff;
}

.element-id {
  color: #28a745;
  margin-left: 4px;
}

.element-class {
  color: #ffc107;
  margin-left: 4px;
}

.element-selector-code {
  font-family: 'Monaco', 'Menlo', monospace;
  background: #f6f8fa;
  padding: 2px 4px;
  border-radius: 3px;
  font-size: 12px;
  color: #d73a49;
  border: 1px solid #e1e4e8;
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

.app-chat-page__preview-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.app-chat-page__preview-actions .ant-btn-link {
  padding: 4px 12px;
  font-size: 13px;
  color: #667eea;
  border-radius: 12px;
  transition: all 0.3s;
}

.app-chat-page__preview-actions .ant-btn-link:hover {
  background: rgba(102, 126, 234, 0.08);
  color: #667eea;
}

.edit-mode-active {
  background-color: #52c41a !important;
  border-color: #52c41a !important;
  color: white !important;
}

.edit-mode-active:hover {
  background-color: #73d13d !important;
  border-color: #73d13d !important;
  color: white !important;
}

.app-chat-page__refresh-btn {
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
</style>
