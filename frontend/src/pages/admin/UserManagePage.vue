<template>
  <div class="user-manage-page">
    <div class="user-manage-page__header">
      <div class="user-manage-page__header-title">
        <h2 class="user-manage-page__title">用户数据</h2>
      </div>
      <div class="user-manage-page__header-stats">
        <a-statistic title="用户总数" :value="total" :value-style="{ color: '#2563eb' }" />
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
import type { TablePaginationConfig } from 'ant-design-vue'
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
  } catch (error) {
    console.error('获取用户列表失败:', error)
    message.error('获取用户列表失败，请稍后重试')
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
const doTableChange = (page: TablePaginationConfig) => {
  searchParams.pageNum = page.current ?? 1
  searchParams.pageSize = page.pageSize ?? 10
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
  try {
    const res = await deleteUser({ id })
    if (res.data.code === 0) {
      message.success('删除成功')
      fetchData()
    } else {
      message.error('删除失败，' + res.data.message)
    }
  } catch (error) {
    console.error('删除用户失败:', error)
    message.error('删除失败，请稍后重试')
  }
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.user-manage-page {
  min-height: 0;
}

.user-manage-page__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.user-manage-page__header-title {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-manage-page__header-icon {
  font-size: 32px;
  color: #2563eb;
}

.user-manage-page__title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.user-manage-page__header-stats {
  display: flex;
  gap: 24px;
}

.user-manage-card {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: none;
}

.user-manage-page__search-wrapper {
  background: #f8fafc;
  border-radius: 6px;
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
  border-color: #2563eb;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
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

.user-manage-page__user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-manage-page__avatar {
  border: 2px solid rgba(37, 99, 235, 0.2);
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
