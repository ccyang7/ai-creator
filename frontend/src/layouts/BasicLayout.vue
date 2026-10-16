<script setup lang="ts">
import { computed } from 'vue'
import { RouterView } from 'vue-router'
import { useRoute } from 'vue-router'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'

const route = useRoute()
const hideFooter = computed(() => Boolean(route.meta.hideFooter))
const fullBleed = computed(() => Boolean(route.meta.fullBleed))
</script>

<template>
  <a-layout class="basic-layout">
    <a-layout-header class="basic-layout__header">
      <GlobalHeader />
    </a-layout-header>

    <a-layout-content
      :class="['basic-layout__content', { 'basic-layout__content--full-bleed': fullBleed }]"
    >
      <RouterView />
    </a-layout-content>

    <a-layout-footer v-if="!hideFooter" class="basic-layout__footer">
      <GlobalFooter />
    </a-layout-footer>
  </a-layout>
</template>

<style scoped>
.basic-layout {
  min-height: 100vh;
  background: var(--ac-page-background);
}

.basic-layout__header {
  position: sticky;
  top: 0;
  z-index: 10;
  padding-inline: 24px;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid rgba(203, 213, 225, 0.72);
  backdrop-filter: blur(14px);
}

.basic-layout__content {
  padding: 24px;
  flex: 1;
  background: var(--ac-page-background);
}

.basic-layout__content--full-bleed {
  padding: 0;
}

.basic-layout__footer {
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.88);
  border-top: 1px solid rgba(203, 213, 225, 0.64);
  backdrop-filter: blur(10px);
}

@media (max-width: 768px) {
  .basic-layout__header {
    padding-inline: 12px;
  }

  .basic-layout__content {
    padding: 12px;
  }

  .basic-layout__content--full-bleed {
    padding: 0;
  }
}
</style>

