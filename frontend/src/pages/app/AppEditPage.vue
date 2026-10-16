<template>
  <div class="app-edit-page">
    <div class="app-edit-page__header">
      <a-button type="text" class="app-edit-page__back-btn" @click="goBack">
        <LeftOutlined />
        返回
      </a-button>
      <h1 class="app-edit-page__title">编辑应用</h1>
    </div>

    <a-card class="app-edit-page__card">
      <a-form
        :model="formData"
        :rules="formRules"
        layout="vertical"
        class="app-edit-page__form"
        @finish="handleSubmit"
      >
        <a-form-item label="应用名称" name="appName">
          <a-input
            v-model:value="formData.appName"
            class="app-edit-page__input"
            placeholder="请输入应用名称"
            :disabled="!canEdit"
          />
        </a-form-item>

        <a-form-item v-if="isAdmin" label="应用封面" name="cover">
          <a-input
            v-model:value="formData.cover"
            class="app-edit-page__input"
            placeholder="请输入封面图片 URL"
          />
          <div v-if="formData.cover" class="app-edit-page__cover-preview">
            <img :src="formData.cover" alt="封面预览" class="app-edit-page__cover-image" />
          </div>
        </a-form-item>

        <a-form-item v-if="isAdmin" label="优先级" name="priority">
          <a-input-number
            v-model:value="formData.priority"
            class="app-edit-page__input"
            :min="0"
            :max="99"
            placeholder="请输入优先级（0-99）"
          />
          <div class="app-edit-page__priority-tip">
            <InfoCircleOutlined />
            优先级为 99 时会自动设为精选应用
          </div>
        </a-form-item>

        <a-form-item label="应用 ID">
          <a-input :value="appInfo.id" disabled />
        </a-form-item>

        <a-form-item label="创建时间">
          <a-input :value="formatTime(appInfo.createTime)" disabled />
        </a-form-item>

        <a-form-item label="生成类型">
          <a-tag :color="getTypeColor(appInfo.codeGenType)">
            {{ appInfo.codeGenType || '未设置' }}
          </a-tag>
        </a-form-item>

        <a-form-item v-if="appInfo.deployKey" label="部署状态">
          <a-tag color="green">已部署</a-tag>
          <div class="app-edit-page__deploy-key">部署 Key: {{ appInfo.deployKey }}</div>
        </a-form-item>

        <a-form-item>
          <a-space>
            <a-button
              v-if="canEdit"
              type="primary"
              html-type="submit"
              :loading="submitting"
              class="app-edit-page__btn"
            >
              保存
            </a-button>
            <a-button @click="goBack" class="app-edit-page__btn">取消</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { LeftOutlined, InfoCircleOutlined } from '@ant-design/icons-vue'
import { getAppById, updateApp, adminUpdateApp } from '@/api/appController'
import { useLoginUserStore } from '@/stores/loginUser'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

const submitting = ref(false)

// 应用信息
const appInfo = ref<API.AppVO>({})

// 表单数据
const formData = reactive({
  appName: '',
  cover: '',
  priority: 0,
})

// 表单验证规则
const formRules = {
  appName: [
    { required: true, message: '请输入应用名称', trigger: 'blur' },
    { min: 1, max: 50, message: '应用名称长度应在 1-50 个字符之间', trigger: 'blur' },
  ],
  cover: [{ type: 'url', message: '请输入有效的 URL', trigger: 'blur' }],
  priority: [{ type: 'number', min: 0, max: 99, message: '优先级应在 0-99 之间', trigger: 'blur' }],
}

// 是否为管理员
const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

// 是否可以编辑
const canEdit = computed(() => {
  if (!appInfo.value.id) return false
  // 管理员可以编辑任何应用
  if (isAdmin.value) return true
  // 普通用户只能编辑自己的应用
  return appInfo.value.userId === loginUserStore.loginUser.id
})

// 格式化时间
const formatTime = (time?: string) => {
  if (!time) return '-'
  return dayjs(time).format('YYYY-MM-DD HH:mm:ss')
}

// 获取类型颜色
const getTypeColor = (type?: string) => {
  switch (type) {
    case 'html':
      return 'green'
    case 'multi_file':
      return 'orange'
    case 'vue_project':
      return 'blue'
    default:
      return 'default'
  }
}

// 加载应用信息
const loadAppInfo = async () => {
  const id = route.params.id as string
  const appId = id

  if (!appId) {
    message.error('应用 ID 无效')
    router.push('/')
    return
  }

  try {
    const res = await getAppById({ id: appId })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      // 检查权限
      if (
        loginUserStore.loginUser.id &&
        appInfo.value.userId !== loginUserStore.loginUser.id &&
        loginUserStore.loginUser.userRole !== 'admin'
      ) {
        message.error('无权编辑此应用')
        router.push('/')
        return
      }

      // 填充表单数据
      formData.appName = appInfo.value.appName || ''
      formData.cover = appInfo.value.cover || ''
      formData.priority = appInfo.value.priority || 0
    } else {
      message.error('应用不存在')
      router.push('/')
    }
  } catch (error) {
    console.error(error)
    message.error('加载应用信息失败')
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!canEdit.value) {
    message.error('无权编辑此应用')
    return
  }

  try {
    submitting.value = true

    let res
    if (isAdmin.value) {
      // 管理员更新
      res = await adminUpdateApp({
        id: appInfo.value.id,
        appName: formData.appName,
        cover: formData.cover,
        priority: formData.priority,
      })
    } else {
      // 普通用户更新
      res = await updateApp({
        id: appInfo.value.id,
        appName: formData.appName,
      })
    }

    if (res.data.code === 0) {
      message.success('保存成功')
      goBack()
    } else {
      message.error(res.data.message || '保存失败')
    }
  } catch (error: unknown) {
    if (axios.isAxiosError(error) && error.response?.status === 403) {
      message.error('无权操作此应用')
    } else {
      console.error(error)
      message.error('保存失败，请重试')
    }
  } finally {
    submitting.value = false
  }
}

// 返回
const goBack = () => {
  router.back()
}

onMounted(() => {
  loadAppInfo()
})
</script>

<style scoped>
.app-edit-page {
  padding: 24px;
  background: var(--ac-page-background);
  min-height: calc(100vh - 64px - 48px);
}

.app-edit-page__header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.app-edit-page__back-btn {
  font-size: 14px;
  color: #666;
}

.app-edit-page__title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}

.app-edit-page__card {
  border: 1px solid rgba(203, 213, 225, 0.78);
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.08);
}

.app-edit-page__form {
  max-width: 600px;
  margin: 0 auto;
}

.app-edit-page__input {
  width: 100%;
  border-radius: 8px;
  transition: all 0.3s;
}

.app-edit-page__input:focus {
  border-color: var(--ac-primary);
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
}

.app-edit-page__cover-preview {
  margin-top: 12px;
  border-radius: 8px;
  overflow: hidden;
  max-width: 300px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.app-edit-page__cover-image {
  width: 100%;
  height: auto;
  display: block;
}

.app-edit-page__priority-tip {
  margin-top: 8px;
  font-size: 13px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 6px;
}

.app-edit-page__deploy-key {
  margin-top: 8px;
  font-size: 13px;
  color: #666;
  font-family: monospace;
}

.app-edit-page__btn {
  border-radius: 8px;
  padding: 6px 24px;
  transition: all 0.3s;
}

.app-edit-page__btn:hover {
  transform: translateY(-2px);
}

:deep(.ant-form-item-label > label) {
  font-weight: 600;
  color: #1a1a1a;
}

:deep(.ant-form-item-required::before) {
  color: #ff4d4f;
}
</style>
