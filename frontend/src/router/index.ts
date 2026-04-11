import { createRouter, createWebHistory } from 'vue-router'

// 页面组件导入
import HomePage from '@/pages/HomePage.vue'
import UserLoginPage from '@/pages/user/UserLoginPage.vue'
import UserRegisterPage from '@/pages/user/UserRegisterPage.vue'
import UserManagePage from '@/pages/admin/UserManagePage.vue'
import AppManagePage from '@/pages/admin/AppManagePage.vue'
import ChatHistoryManagePage from '@/pages/admin/ChatHistoryManagePage.vue'
import AppChatPage from '@/pages/app/AppChatPage.vue'
import AppEditPage from '@/pages/app/AppEditPage.vue'


const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // 首页
    {
      path: '/',
      name: 'home',
      component: HomePage,
    },
    // 用户相关页面
    {
      path:'/user/login',
      name:'用户登录',
      component: UserLoginPage
    },
    {
      path:'/user/register',
      name:'用户注册',
      component: UserRegisterPage
    },
    // 管理员页面
    {
      path:'/admin/userManage',
      name:'用户管理',
      component: UserManagePage,
    },
    {
      path:'/admin/appManage',
      name:'应用管理',
      component: AppManagePage,
    },
    {
      path:'/admin/chatHistoryManage',
      name:'对话管理',
      component: ChatHistoryManagePage,
    },
    // 应用相关页面
    {
      path:'/app/chat/:id',
      name:'应用对话',
      component: AppChatPage,
    },
    {
      path:'/app/edit/:id',
      name:'应用编辑',
      component: AppEditPage,
    },
  ],
})

export default router
