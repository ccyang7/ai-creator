// 根据后端接口生成前端请求和 TS 模型代码
import type { GenerateServiceProps } from '@umijs/openapi'

export default {
  requestLibPath: "import request from '@/request'",
  requestOptionsType: 'Record<string, unknown>',
  schemaPath: 'http://localhost:8200/api/v3/api-docs',
  serversPath: './src',
  hook: {
    customType(schemaObject, namespace, originGetType) {
      if (
        (schemaObject?.type === 'integer' || schemaObject?.type === 'number') &&
        schemaObject.format === 'int64'
      ) {
        return 'string'
      }
      return originGetType(schemaObject, namespace)
    },
  },
} satisfies GenerateServiceProps
