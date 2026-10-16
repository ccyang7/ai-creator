import { useLoginUserStore } from '@/stores/loginUser'
import { message } from 'ant-design-vue'
import router from '@/router'
import { getSafeRedirect } from '@/utils/navigation'

// 是否为首次获取登录用户
let firstFetchLoginUser = true

/**
 * 全局权限校验
 */
router.beforeEach(async (to) => {
  const loginUserStore = useLoginUserStore()
  // 确保页面刷新，首次加载时，能够等后端返回用户信息后再校验权限
  if (firstFetchLoginUser) {
    await loginUserStore.fetchLoginUser()
    firstFetchLoginUser = false
  }

  const loginUser = loginUserStore.loginUser
  const isLoggedIn = Boolean(loginUser.id)

  if (to.meta.guestOnly && isLoggedIn) {
    return getSafeRedirect(to.query.redirect)
  }
  if (to.meta.requiresAuth && !isLoggedIn) {
    message.warning('请先登录')
    return { path: '/user/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && loginUser.userRole !== 'admin') {
    message.error('没有权限')
    return { path: '/' }
  }

  return true
})
