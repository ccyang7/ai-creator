<template>
  <div class="global-header">
    <div class="global-header__left" @click="handleLogoClick">
      <img class="global-header__logo" :src="logoUrl" alt="AI Creator" />
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
      <div v-if="loginUserStore.loginUser.id" class="global-header__account">
        <a-button class="global-header__create-btn" type="primary" @click="goCreate">
          <PlusOutlined />
          <span>新建应用</span>
        </a-button>
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
        <a-button type="primary" @click="goLogin">登录</a-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, h, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { DashboardOutlined, LogoutOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { logout } from '@/api/userController.ts'
import { message } from 'ant-design-vue'
import logoUrl from '@/assets/logo.png'

type HeaderMenuItem = {
  key: string
  label: string
  path: string
  icon?: Component
  roles?: string[]
}

// ==================== 菜单配置 ====================
// 顶部导航菜单配置（根据用户角色过滤显示）
const HEADER_MENU_ITEMS: HeaderMenuItem[] = [
  { key: 'home', label: '首页', path: '/' },
  {
    key: 'admin',
    label: '管理后台',
    icon: DashboardOutlined,
    path: '/admin/userManage',
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
    icon: item.icon ? h(item.icon) : undefined,
  }))
})

// 当前选中的菜单 key，与路由 path 对齐（仅在当前菜单列表中存在时选中）
const selectedKeys = computed(() => {
  const keys = menuItems.value.map((item) => item.key)
  if (route.path.startsWith('/admin')) {
    return keys.includes('/admin/userManage') ? ['/admin/userManage'] : []
  }
  return keys.includes(route.path) ? [route.path] : []
})

async function onMenuClick(info: { key: string }) {
  await router.push(info.key)
}

function handleLogoClick() {
  router.push('/')
}

function goLogin() {
  router.push('/user/login')
}

function goCreate() {
  router.push('/')
}

async function doLogOut() {
  try {
    const res = await logout()
    if (res.data.code === 0) {
      loginUserStore.setLoginUser({ userName: '未登录' })
      message.success('退出登录成功')
      await router.replace('/user/login')
      return
    }
    message.error(res.data.message || '退出登录失败')
  } catch (error) {
    console.error('退出登录失败:', error)
    message.error('退出登录失败，请稍后重试')
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

.global-header__account {
  display: flex;
  align-items: center;
  gap: 14px;
}

.global-header__create-btn {
  height: 34px;
  border-radius: 6px;
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

@media (max-width: 768px) {
  .global-header__create-btn span {
    display: none;
  }

  .global-header__create-btn {
    width: 34px;
    padding-inline: 0;
  }
}
</style>
