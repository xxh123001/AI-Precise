import request from '@/utils/request'

export const systemConfigApi = {
  // 获取全局裁剪配置（管理员）
  getGlobalCropConfig() {
    return request({
      url: '/admin/system-config/global-crop',
      method: 'get'
    })
  },

  // 获取用户全局裁剪配置（用户端）
  getUserGlobalCropConfig() {
    return request({
      url: '/user/system-config/global-crop',
      method: 'get'
    })
  },

  // 更新全局裁剪配置
  updateGlobalCropConfig(params) {
    return request({
      url: '/admin/system-config/global-crop',
      method: 'put',
      params
    })
  }
}

