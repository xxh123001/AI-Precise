import request from '@/utils/request'

export const exportApi = {
  // 导出标注结果 (支持多种筛选条件)
  exportAnnotations(params) {
    // 如果参数较多或包含复杂对象，使用POST方法
    const hasComplexParams = params.projectId || params.taskStatus || params.annotationStatus || params.fileName
    
    if (hasComplexParams) {
      return request({
        url: '/admin/export/annotations',
        method: 'post',
        data: params,
        responseType: 'blob'
      })
    }
    
    // 简单参数使用GET方法
    return request({
      url: '/admin/export/annotations',
      method: 'get',
      params,
      responseType: 'blob'
    })
  },

  // 导出标注数据为Excel (POST方式，支持复杂筛选)
  exportAnnotationsToExcel(data) {
    return request({
      url: '/admin/export/annotations/excel',
      method: 'post',
      data,
      responseType: 'blob'
    })
  },

  // 获取标注统计数据
  getAnnotationStatistics(data) {
    return request({
      url: '/admin/export/annotations/statistics',
      method: 'post',
      data
    })
  },

  // 生成进度报告
  generateProgressReport() {
    return request({
      url: '/admin/export/progress-report',
      method: 'get'
    })
  },

  // 预览标注数据 (支持复杂筛选)
  previewAnnotations(params) {
    return request({
      url: '/admin/export/annotations/preview',
      method: 'post',
      data: params
    })
  },

  // 获取导出历史记录
  getExportHistory(params) {
    return request({
      url: '/admin/export/history',
      method: 'get',
      params
    })
  },

  // 下载历史导出文件
  downloadExportFile(exportId) {
    return request({
      url: `/admin/export/history/${exportId}/download`,
      method: 'get',
      responseType: 'blob'
    })
  }
}
