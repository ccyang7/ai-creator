<script setup lang="ts">
import { RouterView } from 'vue-router'
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
</script>

<template>
  <a-layout class="basic-layout">
    <a-layout-header class="basic-layout__header">
      <GlobalHeader />
    </a-layout-header>

    <a-layout-content class="basic-layout__content">
      <RouterView />
    </a-layout-content>

    <a-layout-footer class="basic-layout__footer">
      <GlobalFooter />
    </a-layout-footer>
  </a-layout>
</template>

<style scoped>
.basic-layout {
  min-height: 100vh;
}

.basic-layout__header {
  position: sticky;
  top: 0;
  z-index: 10;
  padding-inline: 24px;
  background: #fff;
  border-bottom: 1px solid rgba(5, 5, 5, 0.06);
}

.basic-layout__content {
  padding: 24px;
  flex: 1;
  background: #f5f5f5;
}

.basic-layout__footer {
  padding: 16px 24px;
  background: #fff;
}

@media (max-width: 768px) {
  .basic-layout__header {
    padding-inline: 12px;
  }

  .basic-layout__content {
    padding: 12px;
  }
}
</style>

