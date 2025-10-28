<template>
  <div class="project-list-container">
    <!-- 顶部信息栏 -->
    <div class="header">
      <div class="title">
        <h2>📋 我的任务项目</h2>
        <p class="subtitle">选择一个项目开始标注工作</p>
      </div>
      <div class="user-info">
        <span>欢迎，{{ currentUser?.username }}</span>
        <el-button @click="handleLogout" size="small" type="danger" plain>
          退出
        </el-button>
      </div>
    </div>

    <!-- 项目列表 -->
    <div class="projects-container">
      <el-card 
        v-loading="loading"
        shadow="hover"
        class="projects-card"
      >
        <template #header>
          <div class="card-header">
            <span>项目列表</span>
            <el-button @click="fetchProjects" :loading="loading" size="small" type="primary" plain>
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </template>

        <el-empty 
          v-if="!loading && projects.length === 0" 
          description="暂无分配的项目" 
          :image-size="120"
        />

        <div v-else class="projects-grid">
          <el-card 
            v-for="project in projects" 
            :key="project.id"
            class="project-item"
            shadow="hover"
            @click="enterProject(project)"
          >
            <div class="project-content">
              <div class="project-header">
                <h3 v-html="formatProjectName(project.name)"></h3>
                <el-tag 
                  :type="getStatusType(project.status)" 
                  size="small"
                >
                  {{ getStatusText(project.status) }}
                </el-tag>
              </div>
              
              <p class="project-description">{{ project.description || '暂无描述' }}</p>
              
              <div class="project-stats">
                <div class="stat-item">
                  <span class="stat-label">总任务:</span>
                  <span class="stat-value">{{ project.totalTasks || 0 }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">已完成:</span>
                  <span class="stat-value completed">{{ project.completedTasks || 0 }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">进行中:</span>
                  <span class="stat-value in-progress">{{ project.inProgressTasks || 0 }}</span>
                </div>
              </div>
              
              <div class="project-progress">
                <el-progress 
                  :percentage="getProgressPercentage(project)"
                  :color="getProgressColor(project)"
                  :show-text="false"
                  :stroke-width="6"
                />
                <span class="progress-text">{{ getProgressPercentage(project) }}% 完成</span>
              </div>
              
              <div class="project-footer">
                <span class="created-time">创建时间: {{ formatDate(project.createdAt) }}</span>
                <el-button type="primary" size="small">
                  进入项目 <el-icon><ArrowRight /></el-icon>
                </el-button>
              </div>
            </div>
          </el-card>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Refresh, 
  ArrowRight
} from '@element-plus/icons-vue'
import { userTaskApi } from '@/api/userTask'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const authStore = useAuthStore()

// 状态管理
const loading = ref(false)
const projects = ref([])

// 当前用户信息
const currentUser = computed(() => authStore.user)

// 获取项目列表
const fetchProjects = async () => {
  try {
    loading.value = true
    const response = await userTaskApi.getUserProjects()
    
    if (response.success) {
      projects.value = response.data || []
    } else {
      ElMessage.error('获取项目列表失败')
    }
  } catch (error) {
    console.error('获取项目列表失败:', error)
    ElMessage.error('获取项目列表失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 进入项目
const enterProject = (project) => {
  if (project.status === 'inactive') {
    ElMessage.warning('该项目已停用，暂时无法进入')
    return
  }
  
  router.push(`/user/projects/${project.id}/tasks`)
}

// 获取状态类型
const getStatusType = (status) => {
  const statusMap = {
    'active': 'success',
    'inactive': 'danger',
    'completed': 'info'
  }
  return statusMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    'active': '进行中',
    'inactive': '已停用',
    'completed': '已完成'
  }
  return statusMap[status] || '未知'
}

// 获取进度百分比
const getProgressPercentage = (project) => {
  if (!project.totalTasks || project.totalTasks === 0) return 0
  return Math.round((project.completedTasks || 0) / project.totalTasks * 100)
}

// 获取进度条颜色
const getProgressColor = (project) => {
  const percentage = getProgressPercentage(project)
  if (percentage >= 100) return '#52c788' // 完成 - 绿色
  if (percentage >= 60) return '#409eff'  // 进行中 - 蓝色
  if (percentage >= 30) return '#e6a23c'  // 刚开始 - 橙色
  return '#f56c6c' // 未开始 - 红色
}

// 格式化日期
const formatDate = (dateString) => {
  if (!dateString) return ''
  const date = new Date(dateString)
  return date.toLocaleDateString('zh-CN')
}

// 格式化项目名称（将<...>部分换行显示）
const formatProjectName = (name) => {
  if (!name) return ''
  // 将 <xxx> 部分替换为换行 + <xxx>
  return name.replace(/<([^>]+)>/g, '<br><span class="project-batch">$1</span>')
}

// 退出登录
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确认要退出登录吗？', '退出确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await authStore.logout()
    router.push('/login')
  } catch (error) {
    // 用户取消
  }
}

onMounted(() => {
  fetchProjects()
})
</script>

<style scoped>
.project-list-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding: 0 20px;
}

.title h2 {
  margin: 0;
  color: #2c3e50;
  font-size: 28px;
}

.subtitle {
  margin: 5px 0 0 0;
  color: #7f8c8d;
  font-size: 14px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
  color: #2c3e50;
  font-weight: 500;
}

.projects-container {
  max-width: 1200px;
  margin: 0 auto;
}

.projects-card {
  border-radius: 12px;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #2c3e50;
}

.projects-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 15px;
  padding: 15px 0;
}

.project-item {
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 2px solid transparent;
}

.project-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
  border-color: var(--primary-green);
}

.project-content {
  padding: 12px;
}

.project-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}

.project-header h3 {
  margin: 0;
  color: #2c3e50;
  font-size: 16px;
  line-height: 1.5;
  flex: 1;
}

.project-header h3 :deep(.project-batch) {
  display: block;
  font-size: 13px;
  color: #409eff;
  font-weight: 500;
  margin-top: 2px;
}

.project-description {
  color: #7f8c8d;
  font-size: 13px;
  margin-bottom: 12px;
  line-height: 1.4;
  min-height: 30px;
}

.project-stats {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  padding: 10px;
  background: #f8f9fa;
  border-radius: 6px;
}

.stat-item {
  text-align: center;
  flex: 1;
}

.stat-label {
  display: block;
  font-size: 11px;
  color: #7f8c8d;
  margin-bottom: 4px;
}

.stat-value {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.stat-value.completed {
  color: #52c788;
}

.stat-value.in-progress {
  color: #409eff;
}

.project-progress {
  margin-bottom: 12px;
}

.progress-text {
  display: block;
  text-align: center;
  margin-top: 6px;
  font-size: 11px;
  color: #7f8c8d;
}

.project-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #eee;
}

.created-time {
  font-size: 11px;
  color: #95a5a6;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .project-list-container {
    padding: 15px;
  }
  
  .header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }
  
  .projects-grid {
    grid-template-columns: 1fr;
    gap: 15px;
  }
  
  .project-stats {
    flex-direction: column;
    gap: 10px;
  }
  
  .project-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>

