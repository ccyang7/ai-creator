/// <reference types="vite/client" />

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
