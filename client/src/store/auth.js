import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  // 状态
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))

  // 计算属性
  const isAuthenticated = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 'ADMIN')
  const isUser = computed(() => user.value?.role === 'USER')

  // 方法
  const setAuth = (tokenValue, userValue) => {
    token.value = tokenValue
    user.value = userValue
    localStorage.setItem('token', tokenValue)
    localStorage.setItem('user', JSON.stringify(userValue))
  }

  const clearAuth = () => {
    console.log('🧹 清空认证信息')
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    // 清空sessionStorage中可能的其他认证相关数据
    sessionStorage.clear()
  }

  const login = async (credentials) => {
    try {
      const response = await authApi.login(credentials)
      const { accessToken, user: userData } = response.data
      setAuth(accessToken, userData)
      return { success: true }
    } catch (error) {
      return { 
        success: false, 
        message: error.response?.data?.message || '登录失败' 
      }
    }
  }

  const logout = async () => {
    try {
      await authApi.logout()
    } catch (error) {
      console.error('登出失败:', error)
    } finally {
      clearAuth()
    }
  }

  const getCurrentUser = async () => {
    try {
      const response = await authApi.getProfile()
      user.value = response.data
      localStorage.setItem('user', JSON.stringify(response.data))
    } catch (error) {
      console.error('获取用户信息失败:', error)
      clearAuth()
    }
  }

  return {
    token,
    user,
    isAuthenticated,
    isAdmin,
    isUser,
    setAuth,
    clearAuth,
    login,
    logout,
    getCurrentUser
  }
})
