<template>
  <div class="register-page">
    <a-card class="register-card" title="用户注册" :bordered="true">
      <a-form
        :model="formState"
        name="register"
        layout="vertical"
        autocomplete="off"
        @finish="onFinish"
        @finishFailed="onFinishFailed"
      >
        <a-form-item
          label="账号"
          name="userAccount"
          :rules="[
            { required: true, message: '请输入账号' },
            { min: 4, message: '账号长度不能小于4位' },
          ]"
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

        <a-form-item
          label="确认密码"
          name="checkPassword"
          :rules="[
            { required: true, message: '请再次输入密码' },
            { validator: validateCheckPassword, trigger: 'change' },
          ]"
        >
          <a-input-password
            v-model:value="formState.checkPassword"
            placeholder="请再次输入密码"
            size="large"
            allow-clear
          />
        </a-form-item>

        <a-form-item>
          <a-button type="primary" html-type="submit" size="large" block :loading="loading">
            注册
          </a-button>
        </a-form-item>

        <div class="register-page__footer">
          已有账号？
          <router-link :to="toLoginLink">去登录</router-link>
        </div>
      </a-form>
    </a-card>
  </div>
</template>

<script lang="ts" setup>
import { computed, reactive, ref } from 'vue'
import type { RuleObject } from 'ant-design-vue/es/form'
import type { StoreValue } from 'ant-design-vue/es/form/interface'
import { message } from 'ant-design-vue'
import { useRoute, useRouter } from 'vue-router'
import { register } from '@/api/userController'
import { getSafeRedirect } from '@/utils/navigation'

const router = useRouter()
const route = useRoute()

// 避免重复点击触发并发注册请求
const loading = ref(false)

// 直接使用项目内的注册 DTO，字段名与后端接口保持一致
const formState = reactive<API.UserRegisterDTO>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

// 如果是从鉴权拦截跳来的，会携带 redirect，这里透传到登录页，便于登录后回跳原页面
const redirect = computed(() => getSafeRedirect(route.query.redirect, ''))

const toLoginLink = computed(() => {
  if (!redirect.value) return '/user/login'
  return { path: '/user/login', query: { redirect: redirect.value } }
})

// 自定义校验：确认密码必须与密码一致
async function validateCheckPassword(_rule: RuleObject, value: StoreValue) {
  if (!value) return Promise.resolve()
  if (value !== formState.userPassword) {
    return Promise.reject(new Error('两次输入的密码不一致'))
  }
  return Promise.resolve()
}

async function onFinish(values: API.UserRegisterDTO) {
  if (loading.value) return
  loading.value = true
  try {
    const res = await register(values)
    if (res.data.code === 0 && res.data.data) {
      message.success('注册成功，请登录')
      // 注册成功后不自动登录：跳转到登录页由用户登录（也符合后端常见的会话策略）
      await router.replace(toLoginLink.value)
      return
    }
    message.error(res.data.message ?? '注册失败，请重试')
  } catch {
    message.error('网络异常，请稍后重试')
  } finally {
    loading.value = false
  }
}

function onFinishFailed() {
  message.error('请完善注册信息后再提交')
}
</script>

<style scoped>
.register-page {
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

.register-page::before {
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

.register-card {
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

.register-card :deep(.ant-card-head) {
  border-bottom: 1px solid rgba(5, 5, 5, 0.06);
  font-weight: 600;
  font-size: 20px;
}

.register-page__footer {
  text-align: center;
  color: rgba(0, 0, 0, 0.65);
  font-size: 14px;
}

.register-page__footer a {
  margin-left: 4px;
  color: var(--ac-primary);
  font-weight: 500;
  text-decoration: none;
  transition: all 0.3s ease;
}

.register-page__footer a:hover {
  color: var(--ac-primary-hover);
  text-decoration: underline;
}

@media (max-width: 768px) {
  .register-page {
    padding: 12px;
    align-items: flex-start;
    padding-top: 24px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .register-card {
    animation: none;
  }
}
</style>
