import request from '@/utils/request'

export const labelConfigApi = {
  // 获取标签配置列表
  getLabelConfigList(params) {
    return request({
      url: '/admin/label-configs',
      method: 'get',
      params
    })
  },

  // 创建标签配置
  createLabelConfig(data) {
    return request({
      url: '/admin/label-configs',
      method: 'post',
      data
    })
  },

  // 更新标签配置
  updateLabelConfig(id, data) {
    return request({
      url: `/admin/label-configs/${id}`,
      method: 'put',
      data
    })
  },

  // 删除标签配置
  deleteLabelConfig(id) {
    return request({
      url: `/admin/label-configs/${id}`,
      method: 'delete'
    })
  },

  // 激活标签配置
  activateLabelConfig(id) {
    return request({
      url: `/admin/label-configs/${id}/enable`,
      method: 'put'
    })
  },

  // 停用标签配置
  deactivateLabelConfig(id) {
    return request({
      url: `/admin/label-configs/${id}/disable`,
      method: 'put'
    })
  }
}
