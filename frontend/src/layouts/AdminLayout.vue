<template>
  <div class="admin-layout">
    <aside class="admin-layout__sidebar">
      <div class="admin-layout__brand">
        <span class="admin-layout__brand-mark">AC</span>
        <div>
          <strong>管理后台</strong>
          <span>AI Creator</span>
        </div>
      </div>

      <a-menu
        mode="inline"
        :selected-keys="selectedKeys"
        :items="menuItems"
        class="admin-layout__menu"
        @click="onMenuClick"
      />

      <div class="admin-layout__sidebar-note">管理员工作区</div>
    </aside>

    <section class="admin-layout__workspace">
      <div class="admin-layout__toolbar">
        <div>
          <div class="admin-layout__eyebrow">ADMINISTRATION</div>
          <h1>{{ pageTitle }}</h1>
        </div>
        <a-button type="text" @click="goHome">返回前台</a-button>
      </div>
      <div class="admin-layout__content">
        <RouterView />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, h } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { AppstoreOutlined, MessageOutlined, UserOutlined } from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()

const menuItems = [
  { key: '/admin/userManage', icon: h(UserOutlined), label: '用户管理' },
  { key: '/admin/appManage', icon: h(AppstoreOutlined), label: '应用管理' },
  { key: '/admin/chatHistoryManage', icon: h(MessageOutlined), label: '对话记录' },
]

const selectedKeys = computed(() => {
  const match = menuItems.find((item) => route.path.startsWith(item.key))
  return [match?.key || '/admin/userManage']
})

const pageTitle = computed(() => {
  const match = menuItems.find((item) => route.path.startsWith(item.key))
  return match?.label || '管理后台'
})

const onMenuClick = ({ key }: { key: string }) => {
  void router.push(key)
}

const goHome = () => {
  void router.push('/')
}
</script>

<style scoped>
.admin-layout {
  min-height: calc(100vh - 64px);
  display: flex;
  background: var(--ac-page-background);
}

.admin-layout__sidebar {
  width: 232px;
  flex: none;
  display: flex;
  flex-direction: column;
  padding: 24px 14px 16px;
  background: linear-gradient(180deg, #0f172a 0%, #111827 58%, #13213a 100%);
}

.admin-layout__brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 10px 28px;
  color: #fff;
}

.admin-layout__brand-mark {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: #2563eb;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.admin-layout__brand strong,
.admin-layout__brand span:not(.admin-layout__brand-mark) {
  display: block;
}

.admin-layout__brand strong {
  font-size: 14px;
}

.admin-layout__brand span:not(.admin-layout__brand-mark) {
  margin-top: 2px;
  color: #9ca3af;
  font-size: 11px;
}

.admin-layout__menu {
  background: transparent;
  border-inline-end: 0;
  color: #d1d5db;
}

.admin-layout__menu :deep(.ant-menu-item) {
  margin: 4px 0;
  border-radius: 6px;
}

.admin-layout__menu :deep(.ant-menu-item-selected) {
  background: #2563eb;
  color: #fff;
}

.admin-layout__menu :deep(.ant-menu-item:hover) {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.admin-layout__sidebar-note {
  margin-top: auto;
  padding: 14px 10px 0;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  color: #6b7280;
  font-size: 12px;
}

.admin-layout__workspace {
  min-width: 0;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.admin-layout__toolbar {
  min-height: 82px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 28px;
  background: rgba(255, 255, 255, 0.9);
  border-bottom: 1px solid rgba(203, 213, 225, 0.72);
  backdrop-filter: blur(12px);
}

.admin-layout__eyebrow {
  color: #6b7280;
  font-size: 11px;
  letter-spacing: 0.08em;
}

.admin-layout__toolbar h1 {
  margin: 2px 0 0;
  color: #111827;
  font-size: 22px;
  font-weight: 650;
}

.admin-layout__content {
  flex: 1;
  min-width: 0;
  overflow: auto;
  padding: 24px;
}

@media (max-width: 768px) {
  .admin-layout {
    min-height: calc(100vh - 64px);
  }

  .admin-layout__sidebar {
    width: 64px;
    padding: 16px 8px;
  }

  .admin-layout__brand {
    justify-content: center;
    padding: 0 0 20px;
  }

  .admin-layout__brand > div,
  .admin-layout__sidebar-note {
    display: none;
  }

  .admin-layout__menu :deep(.ant-menu-title-content) {
    display: none;
  }

  .admin-layout__menu :deep(.ant-menu-item) {
    padding-inline: 0 !important;
    text-align: center;
  }

  .admin-layout__menu :deep(.ant-menu-item .ant-menu-item-icon) {
    margin-inline-end: 0;
    font-size: 17px;
  }

  .admin-layout__toolbar {
    min-height: 70px;
    padding: 14px 16px;
  }

  .admin-layout__toolbar h1 {
    font-size: 18px;
  }

  .admin-layout__content {
    padding: 12px;
  }
}
</style>
