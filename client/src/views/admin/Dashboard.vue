<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <div class="stats-grid">
      <el-card class="stats-card">
        <div class="stats-item">
          <div class="stats-icon user">
            <el-icon><User /></el-icon>
          </div>
          <div class="stats-content">
            <div class="stats-number">{{ stats.userCount }}</div>
            <div class="stats-label">用户总数</div>
          </div>
        </div>
      </el-card>

      <el-card class="stats-card">
        <div class="stats-item">
          <div class="stats-icon image">
            <el-icon><Picture /></el-icon>
          </div>
          <div class="stats-content">
            <div class="stats-number">{{ stats.imageCount }}</div>
            <div class="stats-label">图像总数</div>
          </div>
        </div>
      </el-card>

      <el-card class="stats-card">
        <div class="stats-item">
          <div class="stats-icon annotation">
            <el-icon><Edit /></el-icon>
          </div>
          <div class="stats-content">
            <div class="stats-number">{{ stats.annotationCount }}</div>
            <div class="stats-label">标注总数</div>
          </div>
        </div>
      </el-card>

      <el-card class="stats-card">
        <div class="stats-item">
          <div class="stats-icon config">
            <el-icon><Collection /></el-icon>
          </div>
          <div class="stats-content">
            <div class="stats-number">{{ stats.labelConfigCount }}</div>
            <div class="stats-label">标签配置</div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 快速操作 -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>快速操作</span>
            </div>
          </template>
          <div class="quick-actions">
            <el-button 
              type="primary" 
              size="large"
              @click="$router.push('/admin/users')"
              class="action-btn"
            >
              <el-icon><UserFilled /></el-icon>
              用户管理
            </el-button>
            <el-button 
              type="success" 
              size="large"
              @click="$router.push('/admin/images')"
              class="action-btn"
            >
              <el-icon><Upload /></el-icon>
              上传图像
            </el-button>
            <el-button 
              type="info" 
              size="large"
              @click="$router.push('/admin/label-configs')"
              class="action-btn"
            >
              <el-icon><Setting /></el-icon>
              标签配置
            </el-button>
            <el-button 
              type="warning" 
              size="large"
              @click="$router.push('/admin/export')"
              class="action-btn"
            >
              <el-icon><Download /></el-icon>
              导出数据
            </el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>系统状态</span>
            </div>
          </template>
          <div class="system-status">
            <div class="status-item">
              <span class="status-label">系统运行状态</span>
              <el-tag :type="getStatusType(systemStatus.systemStatus)">{{ systemStatus.systemStatus }}</el-tag>
            </div>
            <div class="status-item">
              <span class="status-label">数据库连接</span>
              <el-tag :type="getStatusType(systemStatus.databaseStatus)">{{ systemStatus.databaseStatus }}</el-tag>
            </div>
            <div class="status-item">
              <span class="status-label">Redis缓存</span>
              <el-tag :type="getStatusType(systemStatus.redisStatus)">{{ systemStatus.redisStatus }}</el-tag>
            </div>
            <div class="status-item">
              <span class="status-label">文件存储</span>
              <el-tag :type="getStatusType(systemStatus.storageStatus)">{{ systemStatus.storageStatus }}</el-tag>
            </div>
            <div class="status-item">
              <span class="status-label">最后更新时间</span>
              <span>{{ lastUpdateTime }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最近活动 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>最近活动</span>
          <el-button 
            text 
            type="primary" 
            @click="refreshData"
            :loading="loading"
          >
            刷新
          </el-button>
        </div>
      </template>
      <el-table 
        :data="recentActivities" 
        style="width: 100%"
        v-loading="loading"
      >
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getActivityType(row.type)">
              {{ row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="user" label="用户" width="120" />
        <el-table-column prop="time" label="时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.time) }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  User,
  Picture,
  Edit,
  Collection,
  UserFilled,
  Upload,
  Setting,
  Download
} from '@element-plus/icons-vue'
import { dashboardApi } from '@/api/dashboard'

const loading = ref(false)

const stats = reactive({
  userCount: 0,
  imageCount: 0,
  annotationCount: 0,
  labelConfigCount: 0
})

const systemStatus = reactive({
  systemStatus: '未知',
  databaseStatus: '未知', 
  redisStatus: '未知',
  storageStatus: '未知'
})

const lastUpdateTime = ref(new Date().toLocaleString())

const recentActivities = ref([])

const getActivityType = (type) => {
  const typeMap = {
    '用户登录': 'info',
    '图像上传': 'success',
    '标签配置': 'warning',
    '数据导出': 'primary'
  }
  return typeMap[type] || 'info'
}

const formatTime = (time) => {
  return new Date(time).toLocaleString()
}

const getStatusType = (status) => {
  const statusMap = {
    '运行正常': 'success',
    '连接正常': 'success',
    '正常': 'success',
    '连接异常': 'warning',
    '连接失败': 'danger',
    '存在异常': 'warning',
    '未知': 'info'
  }
  return statusMap[status] || 'info'
}

const fetchStats = async () => {
  try {
    loading.value = true
    
    // 调用统计数据API
    const statsResponse = await dashboardApi.getSystemStats()
    if (statsResponse.success) {
      const data = statsResponse.data
      // 正确映射后端返回的嵌套数据结构到前端的扁平结构
      stats.userCount = data.userStats?.totalUsers || 0
      stats.imageCount = data.imageStats?.totalImages || 0
      stats.annotationCount = data.annotationStats?.totalAnnotations || 0
      stats.labelConfigCount = data.systemStats?.activeLabelConfigs || 0
      
      // 映射系统状态
      systemStatus.systemStatus = data.systemStats?.systemStatus || '未知'
      systemStatus.databaseStatus = data.systemStats?.databaseStatus || '未知'
      systemStatus.redisStatus = data.systemStats?.redisStatus || '未知'
      systemStatus.storageStatus = data.systemStats?.storageInfo ? '正常' : '未知'
    }
    
    // 获取最近活动
    const activitiesResponse = await dashboardApi.getRecentActivities(5)
    if (activitiesResponse.success) {
      recentActivities.value = activitiesResponse.data || []
    }
    
    lastUpdateTime.value = new Date().toLocaleString()
  } catch (error) {
    ElMessage.error('获取统计数据失败')
    console.error('获取统计数据失败:', error)
  } finally {
    loading.value = false
  }
}

const refreshData = () => {
  fetchStats()
}

onMounted(() => {
  fetchStats()
})
</script>

<style scoped>
.dashboard {
  padding: 24px;
  background: var(--bg-primary);
  min-height: 100vh;
  position: relative;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

.stats-card {
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
  overflow: hidden;
  position: relative;
}

.stats-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: var(--primary-gradient);
}

.stats-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 32px rgba(66, 211, 146, 0.2);
}

.stats-item {
  display: flex;
  align-items: center;
  padding: 28px;
  position: relative;
}

.stats-icon {
  width: 72px;
  height: 72px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 28px;
  margin-right: 24px;
  position: relative;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.stats-icon::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.2);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.stats-card:hover .stats-icon::after {
  opacity: 1;
}

.stats-icon.user {
  background: var(--primary-gradient);
}

.stats-icon.image {
  background: linear-gradient(135deg, var(--primary-blue) 0%, var(--primary-blue-light) 100%);
}

.stats-icon.annotation {
  background: linear-gradient(135deg, var(--primary-green) 0%, var(--primary-green-light) 100%);
}

.stats-icon.config {
  background: var(--primary-gradient-reverse);
}

.stats-content {
  flex: 1;
}

.stats-number {
  font-size: 42px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
  background: var(--primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.stats-label {
  font-size: 15px;
  color: var(--text-secondary);
  font-weight: 500;
  letter-spacing: 0.025em;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.card-header span {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 18px;
  background: var(--primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

:deep(.el-card) {
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  overflow: hidden;
}

:deep(.el-card:hover) {
  box-shadow: 0 8px 32px rgba(66, 211, 146, 0.15);
  transform: translateY(-2px);
}

:deep(.el-card__header) {
  background: var(--primary-gradient-soft);
  border-bottom: 1px solid rgba(66, 211, 146, 0.1);
  padding: 20px 24px;
  position: relative;
}

:deep(.el-card__header::before) {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: var(--primary-gradient);
  opacity: 0.3;
}

:deep(.el-card__body) {
  padding: 24px;
}

.quick-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.action-btn {
  height: 64px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 12px;
  border: none;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.action-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

.action-btn:hover::before {
  left: 100%;
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.15);
}

.system-status {
  padding: 4px 0;
}

.status-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f1f5f9;
  transition: all 0.2s ease;
}

.status-item:hover {
  background: #f8fafc;
  padding-left: 8px;
  padding-right: 8px;
  border-radius: 8px;
  margin: 0 -8px;
}

.status-item:last-child {
  border-bottom: none;
}

.status-label {
  color: var(--text-secondary);
  font-weight: 500;
  font-size: 15px;
}

:deep(.el-tag) {
  border-radius: 8px;
  font-weight: 500;
  border: none;
  padding: 4px 12px;
}

:deep(.el-table) {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

:deep(.el-table__header) {
  background: #f8fafc;
}

:deep(.el-table th) {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
  border-bottom: 1px solid #e2e8f0;
}

:deep(.el-table td) {
  border-bottom: 1px solid #f1f5f9;
}

:deep(.el-table__row:hover) {
  background: #f8fafc;
}

:deep(.el-button) {
  border-radius: 8px;
  font-weight: 500;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .dashboard {
    padding: 16px;
  }
  
  .stats-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .quick-actions {
    grid-template-columns: 1fr;
  }
  
  .stats-item {
    padding: 20px;
  }
  
  .stats-icon {
    width: 60px;
    height: 60px;
    font-size: 24px;
    margin-right: 16px;
  }
  
  .stats-number {
    font-size: 32px;
  }
}

/* 加载动画 */
@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.6;
  }
}

.loading {
  animation: pulse 2s infinite;
}

/* 微动画效果 */
@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.stats-card {
  animation: slideInUp 0.6s ease;
}

.stats-card:nth-child(1) { animation-delay: 0.1s; }
.stats-card:nth-child(2) { animation-delay: 0.2s; }
.stats-card:nth-child(3) { animation-delay: 0.3s; }
.stats-card:nth-child(4) { animation-delay: 0.4s; }

/* 统一使用白天模式，移除了暗黑模式适配 */
</style>
