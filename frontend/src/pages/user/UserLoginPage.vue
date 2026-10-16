<template>
  <div class="login-page">
    <a-card class="login-card" title="用户登录" :bordered="true">
      <a-form
        :model="formState"
        name="login"
        layout="vertical"
        autocomplete="off"
        @finish="onFinish"
        @finish-failed="onFinishFailed"
      >
        <a-form-item
          label="账号"
          name="userAccount"
          :rules="[{ required: true, message: '请输入账号' }]"
        >
          <a-input
            v-model:value="formState.userAccount"
            placeholder="请输入账号"
            size="large"
            allow-clear
          />
        </a-form-item>

        <a-form-item
          label="密码"
          name="userPassword"
          :rules="[
            { required: true, message: '请输入密码' },
            { min: 8, message: '密码长度不能小于8位' },
          ]"
        >
          <a-input-password
            v-model:value="formState.userPassword"
            placeholder="请输入密码"
            size="large"
            allow-clear
          />
        </a-form-item>

        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" block :loading="loading">
            登录
          </a-button>
        </a-form-item>

        <div class="login-page__footer">
          还没有账号？
          <router-link to="/user/register">立即注册</router-link>
        </div>
      </a-form>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { login } from '@/api/userController.ts'
import { getSafeRedirect } from '@/utils/navigation'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()
const loading = ref(false)

const formState = reactive<API.UserLoginDTO>({
  userAccount: '',
  userPassword: '',
})

/*
 * 表单提交处理函数
 */
const onFinish = async (values: API.UserLoginDTO) => {
  if (loading.value) return

  loading.value = true
  try {
    const res = await login(values)
    if (res.data.code === 0 && res.data.data) {
      await loginUserStore.fetchLoginUser()
      message.success('登录成功')
      await router.replace(getSafeRedirect(route.query.redirect))
      return
    }
    message.error(res.data.message || '登录失败')
  } catch (error) {
    console.error('登录请求失败:', error)
    message.error('网络异常，请稍后重试')
  } finally {
    loading.value = false
  }
}

function onFinishFailed() {
  message.error('请完善登录信息后再提交')
}
</script>

<style scoped>
.login-page {
  min-height: calc(100vh - 64px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background-color: var(--ac-page-bg);
  background-image:
    radial-gradient(circle at 18% 14%, rgba(37, 99, 235, 0.16), transparent 30%),
    radial-gradient(circle at 82% 20%, rgba(8, 145, 178, 0.13), transparent 28%);
  position: relative;
  overflow: hidden;
}

.login-page::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(37, 99, 235, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(37, 99, 235, 0.045) 1px, transparent 1px);
  background-size: 36px 36px;
  -webkit-mask-image: linear-gradient(to bottom, #000, transparent 82%);
  mask-image: linear-gradient(to bottom, #000, transparent 82%);
  pointer-events: none;
}

.login-card {
  width: 100%;
  max-width: 400px;
  border: 1px solid rgba(191, 219, 254, 0.82);
  border-radius: 14px;
  box-shadow: 0 24px 64px rgba(30, 64, 175, 0.14);
  backdrop-filter: blur(14px);
  background: rgba(255, 255, 255, 0.92);
  animation: cardEntrance 0.6s ease-out;
  position: relative;
  z-index: 1;
}

@keyframes cardEntrance {
  0% {
    opacity: 0;
    transform: translateY(30px);
  }
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-card :deep(.ant-card-head) {
  border-bottom: 1px solid rgba(5, 5, 5, 0.06);
  font-weight: 600;
  font-size: 20px;
}

.login-page__footer {
  text-align: center;
  color: rgba(0, 0, 0, 0.65);
  font-size: 14px;
}

.login-page__footer a {
  margin-left: 4px;
  color: var(--ac-primary);
  font-weight: 500;
  text-decoration: none;
  transition: all 0.3s ease;
}

.login-page__footer a:hover {
  color: var(--ac-primary-hover);
  text-decoration: underline;
}

@media (max-width: 768px) {
  .login-page {
    padding: 12px;
    align-items: flex-start;
    padding-top: 24px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .login-card {
    animation: none;
  }
}
</style>
