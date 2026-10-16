<template>
  <a-modal
    v-model:open="visible"
    title="应用详情"
    :footer="null"
    width="480px"
    class="app-detail-modal"
  >
    <div v-if="app" class="app-detail-content">
      <!-- 应用基础信息 -->
      <div class="app-detail-section">
        <h3 class="app-detail-section-title">基础信息</h3>
        <div class="app-detail-info">
          <div class="app-detail-item">
            <span class="app-detail-label">应用名称</span>
            <span class="app-detail-value">{{ app.appName || '未命名应用' }}</span>
          </div>
          <div class="app-detail-item">
            <span class="app-detail-label">创建者</span>
            <div class="app-detail-creator">
              <a-avatar
                :src="app.user?.userAvatar"
                :size="32"
                class="app-detail-avatar"
              >
                <UserOutlined />
              </a-avatar>
              <span class="app-detail-creator-name">{{ app.user?.userName || '未知用户' }}</span>
            </div>
          </div>
          <div class="app-detail-item">
            <span class="app-detail-label">创建时间</span>
            <span class="app-detail-value">{{ formatDate(app.createTime) }}</span>
          </div>
          <div class="app-detail-item">
            <span class="app-detail-label">生成类型</span>
            <span class="app-detail-value">
              <a-tag v-if="app.codeGenType === 'html'" color="green">🌐 HTML</a-tag>
              <a-tag
                v-else-if="app.codeGenType === 'multi_file'"
                color="orange"
              >📁 MULTI_FILE</a-tag>
              <a-tag v-else-if="app.codeGenType === 'vue_project'" color="blue">
                ⚡ VUE_PROJECT
              </a-tag>
              <a-tag v-else>{{ app.codeGenType || '-' }}</a-tag>
            </span>
          </div>
        </div>
      </div>

      <!-- 操作栏（仅本人或管理员可见） -->
      <div v-if="showActions" class="app-detail-section">
        <h3 class="app-detail-section-title">操作</h3>
        <div class="app-detail-actions">
          <a-button type="primary" class="app-detail-action-btn" @click="handleEdit">
            <EditOutlined />
            修改应用
          </a-button>
          <a-popconfirm
            title="确定要删除此应用吗？"
            ok-text="确定"
            cancel-text="取消"
            @confirm="handleDelete"
          >
            <a-button danger class="app-detail-action-btn">
              <DeleteOutlined />
              删除应用
            </a-button>
          </a-popconfirm>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { UserOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons-vue'
import dayjs from 'dayjs'

interface AppDetailModalProps {
  app?: API.AppVO
  visible?: boolean
  showActions?: boolean
}

const props = withDefaults(defineProps<AppDetailModalProps>(), {
  visible: false,
  showActions: false,
})

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'edit', appId: string): void
  (e: 'delete', appId: string): void
}>()

const visible = computed({
  get: () => props.visible ?? false,
  set: (value) => emit('update:visible', value),
})

// 格式化日期
const formatDate = (date?: string) => {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD HH:mm:ss')
}

// 处理编辑
const handleEdit = () => {
  if (props.app?.id) {
    emit('edit', props.app.id)
  }
}

// 处理删除
const handleDelete = () => {
  if (props.app?.id) {
    emit('delete', props.app.id)
  }
}
</script>

<style scoped>
.app-detail-content {
  padding: 8px 0;
}

.app-detail-section {
  margin-bottom: 24px;
}

.app-detail-section:last-child {
  margin-bottom: 0;
}

.app-detail-section-title {
  font-size: 14px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  margin: 0 0 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.app-detail-info {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.app-detail-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-detail-label {
  width: 80px;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.65);
  flex-shrink: 0;
}

.app-detail-value {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.85);
  flex: 1;
}

.app-detail-creator {
  display: flex;
  align-items: center;
  gap: 8px;
}

.app-detail-avatar {
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.app-detail-creator-name {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.85);
  font-weight: 500;
}

.app-detail-actions {
  display: flex;
  gap: 12px;
}

.app-detail-action-btn {
  flex: 1;
  height: 40px;
  font-size: 14px;
  font-weight: 600;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
</style>
