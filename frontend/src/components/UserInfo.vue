<template>
  <div class="user-info" :class="`user-info--${size}`">
    <a-avatar :src="userAvatar" :size="avatarSize" class="user-info__avatar">
      <UserOutlined />
    </a-avatar>
    <span v-if="showName" class="user-info__name">{{ userName || '未知用户' }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { UserOutlined } from '@ant-design/icons-vue'

interface UserInfoProps {
  userAvatar?: string
  userName?: string
  showName?: boolean
  size?: 'small' | 'default' | 'large'
}

const props = withDefaults(defineProps<UserInfoProps>(), {
  showName: true,
  size: 'default',
})

const avatarSize = computed(() => {
  const sizeMap = {
    small: 24,
    default: 32,
    large: 40,
  }
  return sizeMap[props.size] || sizeMap.default
})
</script>

<style scoped>
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-info__avatar {
  border: 2px solid #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.user-info__name {
  font-size: 14px;
  color: rgba(0, 0, 0, 0.85);
  font-weight: 500;
}

.user-info--small .user-info__name {
  font-size: 13px;
}

.user-info--large .user-info__name {
  font-size: 15px;
  font-weight: 600;
}
</style>
