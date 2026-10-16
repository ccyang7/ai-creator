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
      <div class="app-chat-page__header-center">
        <a-radio-group
          v-model:value="workspaceMode"
          class="app-chat-page__view-switcher"
          button-style="solid"
          size="small"
          @change="handleWorkspaceModeChange"
        >
          <a-radio-button value="chat">
            <MessageOutlined />
            <span>对话</span>
          </a-radio-button>
          <a-radio-button value="preview">
            <EyeOutlined />
            <span>预览</span>
          </a-radio-button>
          <a-radio-button value="split" class="app-chat-page__split-option">
            <AppstoreOutlined />
            <span>分屏</span>
          </a-radio-button>
        </a-radio-group>
      </div>
      <div class="app-chat-page__header-right">
        <a-button class="app-chat-page__detail-btn" @click="showDetailModal = true">
          <InfoCircleOutlined />
          应用详情
        </a-button>
        <a-button
          v-if="isOwner && !appInfo.deployKey"
          type="primary"
          class="app-chat-page__deploy-btn"
          :loading="deploying"
          @click="handleDeploy"
        >
          <CloudUploadOutlined />
          部署
        </a-button>
        <a-button v-else-if="isOwner && appInfo.deployKey" class="app-chat-page__deployed-btn" disabled>
          <CheckCircleOutlined />
          已部署
        </a-button>
        <a-button
          v-if="isOwner"
          class="app-chat-page__download-btn"
          :loading="downloading"
          @click="handleDownloadCode"
        >
          <DownloadOutlined />
          下载代码
        </a-button>
        <a-button
          v-if="appInfo.deployKey"
          class="app-chat-page__preview-btn"
          @click="openDeployedUrl"
        >
          <EyeOutlined />
          访问已部署网站
        </a-button>
      </div>
    </div>

    <!-- 核心内容区域 -->
    <div
      ref="mainContainer"
      :class="['app-chat-page__main', `app-chat-page__main--${workspaceMode}`]"
    >
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
                  v-if="message.isStreaming"
                  class="app-chat-page__message-text app-chat-page__message-text--ai app-chat-page__message-text--streaming"
                >
                  {{ message.content }}
                </div>
                <div
                  v-else
                  class="app-chat-page__message-text app-chat-page__message-text--ai"
                  v-html="message.renderedHtml"
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
              @press-enter="handleInputPressEnter"
            />
          </div>
          <div class="app-chat-page__input-actions">
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
              v-if="showPreview"
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
            <a-spin v-if="buildStatus === 'building'" />
            <a-empty :description="previewPlaceholder" />
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
        <div v-if="workspaceMode === 'preview' && isOwner" class="app-chat-page__preview-composer">
          <a-textarea
            v-model:value="userInput"
            class="app-chat-page__preview-input"
            placeholder="继续描述你想修改的内容..."
            :auto-size="{ minRows: 1, maxRows: 2 }"
            :disabled="isLoading || !appInfo.id"
            @press-enter="handleInputPressEnter"
          />
          <a-button
            type="primary"
            class="app-chat-page__preview-send"
            :loading="isLoading"
            @click="handleSendMessage"
          >
            <SendOutlined />
            发送
          </a-button>
        </div>
      </div>
    </div>

    <!-- 应用详情悬浮窗 -->
    <AppDetailModal
      v-model:visible="showDetailModal"
      :app="appInfo"
      :show-actions="canManageApp"
      @edit="handleEditApp"
      @delete="handleDeleteApp"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import bash from 'highlight.js/lib/languages/bash'
import css from 'highlight.js/lib/languages/css'
import java from 'highlight.js/lib/languages/java'
import javascript from 'highlight.js/lib/languages/javascript'
import json from 'highlight.js/lib/languages/json'
import markdown from 'highlight.js/lib/languages/markdown'
import typescript from 'highlight.js/lib/languages/typescript'
import xml from 'highlight.js/lib/languages/xml'
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
  EditOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  InfoCircleOutlined,
  DownloadOutlined,
  MessageOutlined,
  AppstoreOutlined,
} from '@ant-design/icons-vue'
import {
  getAppById,
  deployApp,
  deleteApp,
  adminDeleteApp,
  getBuildStatus,
} from '@/api/appController'
import { downloadFile } from '@/request'
import { listAppChatHistory } from '@/api/chatHistoryController'
import { useLoginUserStore } from '@/stores/loginUser'
import CodeGenTypeTag from '@/components/CodeGenTypeTag.vue'
import { VisualEditor, type ElementInfo } from '@/utils/visualEditor'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const STREAM_FLUSH_INTERVAL = 50

hljs.registerLanguage('bash', bash)
hljs.registerLanguage('css', css)
hljs.registerLanguage('html', xml)
hljs.registerLanguage('java', java)
hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('json', json)
hljs.registerLanguage('markdown', markdown)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('vue', xml)
hljs.registerLanguage('xml', xml)

const escapeHtml = (content: string) =>
  content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')

const md = new MarkdownIt()
md.set({
  html: false,
  breaks: true,
  linkify: true,
  highlight: (content: string, language: string) => {
    if (language && hljs.getLanguage(language)) {
      try {
        return hljs.highlight(content, { language }).value
      } catch (error) {
        console.error('代码高亮失败:', error)
      }
    }
    return escapeHtml(content)
  },
})

// 应用信息
const appInfo = ref<API.AppVO>({})
const appId = ref<string>('')

// 消息列表
interface ChatFile {
  name: string
  path: string
  content?: string
}

interface ChatMessage {
  role: 'user' | 'ai'
  content: string
  renderedHtml?: string
  isStreaming?: boolean
  files?: ChatFile[]
}

const messages = ref<ChatMessage[]>([])
const userInput = ref('')
const isLoading = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)
let activeEventSource: EventSource | null = null
let disposeActiveStream: (() => void) | null = null
let flushActiveStream: (() => void) | null = null
let initialMessageTimer: number | undefined

// 代码生成相关
const showPreview = ref(false)
const previewUrl = ref('')
const previewFrame = ref<HTMLIFrameElement | null>(null)

// Vue 工程构建状态：VUE_PROJECT 生成的是 Vite 源码工程，服务端要跑一次 npm build
// 产出 dist 之后预览才有意义，因此生成结束后需要轮询构建状态
const buildStatus = ref<'none' | 'building' | 'success' | 'failed'>('none')
const buildMessage = ref('')
let buildPollingTimer: number | undefined
const BUILD_POLL_INTERVAL = 2000
const BUILD_POLL_TIMEOUT = 10 * 60 * 1000
// 构建触发与 done 事件之间有时间差，宽限期内出现 none 不视为"不会构建"
const BUILD_NONE_GRACE = 15000

const isVueProject = computed(() => appInfo.value.codeGenType === 'vue_project')

/**
 * 预览区占位文案：Vue 工程构建期间要给出明确进度，否则用户只看到一片空白
 */
const previewPlaceholder = computed(() => {
  if (buildStatus.value === 'building') {
    return buildMessage.value || '正在构建前端工程，请稍候...'
  }
  if (buildStatus.value === 'failed') {
    return `构建失败：${buildMessage.value || '请查看服务端日志后重新生成'}`
  }
  return '等待代码生成完成后自动展示'
})

const stopBuildPolling = () => {
  if (buildPollingTimer !== undefined) {
    window.clearTimeout(buildPollingTimer)
    buildPollingTimer = undefined
  }
}

/**
 * 拉取预览地址并挂载 iframe
 */
const openPreview = () => {
  showPreview.value = true
  const previewBaseUrl =
    import.meta.env.VITE_PREVIEW_BASE_URL || 'http://localhost:8200/api/static'
  previewUrl.value = `${previewBaseUrl}/${appInfo.value.codeGenType}_${appInfo.value.id}/`
  if (!hasUserSelectedWorkspaceMode.value) {
    workspaceMode.value = 'preview'
  }
  refreshPreview()
}

/**
 * 查询一次构建状态
 *
 * @returns 后端返回的状态值，查询失败时返回 null
 */
const fetchBuildStatus = async () => {
  try {
    const res = await getBuildStatus({ appId: String(appId.value) })
    const body = res.data
    buildMessage.value = body?.data?.message ?? ''
    return body?.data?.status ?? 'none'
  } catch (error) {
    console.error('查询构建状态失败:', error)
    buildMessage.value = '构建状态查询失败'
    return null
  }
}

/**
 * 轮询构建状态，直到成功、失败或超时
 *
 * @param silentSuccess 构建成功时是否使用更安静的提示（页面加载时无需重复"代码生成完成"）
 */
const pollBuildStatus = async (silentSuccess = false) => {
  const targetAppId = appId.value
  const startedAt = Date.now()
  buildStatus.value = 'building'
  if (!buildMessage.value) {
    buildMessage.value = '正在构建前端工程...'
  }

  const poll = async () => {
    // 切换了应用或页面已卸载时停止轮询
    if (appId.value !== targetAppId) return
    const status = await fetchBuildStatus()
    if (status === 'success') {
      buildStatus.value = 'success'
      stopBuildPolling()
      openPreview()
      message.success(silentSuccess ? '预览已就绪' : '代码生成完成，工程构建成功，已自动展示预览')
      return
    }
    if (status === 'failed') {
      buildStatus.value = 'failed'
      stopBuildPolling()
      message.error(`工程构建失败：${buildMessage.value || '请查看服务端日志'}`)
      return
    }
    if (status === 'none' && Date.now() - startedAt > BUILD_NONE_GRACE) {
      // 构建触发与 done 事件之间有极小的时间差，超过宽限期仍是 none 说明这次不会构建
      buildStatus.value = 'none'
      buildMessage.value = ''
      stopBuildPolling()
      return
    }
    if (Date.now() - startedAt > BUILD_POLL_TIMEOUT) {
      buildStatus.value = 'failed'
      buildMessage.value = '构建超时，请稍后刷新重试'
      message.error('工程构建超时')
      stopBuildPolling()
      return
    }
    buildStatus.value = 'building'
    buildPollingTimer = window.setTimeout(poll, BUILD_POLL_INTERVAL)
  }

  await poll()
}

/**
 * 页面加载时确认构建状态：已构建好就直接预览，构建中则继续轮询，
 * 从未构建或构建失败不自动挂 iframe
 */
const restorePreviewIfBuilt = async () => {
  if (!hasUserSelectedWorkspaceMode.value) {
    workspaceMode.value = 'preview'
  }
  const status = await fetchBuildStatus()
  if (status === 'success') {
    buildStatus.value = 'success'
    openPreview()
  } else if (status === 'building') {
    await pollBuildStatus(true)
  } else {
    buildStatus.value = status === 'failed' ? 'failed' : 'none'
  }
}

// 部署相关
const deployedUrl = ref('')
const deploying = ref(false)

// 下载相关
const downloading = ref(false)

type WorkspaceMode = 'chat' | 'preview' | 'split'

const WORKSPACE_MODE_STORAGE_KEY = `ai-creator:workspace-mode:${String(route.params.id || 'unknown')}`
const getStoredWorkspaceMode = (): WorkspaceMode | null => {
  try {
    const storedMode = window.localStorage.getItem(WORKSPACE_MODE_STORAGE_KEY)
    if (storedMode === 'chat' || storedMode === 'preview' || storedMode === 'split') {
      return storedMode
    }
  } catch {
    // localStorage 不可用时使用默认模式
  }
  return null
}

const storedWorkspaceMode = getStoredWorkspaceMode()
const workspaceMode = ref<WorkspaceMode>(
  window.innerWidth <= 900 && storedWorkspaceMode === 'split'
    ? 'preview'
    : storedWorkspaceMode || 'chat',
)
const hasUserSelectedWorkspaceMode = ref(storedWorkspaceMode !== null)

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
const isOwner = ref(false)
const isAdmin = computed(() => loginUserStore.loginUser.userRole === 'admin')
const canManageApp = computed(() => isOwner.value || isAdmin.value)

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
let scrollAnimationFrame: number | null = null
let scrollScheduled = false

const scrollToBottom = (force = true) => {
  const container = messagesContainer.value
  if (!container || document.hidden) {
    return
  }

  const distanceToBottom = container.scrollHeight - container.scrollTop - container.clientHeight
  if (!force && distanceToBottom > 120) {
    return
  }
  if (scrollScheduled) {
    return
  }

  scrollScheduled = true
  nextTick(() => {
    scrollAnimationFrame = window.requestAnimationFrame(() => {
      scrollScheduled = false
      scrollAnimationFrame = null
      if (messagesContainer.value && !document.hidden) {
        messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
      }
    })
  })
}

const handleWorkspaceModeChange = () => {
  hasUserSelectedWorkspaceMode.value = true
  try {
    window.localStorage.setItem(WORKSPACE_MODE_STORAGE_KEY, workspaceMode.value)
  } catch {
    // localStorage 不可用时不影响当前页面切换
  }

  if (workspaceMode.value !== 'split' && isEditMode.value) {
    visualEditor.toggleEditMode()
    isEditMode.value = false
    clearSelectedElement()
  }

  if (workspaceMode.value === 'split') {
    nextTick(syncPanelWidths)
  }
}

const syncPanelWidths = () => {
  const containerWidth = mainContainer.value?.offsetWidth || 0
  if (containerWidth <= 0) return

  const minChatWidth = 360
  const maxChatWidth = Math.max(minChatWidth, containerWidth * 0.6)
  chatWidth.value = Math.min(Math.max(chatWidth.value, minChatWidth), maxChatWidth)
  previewWidth.value = Math.max(0, containerWidth - chatWidth.value - 12)
}

const handleViewportResize = () => {
  if (window.innerWidth <= 900 && workspaceMode.value === 'split') {
    workspaceMode.value = 'preview'
    handleWorkspaceModeChange()
    return
  }
  syncPanelWidths()
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
    workspaceMode.value = 'split'
    hasUserSelectedWorkspaceMode.value = true
    try {
      window.localStorage.setItem(WORKSPACE_MODE_STORAGE_KEY, 'split')
    } catch {
      // localStorage 不可用时不影响编辑模式
    }
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
 * 流式阶段不调用此方法，避免对不断增长的内容重复解析和高亮。
 */
const renderMarkdown = (content: string) => {
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
    return false
  }

  try {
    const res = await getAppById({ id: appId.value })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      // 检查是否是当前用户的应用
      isOwner.value = Boolean(
        loginUserStore.loginUser.id && appInfo.value.userId === loginUserStore.loginUser.id,
      )

      const deployKey = appInfo.value.deployKey
      if (deployKey) {
        const deployBaseUrl = import.meta.env.VITE_DEPLOY_BASE_URL || 'http://localhost:88'
        deployedUrl.value = `${deployBaseUrl.replace(/\/$/, '')}/${encodeURIComponent(deployKey)}`
      } else {
        deployedUrl.value = ''
      }
      return true
    } else {
      message.error('应用不存在')
      router.push('/')
      return false
    }
  } catch (error) {
    console.error(error)
    message.error('加载应用信息失败')
    return false
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
          .map((record) => {
            const role: ChatMessage['role'] = record.messageType === 'user' ? 'user' : 'ai'
            const content = record.message || ''
            return {
              role,
              content,
              renderedHtml: role === 'ai' ? renderMarkdown(content) : undefined,
            }
          })
          .reverse() // 因为后端返回的是倒序，需要反转

        if (loadMore) {
          // 加载更多：插入到消息列表前面
          messages.value = [...historyMessages, ...messages.value]
        } else {
          // 首次加载：直接设置消息列表
          messages.value = historyMessages
        }

        // 更新游标（最早的一条消息的创建时间）
        lastCreateTime.value = records[records.length - 1]?.createTime || ''

        // 判断是否还有更多历史
        hasMoreHistory.value = records.length === 10

        // 滚动到底部（仅首次加载）
        if (!loadMore) {
          scrollToBottom()
        }
      } else {
        hasMoreHistory.value = false
      }
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
 * 读取工具调用的参数对象
 */
const extractSseContent = (data: string) => {
  try {
    const parsed = JSON.parse(data) as Record<string, unknown>
    const readText = (key: string) => (typeof parsed[key] === 'string' ? parsed[key] : '')
    const content = readText('content') || readText('d')
    if (content) {
      return content
    }

    const explanation = readText('explanation') || readText('description') || readText('cn')
    const code =
      readText('code') || readText('html') || readText('css') || readText('javascript')
    return explanation && code ? `${explanation}\n\n${code}` : explanation || code
  } catch {
    return data
  }
}

const closeActiveStream = () => {
  disposeActiveStream?.()
  disposeActiveStream = null
  flushActiveStream = null
  activeEventSource?.close()
  activeEventSource = null
}

const handleVisibilityChange = () => {
  if (!document.hidden) {
    flushActiveStream?.()
  }
}

const handleWindowMessage = (event: MessageEvent) => {
  visualEditor.handleIframeMessage(event)
}

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
  if (isLoading.value) return

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
    closeActiveStream()
    // 重新生成时丢弃上一轮的构建轮询，避免旧状态覆盖本轮
    stopBuildPolling()
    buildStatus.value = 'none'
    buildMessage.value = ''

    // 添加 AI 消息占位
    const aiMessageIndex = messages.value.length
    messages.value.push({
      role: 'ai',
      content: '',
      renderedHtml: '',
      isStreaming: true,
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
    activeEventSource = eventSource

    let aiContent = ''
    let pendingChunks: string[] = []
    let flushTimer: number | undefined
    let isDone = false

    const flushStream = () => {
      if (flushTimer !== undefined) {
        window.clearTimeout(flushTimer)
        flushTimer = undefined
      }
      if (pendingChunks.length === 0) {
        return
      }

      aiContent += pendingChunks.join('')
      pendingChunks = []
      const aiMessage = messages.value[aiMessageIndex]
      if (aiMessage) {
        aiMessage.content = aiContent
        scrollToBottom(false)
      }
    }

    const scheduleFlush = () => {
      if (document.hidden || flushTimer !== undefined) {
        return
      }
      flushTimer = window.setTimeout(flushStream, STREAM_FLUSH_INTERVAL)
    }

    const disposeStream = () => {
      if (flushTimer !== undefined) {
        window.clearTimeout(flushTimer)
        flushTimer = undefined
      }
      eventSource.close()
      if (activeEventSource === eventSource) {
        activeEventSource = null
      }
      if (flushActiveStream === flushStream) {
        flushActiveStream = null
      }
      if (disposeActiveStream === disposeStream) {
        disposeActiveStream = null
      }
    }

    flushActiveStream = flushStream
    disposeActiveStream = disposeStream

    // 处理自定义事件（包括 done 事件）
    eventSource.addEventListener('done', () => {
      isDone = true
      flushStream()
      const aiMessage = messages.value[aiMessageIndex]
      if (aiMessage) {
        aiMessage.isStreaming = false
        aiMessage.renderedHtml = renderMarkdown(aiContent)
      }
      disposeStream()
      isLoading.value = false

      // 生成完成后显示预览
      if (isVueProject.value) {
        // Vue 工程是可编译的源码工程，源码在浏览器里跑不起来，
        // 必须等服务端构建出 dist 之后预览才有意义
        if (!hasUserSelectedWorkspaceMode.value) {
          workspaceMode.value = 'preview'
        }
        message.success('代码生成完成，正在构建前端工程...')
        void pollBuildStatus()
      } else if (appInfo.value.codeGenType && aiContent.trim()) {
        showPreview.value = true
        const previewBaseUrl =
          import.meta.env.VITE_PREVIEW_BASE_URL || 'http://localhost:8200/api/static'
        previewUrl.value = `${previewBaseUrl}/${appInfo.value.codeGenType}_${appIdParam}/`
        message.success('代码生成完成，已自动展示预览')
        if (!hasUserSelectedWorkspaceMode.value) {
          workspaceMode.value = 'preview'
        }
        refreshPreview()
      } else if (!aiContent.trim()) {
        message.warning('AI 未生成任何内容')
      } else if (!appInfo.value.codeGenType) {
        message.warning('应用缺少 codeGenType，无法预览')
      }
    })

    // 处理普通消息
    eventSource.onmessage = (event) => {
      const contentToAdd = extractSseContent(event.data)
      if (contentToAdd) {
        pendingChunks.push(contentToAdd)
        scheduleFlush()
      }
    }

    eventSource.onerror = (error) => {
      console.error('SSE 错误:', error)
      if (!isDone) {
        disposeStream()
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

const handleInputPressEnter = (event: KeyboardEvent) => {
  if (event.shiftKey || event.isComposing) return
  event.preventDefault()
  void handleSendMessage()
}

// ==================== 文件选择 ====================
/**
 * 选择文件
 * @param file 选中的文件对象
 */
const selectFile = (file: ChatFile) => {
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
    window.open(deployedUrl.value, '_blank', 'noopener,noreferrer')
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
    const res = isAdmin.value
      ? await adminDeleteApp({ id: appInfo.value.id })
      : await deleteApp({ id: appInfo.value.id })
    if (res.data.code === 0 && res.data.data) {
      message.success('删除成功')
      showDetailModal.value = false
      router.back()
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
  if (workspaceMode.value !== 'split') return
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
 * 2. 右侧预览区域：根据容器宽度保留合理的最小空间
 */
const resize = (e: MouseEvent) => {
  if (!isResizing.value || workspaceMode.value !== 'split' || !mainContainer.value) return

  const diff = e.clientX - startX.value
  const newChatWidth = startChatWidth.value + diff
  const containerWidth = mainContainer.value.offsetWidth
  const maxChatWidth = containerWidth * 0.6
  const minPreviewWidth = Math.min(600, containerWidth * 0.45)
  const minChatWidth = 360

  // 限制最小和最大宽度
  if (newChatWidth >= minChatWidth && newChatWidth <= maxChatWidth) {
    const newPreviewWidth = containerWidth - newChatWidth - 12
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
    return
  }

  // 如果有对话历史，不自动发送
  if (messages.value.length > 0) {
    return
  }

  // 如果有初始提示词，自动发送
  const initialPrompt = appInfo.value.initPrompt || (route.query.prompt as string)
  if (initialPrompt) {
    userInput.value = initialPrompt
    await handleSendMessage()
  }
}

onMounted(async () => {
  const appLoaded = await loadAppInfo()
  if (!appLoaded) return

  // 加载对话历史
  await loadChatHistory()

  // 初始化宽度
  nextTick(syncPanelWidths)

  // 如果有至少 2 条对话记录，展示网站预览
  if (messages.value.length >= 2 && appInfo.value.codeGenType) {
    if (isVueProject.value) {
      // Vue 工程要先确认 dist 有没有构建好，否则预览只会是一片空白
      void restorePreviewIfBuilt()
    } else {
      showPreview.value = true
      const previewBaseUrl =
        import.meta.env.VITE_PREVIEW_BASE_URL || 'http://localhost:8200/api/static'
      previewUrl.value = `${previewBaseUrl}/${appInfo.value.codeGenType}_${appId.value}/`
      if (!hasUserSelectedWorkspaceMode.value) {
        workspaceMode.value = 'preview'
      }
    }
  }

  // 监听 iframe 消息和页面可见性变化
  window.addEventListener('message', handleWindowMessage)
  window.addEventListener('resize', handleViewportResize)
  document.addEventListener('visibilitychange', handleVisibilityChange)

  // 等待应用信息加载完后发送初始消息
  initialMessageTimer = window.setTimeout(() => {
    sendInitialMessage()
  }, 500)
})

// 组件卸载时清理监听器
onUnmounted(() => {
  closeActiveStream()
  stopBuildPolling()
  visualEditor.destroy()
  if (initialMessageTimer !== undefined) {
    window.clearTimeout(initialMessageTimer)
  }
  if (scrollAnimationFrame !== null) {
    window.cancelAnimationFrame(scrollAnimationFrame)
  }
  window.removeEventListener('message', handleWindowMessage)
  window.removeEventListener('resize', handleViewportResize)
  document.removeEventListener('visibilitychange', handleVisibilityChange)
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
  background: var(--ac-page-background);
}

.app-chat-page__header {
  height: 64px;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid rgba(203, 213, 225, 0.72);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 6px 20px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(12px);
  border-radius: 0 0 24px 24px;
  gap: 16px;
}

.app-chat-page__header-left {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.app-chat-page__header-center {
  display: flex;
  justify-content: center;
  margin-left: auto;
}

.app-chat-page__view-switcher {
  white-space: nowrap;
}

.app-chat-page__view-switcher :deep(.ant-radio-button-wrapper) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding-inline: 14px;
}

.app-chat-page__view-switcher :deep(.ant-radio-button-wrapper:first-child) {
  border-start-start-radius: 6px;
  border-end-start-radius: 6px;
}

.app-chat-page__view-switcher :deep(.ant-radio-button-wrapper:last-child) {
  border-start-end-radius: 6px;
  border-end-end-radius: 6px;
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
  border: 2px solid rgba(37, 99, 235, 0.2);
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
  flex: none;
}

.app-chat-page__detail-btn {
  background: rgba(37, 99, 235, 0.07);
  border: 1px solid rgba(37, 99, 235, 0.28);
  color: var(--ac-primary);
  border-radius: 20px;
  font-weight: 500;
  transition: all 0.3s;
}

.app-chat-page__detail-btn:hover {
  background: rgba(37, 99, 235, 0.12);
  border-color: var(--ac-primary);
  transform: translateY(-2px);
}

.app-chat-page__deploy-btn {
  background: linear-gradient(135deg, var(--ac-primary) 0%, var(--ac-primary-hover) 100%);
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
  background: linear-gradient(135deg, #0891b2 0%, #0e7490 100%);
  border: none;
  color: #fff;
  border-radius: 20px;
  margin-left: 8px;
}

.app-chat-page__download-btn:hover {
  background: linear-gradient(135deg, #0e7490 0%, #155e75 100%);
  transform: translateY(-2px);
}

.app-chat-page__preview-btn {
  margin-left: 8px;
  border-radius: 20px;
}

.app-chat-page__main {
  flex: 1;
  display: flex;
  gap: 0;
  padding: 20px;
  overflow: hidden;
  height: 100%;
  min-height: 0;
}

.app-chat-page__main--chat .app-chat-page__chat,
.app-chat-page__main--preview .app-chat-page__preview {
  width: 100% !important;
  flex: 1;
}

.app-chat-page__main--chat .app-chat-page__preview,
.app-chat-page__main--chat .app-chat-page__resizer,
.app-chat-page__main--preview .app-chat-page__chat,
.app-chat-page__main--preview .app-chat-page__resizer {
  display: none;
}

.app-chat-page__chat {
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 24px;
  overflow: hidden;
  border: 1px solid rgba(203, 213, 225, 0.72);
  box-shadow: 0 12px 36px rgba(30, 64, 175, 0.09);
  flex-shrink: 0;
  min-width: 360px;
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
  color: var(--ac-primary);
  font-size: 14px;
}

.app-chat-page__load-more .ant-btn-link:hover {
  color: var(--ac-primary-hover);
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
  background: linear-gradient(135deg, var(--ac-primary) 0%, var(--ac-primary-hover) 100%);
  color: #fff;
}

.app-chat-page__message-avatar--user {
  background: linear-gradient(135deg, #0891b2 0%, #0e7490 100%);
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
  background: linear-gradient(135deg, var(--ac-primary) 0%, var(--ac-primary-hover) 100%);
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

.app-chat-page__message-text--streaming {
  font-family: inherit;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
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
  background: rgba(37, 99, 235, 0.1);
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
  background: var(--ac-primary);
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
  border: 1px solid rgba(37, 99, 235, 0.28);
  border-radius: 16px;
  padding: 14px;
  font-size: 14px;
  resize: none;
  transition: all 0.3s;
}

.app-chat-page__input:hover {
  border-color: rgba(37, 99, 235, 0.46);
}

.app-chat-page__input:focus {
  border-color: var(--ac-primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.app-chat-page__input-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.app-chat-page__action-btn {
  border-radius: 20px;
  background: rgba(37, 99, 235, 0.05);
  border: 1px solid rgba(37, 99, 235, 0.2);
  color: var(--ac-primary);
  font-size: 13px;
  transition: all 0.3s;
}

.app-chat-page__action-btn:hover:not(:disabled) {
  background: rgba(37, 99, 235, 0.1);
  border-color: var(--ac-primary);
}

.app-chat-page__send-btn {
  height: 36px;
  padding: 0 24px;
  border-radius: 18px;
  background: linear-gradient(135deg, var(--ac-primary) 0%, var(--ac-primary-hover) 100%);
  border: none;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 2px 12px rgba(37, 99, 235, 0.26);
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
  background: rgba(37, 99, 235, 0.12);
}

.app-chat-page__resizer-line {
  width: 4px;
  height: 40px;
  background: rgba(37, 99, 235, 0.3);
  border-radius: 2px;
  transition: all 0.2s ease;
}

.app-chat-page__resizer:hover .app-chat-page__resizer-line {
  background: rgba(37, 99, 235, 0.78);
  height: 60px;
}

/* 预览区域样式 */
.app-chat-page__preview {
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 24px;
  overflow: hidden;
  border: 1px solid rgba(203, 213, 225, 0.72);
  box-shadow: 0 12px 36px rgba(30, 64, 175, 0.09);
  flex-shrink: 0;
  min-width: 0;
  height: 100%;
}

.app-chat-page__preview-header {
  height: 48px;
  background: linear-gradient(135deg, rgba(37, 99, 235, 0.035) 0%, rgba(8, 145, 178, 0.035) 100%);
  border-bottom: 1px solid rgba(37, 99, 235, 0.1);
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
  color: var(--ac-primary);
  border-radius: 12px;
  transition: all 0.3s;
}

.app-chat-page__preview-actions .ant-btn-link:hover {
  background: rgba(37, 99, 235, 0.08);
  color: var(--ac-primary);
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
  color: var(--ac-primary);
  border-radius: 12px;
  transition: all 0.3s;
}

.app-chat-page__refresh-btn:hover {
  background: rgba(37, 99, 235, 0.1);
}

.app-chat-page__preview-content {
  flex: 1;
  position: relative;
  background: #fafafa;
  overflow: hidden;
  padding: 0;
}

.app-chat-page__preview-composer {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #e8e8e8;
}

.app-chat-page__preview-input {
  flex: 1;
  min-width: 0;
  border-radius: 6px;
  resize: none;
}

.app-chat-page__preview-send {
  height: 40px;
  flex: none;
  border-radius: 6px;
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

@media (max-width: 900px) {
  .app-chat-page {
    height: calc(100vh - 64px);
    min-height: 620px;
  }

  .app-chat-page__header {
    height: auto;
    min-height: 64px;
    padding: 12px;
    align-items: flex-start;
    gap: 12px;
    flex-wrap: wrap;
  }

  .app-chat-page__app-info {
    min-width: 0;
  }

  .app-chat-page__app-name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .app-chat-page__header-right {
    flex-wrap: wrap;
    width: 100%;
    overflow-x: auto;
    padding-bottom: 2px;
  }

  .app-chat-page__header-center {
    order: 3;
    width: 100%;
    margin-left: 0;
  }

  .app-chat-page__view-switcher {
    display: flex;
    width: 100%;
  }

  .app-chat-page__view-switcher :deep(.ant-radio-button-wrapper) {
    flex: 1;
    justify-content: center;
  }

  .app-chat-page__split-option {
    display: none !important;
  }

  .app-chat-page__main {
    height: 100%;
    min-height: 0;
    overflow: hidden;
    padding: 12px;
  }

  .app-chat-page__chat,
  .app-chat-page__preview {
    width: 100% !important;
    min-width: 0;
    height: 100%;
    min-height: 0;
  }

  .app-chat-page__resizer {
    display: none;
  }

  .app-chat-page__preview-header {
    padding-right: 12px;
  }

  .app-chat-page__preview-composer {
    padding: 10px 12px;
  }
}
</style>
