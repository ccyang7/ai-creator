import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // 首页
    {
      path: '/',
      name: 'home',
      component: () => import('@/pages/HomePage.vue'),
      meta: { fullBleed: true },
    },
    // 用户相关页面
    {
      path: '/user/login',
      name: '用户登录',
      component: () => import('@/pages/user/UserLoginPage.vue'),
      meta: { guestOnly: true },
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: () => import('@/pages/user/UserRegisterPage.vue'),
      meta: { guestOnly: true },
    },
    // 管理员页面
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      redirect: '/admin/userManage',
      meta: { requiresAuth: true, requiresAdmin: true, hideFooter: true, fullBleed: true },
      children: [
        {
          path: 'userManage',
          name: '用户管理',
          component: () => import('@/pages/admin/UserManagePage.vue'),
        },
        {
          path: 'appManage',
          name: '应用管理',
          component: () => import('@/pages/admin/AppManagePage.vue'),
        },
        {
          path: 'chatHistoryManage',
          name: '对话管理',
          component: () => import('@/pages/admin/ChatHistoryManagePage.vue'),
        },
      ],
    },
    // 应用相关页面
    {
      path: '/app/chat/:id',
      name: '应用对话',
      component: () => import('@/pages/app/AppChatPage.vue'),
      meta: { hideFooter: true, immersive: true, fullBleed: true },
    },
    {
      path: '/app/edit/:id',
      name: '应用编辑',
      component: () => import('@/pages/app/AppEditPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/:pathMatch(.*)*',
      name: '页面不存在',
      component: () => import('@/pages/NotFoundPage.vue'),
    },
  ],
})

export default router
