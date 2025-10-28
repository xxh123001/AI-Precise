import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'
import router from '@/router'

// 创建axios实例
const service = axios.create({
  baseURL: '/api', // 统一使用相对路径，由Nginx代理到对应后端域名
  timeout: 300000 // 请求超时时间(5分钟)
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers['Authorization'] = `Bearer ${authStore.token}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data

    // 如果是文件下载等特殊情况，直接返回response
    if (response.config.responseType === 'blob') {
      return response
    }

    // API返回成功
    if (res.success) {
      return res
    } else {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  error => {
    console.error('响应错误:', error)
    
    if (error.response) {
      const { status, data } = error.response
      const authStore = useAuthStore()

      switch (status) {
        case 401:
          // 未登录或token过期 - 立即清空store并跳转
          console.log('🔒 401错误: 未登录或token过期')
          authStore.clearAuth()
          ElMessage.warning('登录状态已过期，请重新登录')
          setTimeout(() => {
            router.push('/login')
          }, 500)
          break
          
        case 403:
          // 权限不足 - 也清空store并跳转登录
          console.log('🔒 403错误: 权限不足')
          authStore.clearAuth()
          ElMessage.error('权限不足，请重新登录')
          setTimeout(() => {
            router.push('/login')
          }, 500)
          break
          
        case 404:
          ElMessage.error('请求的资源不存在')
          break
          
        case 500:
          ElMessage.error('服务器内部错误')
          break
          
        default:
          ElMessage.error(data?.message || `请求失败 (${status})`)
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else if (error.code === 'ERR_NETWORK') {
      // 网络错误，可能是未登录
      console.log('🔒 网络错误，清空认证信息')
      const authStore = useAuthStore()
      authStore.clearAuth()
      ElMessage.error('网络错误，请重新登录')
      setTimeout(() => {
        router.push('/login')
      }, 500)
    } else {
      ElMessage.error('网络错误，请检查网络连接')
    }

    return Promise.reject(error)
  }
)

export default service
