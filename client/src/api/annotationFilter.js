import request from '@/utils/request'

/**
 * 标注筛选相关API
 */
export const annotationFilterApi = {
  /**
   * 搜索标注数据
   * @param {Object} params 筛选参数
   */
  searchAnnotations(params) {
    return request({
      url: '/admin/annotation-filter/search',
      method: 'post',
      data: params
    })
  },

  /**
   * 筛选双标签数据
   * @param {Object} params 筛选参数
   */
  filterDoubleTags(params) {
    return request({
      url: '/admin/annotation-filter/filter/double-tags',
      method: 'post',
      data: params
    })
  },

  /**
   * 导出图像文件
   * @param {Object} params 筛选参数
   */
  exportImages(params) {
    return request({
      url: '/admin/annotation-filter/export/images',
      method: 'post',
      data: params,
      responseType: 'blob', // 重要：设置响应类型为blob用于文件下载
      timeout: 300000 // 5分钟超时，因为导出可能需要较长时间
    })
  },

  /**
   * 获取有标注记录的用户列表
   */
  getAnnotationUsers() {
    return request({
      url: '/admin/annotation-filter/users',
      method: 'get'
    })
  },

  /**
   * 获取常见标签列表
   */
  getCommonTags() {
    return request({
      url: '/admin/annotation-filter/tags/common',
      method: 'get'
    })
  },

  /**
   * 预览筛选结果
   * @param {Object} params 筛选参数
   */
  previewFilterResults(params) {
    return request({
      url: '/admin/annotation-filter/preview',
      method: 'post',
      data: params
    })
  },

  /**
   * 获取筛选统计
   * @param {Object} params 筛选参数
   */
  getFilterStatistics(params) {
    return request({
      url: '/admin/annotation-filter/statistics',
      method: 'post',
      data: params
    })
  }
}

// 筛选条件构建助手
export const filterHelper = {
  /**
   * 创建用户筛选条件
   * @param {number|Array} userIds 用户ID或用户ID数组
   */
  forUsers(userIds) {
    const ids = Array.isArray(userIds) ? userIds : [userIds]
    return {
      userIds: ids,
      annotationStatus: 'COMPLETED',
      pageSize: 50,
      pageNumber: 0,
      sortBy: 'createdAt',
      sortDirection: 'DESC'
    }
  },

  /**
   * 创建双标签筛选条件
   * @param {number|Array} userIds 用户ID
   * @param {Array} tags 标签数组
   */
  forDoubleTags(userIds, tags) {
    const ids = Array.isArray(userIds) ? userIds : [userIds]
    return {
      userIds: ids,
      requiredTags: tags,
      tagMatchMode: 'ALL',
      annotationStatus: 'COMPLETED',
      pageSize: 50,
      pageNumber: 0,
      sortBy: 'createdAt',
      sortDirection: 'DESC'
    }
  },

  /**
   * 创建时间范围筛选条件
   * @param {string} startDate 开始日期 (YYYY-MM-DD)
   * @param {string} endDate 结束日期 (YYYY-MM-DD)
   */
  forDateRange(startDate, endDate) {
    return {
      startDate,
      endDate,
      annotationStatus: 'COMPLETED',
      pageSize: 50,
      pageNumber: 0,
      sortBy: 'createdAt',
      sortDirection: 'DESC'
    }
  },

  /**
   * 创建标签筛选条件
   * @param {Array} requiredTags 必须包含的标签
   * @param {Array} excludeTags 排除的标签
   * @param {string} matchMode 匹配模式 (ALL/ANY)
   */
  forTags(requiredTags = [], excludeTags = [], matchMode = 'ALL') {
    return {
      requiredTags,
      excludeTags,
      tagMatchMode: matchMode,
      annotationStatus: 'COMPLETED',
      pageSize: 50,
      pageNumber: 0,
      sortBy: 'createdAt',
      sortDirection: 'DESC'
    }
  },

  /**
   * 合并筛选条件
   * @param {...Object} filters 多个筛选条件对象
   */
  merge(...filters) {
    const merged = {}
    filters.forEach(filter => {
      Object.keys(filter).forEach(key => {
        if (Array.isArray(filter[key])) {
          // 对于数组类型，合并数组并去重
          merged[key] = [...new Set([...(merged[key] || []), ...filter[key]])]
        } else if (filter[key] !== undefined && filter[key] !== null) {
          // 对于其他类型，后面的值覆盖前面的值
          merged[key] = filter[key]
        }
      })
    })
    return merged
  }
}

// 常用的预设筛选条件
export const presetFilters = {
  // 用户555的双标签筛选
  user555DoubleTags: filterHelper.forDoubleTags(7, ['近端小管', '远端小管']),
  
  // 最近一周的标注
  recentWeek: filterHelper.forDateRange(
    new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    new Date().toISOString().split('T')[0]
  ),
  
  // 最近一个月的标注
  recentMonth: filterHelper.forDateRange(
    new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    new Date().toISOString().split('T')[0]
  ),
  
  // 包含萎缩小管的标注
  withAtrophy: filterHelper.forTags(['萎缩小管']),
  
  // 包含集合管的标注
  withCollecting: filterHelper.forTags(['集合管']),
  
  // 近端+远端双标签
  nearAndFar: filterHelper.forTags(['近端小管', '远端小管'], [], 'ALL'),
  
  // 近端+萎缩双标签
  nearAndAtrophy: filterHelper.forTags(['近端小管', '萎缩小管'], [], 'ALL'),
  
  // 远端+萎缩双标签
  farAndAtrophy: filterHelper.forTags(['远端小管', '萎缩小管'], [], 'ALL')
}

export default annotationFilterApi

