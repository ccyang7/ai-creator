<template>
  <div class="login-page">
    <a-card class="login-card" title="用户登录" :bordered="true">
      <a-form
        :model="formState"
        name="login"
        layout="vertical"
        autocomplete="off"
        @finish="onFinish"
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

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

const formState = reactive<API.UserLoginDTO>({
  userAccount: '',
  userPassword: '',
})

/*
 * 表单提交处理函数
 */
const onFinish = async (values: API.UserLoginDTO) => {
  const res = await login(values)
  //   登录成功
  if (res.data.code === 0 && res.data.data) {
    await loginUserStore.fetchLoginUser()
    message.success('登录成功')
    router.push({
      path: '/',
      replace: true,
    })
  } else {
    message.error('登录失败' + res.data.message)
  }
}

function onFinishFailed() {
  message.error('请完善登录信息后再提交')
}
</script>

<style scoped>
.login-page {
  min-height: 60vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  border-radius: 8px;
  box-shadow:
    0 1px 2px 0 rgba(0, 0, 0, 0.03),
    0 1px 6px -1px rgba(0, 0, 0, 0.02),
    0 2px 4px 0 rgba(0, 0, 0, 0.02);
}

.login-card :deep(.ant-card-head) {
  border-bottom: 1px solid rgba(5, 5, 5, 0.06);
  font-weight: 600;
}

.login-page__footer {
  text-align: center;
  color: rgba(0, 0, 0, 0.65);
  font-size: 14px;
}

.login-page__footer a {
  margin-left: 4px;
  color: #1677ff;
}

.login-page__footer a:hover {
  color: #4096ff;
}

@media (max-width: 768px) {
  .login-page {
    padding: 12px;
    align-items: flex-start;
    padding-top: 24px;
  }
}
</style>
