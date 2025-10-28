import request from '@/utils/request'

export const imageApi = {
  // 获取图像列表
  getImageList(params) {
    return request({
      url: '/admin/images',
      method: 'get',
      params
    })
  },

  // 上传图像
  uploadImage(data) {
    return request({
      url: '/admin/images/upload',
      method: 'post',
      data,
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  // 删除图像
  deleteImage(id) {
    return request({
      url: `/admin/images/${id}`,
      method: 'delete'
    })
  },

  // 批量分配图像
  assignImages(data) {
    return request({
      url: '/admin/images/assign',
      method: 'post',
      data
    })
  },

  // 收回单个图像分配
  revokeAssignment(assignmentId) {
    return request({
      url: `/user/revoke-assignment/${assignmentId}`,
      method: 'delete'
    })
  },

  // 根据图像ID收回分配
  revokeAssignmentByImage(imageId) {
    return request({
      url: `/user/revoke-assignment-by-image/${imageId}`,
      method: 'delete'
    })
  },

  // 批量收回图像分配（根据图像ID）
  batchRevokeAssignmentsByImage(imageIds) {
    return request({
      url: '/user/batch-revoke-assignments',
      method: 'post',
      data: {
        assignmentIds: imageIds
      }
    })
  },

  // 设置图像标签配置
  setImageLabelConfig(id, labelConfigId) {
    return request({
      url: `/admin/images/${id}/label-config`,
      method: 'put',
      params: { labelConfigId }
    })
  },

  // 批量设置图像标签配置
  batchSetImageLabelConfig(data) {
    return request({
      url: '/admin/images/batch/label-config',
      method: 'put',
      data
    })
  },

  // 获取图像的所有标注
  getImageAnnotations(imageId) {
    return request({
      url: `/admin/images/${imageId}/annotations`,
      method: 'get'
    })
  }
}
