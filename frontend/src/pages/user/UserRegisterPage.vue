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
const redirect = computed(() => (route.query.redirect as string) || '')

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
  } catch (e) {
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
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.register-card {
  width: 100%;
  max-width: 400px;
  border-radius: 8px;
  box-shadow:
    0 1px 2px 0 rgba(0, 0, 0, 0.03),
    0 1px 6px -1px rgba(0, 0, 0, 0.02),
    0 2px 4px 0 rgba(0, 0, 0, 0.02);
}

.register-card :deep(.ant-card-head) {
  border-bottom: 1px solid rgba(5, 5, 5, 0.06);
  font-weight: 600;
}

.register-page__footer {
  text-align: center;
  color: rgba(0, 0, 0, 0.65);
  font-size: 14px;
}

.register-page__footer a {
  margin-left: 4px;
  color: #1677ff;
}

.register-page__footer a:hover {
  color: #4096ff;
}

@media (max-width: 768px) {
  .register-page {
    padding: 12px;
    align-items: flex-start;
    padding-top: 24px;
  }
}
</style>
