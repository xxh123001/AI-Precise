<template>
  <div class="data-export">
    <el-card>
      <template #header>
        <span>数据导出</span>
      </template>

      <el-form :model="exportForm" label-width="120px">
        <el-form-item label="导出类型">
          <el-radio-group v-model="exportForm.type">
            <el-radio value="annotations">标注数据</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="筛选条件">
          <el-row :gutter="10" style="margin-bottom: 10px;">
            <el-col :span="8">
              <el-select 
                v-model="exportForm.projectId" 
                placeholder="选择大任务"
                clearable
                filterable
              >
                <el-option 
                  v-for="project in projectList" 
                  :key="project.id"
                  :label="project.name" 
                  :value="project.id" 
                />
              </el-select>
            </el-col>
            <el-col :span="8">
              <el-select 
                v-model="exportForm.userId" 
                placeholder="选择人员"
                clearable
                filterable
              >
                <el-option 
                  v-for="user in userList" 
                  :key="user.id"
                  :label="user.username" 
                  :value="user.id" 
                />
              </el-select>
            </el-col>
            <el-col :span="8">
              <el-select 
                v-model="exportForm.taskStatus" 
                placeholder="任务状态"
                clearable
              >
                <el-option label="全部" value="" />
                <el-option label="已完成" value="COMPLETED" />
                <el-option label="进行中" value="IN_PROGRESS" />
                <el-option label="已分配" value="ASSIGNED" />
              </el-select>
            </el-col>
          </el-row>
          <el-row :gutter="10" style="margin-bottom: 10px;">
            <el-col :span="8">
              <el-select 
                v-model="exportForm.annotationStatus" 
                placeholder="标注状态"
                clearable
              >
                <el-option label="全部" value="" />
                <el-option label="已完成" value="COMPLETED" />
                <el-option label="草稿" value="DRAFT" />
              </el-select>
            </el-col>
            <el-col :span="8">
              <el-input
                v-model="exportForm.fileName"
                placeholder="文件名关键词"
                clearable
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </el-col>
            <el-col :span="8">
              <el-date-picker
                v-model="exportForm.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-col>
          </el-row>
          <el-row :gutter="10">
            <el-col :span="24">
              <el-checkbox-group v-model="exportForm.labelFilters">
                <el-checkbox value="hasAnnotation">仅包含已标注</el-checkbox>
                <el-checkbox value="hasRemark">仅包含备注</el-checkbox>
                <el-checkbox value="excludeEmpty">排除空标注</el-checkbox>
              </el-checkbox-group>
            </el-col>
          </el-row>
        </el-form-item>

        <el-form-item label="导出格式">
          <el-radio-group v-model="exportForm.format">
            <el-radio value="xlsx">Excel (.xlsx)</el-radio>
            <el-radio value="csv">CSV (.csv)</el-radio>
            <el-radio value="json">JSON (.json)</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item>
          <el-button 
            type="primary" 
            @click="handleExport"
            :loading="exporting"
          >
            <el-icon><Download /></el-icon>
            {{ exporting ? '导出中...' : '导出数据' }}
          </el-button>
          <el-button 
            @click="loadPreviewData"
            :loading="loadingPreview"
          >
            <el-icon><Refresh /></el-icon>
            刷新预览
          </el-button>
          <el-button 
            type="success" 
            @click="handleGetStatistics"
            :loading="loadingStats"
          >
            <el-icon><DataAnalysis /></el-icon>
            获取统计
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 实时数据预览表格 -->
      <el-divider>数据预览</el-divider>
      
      <div class="preview-info" v-if="!loadingPreview && totalPreviewCount > 0">
        <el-alert 
          :title="`当前筛选条件下共有 ${totalPreviewCount} 条数据`"
          type="info" 
          :closable="false"
          show-icon
        >
          <template #default>
            <div style="margin-top: 8px;">
              <el-tag v-if="exportForm.projectId" size="small" style="margin-right: 5px;">
                大任务ID: {{ exportForm.projectId }}
              </el-tag>
              <el-tag v-if="exportForm.userId" size="small" style="margin-right: 5px;">
                用户ID: {{ exportForm.userId }}
              </el-tag>
              <el-tag v-if="exportForm.taskStatus" size="small" style="margin-right: 5px;">
                任务状态: {{ exportForm.taskStatus }}
              </el-tag>
              <el-tag v-if="exportForm.annotationStatus" size="small" style="margin-right: 5px;">
                标注状态: {{ exportForm.annotationStatus }}
              </el-tag>
              <el-tag v-if="exportForm.fileName" size="small" style="margin-right: 5px;">
                文件名: {{ exportForm.fileName }}
              </el-tag>
              <el-tag v-if="exportForm.dateRange" size="small" style="margin-right: 5px;">
                日期: {{ exportForm.dateRange[0] }} ~ {{ exportForm.dateRange[1] }}
              </el-tag>
              <el-tag v-if="exportForm.labelFilters.length > 0" size="small" style="margin-right: 5px;">
                额外筛选: {{ exportForm.labelFilters.length }} 项
              </el-tag>
            </div>
          </template>
        </el-alert>
      </div>

      <el-table 
        :data="previewData" 
        style="width: 100%; margin-top: 20px;"
        v-loading="loadingPreview"
        border
        stripe
        max-height="500"
      >
        <el-table-column type="index" label="序号" width="60" fixed />
        <el-table-column
          v-for="column in previewColumns"
          :key="column.prop"
          :prop="column.prop"
          :label="column.label"
          :min-width="column.width || 120"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span v-if="column.prop === 'status' || column.prop === 'taskStatus'">
              <el-tag :type="getTaskStatusType(row[column.prop])" size="small">
                {{ row[column.prop] }}
              </el-tag>
            </span>
            <span v-else-if="column.prop === 'annotationStatus'">
              <el-tag :type="getAnnotationStatusTypeTag(row[column.prop])" size="small">
                {{ row[column.prop] }}
              </el-tag>
            </span>
            <span v-else>{{ row[column.prop] }}</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty 
        v-if="!loadingPreview && previewData.length === 0"
        description="当前筛选条件下暂无数据"
        :image-size="120"
      />

      <!-- 预览数据分页 -->
      <div v-if="previewData.length > 0" class="preview-pagination">
        <el-pagination
          v-model:current-page="previewPagination.current"
          v-model:page-size="previewPagination.size"
          :page-sizes="[10, 20, 50, 100, 200]"
          :total="previewPagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handlePreviewSizeChange"
          @current-change="handlePreviewPageChange"
        />
      </div>
    </el-card>

    <!-- 导出历史 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center;">
          <span>导出历史</span>
          <el-button 
            size="small" 
            type="danger" 
            plain
            @click="handleClearHistory"
            :disabled="exportHistory.length === 0"
          >
            清空历史
          </el-button>
        </div>
      </template>

      <el-table :data="exportHistory" style="width: 100%">
        <el-table-column prop="type" label="导出类型" width="100" />
        <el-table-column prop="format" label="格式" width="80" />
        <el-table-column prop="fileName" label="文件名" min-width="200" />
        <el-table-column label="筛选条件" min-width="250">
          <template #default="{ row }">
            <div style="font-size: 12px; color: #666;">
              <span v-if="row.filters?.projectId">项目ID: {{ row.filters.projectId }} | </span>
              <span v-if="row.filters?.userId">用户ID: {{ row.filters.userId }} | </span>
              <span v-if="row.filters?.taskStatus">任务状态: {{ row.filters.taskStatus }} | </span>
              <span v-if="row.filters?.annotationStatus">标注状态: {{ row.filters.annotationStatus }} | </span>
              <span v-if="row.filters?.fileName">文件名: {{ row.filters.fileName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="导出时间" width="160">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140">
          <template #default="{ row }">
            <div class="action-buttons-horizontal">
              <el-button 
                size="small" 
                type="primary"
                @click="handleDownload(row)"
                :disabled="row.status !== '成功'"
              >
                下载
              </el-button>
              <el-button 
                size="small" 
                type="danger"
                @click="handleDeleteHistory(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-empty 
        v-if="exportHistory.length === 0"
        description="暂无导出历史"
        :image-size="100"
      />
    </el-card>

    <!-- 数据预览对话框 -->
    <el-dialog 
      v-model="showPreviewDialog" 
      title="数据预览" 
      width="80%"
      :close-on-click-modal="false"
    >
      <el-table 
        :data="previewData" 
        style="width: 100%"
        max-height="400"
      >
        <el-table-column
          v-for="column in previewColumns"
          :key="column.prop"
          :prop="column.prop"
          :label="column.label"
          show-overflow-tooltip
        />
      </el-table>
      
      <div style="margin-top: 10px; text-align: center; color: #666;">
        <span v-if="totalPreviewCount > 0">
          预览前 {{ previewData.length }} 条数据，总共 {{ totalPreviewCount }} 条符合条件的数据
          <span v-if="hasMorePreview">（还有更多数据）</span>
        </span>
        <span v-else>暂无符合条件的数据</span>
      </div>
      
      <template #footer>
        <el-button @click="showPreviewDialog = false">关闭</el-button>
        <el-button type="primary" @click="handleExport">确认导出</el-button>
      </template>
    </el-dialog>

    <!-- 统计数据对话框 -->
    <el-dialog 
      v-model="showStatsDialog" 
      title="统计数据" 
      width="60%"
      :close-on-click-modal="false"
    >
      <div v-if="Object.keys(statisticsData).length > 0">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card>
              <h4>总体统计</h4>
              <p>总标注数：{{ statisticsData.totalAnnotations || 0 }}</p>
              <p>已完成：{{ statisticsData.completedAnnotations || 0 }}</p>
              <p>完成率：{{ getCompletionRate() }}%</p>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card>
              <h4>用户统计</h4>
              <div v-if="statisticsData.userStatistics">
                <div 
                  v-for="(count, username) in statisticsData.userStatistics" 
                  :key="username"
                  style="margin-bottom: 5px;"
                >
                  {{ username }}: {{ count }} 条
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>
        
        <el-card style="margin-top: 20px;">
          <h4>按日期统计</h4>
          <div v-if="statisticsData.dateStatistics">
            <div 
              v-for="(count, date) in statisticsData.dateStatistics" 
              :key="date"
              style="display: inline-block; margin-right: 20px; margin-bottom: 5px;"
            >
              {{ date }}: {{ count }} 条
            </div>
          </div>
        </el-card>
      </div>
      
      <template #footer>
        <el-button @click="showStatsDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, DataAnalysis, Search, Refresh } from '@element-plus/icons-vue'
import { exportApi } from '@/api/export'
import { userApi } from '@/api/user'
import { projectApi } from '@/api/project'

const exporting = ref(false)
const loadingStats = ref(false)
const loadingPreview = ref(false)
const showPreviewDialog = ref(false)
const showStatsDialog = ref(false)

const exportForm = reactive({
  type: 'annotations',
  projectId: null,
  userId: null,
  taskStatus: '',
  annotationStatus: '',
  fileName: '',
  dateRange: null,
  labelFilters: [],
  format: 'xlsx'
})

// 预览分页参数
const previewPagination = reactive({
  current: 1,
  size: 20,
  total: 0
})

const userList = ref([])
const projectList = ref([])
const exportHistory = ref([])
const previewData = ref([])
const previewColumns = ref([])
const statisticsData = ref({})
const totalPreviewCount = ref(0)
const hasMorePreview = ref(false)

const fetchUserList = async () => {
  try {
    const response = await userApi.getUserList({ page: 0, size: 1000 })
    if (response.success) {
      userList.value = response.data.content || []
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
  }
}

const fetchProjectList = async () => {
  try {
    const response = await projectApi.getProjects({ page: 0, size: 1000 })
    if (response.success) {
      projectList.value = response.data.content || []
    }
  } catch (error) {
    console.error('获取项目列表失败:', error)
  }
}

const fetchExportHistory = async () => {
  // 尝试从localStorage获取导出历史
  try {
    const historyStr = localStorage.getItem('exportHistory')
    if (historyStr) {
      exportHistory.value = JSON.parse(historyStr)
      // 只保留最近30条记录
      if (exportHistory.value.length > 30) {
        exportHistory.value = exportHistory.value.slice(0, 30)
        localStorage.setItem('exportHistory', JSON.stringify(exportHistory.value))
      }
    }
  } catch (error) {
    console.error('获取导出历史失败:', error)
    exportHistory.value = []
  }
}

// 保存导出记录到历史
const saveExportToHistory = (exportInfo) => {
  try {
    // 添加到历史记录
    const historyItem = {
      id: Date.now(),
      type: '标注数据',
      format: exportInfo.format.toUpperCase(),
      fileName: exportInfo.fileName,
      status: '成功',
      createdAt: new Date().toISOString(),
      filters: {
        projectId: exportForm.projectId,
        userId: exportForm.userId,
        taskStatus: exportForm.taskStatus,
        annotationStatus: exportForm.annotationStatus,
        fileName: exportForm.fileName,
        dateRange: exportForm.dateRange
      }
    }
    
    exportHistory.value.unshift(historyItem)
    
    // 只保留最近30条
    if (exportHistory.value.length > 30) {
      exportHistory.value = exportHistory.value.slice(0, 30)
    }
    
    // 保存到localStorage
    localStorage.setItem('exportHistory', JSON.stringify(exportHistory.value))
  } catch (error) {
    console.error('保存导出历史失败:', error)
  }
}

// 加载预览数据
const loadPreviewData = async () => {
  try {
    
    loadingPreview.value = true
    
    const params = {
      projectId: exportForm.projectId,
      userId: exportForm.userId,
      taskStatus: exportForm.taskStatus,
      annotationStatus: exportForm.annotationStatus,
      fileName: exportForm.fileName,
      startDate: exportForm.dateRange ? exportForm.dateRange[0] : undefined,
      endDate: exportForm.dateRange ? exportForm.dateRange[1] : undefined,
      hasAnnotation: exportForm.labelFilters.includes('hasAnnotation') ? true : undefined,
      hasRemark: exportForm.labelFilters.includes('hasRemark') ? true : undefined,
      excludeEmpty: exportForm.labelFilters.includes('excludeEmpty') ? true : undefined,
      page: previewPagination.current - 1, // 后端从0开始
      size: previewPagination.size
    }
    
    const response = await exportApi.previewAnnotations(params)
    
    if (response.success && response.data) {
      // 设置预览数据
      previewData.value = response.data.data || []
      previewColumns.value = response.data.columns || []
      totalPreviewCount.value = response.data.total || 0
      previewPagination.total = response.data.total || 0
      hasMorePreview.value = response.data.hasMore || false
    } else {
      previewData.value = []
      previewColumns.value = []
      totalPreviewCount.value = 0
      previewPagination.total = 0
    }
  } catch (error) {
    console.error('加载预览数据失败:', error)
    previewData.value = []
    previewColumns.value = []
    totalPreviewCount.value = 0
    previewPagination.total = 0
  } finally {
    loadingPreview.value = false
  }
}

const handlePreview = async () => {
  await loadPreviewData()
  if (previewData.value.length > 0) {
    showPreviewDialog.value = true
  } else {
    ElMessage.warning('当前筛选条件下暂无数据')
  }
}

const handleExport = async () => {
  try {
    exporting.value = true
    
    const params = {
      projectId: exportForm.projectId,
      userId: exportForm.userId,
      taskStatus: exportForm.taskStatus,
      annotationStatus: exportForm.annotationStatus,
      fileName: exportForm.fileName,
      startDate: exportForm.dateRange ? exportForm.dateRange[0] : undefined,
      endDate: exportForm.dateRange ? exportForm.dateRange[1] : undefined,
      hasAnnotation: exportForm.labelFilters.includes('hasAnnotation') ? true : undefined,
      hasRemark: exportForm.labelFilters.includes('hasRemark') ? true : undefined,
      excludeEmpty: exportForm.labelFilters.includes('excludeEmpty') ? true : undefined,
      format: exportForm.format
    }
    
    const response = await exportApi.exportAnnotations(params)
    
    // 根据格式设置不同的MIME类型和处理方式
    let mimeType, fileName
    
    if (exportForm.format === 'json') {
      mimeType = 'application/json'
      fileName = `${exportForm.type}_${new Date().toISOString().split('T')[0]}.json`
      
      // 对于JSON格式，需要将响应数据转换为字符串
      const jsonContent = typeof response.data === 'string' ? response.data : JSON.stringify(response.data, null, 2)
      const blob = new Blob([jsonContent], { type: mimeType })
      
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = fileName
      
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      
    } else if (exportForm.format === 'csv') {
      mimeType = 'text/csv'
      fileName = `${exportForm.type}_${new Date().toISOString().split('T')[0]}.csv`
      
      const blob = new Blob([response.data], { type: mimeType })
      
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = fileName
      
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      
    } else {
      // Excel格式
      mimeType = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      fileName = `${exportForm.type}_${new Date().toISOString().split('T')[0]}.xlsx`
      
      const blob = new Blob([response.data], { type: mimeType })
      
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = fileName
      
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    }
    
    ElMessage.success(`导出成功：${fileName}`)
    
    // 保存到导出历史
    saveExportToHistory({
      format: exportForm.format,
      fileName: fileName
    })
    
    // 关闭预览对话框
    showPreviewDialog.value = false
    
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const handleGetStatistics = async () => {
  try {
    loadingStats.value = true
    
    const params = {
      projectId: exportForm.projectId,
      userId: exportForm.userId,
      taskStatus: exportForm.taskStatus,
      annotationStatus: exportForm.annotationStatus,
      fileName: exportForm.fileName,
      startDate: exportForm.dateRange ? exportForm.dateRange[0] : undefined,
      endDate: exportForm.dateRange ? exportForm.dateRange[1] : undefined,
      hasAnnotation: exportForm.labelFilters.includes('hasAnnotation') ? true : undefined,
      hasRemark: exportForm.labelFilters.includes('hasRemark') ? true : undefined,
      excludeEmpty: exportForm.labelFilters.includes('excludeEmpty') ? true : undefined
    }
    
    const response = await exportApi.getAnnotationStatistics(params)
    if (response.success) {
      statisticsData.value = response.data || {}
      showStatsDialog.value = true
      ElMessage.success('统计数据获取成功')
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    ElMessage.error('获取统计数据失败')
  } finally {
    loadingStats.value = false
  }
}

const getCompletionRate = () => {
  if (!statisticsData.value.totalAnnotations || statisticsData.value.totalAnnotations === 0) {
    return 0
  }
  const rate = (statisticsData.value.completedAnnotations / statisticsData.value.totalAnnotations) * 100
  return Math.round(rate * 100) / 100 // 保留两位小数
}

const handleDownload = (row) => {
  ElMessage.info('此功能需要后端支持导出历史存储')
  // TODO: 当后端支持导出历史存储后，可以从服务器下载历史文件
  console.log('下载历史记录:', row)
}

const handleDeleteHistory = (row) => {
  try {
    const index = exportHistory.value.findIndex(item => item.id === row.id)
    if (index > -1) {
      exportHistory.value.splice(index, 1)
      localStorage.setItem('exportHistory', JSON.stringify(exportHistory.value))
      ElMessage.success('删除成功')
    }
  } catch (error) {
    console.error('删除历史记录失败:', error)
    ElMessage.error('删除失败')
  }
}

const handleClearHistory = () => {
  try {
    exportHistory.value = []
    localStorage.removeItem('exportHistory')
    ElMessage.success('清空成功')
  } catch (error) {
    console.error('清空历史记录失败:', error)
    ElMessage.error('清空失败')
  }
}

const getStatusType = (status) => {
  const typeMap = {
    '成功': 'success',
    '进行中': 'warning',
    '失败': 'danger'
  }
  return typeMap[status] || 'info'
}

const formatDate = (dateStr) => {
  return dateStr ? new Date(dateStr).toLocaleString() : ''
}

// 任务状态类型
const getTaskStatusType = (status) => {
  const typeMap = {
    'COMPLETED': 'success',
    'IN_PROGRESS': 'warning',
    'ASSIGNED': 'info',
    '已完成': 'success',
    '进行中': 'warning',
    '已分配': 'info'
  }
  return typeMap[status] || 'info'
}

// 标注状态类型
const getAnnotationStatusTypeTag = (status) => {
  const typeMap = {
    'COMPLETED': 'success',
    'DRAFT': 'warning',
    '已完成': 'success',
    '草稿': 'warning'
  }
  return typeMap[status] || 'info'
}

// 预览分页处理
const handlePreviewPageChange = () => {
  loadPreviewData()
}

const handlePreviewSizeChange = () => {
  previewPagination.current = 1
  loadPreviewData()
}

// 监听筛选条件变化，自动刷新预览
let filterChangeTimer = null
watch(
  () => [
    exportForm.projectId,
    exportForm.userId,
    exportForm.taskStatus,
    exportForm.annotationStatus,
    exportForm.fileName,
    exportForm.dateRange,
    exportForm.labelFilters
  ],
  () => {
    // 防抖处理，避免频繁请求
    clearTimeout(filterChangeTimer)
    filterChangeTimer = setTimeout(() => {
      // 重置到第一页
      previewPagination.current = 1
      loadPreviewData()
    }, 500)
  },
  { deep: true }
)

onMounted(() => {
  fetchUserList()
  fetchProjectList()
  fetchExportHistory()
  // 初始加载预览数据
  loadPreviewData()
})
</script>

<style scoped>
.data-export {
  height: 100%;
}

/* 操作按钮多行排列 */
.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: stretch;
  min-width: 60px;
}

.action-buttons .el-button {
  margin: 0 !important;
  padding: 4px 8px;
  font-size: 11px;
  white-space: nowrap;
  width: 100%;
  text-align: center;
}

/* 操作按钮水平排列 */
.action-buttons-horizontal {
  display: flex;
  gap: 4px;
  align-items: center;
}

.action-buttons-horizontal .el-button {
  margin: 0 !important;
  padding: 4px 8px;
  font-size: 11px;
  white-space: nowrap;
}

/* 预览信息区域 */
.preview-info {
  margin-top: 20px;
}

.preview-info .el-alert {
  margin-bottom: 10px;
}

/* 预览分页区域 */
.preview-pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}
</style>
