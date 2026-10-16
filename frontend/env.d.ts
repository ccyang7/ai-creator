/// <reference types="vite/client" />

// 环境变量类型定义
interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  readonly VITE_PREVIEW_BASE_URL: string
  readonly VITE_DEPLOY_BASE_URL: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
