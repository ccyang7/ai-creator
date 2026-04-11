import axios from 'axios'
import { message } from 'ant-design-vue'

// 创建 Axios 实例
const myAxios = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8200/api',
  timeout: 60000,
  withCredentials: true,
})

// 全局请求拦截器
myAxios.interceptors.request.use(
  function (config) {
    // Do something before request is sent
    return config
  },
  function (error) {
    // Do something with request error
    return Promise.reject(error)
  },
)

// 全局响应拦截器
myAxios.interceptors.response.use(
  function (response) {
    const { data } = response
    // 未登录
    if (data.code === 40100) {
      // 不是获取用户信息的请求，并且用户目前不是已经在用户登录页面，则跳转到登录页面
      if (
        !response.request.responseURL.includes('user/get/login') &&
        !window.location.pathname.includes('/user/login')
      ) {
        message.warning('请先登录')
        window.location.href = `/user/login?redirect=${window.location.href}`
      }
    }
    return response
  },
  function (error) {
    // Any status codes that falls outside the range of 2xx cause this function to trigger
    // Do something with request error
    return Promise.reject(error)
  },
)

/**
 * 文件下载请求
 * 专门用于处理文件下载，返回 Blob 数据
 * @param url 请求 URL
 * @param params 请求参数
 * @param filename 文件名（可选）
 */
export const downloadFile = async (
  url: string,
  params?: Record<string, any>,
  filename?: string,
) => {
  try {
    const response = await myAxios.get(url, {
      params,
      responseType: 'blob', // 重要：设置为 blob 以接收二进制数据
    })

    // 检查响应头获取文件名
    let downloadFilename = filename || 'download.zip'
    const contentDisposition = response.headers['content-disposition']
    if (contentDisposition) {
      const filenameMatch = contentDisposition.match(/filename="(.+?)"/)
      if (filenameMatch && filenameMatch[1]) {
        downloadFilename = filenameMatch[1]
      }
    }

    // 创建 Blob 对象
    const blob = new Blob([response.data], { type: 'application/zip' })

    // 创建下载链接
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = downloadFilename
    document.body.appendChild(link)
    link.click()

    // 清理
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)

    return { success: true, filename: downloadFilename }
  } catch (error) {
    console.error('下载失败:', error)
    throw error
  }
}

export default myAxios
