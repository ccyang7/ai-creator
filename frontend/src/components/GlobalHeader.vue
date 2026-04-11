<template>
  <div class="global-header">
    <div class="global-header__left" @click="handleLogoClick">
      <img class="global-header__logo" src="/src/assets/logo.svg" alt="logo" />
      <div class="global-header__title">AI Creator</div>
    </div>

    <div class="global-header__menu">
      <a-menu
        mode="horizontal"
        :selected-keys="selectedKeys"
        :items="menuItems"
        @click="onMenuClick"
      />
    </div>

    <div class="global-header__right">
      <div v-if="loginUserStore.loginUser.id">
        <a-dropdown>
          <a-space>
            <a-avatar :src="loginUserStore.loginUser.userAvatar" />
            {{ loginUserStore.loginUser.userName ?? '无名' }}
          </a-space>

          <template #overlay>
            <a-menu>
              <a-menu-item @click="doLogOut">
                <LogoutOutlined />
                <span>退出登录</span>
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
      <div v-else>
        <a-button type="primary" href="/user/login" @click="goLogin">登录</a-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { LogoutOutlined } from '@ant-design/icons-vue'
import { logout } from '@/api/userController.ts'
import { message } from 'ant-design-vue'

type HeaderMenuItem = {
  key: string
  label: string
  path: string
  roles?: string[]
}

// ==================== 菜单配置 ====================
// 顶部导航菜单配置（根据用户角色过滤显示）
const HEADER_MENU_ITEMS: HeaderMenuItem[] = [
  { key: 'home', label: '首页', path: '/' },
  {
    key: 'admin-user-manage',
    label: '用户管理',
    path: '/admin/userManage',
    roles: ['admin'],
  },
  {
    key: 'admin-app-manage',
    label: '应用管理',
    path: '/admin/appManage',
    roles: ['admin'],
  },
  {
    key: 'admin-chat-history-manage',
    label: '对话管理',
    path: '/admin/chatHistoryManage',
    roles: ['admin'],
  },
]

// 全局登录用户状态
const loginUserStore = useLoginUserStore()

const route = useRoute()
const router = useRouter()

// 根据当前用户角色过滤菜单项：未配置 roles 的菜单所有登录用户可见，配置了则仅指定角色可见
const menuItems = computed(() => {
  const userRole = loginUserStore.loginUser.userRole
  return HEADER_MENU_ITEMS.filter((item) => {
    if (!item.roles || item.roles.length === 0) {
      return true
    }
    return !!userRole && item.roles.includes(userRole)
  }).map((item) => ({
    key: item.path,
    label: item.label,
  }))
})

// 当前选中的菜单 key，与路由 path 对齐（仅在当前菜单列表中存在时选中）
const selectedKeys = computed(() => {
  const keys = menuItems.value.map((item) => item.key)
  return keys.includes(route.path) ? [route.path] : []
})

function onMenuClick(info: { key: string }) {
  router.push(info.key).then(() => {
    window.location.reload()
  })
}

function handleLogoClick() {
  router.push('/')
}

function goLogin() {
  router.push('/user/login')
}

async function doLogOut() {
  const res = await logout()
  if (res.data.code === 0) {
    // 重置登录用户为未登录态
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    router.push('/user/login')
  } else {
    message.error('退出登录失败')
  }
}
</script>

<style scoped>
.global-header {
  height: 100%;
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.global-header__left {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  user-select: none;
}

.global-header__logo {
  width: 32px;
  height: 32px;
  object-fit: contain;
  border-radius: 6px;
  background: rgba(0, 0, 0, 0);
}

.global-header__title {
  color: rgba(0, 0, 0, 0.88);
  font-size: 16px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
}

.global-header__menu {
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */
}

.global-header__menu::-webkit-scrollbar {
  display: none;
}

.global-header__right {
  display: flex;
  align-items: center;
  flex: none;
}

:deep(.ant-menu) {
  background: transparent;
  border-bottom: none;
  color: rgba(0, 0, 0, 0.65);
  min-width: max-content;
}

:deep(.ant-menu-horizontal) {
  border-bottom: none;
}

:deep(.ant-menu-horizontal > .ant-menu-item),
:deep(.ant-menu-horizontal > .ant-menu-submenu) {
  margin-inline: 4px;
  border-radius: 8px;
}

:deep(.ant-menu-horizontal > .ant-menu-item-selected) {
  color: rgba(0, 0, 0, 0.88);
  background: rgba(22, 119, 255, 0.08);
}

:deep(.ant-menu-horizontal > .ant-menu-item:hover) {
  background: rgba(0, 0, 0, 0.04);
}
</style>
