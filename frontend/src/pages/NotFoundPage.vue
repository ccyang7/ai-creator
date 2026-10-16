<template>
  <div class="not-found-page">
    <a-empty description="页面不存在">
      <a-button type="primary" @click="router.replace('/')">返回首页</a-button>
    </a-empty>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<style scoped>
.not-found-page {
  min-height: 60vh;
  display: grid;
  place-items: center;
  border: 1px solid rgba(203, 213, 225, 0.72);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
  backdrop-filter: blur(10px);
}
</style>
