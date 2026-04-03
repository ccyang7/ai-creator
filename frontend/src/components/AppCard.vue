<template>
  <div class="app-card" :class="`app-card--${type}`" @click="handleCardClick">
    <!-- 封面图 -->
    <div class="app-card__cover">
      <img v-if="app.cover" :src="app.cover" :alt="app.appName" class="app-card__image" />
      <div v-else class="app-card__placeholder">
        <span class="app-card__placeholder-icon">🚀</span>
      </div>

      <!-- 悬浮操作层 -->
      <div v-if="showActions" class="app-card__overlay">
        <div class="app-card__actions">
          <a-button type="primary" class="app-card__action-btn" @click.stop="handleViewChat">
            <MessageOutlined />
            查看对话
          </a-button>
          <a-button
            v-if="app.deployKey"
            type="default"
            class="app-card__action-btn"
            @click.stop="handleViewDeploy"
          >
            <EyeOutlined />
            查看作品
          </a-button>
        </div>
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
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { MessageOutlined, EyeOutlined, UserOutlined } from '@ant-design/icons-vue'

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

const emit = defineEmits<{
  (e: 'view-chat', appId: string): void
  (e: 'view-deploy', appId: string, deployKey: string): void
  (e: 'click', appId: string): void
}>()

// 处理卡片点击
const handleCardClick = () => {
  emit('click', props.app.id!)
}

// 查看对话
const handleViewChat = () => {
  emit('view-chat', props.app.id!)
}

// 查看部署
const handleViewDeploy = () => {
  if (props.app.deployKey) {
    emit('view-deploy', props.app.id!, props.app.deployKey)
  }
}
</script>

<style scoped>
.app-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transition: all 0.3s;
  cursor: pointer;
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.app-card__placeholder-icon {
  font-size: 64px;
  opacity: 0.6;
}

.app-card__overlay {
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

.app-card:hover .app-card__overlay {
  opacity: 1;
}

.app-card__actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
}

.app-card__action-btn {
  min-width: 140px;
  height: 40px;
  font-size: 14px;
  font-weight: 600;
  border-radius: 20px;
  backdrop-filter: blur(10px);
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.app-card__action-btn:first-child {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  color: #fff;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
}

.app-card__action-btn:first-child:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
}

.app-card__action-btn:last-child {
  background: rgba(255, 255, 255, 0.95);
  border: 2px solid rgba(102, 126, 234, 0.3);
  color: #667eea;
}

.app-card__action-btn:last-child:hover {
  background: #fff;
  border-color: #667eea;
  transform: translateY(-2px);
}

.app-card__info {
  padding: 16px 20px;
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
</style>
