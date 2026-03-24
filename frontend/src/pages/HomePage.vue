<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { addApp, listMyAppByPage, listFeaturedAppByPage } from '@/api/appController'
import {
  RocketOutlined,
  FireOutlined,
  FolderOutlined,
  StarOutlined,
  RightOutlined,
  SendOutlined,
  PaperClipOutlined,
  ThunderboltOutlined,
  SearchOutlined,
} from '@ant-design/icons-vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 提示词输入
const promptInput = ref('')
const isCreating = ref(false)
const codeGenType = ref('NoCode')

// 我的应用列表
const myApps = ref<API.AppVO[]>([])
const myAppsLoading = ref(false)
const myAppsPagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})
const myAppsSearchName = ref('')

// 精选应用列表
const featuredApps = ref<API.AppVO[]>([])
const featuredAppsLoading = ref(false)
const featuredAppsPagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})
const featuredAppsSearchName = ref('')

// 快捷模板
const quickTemplates = [
  { icon: '🛍️', text: '波普风电商页面' },
  { icon: '🏢', text: '企业网站' },
  { icon: '📊', text: '电商运营后台' },
  { icon: '💬', text: '暗黑话题社区' },
]

// 创建应用
const handleCreateApp = async () => {
  if (!promptInput.value.trim()) {
    message.warning('请输入提示词')
    return
  }

  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    router.push('/user/login')
    return
  }

  try {
    isCreating.value = true
    const res = await addApp({
      initPrompt: promptInput.value,
    })

    if (res.data.code === 0 && res.data.data) {
      const appId = res.data.data
      message.success('应用创建成功，开始生成')
      await router.push(`/app/chat/${appId}?prompt=${encodeURIComponent(promptInput.value)}`)
    } else {
      message.error(res.data.message || '创建应用失败')
    }
  } catch (error) {
    console.error(error)
    message.error('创建应用失败，请重试')
  } finally {
    isCreating.value = false
  }
}

// 加载我的应用
const loadMyApps = async () => {
  if (!loginUserStore.loginUser.id) {
    return
  }

  myAppsLoading.value = true
  try {
    const res = await listMyAppByPage({
      pageNum: myAppsPagination.current,
      pageSize: myAppsPagination.pageSize,
      appName: myAppsSearchName.value,
    })

    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records || []
      myAppsPagination.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error(error)
  } finally {
    myAppsLoading.value = false
  }
}

// 我的应用搜索
const handleMyAppsSearch = () => {
  myAppsPagination.current = 1
  loadMyApps()
}

// 我的应用分页变化
const handleMyAppsPageChange = (page: number) => {
  myAppsPagination.current = page
  loadMyApps()
}

// 加载精选应用
const loadFeaturedApps = async () => {
  featuredAppsLoading.value = true
  try {
    const res = await listFeaturedAppByPage({
      pageNum: featuredAppsPagination.current,
      pageSize: featuredAppsPagination.pageSize,
      appName: featuredAppsSearchName.value,
    })

    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.records || []
      featuredAppsPagination.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error(error)
  } finally {
    featuredAppsLoading.value = false
  }
}

// 精选应用搜索
const handleFeaturedAppsSearch = () => {
  featuredAppsPagination.current = 1
  loadFeaturedApps()
}

// 精选应用分页变化
const handleFeaturedAppsPageChange = (page: number) => {
  featuredAppsPagination.current = page
  loadFeaturedApps()
}

// 使用快捷模板
const useTemplate = (template: string) => {
  promptInput.value = template
}

// 跳转到应用详情
const goToAppChat = (appId: string) => {
  router.push(`/app/chat/${appId}`)
}

onMounted(() => {
  loadMyApps()
  loadFeaturedApps()
})
</script>

<template>
  <div class="home-page">
    <!-- 头部区域 -->
    <div class="home-page__hero">
      <div class="home-page__hero-bg">
        <div class="home-page__hero-gradient"></div>
      </div>

      <div class="home-page__hero-content">
        <h1 class="home-page__title">
          <span class="home-page__title-text">一句话</span>
          <div class="home-page__logo-wrapper">
            <img class="home-page__logo" src="@/assets/logo.svg" alt="logo" />
          </div>
          <span class="home-page__title-text">呈所想</span>
        </h1>
        <p class="home-page__subtitle">
          <span class="home-page__subtitle-icon">🔥</span>
          与 AI 对话轻松创建应用和网站
          <span class="home-page__subtitle-icon">🔥</span>
        </p>

        <!-- 提示词输入框 -->
        <div class="home-page__input-card">
          <div class="home-page__input-wrapper">
            <a-textarea
              v-model:value="promptInput"
              class="home-page__input"
              placeholder="用 NoCode 创建一个数据分析看板，用来分析......"
              :auto-size="{ minRows: 4, maxRows: 6 }"
              :disabled="isCreating"
              @press-enter="handleCreateApp"
            />
          </div>
          <div class="home-page__input-actions">
            <div class="home-page__input-left">
              <a-button class="home-page__action-btn" disabled> 上传</a-button>
              <a-button class="home-page__action-btn" disabled> 优化</a-button>
            </div>
            <a-button
              type="primary"
              class="home-page__submit-btn"
              :loading="isCreating"
              @click="handleCreateApp"
            >
              开始创建
            </a-button>
          </div>
        </div>

        <!-- 快捷模板 -->
        <div class="home-page__templates">
          <span class="home-page__templates-label">快速开始：</span>
          <a-button
            v-for="template in quickTemplates"
            :key="template.text"
            class="home-page__template-btn"
            @click="useTemplate(template.text)"
          >
            <span class="home-page__template-icon">{{ template.icon }}</span>
            {{ template.text }}
          </a-button>
        </div>
      </div>
    </div>

    <!-- 我的作品 -->
    <div v-if="loginUserStore.loginUser.id" class="home-page__section">
      <div class="home-page__section-header">
        <div class="home-page__section-title-wrapper">
          <span class="home-page__section-icon">📁</span>
          <h2 class="home-page__section-title">我的作品</h2>
        </div>
        <div class="home-page__search-box">
          <a-input
            v-model:value="myAppsSearchName"
            class="home-page__search-input"
            placeholder="搜索应用名称"
            allow-clear
            @press-enter="handleMyAppsSearch"
          >
            <template #suffix>
              <SearchOutlined @click="handleMyAppsSearch" style="cursor: pointer" />
            </template>
          </a-input>
        </div>
      </div>

      <a-spin :spinning="myAppsLoading" tip="加载中...">
        <div v-if="myApps.length > 0" class="home-page__apps-grid">
          <div
            v-for="app in myApps"
            :key="app.id"
            class="home-page__app-card"
            @click="goToAppChat(app.id!)"
          >
            <div class="home-page__app-cover">
              <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
              <div v-else class="home-page__app-placeholder">
                <span class="home-page__app-placeholder-icon">🚀</span>
              </div>
              <div class="home-page__app-overlay">
                <span class="home-page__app-overlay-text">点击查看</span>
              </div>
            </div>
            <div class="home-page__app-info">
              <h3 class="home-page__app-name">{{ app.appName || '未命名应用' }}</h3>
              <p class="home-page__app-time">
                <span class="home-page__app-time-dot"></span>
                创建于 {{ new Date(app.createTime!).toLocaleDateString() }}
              </p>
            </div>
          </div>
        </div>
        <a-empty v-else description="暂无作品，快去创建一个吧" />

        <!-- 分页 -->
        <div v-if="myApps.length > 0" class="home-page__pagination">
          <a-pagination
            v-model:current="myAppsPagination.current"
            :total="myAppsPagination.total"
            :page-size="myAppsPagination.pageSize"
            show-less-items
            @change="handleMyAppsPageChange"
          />
        </div>
      </a-spin>
    </div>

    <!-- 案例广场 -->
    <div class="home-page__section home-page__section--featured">
      <div class="home-page__section-header">
        <div class="home-page__section-title-wrapper">
          <span class="home-page__section-icon home-page__section-icon--star">⭐</span>
          <h2 class="home-page__section-title">案例广场</h2>
        </div>
        <div class="home-page__search-box">
          <a-input
            v-model:value="featuredAppsSearchName"
            class="home-page__search-input"
            placeholder="搜索应用名称"
            allow-clear
            @press-enter="handleFeaturedAppsSearch"
          >
            <template #suffix>
              <SearchOutlined @click="handleFeaturedAppsSearch" style="cursor: pointer" />
            </template>
          </a-input>
        </div>
      </div>

      <div class="home-page__categories">
        <a-radio-group button-style="solid">
          <a-radio-button value="all">全部</a-radio-button>
          <a-radio-button value="tool">工具</a-radio-button>
          <a-radio-button value="website">网站</a-radio-button>
          <a-radio-button value="analysis">数据分析</a-radio-button>
          <a-radio-button value="activity">活动页面</a-radio-button>
          <a-radio-button value="manage">管理平台</a-radio-button>
        </a-radio-group>
      </div>

      <a-spin :spinning="featuredAppsLoading" tip="加载中...">
        <div v-if="featuredApps.length > 0" class="home-page__apps-grid">
          <div
            v-for="app in featuredApps"
            :key="app.id"
            class="home-page__app-card home-page__app-card--featured"
            @click="goToAppChat(app.id!)"
          >
            <div class="home-page__app-cover">
              <img v-if="app.cover" :src="app.cover" :alt="app.appName" />
              <div v-else class="home-page__app-placeholder">
                <RocketOutlined class="home-page__app-placeholder-icon" />
              </div>
              <div class="home-page__app-overlay">
                <span class="home-page__app-overlay-text">查看案例</span>
              </div>
            </div>
            <div class="home-page__app-info">
              <h3 class="home-page__app-name">{{ app.appName || '未命名应用' }}</h3>
              <p class="home-page__app-user">
                <a-avatar :size="20" :src="app.user?.userAvatar" class="home-page__app-avatar" />
                <span class="home-page__app-user-name">{{ app.user?.userName || '未知用户' }}</span>
              </p>
            </div>
          </div>
        </div>
        <a-empty v-else description="暂无精选应用" />

        <!-- 分页 -->
        <div v-if="featuredApps.length > 0" class="home-page__pagination">
          <a-pagination
            v-model:current="featuredAppsPagination.current"
            :total="featuredAppsPagination.total"
            :page-size="featuredAppsPagination.pageSize"
            show-less-items
            @change="handleFeaturedAppsPageChange"
          />
        </div>
      </a-spin>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
}

.home-page__hero {
  position: relative;
  padding: 80px 24px 60px;
  text-align: center;
  overflow: hidden;
}

.home-page__hero-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
}

.home-page__hero-gradient {
  position: absolute;
  width: 200%;
  height: 200%;
  top: -50%;
  left: -50%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, transparent 70%);
  animation: pulse 8s ease-in-out infinite;
}

@keyframes pulse {
  0%,
  100% {
    transform: scale(1) rotate(0deg);
  }
  50% {
    transform: scale(1.1) rotate(10deg);
  }
}

.home-page__hero-content {
  position: relative;
  z-index: 1;
  max-width: 900px;
  margin: 0 auto;
}

.home-page__title {
  font-size: 56px;
  font-weight: 800;
  color: #fff;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  text-shadow: 0 2px 20px rgba(0, 0, 0, 0.2);
  animation: fadeInDown 0.8s ease-out;
}

.home-page__title-text {
  background: linear-gradient(135deg, #fff 0%, rgba(255, 255, 255, 0.9) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.home-page__logo-wrapper {
  width: 64px;
  height: 64px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  animation: float 3s ease-in-out infinite;
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

.home-page__logo {
  width: 40px;
  height: 40px;
}

.home-page__subtitle {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.95);
  margin-bottom: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  animation: fadeInUp 0.8s ease-out 0.2s both;
}

.home-page__subtitle-icon {
  color: #ffd700;
  animation: glow 2s ease-in-out infinite;
}

@keyframes glow {
  0%,
  100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.8;
    transform: scale(1.1);
  }
}

.home-page__input-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 24px;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.15);
  margin-bottom: 24px;
  animation: fadeInUp 0.8s ease-out 0.4s both;
  transition: all 0.3s ease;
}

.home-page__input-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 50px rgba(0, 0, 0, 0.2);
}

.home-page__input-wrapper {
  margin-bottom: 16px;
}

.home-page__input {
  border: 2px solid rgba(102, 126, 234, 0.2);
  border-radius: 16px;
  padding: 16px;
  font-size: 16px;
  transition: all 0.3s;
  resize: none;
}

.home-page__input:hover {
  border-color: rgba(102, 126, 234, 0.4);
}

.home-page__input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.home-page__input-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.home-page__input-left {
  display: flex;
  gap: 12px;
}

.home-page__action-btn {
  border-radius: 20px;
  background: rgba(102, 126, 234, 0.05);
  border: 1px solid rgba(102, 126, 234, 0.2);
  color: #667eea;
  transition: all 0.3s;
}

.home-page__action-btn:hover:not(:disabled) {
  background: rgba(102, 126, 234, 0.1);
  border-color: #667eea;
}

.home-page__submit-btn {
  height: 44px;
  padding: 0 32px;
  border-radius: 22px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  font-size: 16px;
  font-weight: 600;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
  transition: all 0.3s;
}

.home-page__submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}

.home-page__templates {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
  animation: fadeInUp 0.8s ease-out 0.6s both;
}

.home-page__templates-label {
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  font-weight: 500;
}

.home-page__template-btn {
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: #fff;
  font-size: 14px;
  transition: all 0.3s;
}

.home-page__template-btn:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: translateY(-2px);
}

.home-page__template-icon {
  margin-right: 6px;
}

.home-page__section {
  background: #f8f9fa;
  padding: 60px 24px;
  position: relative;
}

.home-page__section--featured {
  background: #fff;
}

.home-page__section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
  max-width: 1400px;
  margin-left: auto;
  margin-right: auto;
}

.home-page__search-box {
  flex-shrink: 0;
}

.home-page__search-input {
  width: 240px;
  border-radius: 20px;
  border: 2px solid rgba(102, 126, 234, 0.2);
  transition: all 0.3s;
}

.home-page__search-input:hover {
  border-color: rgba(102, 126, 234, 0.4);
}

.home-page__search-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.home-page__pagination {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}

.home-page__section-title-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
}

.home-page__section-icon {
  font-size: 28px;
  color: #667eea;
}

.home-page__section-icon--star {
  color: #ffd700;
}

.home-page__section-title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}

.home-page__more-btn {
  font-size: 15px;
  color: #667eea;
  font-weight: 500;
}

.home-page__categories {
  margin-bottom: 32px;
  max-width: 1400px;
  margin-left: auto;
  margin-right: auto;
}

.home-page__apps-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.home-page__app-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.home-page__app-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
}

.home-page__app-card--featured {
  border: 2px solid transparent;
}

.home-page__app-card--featured:hover {
  border-color: #ffd700;
}

.home-page__app-cover {
  width: 100%;
  height: 180px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

.home-page__app-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s;
}

.home-page__app-card:hover .home-page__app-cover img {
  transform: scale(1.05);
}

.home-page__app-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.3) 0%, rgba(118, 75, 162, 0.3) 100%);
}

.home-page__app-placeholder-icon {
  font-size: 64px;
  color: rgba(255, 255, 255, 0.6);
}

.home-page__app-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.home-page__app-card:hover .home-page__app-overlay {
  opacity: 1;
}

.home-page__app-overlay-text {
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  background: rgba(102, 126, 234, 0.9);
  padding: 8px 20px;
  border-radius: 20px;
  backdrop-filter: blur(10px);
}

.home-page__app-info {
  padding: 20px;
}

.home-page__app-name {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0 0 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.home-page__app-time {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.5);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 6px;
}

.home-page__app-time-dot {
  width: 6px;
  height: 6px;
  background: #667eea;
  border-radius: 50%;
}

.home-page__app-user {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.5);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.home-page__app-avatar {
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.home-page__app-user-name {
  font-weight: 500;
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 768px) {
  .home-page__hero {
    padding: 60px 16px 40px;
  }

  .home-page__title {
    font-size: 36px;
    flex-wrap: wrap;
  }

  .home-page__logo-wrapper {
    width: 48px;
    height: 48px;
  }

  .home-page__logo {
    width: 30px;
    height: 30px;
  }

  .home-page__input-card {
    padding: 16px;
  }

  .home-page__input-actions {
    flex-direction: column;
    gap: 12px;
  }

  .home-page__submit-btn {
    width: 100%;
  }

  .home-page__apps-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 16px;
  }

  .home-page__section {
    padding: 40px 16px;
  }
}
</style>
