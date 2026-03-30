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

declare module 'markdown-it' {
  export interface MarkdownItOptions {
    html?: boolean
    xhtmlOut?: boolean
    breaks?: boolean
    langPrefix?: string
    linkify?: boolean
    typographer?: boolean
    quotes?: string
    highlight?: (str: string, lang: string) => string
  }

  export interface Token {
    type: string
    tag: string
    attrs: [string, string][] | null
    map: number[] | null
    nesting: number
    level: number
    children: Token[] | null
    content: string
    markup: string
    info: string
    meta: any
    block: boolean
    hidden: boolean
  }

  export interface MarkdownIt {
    render(src: string): string
    renderInline(src: string): string
    use(plugin: any, ...params: any[]): this
    set(options: MarkdownItOptions): this
  }

  const MarkdownIt: {
    new (presetName?: 'default' | 'zero' | 'commonmark', options?: MarkdownItOptions): MarkdownIt
    (presetName?: 'default' | 'zero' | 'commonmark', options?: MarkdownItOptions): MarkdownIt
  }

  export default MarkdownIt
}
