import request from '@/utils/request'

export const userTaskApi = {
  // 获取分配的标注任务
  getAssignments(params = {}) {
    return request({
      url: '/user/assignments',
      method: 'get',
      params: {
        page: params.page || 1,
        size: params.size || 10,
        ...params
      }
    })
  },

  // 获取图像详情
  getImageDetail(id) {
    return request({
      url: `/user/images/${id}`,
      method: 'get'
    })
  },

  // 保存标注结果
  saveAnnotation(data) {
    return request({
      url: '/user/annotations',
      method: 'post',
      data
    })
  },

  // 更新标注结果
  updateAnnotation(id, data) {
    return request({
      url: `/user/annotations/${id}`,
      method: 'put',
      data
    })
  },

  // 获取标注历史
  getAnnotationHistory() {
    return request({
      url: '/user/annotations',
      method: 'get'
    })
  },

  // 获取图像标注结果
  getImageAnnotation(imageId) {
    return request({
      url: `/user/annotations/image/${imageId}`,
      method: 'get'
    })
  },

  // 获取用户任务ID列表（用于快速导航）
  getTaskIdList() {
    return request({
      url: '/user/task-list',
      method: 'get'
    })
  },

  // 根据任务ID获取任务详情
  getTaskById(taskId) {
    return request({
      url: `/user/task/${taskId}`,
      method: 'get'
    })
  },

  // 标记任务为完成状态
  completeTask(taskId) {
    return request({
      url: `/user/task/${taskId}/complete`,
      method: 'post'
    })
  },

  // 获取用户的项目列表
  getUserProjects() {
    return request({
      url: '/user/projects',
      method: 'get'
    })
  },

  // 获取项目详情和任务列表
  getProjectTasks(projectId) {
    return request({
      url: `/user/projects/${projectId}/tasks`,
      method: 'get'
    })
  },

  // 获取项目详情
  getProjectDetail(projectId) {
    return request({
      url: `/user/projects/${projectId}`,
      method: 'get'
    })
  }
}
