import request from '@/utils/request'

export const userApi = {
  // 获取用户列表
  getUserList(params) {
    return request({
      url: '/admin/users',
      method: 'get',
      params
    })
  },

  // 创建用户
  createUser(data) {
    return request({
      url: '/admin/users',
      method: 'post',
      data
    })
  },

  // 更新用户
  updateUser(id, data) {
    return request({
      url: `/admin/users/${id}`,
      method: 'put',
      data
    })
  },

  // 删除用户
  deleteUser(id) {
    return request({
      url: `/admin/users/${id}`,
      method: 'delete'
    })
  },

  // 启用用户
  enableUser(id) {
    return request({
      url: `/admin/users/${id}/enable`,
      method: 'put'
    })
  },

  // 禁用用户
  disableUser(id) {
    return request({
      url: `/admin/users/${id}/disable`,
      method: 'put'
    })
  },

  // 重置密码
  resetPassword(id, newPassword) {
    return request({
      url: `/admin/users/${id}/reset-password`,
      method: 'put',
      params: { newPassword }
    })
  },

  // 获取用户详情
  getUserById(id) {
    return request({
      url: `/admin/users/${id}`,
      method: 'get'
    })
  },

  // 获取用户的项目列表
  getUserProjects(userId) {
    return request({
      url: `/admin/users/${userId}/projects`,
      method: 'get'
    })
  },

  // 获取标签配置列表
  getLabelConfigs(params = {}) {
    return request({
      url: '/admin/label-configs',
      method: 'get',
      params: {
        page: 0,
        size: 100,
        ...params
      }
    })
  }
}
