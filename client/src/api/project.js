import request from '@/utils/request'

export const projectApi = {
  // 获取项目列表
  getProjects(params = {}) {
    return request({
      url: '/admin/projects',
      method: 'get',
      params
    })
  },

  // 创建项目
  create(data) {
    return request({
      url: '/admin/projects',
      method: 'post',
      data
    })
  },

  // 获取项目详情
  getProjectDetail(id) {
    return request({
      url: `/admin/projects/${id}`,
      method: 'get'
    })
  },

  // 获取项目用户列表
  getProjectUsers(id) {
    return request({
      url: `/admin/projects/${id}/users`,
      method: 'get'
    })
  },

  // 更新项目
  update(id, data) {
    return request({
      url: `/admin/projects/${id}`,
      method: 'put',
      data
    })
  },

  // 删除项目
  delete(id) {
    return request({
      url: `/admin/projects/${id}`,
      method: 'delete'
    })
  },

  // 添加用户到项目
  addUsersToProject(projectId, userIds) {
    return request({
      url: `/admin/projects/${projectId}/users`,
      method: 'post',
      data: { userIds }
    })
  },

  // 从项目中移除用户
  removeUserFromProject(projectId, userId) {
    return request({
      url: `/admin/projects/${projectId}/users/${userId}`,
      method: 'delete'
    })
  }
}
