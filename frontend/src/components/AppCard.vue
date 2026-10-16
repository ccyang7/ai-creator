<template>
  <div class="app-card" :class="`app-card--${type}`" @click="handleCardClick">
    <!-- 封面图 -->
    <div class="app-card__cover">
      <img v-if="app.cover" :src="app.cover" :alt="app.appName" class="app-card__image" />
      <div
        v-else
        :class="['app-card__placeholder', `app-card__placeholder--${app.codeGenType || 'default'}`]"
      >
        <div class="app-card__placeholder-mark">
          <CodeOutlined />
          <span>{{ coverLabel }}</span>
        </div>
        <span class="app-card__placeholder-caption">AI Creator</span>
      </div>

    </div>

    <!-- 应用信息 -->
    <div class="app-card__info">
      <div class="app-card__info-left">
        <a-avatar :src="app.user?.userAvatar" :size="40" class="app-card__avatar">
          <UserOutlined />
        </a-avatar>
      </div>
      <div class="app-card__info-right">
        <h3 class="app-card__name">{{ app.appName || '未命名应用' }}</h3>
        <p class="app-card__user-name">{{ app.user?.userName || '未知用户' }}</p>
        <div class="app-card__meta">
          <CodeGenTypeTag v-if="app.codeGenType" :type="app.codeGenType" />
          <span :class="['app-card__status', { 'app-card__status--online': app.deployKey }]">
            {{ app.deployKey ? '已部署' : '未部署' }}
          </span>
        </div>
      </div>
    </div>

    <div v-if="showActions" class="app-card__actions">
      <a-button type="primary" class="app-card__action-btn" @click.stop="handlePrimaryAction">
        <CopyOutlined v-if="isFeatured && app.initPrompt" />
        <MessageOutlined v-else />
        {{ isFeatured && app.initPrompt ? '基于此创建' : isFeatured ? '查看案例' : '继续创作' }}
      </a-button>
      <a-button
        v-if="isFeatured && app.initPrompt"
        type="default"
        class="app-card__action-btn"
        @click.stop="handleViewChat"
      >
        <MessageOutlined />
        查看案例
      </a-button>
      <a-button
        v-if="!isFeatured && app.deployKey"
        type="default"
        class="app-card__action-btn"
        @click.stop="handleViewDeploy"
      >
        <EyeOutlined />
        打开应用
      </a-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { CodeOutlined, CopyOutlined, MessageOutlined, EyeOutlined, UserOutlined } from '@ant-design/icons-vue'
import CodeGenTypeTag from '@/components/CodeGenTypeTag.vue'
import { computed } from 'vue'

interface AppCardProps {
  app: API.AppVO
  type?: 'default' | 'featured'
  showActions?: boolean
  isFeatured?: boolean
}

const props = withDefaults(defineProps<AppCardProps>(), {
  type: 'default',
  showActions: true,
  isFeatured: false,
})

const coverLabel = computed(() => {
  if (props.app.codeGenType === 'vue_project') return 'VUE'
  if (props.app.codeGenType === 'multi_file') return 'WEB'
  return 'HTML'
})

const emit = defineEmits<{
  (e: 'view-chat', appId: string): void
  (e: 'view-deploy', deployKey: string): void
  (e: 'use-template', app: API.AppVO): void
  (e: 'click', appId: string): void
}>()

// 处理卡片点击
const handleCardClick = () => {
  if (props.app.id) {
    emit('click', props.app.id)
  }
}

// 查看对话
const handleViewChat = () => {
  if (props.app.id) {
    emit('view-chat', props.app.id)
  }
}

const handlePrimaryAction = () => {
  if (props.isFeatured && props.app.initPrompt) {
    emit('use-template', props.app)
    return
  }
  handleViewChat()
}

// 查看部署
const handleViewDeploy = () => {
  if (props.app.deployKey) {
    emit('view-deploy', props.app.deployKey)
  }
}
</script>

<style scoped>
.app-card {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.05);
  transition: all 0.3s;
  cursor: pointer;
}

.app-card:hover {
  transform: translateY(-2px);
  border-color: #bfdbfe;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.08);
}

.app-card__cover {
  position: relative;
  width: 100%;
  padding-top: 56.25%; /* 16:9 比例 */
  overflow: hidden;
  background: #f5f5f5;
}

.app-card__image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-card__placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eff6ff;
  color: #2563eb;
}

.app-card__placeholder--multi_file {
  background: #ecfeff;
  color: #0e7490;
}

.app-card__placeholder--vue_project {
  background: #f0fdf4;
  color: #15803d;
}

.app-card__placeholder-mark {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0;
}

.app-card__placeholder-caption {
  position: absolute;
  right: 16px;
  bottom: 12px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.08em;
  opacity: 0.65;
}

.app-card__actions {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 16px 16px;
}

.app-card__action-btn {
  min-width: 0;
  flex: 1;
  height: 36px;
  font-size: 14px;
  border-radius: 6px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
}

.app-card__action-btn:first-child {
  background: #2563eb;
  border: none;
  color: #fff;
}

.app-card__action-btn:first-child:hover {
  background: #1d4ed8;
}

.app-card__action-btn:last-child {
  background: #fff;
  border: 1px solid #cbd5e1;
  color: #334155;
}

.app-card__action-btn:last-child:hover {
  border-color: #2563eb;
  color: #1d4ed8;
}

.app-card__info {
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-card__info-left {
  flex-shrink: 0;
}

.app-card__avatar {
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.app-card__info-right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.app-card__name {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-card__user-name {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.app-card__meta {
  min-height: 22px;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.app-card__status {
  color: #94a3b8;
  font-size: 12px;
}

.app-card__status--online {
  color: #15803d;
}
</style>
