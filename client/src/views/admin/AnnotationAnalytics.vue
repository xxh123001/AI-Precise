<template>
  <div class="annotation-analytics">
    <!-- 页面头部 -->
    <el-card>
      <template #header>
        <div class="page-header">
          <h2>标注数据分析</h2>
          <div class="header-actions">
            <el-button type="primary" @click="refreshData" :loading="loading">
              <el-icon><Refresh /></el-icon>
              刷新数据
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选条件 -->
      <div class="filter-section">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-select 
              v-model="filters.userId" 
              placeholder="选择标注人"
              clearable
              @change="handleFilterChange"
            >
              <el-option 
                v-for="user in userList" 
                :key="user.id"
                :label="user.username" 
                :value="user.id" 
              />
            </el-select>
          </el-col>
          <el-col :span="6">
            <el-date-picker
              v-model="filters.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD"
              @change="handleFilterChange"
            />
          </el-col>
          <el-col :span="6">
            <el-select 
              v-model="filters.timeRange" 
              placeholder="时间范围"
              @change="handleFilterChange"
            >
              <el-option label="最近7天" value="week" />
              <el-option label="最近30天" value="month" />
              <el-option label="最近90天" value="quarter" />
              <el-option label="全部时间" value="all" />
            </el-select>
          </el-col>
          <el-col :span="6">
            <el-button @click="resetFilters">重置筛选</el-button>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 总体统计卡片 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-icon total">
              <el-icon><DataBoard /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ overallStats.totalAnnotations || 0 }}</div>
              <div class="stat-label">总标注数</div>
              <div class="stat-change positive" v-if="overallStats.annotationGrowth > 0">
                +{{ overallStats.annotationGrowth }}%
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-icon users">
              <el-icon><User /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ overallStats.activeAnnotators || 0 }}</div>
              <div class="stat-label">活跃标注人</div>
              <div class="stat-change positive" v-if="overallStats.userGrowth > 0">
                +{{ overallStats.userGrowth }}%
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-icon accuracy">
              <el-icon><DataAnalysis /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ overallStats.averageAccuracy || 0 }}%</div>
              <div class="stat-label">平均质量分</div>
              <div class="stat-change positive" v-if="overallStats.accuracyTrend > 0">
                +{{ overallStats.accuracyTrend }}%
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-icon productivity">
              <el-icon><Timer /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ overallStats.avgAnnotationTime || 0 }}min</div>
              <div class="stat-label">平均标注时间</div>
              <div class="stat-change negative" v-if="overallStats.timeTrend < 0">
                {{ overallStats.timeTrend }}%
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表展示 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 标注人工作量统计 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="chart-header">
              <span>标注人工作量统计</span>
              <el-dropdown @command="handleWorkloadCommand">
                <el-button type="text">
                  {{ workloadViewType === 'count' ? '按数量' : '按时长' }}
                  <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="count">按标注数量</el-dropdown-item>
                    <el-dropdown-item command="time">按标注时长</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
          <div ref="workloadChart" class="chart-container" v-loading="chartLoading.workload"></div>
        </el-card>
      </el-col>

      <!-- 标注进度趋势 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>标注进度趋势</span>
          </template>
          <div ref="progressChart" class="chart-container" v-loading="chartLoading.progress"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <!-- 标注质量分布 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>标注状态分布</span>
          </template>
          <div ref="statusChart" class="chart-container" v-loading="chartLoading.status"></div>
        </el-card>
      </el-col>

      <!-- 标注类别统计 -->
      <el-col :span="12">
        <el-card>
          <template #header>
            <span>标注类别统计</span>
          </template>
          <div ref="categoryChart" class="chart-container" v-loading="chartLoading.category"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 标注人详细排行榜 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <span>标注人排行榜</span>
      </template>
      
      <el-table :data="annotatorRanking" style="width: 100%">
        <el-table-column type="index" label="排名" width="80" />
        <el-table-column prop="username" label="标注人" width="150" />
        <el-table-column label="总标注数" width="120">
          <template #default="{ row }">
            <el-tag type="primary">{{ row.totalAnnotations }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="已完成" width="100">
          <template #default="{ row }">
            <el-tag type="success">{{ row.completedAnnotations }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="完成率" width="120">
          <template #default="{ row }">
            <el-progress 
              :percentage="row.completionRate"
              :status="row.completionRate === 100 ? 'success' : 'primary'"
              :stroke-width="8"
            />
          </template>
        </el-table-column>
        <el-table-column label="平均质量分" width="120">
          <template #default="{ row }">
            <el-rate 
              v-model="row.qualityScore" 
              disabled 
              show-score 
              text-color="#ff9900"
              :max="5"
            />
          </template>
        </el-table-column>
        <el-table-column label="平均用时" width="100">
          <template #default="{ row }">
            {{ row.avgTime }}min
          </template>
        </el-table-column>
        <el-table-column label="最后标注时间" width="150">
          <template #default="{ row }">
            {{ formatDate(row.lastAnnotationTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" type="primary" @click="viewAnnotatorDetail(row)">
                详情
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 标注预测对比分析 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <div class="section-header">
          <span>标注vs预测结果对比</span>
          <el-button type="primary" @click="generateComparisonReport">
            生成对比报告
          </el-button>
        </div>
      </template>
      
      <el-row :gutter="20">
        <el-col :span="12">
          <div class="comparison-chart">
            <h4>准确率对比</h4>
            <div ref="accuracyComparisonChart" class="chart-container" v-loading="chartLoading.comparison"></div>
          </div>
        </el-col>
        <el-col :span="12">
          <div class="comparison-stats">
            <h4>对比统计</h4>
            <el-descriptions :column="1" border>
              <el-descriptions-item label="总对比样本">{{ comparisonStats.totalSamples }}</el-descriptions-item>
              <el-descriptions-item label="完全匹配">{{ comparisonStats.perfectMatch }}</el-descriptions-item>
              <el-descriptions-item label="部分匹配">{{ comparisonStats.partialMatch }}</el-descriptions-item>
              <el-descriptions-item label="完全不匹配">{{ comparisonStats.noMatch }}</el-descriptions-item>
              <el-descriptions-item label="平均一致性">{{ comparisonStats.avgConsistency }}%</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 标注人详情对话框 -->
    <el-dialog
      v-model="showAnnotatorDialog"
      title="标注人详情"
      width="80%"
      :close-on-click-modal="false"
    >
      <div v-if="currentAnnotator" class="annotator-detail">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card>
              <template #header>基本信息</template>
              <el-descriptions :column="1" border>
                <el-descriptions-item label="用户名">{{ currentAnnotator.username }}</el-descriptions-item>
                <el-descriptions-item label="总标注数">{{ currentAnnotator.totalAnnotations }}</el-descriptions-item>
                <el-descriptions-item label="完成率">{{ currentAnnotator.completionRate }}%</el-descriptions-item>
                <el-descriptions-item label="平均质量分">{{ currentAnnotator.qualityScore }}/5</el-descriptions-item>
                <el-descriptions-item label="注册时间">{{ formatDate(currentAnnotator.registrationTime) }}</el-descriptions-item>
                <el-descriptions-item label="最后活跃">{{ formatDate(currentAnnotator.lastAnnotationTime) }}</el-descriptions-item>
              </el-descriptions>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card>
              <template #header>标注趋势</template>
              <div ref="annotatorTrendChart" class="chart-container-small"></div>
            </el-card>
          </el-col>
        </el-row>
        
        <el-card style="margin-top: 20px;">
          <template #header>最近标注记录</template>
          <el-table :data="currentAnnotator.recentAnnotations" style="width: 100%">
            <el-table-column prop="imageId" label="图片ID" width="100" />
            <el-table-column prop="imageName" label="图片名称" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="annotationTime" label="标注时间" width="150">
              <template #default="{ row }">
                {{ formatDate(row.annotationTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="timeTaken" label="用时" width="100">
              <template #default="{ row }">
                {{ row.timeTaken }}min
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>

      <template #footer>
        <el-button @click="showAnnotatorDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { 
  Refresh, 
  DataBoard, 
  User, 
  DataAnalysis, 
  Timer,
  ArrowDown
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { userApi } from '@/api/user'
import { exportApi } from '@/api/export'

// 响应式数据
const loading = ref(false)
const userList = ref([])
const workloadViewType = ref('count')

// 筛选条件
const filters = reactive({
  userId: null,
  dateRange: null,
  timeRange: 'month'
})

// 图表加载状态
const chartLoading = reactive({
  workload: false,
  progress: false,
  status: false,
  category: false,
  comparison: false
})

// 总体统计数据
const overallStats = reactive({
  totalAnnotations: 0,
  activeAnnotators: 0,
  averageAccuracy: 0,
  avgAnnotationTime: 0,
  annotationGrowth: 0,
  userGrowth: 0,
  accuracyTrend: 0,
  timeTrend: 0
})

// 标注人排行数据
const annotatorRanking = ref([])

// 对比统计数据
const comparisonStats = reactive({
  totalSamples: 0,
  perfectMatch: 0,
  partialMatch: 0,
  noMatch: 0,
  avgConsistency: 0
})

// 对话框状态
const showAnnotatorDialog = ref(false)
const currentAnnotator = ref(null)

// 图表引用
const workloadChart = ref()
const progressChart = ref()
const statusChart = ref()
const categoryChart = ref()
const accuracyComparisonChart = ref()
const annotatorTrendChart = ref()

// 图表实例
let workloadChartInstance = null
let progressChartInstance = null
let statusChartInstance = null
let categoryChartInstance = null
let accuracyComparisonChartInstance = null
let annotatorTrendChartInstance = null

// 方法定义
const fetchAnalyticsData = async () => {
  try {
    loading.value = true
    
    // 获取总体统计
    await fetchOverallStats()
    
    // 获取标注人排行
    await fetchAnnotatorRanking()
    
    // 获取对比统计
    await fetchComparisonStats()
    
    // 更新图表
    await updateAllCharts()
    
  } catch (error) {
    console.error('获取分析数据失败:', error)
    ElMessage.error('获取分析数据失败')
  } finally {
    loading.value = false
  }
}

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

const fetchOverallStats = async () => {
  try {
    const params = {
      userId: filters.userId,
      startDate: filters.dateRange?.[0],
      endDate: filters.dateRange?.[1],
      timeRange: filters.timeRange
    }
    
    const response = await exportApi.getAnnotationStatistics(params)
    if (response.success) {
      const data = response.data
      
      // 模拟数据，实际应该从API获取
      overallStats.totalAnnotations = data.totalAnnotations || 1250
      overallStats.activeAnnotators = data.activeAnnotators || 12
      overallStats.averageAccuracy = data.averageAccuracy || 94.5
      overallStats.avgAnnotationTime = data.avgAnnotationTime || 3.2
      overallStats.annotationGrowth = data.annotationGrowth || 12.5
      overallStats.userGrowth = data.userGrowth || 8.3
      overallStats.accuracyTrend = data.accuracyTrend || 2.1
      overallStats.timeTrend = data.timeTrend || -5.2
    }
  } catch (error) {
    console.error('获取总体统计失败:', error)
  }
}

const fetchAnnotatorRanking = async () => {
  try {
    // 模拟排行榜数据，实际应该从API获取
    annotatorRanking.value = [
      {
        id: 1,
        username: 'annotator1',
        totalAnnotations: 320,
        completedAnnotations: 295,
        completionRate: 92.2,
        qualityScore: 4.5,
        avgTime: 2.8,
        lastAnnotationTime: '2024-01-15T10:30:00',
        recentAnnotations: []
      },
      {
        id: 2,
        username: 'annotator2',
        totalAnnotations: 285,
        completedAnnotations: 270,
        completionRate: 94.7,
        qualityScore: 4.2,
        avgTime: 3.1,
        lastAnnotationTime: '2024-01-15T09:45:00',
        recentAnnotations: []
      },
      {
        id: 3,
        username: 'annotator3',
        totalAnnotations: 250,
        completedAnnotations: 240,
        completionRate: 96.0,
        qualityScore: 4.8,
        avgTime: 2.5,
        lastAnnotationTime: '2024-01-15T11:20:00',
        recentAnnotations: []
      }
    ]
  } catch (error) {
    console.error('获取标注人排行失败:', error)
  }
}

const fetchComparisonStats = async () => {
  try {
    // 模拟对比统计数据
    comparisonStats.totalSamples = 856
    comparisonStats.perfectMatch = 623
    comparisonStats.partialMatch = 178
    comparisonStats.noMatch = 55
    comparisonStats.avgConsistency = 87.6
  } catch (error) {
    console.error('获取对比统计失败:', error)
  }
}

const updateAllCharts = async () => {
  await nextTick()
  
  updateWorkloadChart()
  updateProgressChart()
  updateStatusChart()
  updateCategoryChart()
  updateAccuracyComparisonChart()
}

const updateWorkloadChart = () => {
  if (!workloadChart.value) return
  
  chartLoading.workload = true
  
  if (workloadChartInstance) {
    workloadChartInstance.dispose()
  }
  
  workloadChartInstance = echarts.init(workloadChart.value)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      boundaryGap: [0, 0.01]
    },
    yAxis: {
      type: 'category',
      data: ['张三', '李四', '王五', '赵六', '孙七']
    },
    series: [
      {
        name: workloadViewType.value === 'count' ? '标注数量' : '标注时长(小时)',
        type: 'bar',
        data: workloadViewType.value === 'count' ? [320, 285, 250, 180, 150] : [12.5, 11.2, 9.8, 7.2, 6.1],
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#667eea' },
            { offset: 1, color: '#764ba2' }
          ])
        }
      }
    ]
  }
  
  workloadChartInstance.setOption(option)
  chartLoading.workload = false
}

const updateProgressChart = () => {
  if (!progressChart.value) return
  
  chartLoading.progress = true
  
  if (progressChartInstance) {
    progressChartInstance.dispose()
  }
  
  progressChartInstance = echarts.init(progressChart.value)
  
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['已完成', '进行中', '已分配']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '已完成',
        type: 'line',
        stack: 'Total',
        data: [120, 132, 101, 134, 90, 230, 210],
        itemStyle: { color: '#67c23a' }
      },
      {
        name: '进行中',
        type: 'line',
        stack: 'Total',
        data: [220, 182, 191, 234, 290, 330, 310],
        itemStyle: { color: '#e6a23c' }
      },
      {
        name: '已分配',
        type: 'line',
        stack: 'Total',
        data: [150, 232, 201, 154, 190, 330, 410],
        itemStyle: { color: '#909399' }
      }
    ]
  }
  
  progressChartInstance.setOption(option)
  chartLoading.progress = false
}

const updateStatusChart = () => {
  if (!statusChart.value) return
  
  chartLoading.status = true
  
  if (statusChartInstance) {
    statusChartInstance.dispose()
  }
  
  statusChartInstance = echarts.init(statusChart.value)
  
  const option = {
    tooltip: {
      trigger: 'item'
    },
    legend: {
      orient: 'vertical',
      left: 'left'
    },
    series: [
      {
        name: '标注状态',
        type: 'pie',
        radius: '50%',
        data: [
          { value: 623, name: '已完成', itemStyle: { color: '#67c23a' } },
          { value: 178, name: '进行中', itemStyle: { color: '#e6a23c' } },
          { value: 55, name: '已分配', itemStyle: { color: '#909399' } }
        ],
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)'
          }
        }
      }
    ]
  }
  
  statusChartInstance.setOption(option)
  chartLoading.status = false
}

const updateCategoryChart = () => {
  if (!categoryChart.value) return
  
  chartLoading.category = true
  
  if (categoryChartInstance) {
    categoryChartInstance.dispose()
  }
  
  categoryChartInstance = echarts.init(categoryChart.value)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['动物', '植物', '建筑', '人物', '交通工具', '其他']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '标注数量',
        type: 'bar',
        data: [250, 180, 320, 150, 200, 100],
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#667eea' },
            { offset: 1, color: '#764ba2' }
          ])
        }
      }
    ]
  }
  
  categoryChartInstance.setOption(option)
  chartLoading.category = false
}

const updateAccuracyComparisonChart = () => {
  if (!accuracyComparisonChart.value) return
  
  chartLoading.comparison = true
  
  if (accuracyComparisonChartInstance) {
    accuracyComparisonChartInstance.dispose()
  }
  
  accuracyComparisonChartInstance = echarts.init(accuracyComparisonChart.value)
  
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    legend: {
      data: ['人工标注', 'AI预测', '一致性']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['动物', '植物', '建筑', '人物', '交通工具']
    },
    yAxis: [
      {
        type: 'value',
        name: '准确率(%)',
        min: 0,
        max: 100
      },
      {
        type: 'value',
        name: '一致性(%)',
        min: 0,
        max: 100
      }
    ],
    series: [
      {
        name: '人工标注',
        type: 'bar',
        data: [95, 92, 88, 94, 90],
        itemStyle: { color: '#67c23a' }
      },
      {
        name: 'AI预测',
        type: 'bar',
        data: [88, 85, 82, 87, 84],
        itemStyle: { color: '#409eff' }
      },
      {
        name: '一致性',
        type: 'line',
        yAxisIndex: 1,
        data: [85, 88, 90, 82, 87],
        itemStyle: { color: '#e6a23c' }
      }
    ]
  }
  
  accuracyComparisonChartInstance.setOption(option)
  chartLoading.comparison = false
}

// 事件处理
const handleFilterChange = () => {
  fetchAnalyticsData()
}

const resetFilters = () => {
  filters.userId = null
  filters.dateRange = null
  filters.timeRange = 'month'
  fetchAnalyticsData()
}

const handleWorkloadCommand = (command) => {
  workloadViewType.value = command
  updateWorkloadChart()
}

const refreshData = () => {
  fetchAnalyticsData()
  fetchUserList()
}

const viewAnnotatorDetail = (annotator) => {
  currentAnnotator.value = annotator
  showAnnotatorDialog.value = true
  
  // 更新标注人趋势图
  nextTick(() => {
    updateAnnotatorTrendChart()
  })
}

const updateAnnotatorTrendChart = () => {
  if (!annotatorTrendChart.value) return
  
  if (annotatorTrendChartInstance) {
    annotatorTrendChartInstance.dispose()
  }
  
  annotatorTrendChartInstance = echarts.init(annotatorTrendChart.value)
  
  const option = {
    tooltip: {
      trigger: 'axis'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '标注数量',
        type: 'line',
        data: [12, 18, 15, 20, 25, 8, 5],
        smooth: true,
        itemStyle: { color: '#667eea' }
      }
    ]
  }
  
  annotatorTrendChartInstance.setOption(option)
}

const generateComparisonReport = () => {
  ElMessage.info('对比报告生成功能开发中...')
}

// 工具函数
const getStatusType = (status) => {
  const typeMap = {
    'ASSIGNED': 'info',
    'IN_PROGRESS': 'warning',
    'COMPLETED': 'success'
  }
  return typeMap[status] || 'info'
}

const getStatusText = (status) => {
  const textMap = {
    'ASSIGNED': '已分配',
    'IN_PROGRESS': '进行中',
    'COMPLETED': '已完成'
  }
  return textMap[status] || status
}

const formatDate = (dateStr) => {
  return dateStr ? new Date(dateStr).toLocaleString() : ''
}

// 生命周期
onMounted(() => {
  fetchAnalyticsData()
  fetchUserList()
})
</script>

<style scoped>
.annotation-analytics {
  padding: 20px;
  background: #f5f5f5;
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

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-header h2 {
  margin: 0;
  color: #333;
}

.filter-section {
  margin-top: 20px;
}

.stat-card {
  margin-bottom: 0;
}

.stat-item {
  display: flex;
  align-items: center;
  padding: 20px;
}

.stat-icon {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  margin-right: 20px;
}

.stat-icon.total {
  background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
}

.stat-icon.users {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.stat-icon.accuracy {
  background: linear-gradient(135deg, #e6a23c 0%, #f56c6c 100%);
}

.stat-icon.productivity {
  background: linear-gradient(135deg, #909399 0%, #606266 100%);
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #333;
  line-height: 1;
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-top: 5px;
}

.stat-change {
  font-size: 12px;
  margin-top: 5px;
}

.stat-change.positive {
  color: #67c23a;
}

.stat-change.negative {
  color: #f56c6c;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chart-container {
  height: 300px;
  width: 100%;
}

.chart-container-small {
  height: 200px;
  width: 100%;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.comparison-chart h4,
.comparison-stats h4 {
  margin: 0 0 15px 0;
  color: #333;
}

.annotator-detail {
  padding: 20px 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .annotation-analytics {
    padding: 10px;
  }
  
  .filter-section .el-row .el-col {
    margin-bottom: 10px;
  }
  
  .stat-item {
    padding: 15px;
  }
  
  .stat-icon {
    width: 50px;
    height: 50px;
    font-size: 20px;
    margin-right: 15px;
  }
  
  .stat-value {
    font-size: 24px;
  }
  
  .chart-container {
    height: 250px;
  }
}
</style>
