import request from '@/utils/request'

export const authApi = {
  // 用户登录
  login(data) {
    return request({
      url: '/auth/login',
      method: 'post',
      data
    })
  },

  // 用户登出
  logout() {
    return request({
      url: '/auth/logout',
      method: 'post'
    })
  },

  // 获取用户信息
  getProfile() {
    return request({
      url: '/auth/profile',
      method: 'get'
    })
  },

  // 刷新令牌
  refreshToken() {
    return request({
      url: '/auth/refresh',
      method: 'post'
    })
  }
}
