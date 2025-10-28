import request from '@/utils/request'

export const dashboardApi = {
  // 获取系统统计数据
  getSystemStats() {
    return request({
      url: '/admin/dashboard/stats',
      method: 'get'
    })
  },

  // 获取最近活动记录
  getRecentActivities(limit = 10) {
    return request({
      url: '/admin/dashboard/activities',
      method: 'get',
      params: { limit }
    })
  }
}
