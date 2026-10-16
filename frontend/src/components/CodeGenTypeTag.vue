<template>
  <a-tag :color="tagConfig.color" class="code-gen-type-tag">
    <span class="code-gen-type-tag__icon">{{ tagConfig.icon }}</span>
    <span class="code-gen-type-tag__text">{{ tagConfig.text }}</span>
  </a-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface CodeGenTypeTagProps {
  type?: string
}

const props = withDefaults(defineProps<CodeGenTypeTagProps>(), {
  type: '',
})

const tagConfig = computed(() => {
  const typeMap: Record<string, { icon: string; text: string; color: string }> = {
    html: { icon: '🌐', text: 'HTML', color: 'green' },
    multi_file: { icon: '📁', text: 'MULTI_FILE', color: 'orange' },
    vue_project: { icon: '⚡', text: 'VUE_PROJECT', color: 'blue' },
  }

  const config = typeMap[props.type || '']
  if (config) {
    return config
  }

  // 默认配置
  return {
    icon: '📄',
    text: props.type || '-',
    color: 'default',
  }
})
</script>

<style scoped>
.code-gen-type-tag {
  font-size: 12px;
  padding: 2px 12px;
  border-radius: 12px;
  font-weight: 500;
}

.code-gen-type-tag__icon {
  margin-right: 4px;
}

.code-gen-type-tag__text {
  letter-spacing: 0.5px;
}
</style>
