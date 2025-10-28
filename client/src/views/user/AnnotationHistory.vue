<template>
  <div class="annotation-history">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>标注历史</span>
          <div class="header-stats">
            <el-tag type="info">总计: {{ pagination.total }}</el-tag>
            <el-tag type="success">已完成: {{ stats.completed }}</el-tag>
            <el-tag type="warning">草稿: {{ stats.draft }}</el-tag>
          </div>
        </div>
      </template>

      <!-- 筛选区域 -->
      <div class="filter-section">
        <el-form :model="filterForm" inline>
          <el-form-item label="状态筛选">
            <el-select 
              v-model="filterForm.status" 
              placeholder="选择状态"
              clearable
              @change="handleFilter"
            >
              <el-option label="已完成" value="COMPLETED" />
              <el-option label="草稿" value="DRAFT" />
            </el-select>
          </el-form-item>
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="filterForm.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              @change="handleFilter"
            />
          </el-form-item>
          <el-form-item>
            <el-button @click="handleReset">重置</el-button>
            <el-button type="primary" @click="handleExport">导出记录</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 历史记录表格 -->
      <el-table 
        :data="historyList" 
        style="width: 100%"
        v-loading="loading"
      >
        <el-table-column width="80">
          <template #default="{ row }">
            <div class="image-thumb">
              <img :src="row.image.url" :alt="row.image.originalName" />
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="image.originalName" label="图像名称" min-width="200">
          <template #default="{ row }">
            <div class="image-info">
              <div class="image-name">{{ row.image.originalName }}</div>
              <div class="image-meta">ID: {{ row.image.id }}</div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="标注内容" min-width="300">
          <template #default="{ row }">
            <div class="annotation-preview">
              <div 
                v-for="(value, key) in parseLabels(row.labels)" 
                :key="key"
                class="label-item"
              >
                <span class="label-key">{{ key }}:</span>
                <span class="label-value">{{ value }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="updatedAt" label="更新时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.updatedAt) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button 
              size="small" 
              @click="handleViewDetail(row)"
            >
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-section" v-if="historyList.length > 0">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>

      <!-- 空状态 -->
      <el-empty 
        v-if="!loading && historyList.length === 0" 
        description="暂无标注历史记录"
        :image-size="120"
      />
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog 
      v-model="showDetailDialog" 
      title="标注详情" 
      width="800px"
    >
      <div v-if="selectedRecord" class="detail-content">
        <div class="detail-section">
          <h4>图像信息</h4>
          <div class="image-detail">
            <img 
              :src="selectedRecord.image.url" 
              :alt="selectedRecord.image.originalName"
              class="detail-image"
            />
            <div class="image-info">
              <p><strong>文件名:</strong> {{ selectedRecord.image.originalName }}</p>
              <p><strong>图像ID:</strong> {{ selectedRecord.image.id }}</p>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h4>标注内容</h4>
          <div class="labels-detail">
            <div 
              v-for="(value, key) in parseLabels(selectedRecord.labels)" 
              :key="key"
              class="label-detail-item"
            >
              <div class="label-key">{{ key }}</div>
              <div class="label-value">{{ value }}</div>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h4>记录信息</h4>
          <div class="record-info">
            <p><strong>状态:</strong> 
              <el-tag :type="getStatusType(selectedRecord.status)">
                {{ getStatusText(selectedRecord.status) }}
              </el-tag>
            </p>
            <p><strong>创建时间:</strong> {{ formatDate(selectedRecord.createdAt) }}</p>
            <p><strong>更新时间:</strong> {{ formatDate(selectedRecord.updatedAt) }}</p>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="showDetailDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { userTaskApi } from '@/api/userTask'

const loading = ref(false)
const showDetailDialog = ref(false)
const selectedRecord = ref(null)

const filterForm = reactive({
  status: '',
  dateRange: []
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const historyList = ref([])

const stats = computed(() => {
  return {
    completed: historyList.value.filter(item => item.status === 'COMPLETED').length,
    draft: historyList.value.filter(item => item.status === 'DRAFT').length
  }
})

const fetchHistoryList = async () => {
  try {
    loading.value = true
    
    const params = {
      page: pagination.page - 1,
      size: pagination.size,
      status: filterForm.status,
      startDate: filterForm.dateRange?.[0],
      endDate: filterForm.dateRange?.[1]
    }
    
    const response = await userTaskApi.getAnnotationHistory(params)
    if (response.success) {
      historyList.value = response.data.content || []
      pagination.total = response.data.totalElements || 0
    }
  } catch (error) {
    ElMessage.error('获取历史记录失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  pagination.page = 1
  fetchHistoryList()
}

const handleReset = () => {
  Object.assign(filterForm, {
    status: '',
    dateRange: []
  })
  pagination.page = 1
  fetchHistoryList()
}

const handleExport = () => {
  ElMessage.success('导出功能开发中...')
}

const handleViewDetail = (row) => {
  selectedRecord.value = row
  showDetailDialog.value = true
}

const handlePageChange = () => {
  fetchHistoryList()
}

const handleSizeChange = () => {
  pagination.page = 1
  fetchHistoryList()
}

const getStatusType = (status) => {
  const typeMap = {
    'COMPLETED': 'success',
    'DRAFT': 'warning'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status) => {
  const textMap = {
    'COMPLETED': '已完成',
    'DRAFT': '草稿'
  }
  return textMap[status] || status
}

const parseLabels = (labelsJson) => {
  try {
    return JSON.parse(labelsJson)
  } catch (error) {
    return {}
  }
}

const formatDate = (dateStr) => {
  return dateStr ? new Date(dateStr).toLocaleString() : ''
}

onMounted(() => {
  fetchHistoryList()
})
</script>

<style scoped>
.annotation-history {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-stats .el-tag {
  margin-left: 8px;
}

.filter-section {
  margin-bottom: 20px;
  padding: 15px;
  background: #f5f5f5;
  border-radius: 4px;
}

.image-thumb {
  width: 60px;
  height: 48px;
  border-radius: 4px;
  overflow: hidden;
}

.image-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-info {
  line-height: 1.4;
}

.image-name {
  font-weight: bold;
  margin-bottom: 2px;
}

.image-meta {
  font-size: 12px;
  color: #999;
}

.annotation-preview {
  max-height: 100px;
  overflow-y: auto;
}

.label-item {
  display: flex;
  margin-bottom: 4px;
  font-size: 13px;
}

.label-key {
  font-weight: bold;
  color: #666;
  margin-right: 8px;
  min-width: 80px;
}

.label-value {
  color: #333;
  flex: 1;
}

.pagination-section {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.detail-content {
  max-height: 600px;
  overflow-y: auto;
}

.detail-section {
  margin-bottom: 25px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.detail-section:last-child {
  border-bottom: none;
}

.detail-section h4 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 16px;
}

.image-detail {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.detail-image {
  width: 200px;
  height: 150px;
  object-fit: contain;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.image-info p {
  margin: 8px 0;
  color: #666;
}

.labels-detail {
  display: grid;
  gap: 15px;
}

.label-detail-item {
  background: #f9f9f9;
  padding: 12px;
  border-radius: 6px;
  border-left: 3px solid #409eff;
}

.label-detail-item .label-key {
  font-weight: bold;
  color: #333;
  margin-bottom: 5px;
}

.label-detail-item .label-value {
  color: #666;
  line-height: 1.5;
}

.record-info p {
  margin: 10px 0;
  color: #666;
}

/* 统一使用白天模式，移除了暗黑模式适配 */
</style>
