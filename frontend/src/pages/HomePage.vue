<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { addApp, listMyAppByPage, listFeaturedAppByPage } from '@/api/appController'
import AppCard from '@/components/AppCard.vue'
import ParticleField from '@/components/ParticleField.vue'
import { PlusOutlined, RocketOutlined, SearchOutlined } from '@ant-design/icons-vue'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 提示词输入
const promptInput = ref('')
const promptInputRef = ref<{ focus?: () => void } | null>(null)
const isCreating = ref(false)
const activeLibrary = ref<'mine' | 'featured'>('featured')
const activeTemplateCategory = ref('recommended')

// 我的应用列表
const myApps = ref<API.AppVO[]>([])
const myAppsLoading = ref(false)
const myAppsPagination = reactive({
  current: 1,
  pageSize: 8,
  total: 0,
})
const myAppsSearchName = ref('')

// 精选应用列表
const featuredApps = ref<API.AppVO[]>([])
const featuredAppsLoading = ref(false)
const featuredAppsPagination = reactive({
  current: 1,
  pageSize: 8,
  total: 0,
})
const featuredAppsSearchName = ref('')

// 快捷模板
const quickTemplates = [
  {
    icon: '🏢',
    text: '企业官网',
    category: 'recommended',
    prompt:
      '请帮我创建一个轻量级的企业官网，包含首页、关于我们、产品服务、联系我们等页面。设计风格简洁专业，主色调为蓝色，要有响应式布局，适合中小企业展示公司形象和业务。',
  },
  {
    icon: '📄',
    text: '简单页面',
    category: 'recommended',
    prompt:
      '请帮我创建一个简单的单页面网站，包含头部导航、英雄区域、特性介绍、案例展示和页脚。设计风格现代简约，配色清新，适合个人作品集或小型产品介绍。',
  },
  {
    icon: '🛍️',
    text: '产品落地页',
    category: 'product',
    prompt:
      '请创建一个高转化的 SaaS 产品落地页，包含产品价值、核心功能、客户评价、定价方案、常见问题和明确的注册按钮。风格专业、留白充足、适配移动端。',
  },
  {
    icon: '📊',
    text: '数据看板',
    category: 'dashboard',
    prompt:
      '请创建一个数据分析后台，包含顶部筛选器、关键指标卡、趋势图、排行表格和最近活动。使用清晰的状态颜色，布局紧凑，适合桌面端运营人员使用。',
  },
  {
    icon: '🎨',
    text: '个人作品集',
    category: 'website',
    prompt:
      '请创建一个设计师个人作品集网站，包含个人介绍、精选作品、服务能力、客户评价和联系入口。视觉简洁有质感，突出项目图片和案例叙事。',
  },
  {
    icon: '📣',
    text: '活动页面',
    category: 'website',
    prompt:
      '请创建一个活动报名页面，包含活动主题、时间地点、嘉宾介绍、日程安排、报名表单和常见问题。视觉有活力但保持专业，移动端优先。',
  },
]

const templateCategories = [
  { key: 'recommended', label: '推荐' },
  { key: 'website', label: '网站' },
  { key: 'product', label: '产品' },
  { key: 'dashboard', label: '后台' },
]

const visibleQuickTemplates = computed(() =>
  quickTemplates.filter((template) => template.category === activeTemplateCategory.value),
)

// 创建应用
const handleCreateApp = async () => {
  if (isCreating.value) return

  const prompt = promptInput.value.trim()
  if (!prompt) {
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
      initPrompt: prompt,
    })

    if (res.data.code === 0 && res.data.data) {
      const appId = res.data.data
      message.success('应用创建成功，开始生成')
      await router.push(`/app/chat/${appId}`)
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

const handlePromptEnter = (event: KeyboardEvent) => {
  if (event.shiftKey || event.isComposing) return
  event.preventDefault()
  void handleCreateApp()
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
const useTemplate = (template: (typeof quickTemplates)[0]) => {
  promptInput.value = template.prompt
}

const focusPrompt = async () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
  await nextTick()
  promptInputRef.value?.focus?.()
}

const startWithRecommendedTemplate = async () => {
  const recommendedTemplate = quickTemplates[0]
  if (recommendedTemplate) promptInput.value = recommendedTemplate.prompt
  await focusPrompt()
}

const useFeaturedTemplate = async (app: API.AppVO) => {
  if (app.initPrompt) {
    promptInput.value = app.initPrompt
    await focusPrompt()
    return
  }
  if (app.id) goToAppChat(app.id, true)
}

// 跳转到应用详情
const goToAppChat = (appId: string, isFeatured: boolean = false) => {
  // 如果是精选应用（别人的作品），添加 view 参数，不自动发送消息
  if (isFeatured) {
    router.push(`/app/chat/${appId}?view=1`)
  } else {
    router.push(`/app/chat/${appId}`)
  }
}

// 处理查看对话
const handleViewChat = (appId: string, isFeatured: boolean = false) => {
  goToAppChat(appId, isFeatured)
}

// 处理查看部署
const handleViewDeploy = (deployKey: string) => {
  const deployBaseUrl = import.meta.env.VITE_DEPLOY_BASE_URL || 'http://localhost:88'
  const url = `${deployBaseUrl.replace(/\/$/, '')}/${encodeURIComponent(deployKey)}`
  window.open(url, '_blank', 'noopener,noreferrer')
}

onMounted(() => {
  if (loginUserStore.loginUser.id) {
    activeLibrary.value = 'mine'
  }
  loadMyApps()
  loadFeaturedApps()
})
</script>

<template>
  <div class="home-page">
    <!-- 头部区域 -->
    <div class="home-page__hero">
      <ParticleField />
      <div class="home-page__hero-content">
        <div class="home-page__eyebrow">AI CREATOR</div>
        <h1 class="home-page__title">描述需求，开始创建</h1>
        <p class="home-page__subtitle">从一个想法开始，生成、预览并持续完善你的前端应用。</p>

        <!-- 提示词输入框 -->
        <div class="home-page__input-card">
          <div class="home-page__input-wrapper">
            <a-textarea
              ref="promptInputRef"
              v-model:value="promptInput"
              class="home-page__input"
              placeholder="例如：创建一个面向独立开发者的产品发布页，包含定价、常见问题和联系表单..."
              :auto-size="{ minRows: 4, maxRows: 6 }"
              :disabled="isCreating"
              @press-enter="handlePromptEnter"
            />
          </div>
          <div class="home-page__input-actions">
            <a-button
              type="primary"
              class="home-page__submit-btn"
              :loading="isCreating"
              @click="handleCreateApp"
            >
              <RocketOutlined />
              开始创建
            </a-button>
          </div>
        </div>

        <!-- 快捷模板 -->
        <div class="home-page__template-categories" role="tablist" aria-label="模板分类">
          <button
            v-for="category in templateCategories"
            :key="category.key"
            type="button"
            :class="[
              'home-page__template-category',
              { 'home-page__template-category--active': activeTemplateCategory === category.key },
            ]"
            @click="activeTemplateCategory = category.key"
          >
            {{ category.label }}
          </button>
        </div>
        <div class="home-page__templates">
          <span class="home-page__templates-label">快速开始：</span>
          <a-button
            v-for="template in visibleQuickTemplates"
            :key="template.text"
            class="home-page__template-btn"
            @click="useTemplate(template)"
          >
            <span class="home-page__template-icon">{{ template.icon }}</span>
            {{ template.text }}
          </a-button>
        </div>
      </div>
    </div>

    <div v-if="loginUserStore.loginUser.id" class="home-page__library-nav">
      <div class="home-page__library-nav-inner">
        <div>
          <div class="home-page__library-label">项目工作区</div>
          <h2>继续创作或浏览案例</h2>
        </div>
        <a-radio-group v-model:value="activeLibrary" button-style="solid">
          <a-radio-button value="mine">我的作品 {{ myAppsPagination.total }}</a-radio-button>
          <a-radio-button value="featured">
            案例广场 {{ featuredAppsPagination.total }}
          </a-radio-button>
        </a-radio-group>
      </div>
    </div>

    <!-- 我的作品 -->
    <div
      v-if="loginUserStore.loginUser.id && activeLibrary === 'mine'"
      class="home-page__section"
    >
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
          <AppCard
            v-for="app in myApps"
            :key="app.id"
            :app="app"
            type="default"
            :show-actions="true"
            :is-featured="false"
            @view-chat="handleViewChat"
            @view-deploy="handleViewDeploy"
          />
        </div>
        <div v-else class="home-page__empty-state">
          <a-empty description="还没有作品，从一个想法开始吧" />
          <a-button type="primary" @click="focusPrompt">
            <PlusOutlined />
            创建第一个应用
          </a-button>
        </div>

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
    <div
      v-if="!loginUserStore.loginUser.id || activeLibrary === 'featured'"
      class="home-page__section home-page__section--featured"
    >
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

      <a-spin :spinning="featuredAppsLoading" tip="加载中...">
        <div v-if="featuredApps.length > 0" class="home-page__apps-grid">
          <AppCard
            v-for="app in featuredApps"
            :key="app.id"
            :app="app"
            type="featured"
            :show-actions="true"
            :is-featured="true"
            @view-chat="handleViewChat"
            @view-deploy="handleViewDeploy"
            @use-template="useFeaturedTemplate"
          />
        </div>
        <div v-else class="home-page__empty-state">
          <a-empty description="精选案例正在准备中" />
          <a-button type="primary" @click="startWithRecommendedTemplate">
            <RocketOutlined />
            用模板开始创建
          </a-button>
        </div>

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
  width: 100%;
  overflow-x: hidden;
  background: var(--ac-page-background);
  position: relative;
}

.home-page__hero {
  position: relative;
  min-height: 436px;
  padding: 52px 24px 42px;
  text-align: center;
  overflow: hidden;
  background-color: var(--ac-surface-soft);
  background-image:
    linear-gradient(rgba(37, 99, 235, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(37, 99, 235, 0.035) 1px, transparent 1px),
    radial-gradient(circle at 16% 8%, rgba(37, 99, 235, 0.12), transparent 32%),
    radial-gradient(circle at 86% 18%, rgba(8, 145, 178, 0.1), transparent 28%);
  background-size: 32px 32px, 32px 32px, auto, auto;
  border-bottom: 1px solid #dbe3ef;
}

.home-page__hero-content {
  position: relative;
  z-index: 1;
  max-width: 860px;
  margin: 0 auto;
  min-width: 0;
}

.home-page__eyebrow {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  margin-bottom: 10px;
  padding: 0 9px;
  color: #1d4ed8;
  background: rgba(239, 246, 255, 0.86);
  border: 1px solid #bfdbfe;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
}

.home-page__title {
  margin: 0 0 10px;
  color: #111827;
  font-size: 36px;
  font-weight: 700;
  letter-spacing: 0;
}

.home-page__subtitle {
  margin: 0 0 28px;
  color: #6b7280;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.home-page__input-card {
  margin-bottom: 18px;
  padding: 18px;
  background: #fff;
  border: 1px solid #cbd8e8;
  border-radius: 8px;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.09);
}

.home-page__input-card:hover {
  border-color: #b9c8de;
}

.home-page__input-wrapper {
  margin-bottom: 16px;
}

.home-page__input {
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  padding: 16px;
  font-size: 16px;
  transition: all 0.3s;
  resize: none;
}

.home-page__input:hover {
  border-color: rgba(37, 99, 235, 0.4);
}

.home-page__input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}

.home-page__input-actions {
  display: flex;
  justify-content: flex-end;
  align-items: center;
}

.home-page__submit-btn {
  height: 42px;
  padding: 0 24px;
  border-radius: 6px;
  background: #2563eb;
  border: none;
  font-size: 16px;
  font-weight: 600;
  box-shadow: none;
  transition: all 0.3s;
}

.home-page__submit-btn:hover:not(:disabled) {
  background: #1d4ed8;
}

.home-page__templates {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  flex-wrap: wrap;
}

.home-page__template-categories {
  display: inline-flex;
  gap: 4px;
  margin-bottom: 10px;
  padding: 3px;
  border: 1px solid #dbe3ef;
  border-radius: 7px;
  background: rgba(255, 255, 255, 0.72);
}

.home-page__template-category {
  min-height: 28px;
  padding: 0 12px;
  border: 0;
  border-radius: 5px;
  background: transparent;
  color: #64748b;
  font: inherit;
  font-size: 13px;
  cursor: pointer;
}

.home-page__template-category:hover {
  color: #1d4ed8;
}

.home-page__template-category--active {
  background: #fff;
  color: #1d4ed8;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.1);
}

.home-page__templates-label {
  color: #6b7280;
  font-size: 14px;
  font-weight: 500;
}

.home-page__template-btn {
  border-radius: 6px;
  background: #f8fafc;
  border: 1px solid #dbe3ef;
  color: #334155;
  font-size: 14px;
  transition: all 0.3s;
}

.home-page__template-btn:hover {
  background: #eff6ff;
  border-color: #93c5fd;
  color: #1d4ed8;
}

.home-page__template-icon {
  margin-right: 6px;
}

.home-page__empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 18px 0 8px;
}

.home-page__section {
  background: transparent;
  padding: 32px 24px 48px;
  position: relative;
}

.home-page__section--featured {
  background: transparent;
}

.home-page__section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  max-width: 1400px;
  margin-left: auto;
  margin-right: auto;
}

.home-page__search-box {
  flex-shrink: 0;
}

.home-page__search-input {
  width: 240px;
  border-radius: 6px;
  border: 1px solid #cbd5e1;
  transition: all 0.3s;
}

.home-page__search-input:hover {
  border-color: #93c5fd;
}

.home-page__search-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
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
  font-size: 24px;
}

.home-page__section-icon--star {
  color: #ffd700;
}

.home-page__section-title {
  font-size: 22px;
  font-weight: 650;
  color: #111827;
  margin: 0;
}

.home-page__apps-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 18px;
  max-width: 1400px;
  margin: 0 auto;
}

.home-page__library-nav {
  padding: 24px 24px 0;
  background: transparent;
}

.home-page__library-nav-inner {
  max-width: 1400px;
  margin: 0 auto;
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #dbe3ef;
}

.home-page__library-label {
  margin-bottom: 2px;
  color: #6b7280;
  font-size: 12px;
}

.home-page__library-nav h2 {
  margin: 0;
  color: #111827;
  font-size: 20px;
  font-weight: 650;
}

.home-page__library-nav :deep(.ant-radio-button-wrapper) {
  min-width: 128px;
  text-align: center;
}

@media (max-width: 768px) {
  .home-page__hero {
    min-height: 404px;
    padding: 34px 16px 30px;
  }

  .home-page__title {
    font-size: 28px;
  }

  .home-page__subtitle {
    display: block;
    line-height: 1.7;
  }

  .home-page__input-card {
    padding: 16px;
    width: 100%;
  }

  .home-page__input-actions {
    flex-direction: column;
    gap: 12px;
  }

  .home-page__submit-btn {
    width: 100%;
  }

  .home-page__apps-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 16px;
  }

  .home-page__templates {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    width: 100%;
  }

  .home-page__template-categories {
    max-width: 100%;
    overflow-x: auto;
  }

  .home-page__template-category {
    flex: 0 0 auto;
    padding-inline: 10px;
  }

  .home-page__templates-label {
    grid-column: 1 / -1;
  }

  .home-page__template-btn {
    width: 100%;
    min-width: 0;
  }

  .home-page__section {
    padding: 24px 16px 36px;
  }

  .home-page__library-nav {
    padding: 20px 16px 0;
  }

  .home-page__library-nav-inner,
  .home-page__section-header {
    align-items: stretch;
    flex-direction: column;
  }

  .home-page__library-nav :deep(.ant-radio-group) {
    display: flex;
    width: 100%;
  }

  .home-page__library-nav :deep(.ant-radio-button-wrapper) {
    min-width: 0;
    flex: 1;
  }

  .home-page__search-box,
  .home-page__search-input {
    width: 100%;
  }
}
</style>
