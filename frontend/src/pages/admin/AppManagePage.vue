<template>
  <div class="app-manage-page">
    <div class="app-manage-page__header">
      <div class="app-manage-page__header-title">
        <h2 class="app-manage-page__title">应用数据</h2>
      </div>
      <div class="app-manage-page__header-stats">
        <a-statistic title="应用总数" :value="total" :value-style="{ color: '#2563eb' }" />
      </div>
    </div>

    <a-card class="app-manage-card">
      <div class="app-manage-page__search-wrapper">
        <a-form
          layout="inline"
          :model="searchParams"
          class="app-manage-page__search"
          @finish="doSearch"
        >
          <a-form-item label="应用名称">
            <a-input
              v-model:value="searchParams.appName"
              allow-clear
              placeholder="请输入名称"
              class="app-manage-page__input"
            />
          </a-form-item>
          <a-form-item label="创建者 ID">
            <a-input
              v-model:value="searchParams.userId"
              allow-clear
              placeholder="用户 ID"
              class="app-manage-page__input"
            />
          </a-form-item>
          <a-form-item label="生成类型">
            <a-select
              v-model:value="searchParams.codeGenType"
              allow-clear
              style="width: 150px"
              placeholder="选择类型"
            >
              <a-select-option value="html">
                <span class="app-manage-page__type-tag app-manage-page__type-tag--html">HTML</span>
              </a-select-option>
              <a-select-option value="multi_file">
                <span class="app-manage-page__type-tag app-manage-page__type-tag--multi"
                  >MULTI_FILE</span
                >
              </a-select-option>
              <a-select-option value="vue_project">
                <span class="app-manage-page__type-tag app-manage-page__type-tag--vue">
                  VUE_PROJECT
                </span>
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" html-type="submit" class="app-manage-page__btn">
                搜索
              </a-button>
              <a-button @click="resetSearch" class="app-manage-page__btn"> 重置 </a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <a-table
        :columns="columns"
        :data-source="data"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        class="app-manage-page__table"
        @change="doTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'appName'">
            <div class="app-manage-page__app-info">
              <a-avatar
                :src="record.cover"
                :size="44"
                shape="square"
                class="app-manage-page__app-avatar"
              >
                {{ record.appName?.[0] || 'A' }}
              </a-avatar>
              <div class="app-manage-page__app-name">
                <div class="app-manage-page__app-name-text">{{ record.appName || '未命名' }}</div>
                <div class="app-manage-page__app-id">ID: {{ record.id }}</div>
              </div>
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'codeGenType'">
            <CodeGenTypeTag :type="record.codeGenType" />
          </template>
          <template v-else-if="column.dataIndex === 'userId'">
            <div class="app-manage-page__user">👤 {{ record.userId || '-' }}</div>
          </template>
          <template v-else-if="column.dataIndex === 'priority'">
            <a-tag
              v-if="record.priority === 99"
              color="gold"
              class="app-manage-page__tag app-manage-page__tag--featured"
            >
              ⭐ 精选
            </a-tag>
            <a-tag
              v-else-if="record.priority === 0"
              color="green"
              class="app-manage-page__tag app-manage-page__tag--normal"
            >
              普通
            </a-tag>
            <span v-else class="app-manage-page__priority">{{ record.priority || 0 }}</span>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            <div class="app-manage-page__time">
              {{ record.createTime ? dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') : '-' }}
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'deployedTime'">
            <div class="app-manage-page__time">
              {{
                record.deployedTime ? dayjs(record.deployedTime).format('YYYY-MM-DD HH:mm:ss') : '-'
              }}
            </div>
          </template>
          <template v-else-if="column.key === 'action'">
            <div class="app-manage-page__actions">
              <a-button
                type="link"
                size="small"
                class="app-manage-page__action-btn"
                @click="doEdit(record)"
              >
                编辑
              </a-button>
              <a-button
                v-if="record.priority !== 99"
                size="small"
                class="app-manage-page__action-btn app-manage-page__action-btn--star"
                @click="doSetFeatured(record)"
              >
                ⭐ 精选
              </a-button>
              <a-button
                v-else
                size="small"
                class="app-manage-page__action-btn app-manage-page__action-btn--unstar"
                @click="doCancelFeatured(record)"
              >
                取消精选
              </a-button>
              <a-popconfirm
                title="确认删除该应用？"
                ok-text="确认"
                cancel-text="取消"
                @confirm="record.id && doDelete(record.id)"
              >
                <a-button
                  danger
                  size="small"
                  class="app-manage-page__action-btn app-manage-page__action-btn--danger"
                >
                  删除
                </a-button>
              </a-popconfirm>
            </div>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { TablePaginationConfig } from 'ant-design-vue'
import dayjs from 'dayjs'
import CodeGenTypeTag from '@/components/CodeGenTypeTag.vue'
import { adminListAppByPage, adminDeleteApp, adminUpdateApp } from '@/api/appController'

const router = useRouter()

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 90,
  },
  {
    title: '应用信息',
    dataIndex: 'appName',
    width: 200,
  },
  {
    title: '类型',
    dataIndex: 'codeGenType',
    width: 120,
  },
  {
    title: '用户 ID',
    dataIndex: 'userId',
    width: 100,
  },
  {
    title: '部署 Key',
    dataIndex: 'deployKey',
    width: 120,
  },
  {
    title: '部署时间',
    dataIndex: 'deployedTime',
    width: 180,
  },
  {
    title: '优先级',
    dataIndex: 'priority',
    width: 90,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 180,
  },
  {
    title: '操作',
    key: 'action',
    width: 220,
    fixed: 'right',
  },
]

const data = ref<API.AppVO[]>([])
const total = ref(0)
const loading = ref(false)

const searchParams = reactive<API.AppAdminQueryDTO>({
  pageNum: 1,
  pageSize: 10,
  appName: '',
  codeGenType: '',
  userId: '',
})

const fetchData = async () => {
  loading.value = true
  try {
    const res = await adminListAppByPage({
      ...searchParams,
    })
    if (res.data.code === 0 && res.data.data) {
      data.value = res.data.data.records || []
      total.value = res.data.data.totalRow || 0
      // 同步更新分页组件的 total
      pagination.total = total.value
    }
  } catch (error) {
    console.error(error)
    message.error('加载失败，请重试')
  } finally {
    loading.value = false
  }
}

const doSearch = () => {
  searchParams.pageNum = 1
  pagination.current = 1 // 同步更新分页组件
  fetchData()
}

const resetSearch = () => {
  searchParams.pageNum = 1
  pagination.current = 1 // 同步更新分页组件
  searchParams.appName = ''
  searchParams.codeGenType = ''
  searchParams.userId = ''
  fetchData()
}

const doTableChange = (pag: TablePaginationConfig) => {
  const current = pag.current ?? 1
  const pageSize = pag.pageSize ?? 10
  // 同步更新分页组件和查询参数
  pagination.current = current
  pagination.pageSize = pageSize
  searchParams.pageNum = current
  searchParams.pageSize = pageSize
  fetchData()
}

const doDelete = async (id: string) => {
  try {
    const res = await adminDeleteApp({ id })
    if (res.data.code === 0) {
      message.success('删除成功')
      // 如果当前页只有一条数据，删除后回到上一页
      if (data.value.length === 1 && pagination.current > 1) {
        pagination.current -= 1
        searchParams.pageNum = pagination.current
      }
      fetchData()
    } else {
      message.error(res.data.message || '删除失败')
    }
  } catch (error) {
    console.error(error)
    message.error('删除失败，请重试')
  }
}

const doEdit = (record: API.AppVO) => {
  if (record.id) {
    router.push(`/app/edit/${record.id}`)
  }
}

const doSetFeatured = async (record: API.AppVO) => {
  try {
    const res = await adminUpdateApp({
      id: record.id,
      appName: record.appName,
      cover: record.cover,
      priority: 99,
    })
    if (res.data.code === 0) {
      message.success('已设为精选')
      fetchData()
    } else {
      message.error(res.data.message || '设置失败')
    }
  } catch (error) {
    console.error(error)
    message.error('设置失败，请重试')
  }
}

const doCancelFeatured = async (record: API.AppVO) => {
  try {
    const res = await adminUpdateApp({
      id: record.id,
      appName: record.appName,
      cover: record.cover,
      priority: 0,
    })
    if (res.data.code === 0) {
      message.success('已取消精选')
      fetchData()
    } else {
      message.error(res.data.message || '取消失败')
    }
  } catch (error) {
    console.error(error)
    message.error('取消失败，请重试')
  }
}

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
    pagination.current = 1 // 切换每页条数时，重置到第一页
    searchParams.pageNum = 1
    fetchData()
  },
})

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.app-manage-page {
  min-height: 0;
}

.app-manage-page__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.app-manage-page__header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-manage-page__header-icon {
  font-size: 32px;
  color: #2563eb;
}

.app-manage-page__title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.app-manage-page__header-stats {
  display: flex;
  gap: 24px;
}

.app-manage-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: none;
}

.app-manage-page__search-wrapper {
  background: #f8fafc;
  border-radius: 6px;
  margin: -16px -16px 16px;
}

.app-manage-page__search {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.app-manage-page__input {
  width: 150px;
  border-radius: 8px;
  transition: all 0.3s;
}

.app-manage-page__input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
}

.app-manage-page__type-tag {
  font-weight: 600;
}

.app-manage-page__type-tag--html {
  color: #52c41a;
}

.app-manage-page__type-tag--multi {
  color: #fa8c16;
}

.app-manage-page__type-tag--vue {
  color: #1677ff;
}

.app-manage-page__tag--featured {
  border-color: #faad14;
  background: rgba(250, 173, 20, 0.1);
}

.app-manage-page__tag--normal {
  border-color: #52c41a;
  background: rgba(82, 196, 26, 0.1);
}

.app-manage-page__btn {
  border-radius: 8px;
  transition: all 0.3s;
}

.app-manage-page__btn:hover {
  transform: translateY(-2px);
}

.app-manage-page__table {
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

.app-manage-page__app-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-manage-page__app-avatar {
  border: 2px solid rgba(37, 99, 235, 0.2);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.app-manage-page__app-name {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.app-manage-page__app-name-text {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 14px;
}

.app-manage-page__app-id {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.app-manage-page__user {
  display: flex;
  align-items: center;
  gap: 6px;
  color: rgba(0, 0, 0, 0.65);
  font-size: 13px;
}

.app-manage-page__priority {
  font-weight: 600;
  color: #1a1a1a;
}

.app-manage-page__time {
  color: rgba(0, 0, 0, 0.65);
  font-size: 13px;
  font-family: 'Courier New', monospace;
}

.app-manage-page__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.app-manage-page__action-btn {
  border-radius: 6px;
  transition: all 0.3s;
  font-size: 12px;
}

.app-manage-page__action-btn:hover {
  transform: translateY(-1px);
}

.app-manage-page__action-btn--star {
  color: #faad14;
  border-color: rgba(250, 173, 20, 0.3);
}

.app-manage-page__action-btn--star:hover {
  background: rgba(250, 173, 20, 0.1);
  border-color: #faad14;
}

.app-manage-page__action-btn--unstar {
  color: #8c8c8c;
  border-color: rgba(140, 140, 140, 0.3);
}

.app-manage-page__action-btn--unstar:hover {
  background: rgba(140, 140, 140, 0.1);
  border-color: #8c8c8c;
}

.app-manage-page__action-btn--danger {
  font-size: 12px;
}

@media (max-width: 768px) {
  .app-manage-page {
    padding: 12px;
  }

  .app-manage-page__header {
    flex-direction: column;
    gap: 16px;
  }

  .app-manage-page__search {
    flex-direction: column;
  }

  .app-manage-page__input {
    width: 100%;
  }
}
</style>
