<template>
  <div class="chat-history-manage-page">
    <!-- 页面头部 -->
    <div class="chat-history-manage-page__header">
      <div class="chat-history-manage-page__header-title">
        <h2 class="chat-history-manage-page__title">对话数据</h2>
      </div>
      <!-- 统计信息 -->
      <div class="chat-history-manage-page__header-stats">
        <a-statistic title="对话总数" :value="total" :value-style="{ color: '#2563eb' }"></a-statistic>
      </div>
    </div>

    <a-card class="chat-history-manage-card">
      <!-- 搜索表单 -->
      <div class="chat-history-manage-page__search-wrapper">
        <a-form
          layout="inline"
          :model="searchParams"
          class="chat-history-manage-page__search"
          @finish="doSearch"
        >
          <!-- 消息类型筛选 -->
          <a-form-item label="消息类型">
          <a-select
            v-model:value="searchParams.messageType"
            allow-clear
            style="width: 150px"
            placeholder="选择类型"
          >
            <a-select-option value="user">
            <a-tag color="blue">👤 用户</a-tag>
          </a-select-option>
          <a-select-option value="ai">
            <a-tag color="green">🤖 AI</a-tag>
          </a-select-option>
          </a-select>
        </a-form-item>
        <!-- 应用 ID 筛选 -->
        <a-form-item label="应用 ID">
          <a-input
            v-model:value="searchParams.appId"
            allow-clear
            placeholder="应用 ID"
            class="chat-history-manage-page__input"
          />
        </a-form-item>
        <!-- 用户 ID 筛选 -->
        <a-form-item label="用户 ID">
          <a-input
            v-model:value="searchParams.userId"
            allow-clear
            placeholder="用户 ID"
            class="chat-history-manage-page__input"
          />
        </a-form-item>
        <!-- 搜索按钮 -->
        <a-form-item>
            <a-space>
              <a-button type="primary" html-type="submit" class="chat-history-manage-page__btn">
                搜索
              </a-button>
              <a-button @click="resetSearch" class="chat-history-manage-page__btn"> 重置 </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- 数据表格 -->
      <a-table
        :columns="columns"
        :data-source="data"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        class="chat-history-manage-page__table"
        @change="doTableChange"
      >
        <template #bodyCell="{ column, record }">
          <!-- 消息类型列 -->
          <template v-if="column.dataIndex === 'messageType'">
            <a-tag
              v-if="record.messageType === 'user'"
              color="blue"
              class="chat-history-manage-page__type-tag"
            >
              👤 用户
            </a-tag>
            <a-tag v-else color="green" class="chat-history-manage-page__type-tag">
              🤖 AI
            </a-tag>
          </template>
          <!-- 消息内容列 -->
          <template v-else-if="column.dataIndex === 'message'">
            <div class="chat-history-manage-page__message">
              {{ record.message || '-' }}
            </div>
          </template>
          <!-- 应用 ID 列 -->
          <template v-else-if="column.dataIndex === 'appId'">
            <div class="chat-history-manage-page__app">
              {{ record.appId || '-' }}
            </div>
          </template>
          <!-- 用户 ID 列 -->
          <template v-else-if="column.dataIndex === 'userId'">
            <div class="chat-history-manage-page__user">
              {{ record.userId || '-' }}
            </div>
          </template>
          <!-- 创建时间列 -->
          <template v-else-if="column.dataIndex === 'createTime'">
            <div class="chat-history-manage-page__time">
              {{ record.createTime ? dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') : '-' }}
            </div>
          </template>
          <!-- 操作列 -->
          <template v-else-if="column.key === 'action'">
            <div class="chat-history-manage-page__actions">
              <a-button type="link" size="small" @click="doView(record)">
                查看详情
              </a-button>
            </div>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 消息详情弹窗 -->
    <a-modal
      v-model:open="detailModalVisible"
      title="消息详情"
      :footer="null"
      width="800px"
    >
      <div v-if="currentRecord" class="chat-history-detail">
        <!-- 消息类型 -->
        <div class="chat-history-detail__item">
          <span class="chat-history-detail__label">消息类型：</span>
          <span class="chat-history-detail__value">
            <a-tag
              v-if="currentRecord.messageType === 'user'"
              color="blue"
            >
              👤 用户
            </a-tag>
            <a-tag v-else color="green">
              🤖 AI
            </a-tag>
          </span>
        </div>
        <!-- 消息内容 -->
        <div class="chat-history-detail__item">
          <span class="chat-history-detail__label">消息内容：</span>
          <div class="chat-history-detail__content">
            {{ currentRecord.message || '-' }}
          </div>
        </div>
        <!-- 应用 ID -->
        <div class="chat-history-detail__item">
          <span class="chat-history-detail__label">应用 ID：</span>
          <span class="chat-history-detail__value">{{ currentRecord.appId || '-' }}</span>
        </div>
        <!-- 用户 ID -->
        <div class="chat-history-detail__item">
          <span class="chat-history-detail__label">用户 ID：</span>
          <span class="chat-history-detail__value">{{ currentRecord.userId || '-' }}</span>
        </div>
        <!-- 创建时间 -->
        <div class="chat-history-detail__item">
          <span class="chat-history-detail__label">创建时间：</span>
          <span class="chat-history-detail__value">
            {{ currentRecord.createTime ? dayjs(currentRecord.createTime).format('YYYY-MM-DD HH:mm:ss') : '-' }}
          </span>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import dayjs from 'dayjs'
import { listAllChatHistoryByPageForAdmin } from '@/api/chatHistoryController'

// ==================== 表格列配置 ====================
const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 90,
  },
  {
    title: '消息类型',
    dataIndex: 'messageType',
    width: 100,
  },
  {
    title: '消息内容',
    dataIndex: 'message',
    width: 300,
  },
  {
    title: '应用 ID',
    dataIndex: 'appId',
    width: 100,
  },
  {
    title: '用户 ID',
    dataIndex: 'userId',
    width: 100,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    width: 180,
    fixed: 'right',
  },
]

// ==================== 状态管理 ====================
const data = ref<API.ChatHistory[]>([]) // 对话历史列表数据
const total = ref(0) // 总记录数
const loading = ref(false) // 加载状态
const detailModalVisible = ref(false) // 详情弹窗是否可见
const currentRecord = ref<API.ChatHistory | null>(null) // 当前查看的记录

// 搜索参数
const searchParams = reactive<API.ChatHistoryQueryDTO>({
  pageNum: 1,
  pageSize: 10,
  appId: undefined,
  userId: undefined,
  messageType: undefined,
})

// ==================== 数据加载 ====================
/**
 * 加载对话历史数据
 * 调用后端接口获取分页数据
 */
const fetchData = async () => {
  loading.value = true
  try {
    const res = await listAllChatHistoryByPageForAdmin({
    ...searchParams,
  })
    if (res.data.code === 0 && res.data.data) {
      data.value = res.data.data.records || []
      total.value = res.data.data.totalRow || 0
      pagination.total = total.value
    }
  } catch (error) {
    console.error(error)
    message.error('加载失败，请重试')
  } finally {
    loading.value = false
  }
}

// ==================== 搜索功能 ====================
/**
 * 执行搜索
 * 重置到第一页并重新加载数据
 */
const doSearch = () => {
  searchParams.pageNum = 1
  pagination.current = 1
  fetchData()
}

/**
 * 重置搜索条件
 * 清空所有搜索参数并重新加载
 */
const resetSearch = () => {
  searchParams.pageNum = 1
  pagination.current = 1
  searchParams.appId = undefined
  searchParams.userId = undefined
  searchParams.messageType = undefined
  fetchData()
}

// ==================== 分页处理 ====================
/**
 * 表格分页变化处理
 * @param pag 分页参数
 */
const doTableChange = (pag: TablePaginationConfig) => {
  const current = pag.current ?? 1
  const pageSize = pag.pageSize ?? 10
  pagination.current = current
  pagination.pageSize = pageSize
  searchParams.pageNum = current
  searchParams.pageSize = pageSize
  fetchData()
}

// ==================== 查看详情 ====================
/**
 * 查看消息详情
 * @param record 对话历史记录
 */
const doView = (record: API.ChatHistory) => {
  currentRecord.value = record
  detailModalVisible.value = true
}

// 分页配置
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total: number) => `共 ${total} 条`,
  // 每页条数变化时的回调
  onShowSizeChange: (current: number, size: number) => {
    pagination.pageSize = size
    searchParams.pageSize = size
    pagination.current = 1
    searchParams.pageNum = 1
    fetchData()
  },
})

// ==================== 生命周期 ====================
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.chat-history-manage-page {
  min-height: 0;
}

.chat-history-manage-page__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.chat-history-manage-page__header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.chat-history-manage-page__header-icon {
  font-size: 32px;
  color: #2563eb;
}

.chat-history-manage-page__title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.chat-history-manage-page__header-stats {
  display: flex;
  gap: 24px;
}

.chat-history-manage-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: none;
}

.chat-history-manage-page__search-wrapper {
  background: #f8fafc;
  border-radius: 6px;
  margin: -16px -16px 16px;
}

.chat-history-manage-page__search {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.chat-history-manage-page__input {
  width: 150px;
  border-radius: 8px;
  transition: all 0.3s;
}

.chat-history-manage-page__input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
}

.chat-history-manage-page__type-tag {
  font-weight: 600;
}

.chat-history-manage-page__btn {
  border-radius: 8px;
  transition: all 0.3s;
}

.chat-history-manage-page__btn:hover {
  transform: translateY(-2px);
}

.chat-history-manage-page__table {
  margin-top: 16px;
}

:deep(.ant-table) {
  background: #fff;
  border-radius: 6px;
}

:deep(.ant-table-thead > tr > th) {
  background: #f8fafc;
  color: #1a1a1a;
  font-weight: 600;
  padding: 14px 16px;
}

:deep(.ant-table-tbody > tr:hover > td) {
  background: rgba(37, 99, 235, 0.04);
}

:deep(.ant-table-tbody > tr > td) {
  padding: 14px 16px;
}

.chat-history-manage-page__message {
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: rgba(0, 0, 0, 0.65);
  font-size: 13px;
}

.chat-history-manage-page__app,
.chat-history-manage-page__user {
  display: flex;
  align-items: center;
  gap: 6px;
  color: rgba(0, 0, 0, 0.65);
  font-size: 13px;
}

.chat-history-manage-page__time {
  color: rgba(0, 0, 0, 0.65);
  font-size: 13px;
}

.chat-history-manage-page__actions {
  display: flex;
  gap: 8px;
}

.chat-history-manage-page__action-btn {
  padding: 0 8px;
  height: 28px;
  font-size: 13px;
}

.chat-history-detail {
  padding: 16px;
}

.chat-history-detail__item {
  margin-bottom: 16px;
}

.chat-history-detail__item:last-child {
  margin-bottom: 0;
}

.chat-history-detail__label {
  font-weight: 600;
  color: rgba(0, 0, 0, 0.85);
  margin-bottom: 8px;
  display: block;
}

.chat-history-detail__value {
  color: rgba(0, 0, 0, 0.65);
}

.chat-history-detail__content {
  background: #f5f7fa;
  padding: 12px;
  border-radius: 8px;
  max-height: 200px;
  overflow-y: auto;
  word-break: break-word;
}
</style>
