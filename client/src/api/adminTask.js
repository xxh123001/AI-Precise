import request from '@/utils/request'

/**
 * 管理员任务管理相关API
 */
export const adminTaskApi = {
  /**
   * 获取任务分配列表
   * @param {Object} params 查询参数
   */
  getTaskAssignments(params) {
    return request({
      url: '/admin/tasks/assignments',
      method: 'get',
      params: {
        page: params.page || 0,
        size: params.size || 50,
        userId: params.userId,
        projectId: params.projectId,
        status: params.status,
        startDate: params.startDate,
        endDate: params.endDate,
        keyword: params.keyword
      }
    })
  },

  /**
   * 获取任务统计信息
   */
  getTaskStatistics(params) {
    return request({
      url: '/admin/tasks/statistics',
      method: 'get',
      params
    })
  },

  /**
   * 获取任务用户列表
   */
  getTaskUsers() {
    return request({
      url: '/admin/tasks/users',
      method: 'get'
    })
  },

  /**
   * 获取任务的标注数据
   * @param {number} imageId 图像ID
   * @param {number} userId 用户ID
   */
  getAnnotation(imageId, userId) {
    return request({
      url: `/admin/tasks/annotation/${imageId}/${userId}`,
      method: 'get'
    })
  },

  /**
   * 更新标注数据
   * @param {number} annotationId 标注ID
   * @param {Object} data 标注数据
   */
  updateAnnotation(annotationId, data) {
    return request({
      url: `/admin/tasks/annotation/${annotationId}`,
      method: 'put',
      data
    })
  }
}

export default adminTaskApi
