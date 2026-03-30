<template>
  <div class="user-manage-page">
    <div class="user-manage-page__header">
      <div class="user-manage-page__header-title">
        <span class="user-manage-page__header-icon">👤</span>
        <h1 class="user-manage-page__title">用户管理</h1>
      </div>
      <div class="user-manage-page__header-stats">
        <a-statistic title="用户总数" :value="total" :value-style="{ color: '#667eea' }" />
      </div>
    </div>

    <a-card class="user-manage-card">
      <!-- 搜索区域 -->
      <div class="user-manage-page__search-wrapper">
        <a-form
          layout="inline"
          :model="searchParams"
          class="user-manage-page__search"
          @finish="doSearch"
        >
          <a-form-item label="账号">
            <a-input
              v-model:value="searchParams.userAccount"
              allow-clear
              placeholder="请输入账号"
              class="user-manage-page__input"
            />
          </a-form-item>
          <a-form-item label="用户名">
            <a-input
              v-model:value="searchParams.userName"
              allow-clear
              placeholder="请输入用户名"
              class="user-manage-page__input"
            />
          </a-form-item>
          <a-form-item label="角色">
            <a-select
              v-model:value="searchParams.userRole"
              allow-clear
              style="width: 140px"
              placeholder="选择角色"
            >
              <a-select-option value="admin">
                <span class="user-manage-page__role-tag user-manage-page__role-tag--admin"
                  >管理员</span
                >
              </a-select-option>
              <a-select-option value="user">
                <span class="user-manage-page__role-tag user-manage-page__role-tag--user"
                  >普通用户</span
                >
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" html-type="submit" class="user-manage-page__btn"
                >搜索</a-button
              >
              <a-button @click="resetSearch" class="user-manage-page__btn">重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- 表格区域 -->
      <a-table
        :columns="columns"
        :data-source="data"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        class="user-manage-page__table"
        @change="doTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'userAvatar'">
            <div class="user-manage-page__user-info">
              <a-avatar
                :src="record.userAvatar || 'https://api.dicebear.com/7.x/miniavs/svg?seed=default'"
                :size="44"
                class="user-manage-page__avatar"
              />
            </div>
          </template>
          <template v-else-if="column.dataIndex === 'userRole'">
            <a-tag v-if="record.userRole === 'admin'" color="green" class="user-manage-page__tag">
              👑 管理员
            </a-tag>
            <a-tag v-else color="blue" class="user-manage-page__tag"> 👤 普通用户 </a-tag>
          </template>
          <template v-else-if="column.dataIndex === 'createTime'">
            <div class="user-manage-page__time">
              {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
            </div>
          </template>
          <template v-else-if="column.key === 'action'">
            <div class="user-manage-page__actions">
              <a-popconfirm
                title="确认删除该用户？"
                ok-text="确认"
                cancel-text="取消"
                @confirm="doDelete(record.id)"
              >
                <a-button
                  danger
                  type="link"
                  size="small"
                  class="user-manage-page__action-btn user-manage-page__action-btn--danger"
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

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { deleteUser, listUserVoByPage } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'

const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 80,
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
    width: 140,
  },
  {
    title: '用户名',
    dataIndex: 'userName',
    width: 140,
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
    width: 120,
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
    ellipsis: true,
  },
  {
    title: '角色',
    dataIndex: 'userRole',
    width: 120,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    width: 200,
  },
  {
    title: '操作',
    key: 'action',
    width: 100,
  },
]

// 数据
const data = ref<API.UserVO[]>([])
const total = ref(0)
const loading = ref(false)

// 搜索条件
const searchParams = reactive<API.UserQueryDTO>({
  pageNum: 1,
  pageSize: 10,
})

// 获取数据
const fetchData = async () => {
  loading.value = true
  try {
    const res = await listUserVoByPage({
      ...searchParams,
    })
    if (res.data.data) {
      data.value = res.data.data.records ?? []
      total.value = res.data.data.totalRow ?? 0
    } else {
      message.error('获取数据失败，' + res.data.message)
    }
  } finally {
    loading.value = false
  }
}

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.pageNum ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: number) => `共 ${total} 条`,
  }
})

// 表格变化处理
const doTableChange = (page: any) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 搜索
const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

// 重置搜索
const resetSearch = () => {
  searchParams.userAccount = undefined
  searchParams.userName = undefined
  searchParams.userRole = undefined
  searchParams.pageNum = 1
  fetchData()
}

// 删除数据
const doDelete = async (id: string) => {
  if (!id) {
    return
  }
  const res = await deleteUser({ id })
  if (res.data.code === 0) {
    message.success('删除成功')
    fetchData()
  } else {
    message.error('删除失败，' + res.data.message)
  }
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.user-manage-page {
  padding: 24px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  min-height: calc(100vh - 64px - 48px);
}

.user-manage-page__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}

.user-manage-page__header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-manage-page__header-icon {
  font-size: 32px;
  color: #667eea;
}

.user-manage-page__title {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a1a;
  margin: 0;
}

.user-manage-page__header-stats {
  display: flex;
  gap: 24px;
}

.user-manage-card {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
}

.user-manage-page__search-wrapper {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.02) 0%, rgba(118, 75, 162, 0.02) 100%);
  border-radius: 12px;
  margin: -16px -16px 16px;
}

.user-manage-page__search {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.user-manage-page__input {
  width: 150px;
  border-radius: 8px;
  transition: all 0.3s;
}

.user-manage-page__input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

.user-manage-page__role-tag {
  font-weight: 600;
}

.user-manage-page__role-tag--admin {
  color: #52c41a;
}

.user-manage-page__role-tag--user {
  color: #1677ff;
}

.user-manage-page__btn {
  border-radius: 8px;
  transition: all 0.3s;
}

.user-manage-page__btn:hover {
  transform: translateY(-2px);
}

.user-manage-page__table {
  margin-top: 16px;
}

:deep(.ant-table) {
  background: #fff;
  border-radius: 12px;
}

:deep(.ant-table-thead > tr > th) {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.08) 0%, rgba(118, 75, 162, 0.08) 100%);
  color: #1a1a1a;
  font-weight: 600;
  padding: 14px 16px;
}

:deep(.ant-table-tbody > tr:hover > td) {
  background: rgba(102, 126, 234, 0.04);
}

:deep(.ant-table-tbody > tr > td) {
  padding: 14px 16px;
}

.user-manage-page__user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-manage-page__avatar {
  border: 2px solid rgba(102, 126, 234, 0.2);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.user-manage-page__user-name {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-manage-page__user-name-text {
  font-weight: 600;
  color: #1a1a1a;
  font-size: 14px;
}

.user-manage-page__user-id {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.user-manage-page__tag {
  font-weight: 600;
  border-radius: 6px;
  padding: 2px 10px;
}

.user-manage-page__time {
  color: rgba(0, 0, 0, 0.65);
  font-size: 13px;
  font-family: 'Courier New', monospace;
}

.user-manage-page__actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.user-manage-page__action-btn {
  border-radius: 6px;
  transition: all 0.3s;
  font-size: 12px;
}

.user-manage-page__action-btn:hover {
  transform: translateY(-1px);
}

.user-manage-page__action-btn--danger {
  font-size: 12px;
}

@media (max-width: 768px) {
  .user-manage-page {
    padding: 12px;
  }

  .user-manage-page__header {
    flex-direction: column;
    gap: 16px;
  }

  .user-manage-page__search {
    flex-direction: column;
  }

  .user-manage-page__input {
    width: 100%;
  }
}
</style>
