<template>
  <div class="project-management">
    <el-card>
      <template #header>
        <div class="page-header">
          <h2>📋 大任务管理</h2>
          <div class="header-actions">
            <el-button type="primary" @click="showCreateDialog = true">
              <el-icon><Plus /></el-icon>
              创建大任务
            </el-button>
            <el-button @click="refreshData" :loading="loading">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 项目/大任务列表 -->
      <el-table 
        :data="projectList" 
        v-loading="loading"
        style="width: 100%"
        @row-click="handleRowClick"
        row-class-name="project-row"
      >
        <el-table-column prop="id" label="项目ID" width="80" />
        <el-table-column prop="name" label="项目名称" min-width="200" />
        <el-table-column prop="description" label="项目描述" min-width="250" />
        
        <el-table-column label="任务统计" width="160">
          <template #default="{ row }">
            <div class="task-stats">
              <el-tag size="small">总计: {{ row.totalTasks }}</el-tag>
              <el-tag type="success" size="small">完成: {{ row.completedTasks }}</el-tag>
              <el-tag type="warning" size="small">进行: {{ row.inProgressTasks }}</el-tag>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="人员分配" width="120">
          <template #default="{ row }">
            <el-tag type="info" size="small">{{ row.assignedUsers || 0 }} 人</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="进度" width="120">
          <template #default="{ row }">
            <el-progress 
              :percentage="getProgressPercentage(row)"
              :stroke-width="6"
              :show-text="false"
            />
            <div class="progress-text">{{ getProgressPercentage(row) }}%</div>
          </template>
        </el-table-column>

        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="createdAt" label="创建时间" width="150">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>

        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button-group>
              <el-button 
                size="small" 
                type="primary" 
                @click.stop="viewProjectTasks(row)"
                title="查看批次任务（按人员分组）"
              >
                <el-icon><List /></el-icon>
                批次任务
              </el-button>
              
              <el-button 
                size="small" 
                type="success" 
                @click.stop="viewProjectUsers(row)"
                title="查看项目人员"
              >
                <el-icon><User /></el-icon>
                项目人员
              </el-button>
              
              <el-dropdown @command="(cmd) => handleDropdownCommand(cmd, row)">
                <el-button size="small" type="info">
                  更多 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="edit">编辑项目</el-dropdown-item>
                    <el-dropdown-item command="statistics">项目统计</el-dropdown-item>
                    <el-dropdown-item command="export">导出数据</el-dropdown-item>
                    <el-dropdown-item command="delete" divided>删除项目</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </el-button-group>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :page-sizes="[10, 20, 50, 100]"
        :total="pagination.total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="fetchProjects"
        @current-change="fetchProjects"
        style="margin-top: 20px; text-align: right"
      />
    </el-card>

    <!-- 创建大任务对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="创建大任务"
      width="600px"
    >
      <el-form
        :model="createForm"
        :rules="createRules"
        ref="createFormRef"
        label-width="100px"
      >
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入项目名称" />
        </el-form-item>
        
        <el-form-item label="项目描述" prop="description">
          <el-input
            v-model="createForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入项目描述"
          />
        </el-form-item>
        
        <el-form-item label="标签配置" prop="labelConfigId">
          <el-select 
            v-model="createForm.labelConfigId" 
            placeholder="选择标签配置"
            @focus="fetchLabelConfigs"
            :loading="loading"
          >
            <el-option 
              v-for="config in labelConfigs" 
              :key="config.id"
              :label="`${config.name}`" 
              :value="config.id"
            >
              <div class="label-config-option">
                <span class="config-name">{{ config.name }}</span>
                <span class="config-desc" v-if="config.description">{{ config.description }}</span>
              </div>
            </el-option>
          </el-select>
          <div class="config-debug">
            <el-text size="small" :type="labelConfigs.length > 0 ? 'success' : 'warning'">
              {{ labelConfigs.length > 0 ? `已加载 ${labelConfigs.length} 个标签配置` : '未找到标签配置，点击下拉框刷新' }}
            </el-text>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">
          创建
        </el-button>
      </template>
    </el-dialog>

    <!-- 项目人员查看对话框 -->
    <el-dialog
      v-model="showUsersDialog"
      :title="`${selectedProject?.name} - 项目人员`"
      width="900px"
    >
      <div v-if="selectedProject">
        <!-- 添加人员按钮 -->
        <div class="dialog-header-actions">
          <el-button 
            type="primary" 
            @click="showAddUserDialog = true"
            :disabled="loadingProjectUsers"
          >
            <el-icon><Plus /></el-icon>
            添加人员
          </el-button>
          <el-button 
            @click="refreshProjectUsers"
            :loading="loadingProjectUsers"
          >
            <el-icon><Refresh /></el-icon>
            刷新
          </el-button>
        </div>

        <!-- 项目人员表格 -->
        <el-table 
          :data="projectUsers" 
          style="width: 100%; margin-top: 15px;"
          v-loading="loadingProjectUsers"
        >
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="email" label="邮箱" />
          <el-table-column label="任务统计" width="200">
            <template #default="{ row }">
              <div class="user-stats">
                <el-tag size="small">总计: {{ row.totalTasks }}</el-tag>
                <el-tag type="success" size="small">完成: {{ row.completedTasks }}</el-tag>
                <el-tag type="warning" size="small">进行: {{ row.inProgressTasks }}</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <el-button-group>
                <el-button 
                  size="small" 
                  type="primary"
                  @click="viewUserTasks(row)"
                >
                  查看任务
                </el-button>
                <el-button 
                  size="small" 
                  type="danger"
                  @click="removeUserFromProject(row)"
                >
                  移除
                </el-button>
              </el-button-group>
            </template>
          </el-table-column>
        </el-table>

        <el-empty 
          v-if="!loadingProjectUsers && projectUsers.length === 0"
          description="该项目暂未分配人员"
          :image-size="80"
        >
          <template #description>
            <p>该项目暂未分配人员</p>
            <el-button type="primary" @click="showAddUserDialog = true">
              立即添加人员
            </el-button>
          </template>
        </el-empty>
      </div>

      <template #footer>
        <el-button @click="showUsersDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 添加人员到项目对话框 -->
    <el-dialog
      v-model="showAddUserDialog"
      title="添加人员到项目"
      width="600px"
      destroy-on-close
    >
      <div class="add-user-content">
        <p class="project-info">
          正在为项目 <strong>{{ selectedProject?.name }}</strong> 添加人员
        </p>

        <!-- 用户搜索和选择 -->
        <el-form label-width="100px">
          <el-form-item label="搜索用户">
            <el-input
              v-model="userSearchKeyword"
              placeholder="输入用户名搜索"
              @input="searchAvailableUsers"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="选择用户">
            <el-select
              v-model="selectedUserIds"
              multiple
              placeholder="选择要添加的用户"
              style="width: 100%"
              @focus="fetchAvailableUsers"
            >
              <el-option
                v-for="user in availableUsers"
                :key="user.id"
                :label="`${user.username} (${user.email || '无邮箱'})`"
                :value="user.id"
              >
                <div class="user-option">
                  <span class="user-name">{{ user.username }}</span>
                  <span class="user-email">{{ user.email || '无邮箱' }}</span>
                </div>
              </el-option>
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="showAddUserDialog = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="handleAddUsers"
          :loading="addingUsers"
          :disabled="selectedUserIds.length === 0"
        >
          添加 {{ selectedUserIds.length > 0 ? `(${selectedUserIds.length}人)` : '' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Plus, 
  Refresh, 
  DataBoard, 
  List, 
  User, 
  ArrowDown,
  Search
} from '@element-plus/icons-vue'
import { projectApi } from '@/api/project'
import { userApi } from '@/api/user'

const router = useRouter()

// 状态管理
const loading = ref(false)
const creating = ref(false)
const projectList = ref([])
const labelConfigs = ref([])
const projectUsers = ref([])

// 对话框状态
const showCreateDialog = ref(false)
const showUsersDialog = ref(false)
const showAddUserDialog = ref(false)
const selectedProject = ref(null)

// 添加用户相关状态
const availableUsers = ref([])
const selectedUserIds = ref([])
const userSearchKeyword = ref('')
const addingUsers = ref(false)
const loadingProjectUsers = ref(false)

// 分页
const pagination = ref({
  page: 1,
  size: 20,
  total: 0
})

// 创建表单
const createForm = ref({
  name: '',
  description: '',
  labelConfigId: null
})

const createFormRef = ref()

const createRules = {
  name: [
    { required: true, message: '请输入项目名称', trigger: 'blur' }
  ],
  labelConfigId: [
    { required: true, message: '请选择标签配置', trigger: 'change' }
  ]
}

// 获取项目列表
const fetchProjects = async () => {
  try {
    loading.value = true
    
    const params = {
      page: pagination.value.page - 1,
      size: pagination.value.size
    }
    
    const response = await projectApi.getProjects(params)
    
    if (response.success) {
      projectList.value = response.data || []
      pagination.value.total = response.data?.length || 0
      
      console.log('获取到项目列表:', projectList.value)
    } else {
      console.error('获取项目列表失败:', response.message)
      ElMessage.error(response.message || '获取项目列表失败')
    }

  } catch (error) {
    console.error('获取项目列表失败:', error)
    ElMessage.error('获取项目列表失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

// 获取标签配置
const fetchLabelConfigs = async () => {
  try {
    console.log('正在获取标签配置...')
    const response = await userApi.getLabelConfigs()
    console.log('标签配置API完整响应:', JSON.stringify(response, null, 2))
    
    if (response.success) {
      // 根据实际API响应结构解析数据
      let configs = []
      
      if (response.data?.content) {
        // 分页数据结构
        configs = response.data.content
      } else if (Array.isArray(response.data)) {
        // 直接数组结构
        configs = response.data
      } else if (response.data) {
        // 单个对象
        configs = [response.data]
      }
      
      labelConfigs.value = configs.map(config => ({
        id: config.id,
        name: config.name,
        description: config.description || config.desc
      }))
      
      console.log('解析后的标签配置:', labelConfigs.value)
      
      if (labelConfigs.value.length === 0) {
        console.warn('警告：标签配置列表为空')
        ElMessage.warning('未找到可用的标签配置')
      }
    } else {
      console.error('获取标签配置失败:', response.message)
      ElMessage.error(`获取标签配置失败: ${response.message}`)
      labelConfigs.value = []
    }
  } catch (error) {
    console.error('获取标签配置异常:', error)
    ElMessage.error('获取标签配置失败，请检查网络连接')
    
    // 使用真实数据作为回退方案
    labelConfigs.value = [
      { id: 2, name: '肾小管节段', description: '肾小管节段配置' }
    ]
    console.log('使用回退数据:', labelConfigs.value)
  }
}

// 刷新数据
const refreshData = () => {
  fetchProjects()
}

// 创建项目
const handleCreate = async () => {
  try {
    await createFormRef.value.validate()
    creating.value = true
    
    const response = await projectApi.create(createForm.value)
    
    if (response.success) {
      ElMessage.success('大任务创建成功')
      showCreateDialog.value = false
      resetCreateForm()
      fetchProjects()
    } else {
      ElMessage.error(response.message || '创建项目失败')
    }
    
  } catch (error) {
    if (error !== false) { // 不是表单验证错误
      console.error('创建项目失败:', error)
      ElMessage.error('创建项目失败，请检查网络连接')
    }
  } finally {
    creating.value = false
  }
}

// 重置创建表单
const resetCreateForm = () => {
  createForm.value = {
    name: '',
    description: '',
    labelConfigId: null
  }
  createFormRef.value?.clearValidate()
}

// 查看项目批次任务（方案一：按人员分组）
const viewProjectTasks = (project) => {
  router.push(`/admin/annotation-tasks?projectId=${project.id}&groupBy=user`)
}

// 查看项目人员（方案二：通过人员查看任务）
const viewProjectUsers = (project) => {
  selectedProject.value = project
  fetchProjectUsers(project.id)
  showUsersDialog.value = true
}

// 获取项目人员列表
const fetchProjectUsers = async (projectId) => {
  try {
    loadingProjectUsers.value = true
    const response = await projectApi.getProjectUsers(projectId)
    
    if (response.success) {
      projectUsers.value = response.data || []
      console.log(`项目 ${projectId} 的人员列表:`, projectUsers.value)
    } else {
      console.error('获取项目人员失败:', response.message)
      projectUsers.value = []
      // 如果是404或者没有数据，不显示错误消息
      if (response.code !== 404) {
        ElMessage.error(response.message || '获取项目人员失败')
      }
    }
    
  } catch (error) {
    console.error('获取项目人员失败:', error)
    projectUsers.value = []
    ElMessage.error('获取项目人员失败')
  } finally {
    loadingProjectUsers.value = false
  }
}

// 刷新项目人员
const refreshProjectUsers = () => {
  if (selectedProject.value) {
    fetchProjectUsers(selectedProject.value.id)
  }
}

// 获取可用用户列表（不在当前项目中的用户）
const fetchAvailableUsers = async () => {
  try {
    const response = await userApi.getUserList({ 
      page: 0, 
      size: 100,
      status: 'ACTIVE'
    })
    
    if (response.success) {
      const allUsers = response.data?.content || response.data || []
      // 过滤掉已经在项目中的用户
      const existingUserIds = projectUsers.value.map(u => u.id)
      availableUsers.value = allUsers.filter(user => 
        user.role === 'USER' && !existingUserIds.includes(user.id)
      )
    } else {
      ElMessage.error('获取用户列表失败')
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败')
  }
}

// 搜索用户
const searchAvailableUsers = () => {
  // 简单的前端搜索过滤
  // 也可以调用后端API进行搜索
}

// 添加用户到项目
const handleAddUsers = async () => {
  if (selectedUserIds.value.length === 0) {
    ElMessage.warning('请选择要添加的用户')
    return
  }

  try {
    addingUsers.value = true
    
    // 调用API添加用户到项目
    const response = await projectApi.addUsersToProject(selectedProject.value.id, selectedUserIds.value)
    
    if (response.success) {
      ElMessage.success(`成功添加 ${selectedUserIds.value.length} 个用户到项目`)
      showAddUserDialog.value = false
      selectedUserIds.value = []
      // 刷新项目人员列表
      await fetchProjectUsers(selectedProject.value.id)
      // 刷新项目列表以更新统计数据
      await fetchProjects()
    } else {
      ElMessage.error(response.message || '添加用户失败')
    }
    
  } catch (error) {
    console.error('添加用户失败:', error)
    ElMessage.error('添加用户失败')
  } finally {
    addingUsers.value = false
  }
}

// 从项目中移除用户
const removeUserFromProject = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定要从项目中移除用户"${user.username}"吗？这将移除该用户的所有任务分配。`,
      '移除确认',
      {
        confirmButtonText: '确定移除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const response = await projectApi.removeUserFromProject(selectedProject.value.id, user.id)
    
    if (response.success) {
      ElMessage.success(`已从项目中移除用户 ${user.username}`)
      // 刷新项目人员列表
      await fetchProjectUsers(selectedProject.value.id)
      // 刷新项目列表以更新统计数据
      await fetchProjects()
    } else {
      ElMessage.error(response.message || '移除用户失败')
    }
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('移除用户失败:', error)
      ElMessage.error('移除用户失败')
    }
  }
}

// 查看用户任务（方案二的具体实现）
const viewUserTasks = (user) => {
  // 跳转到个人任务列表页面
  router.push(`/admin/user-tasks?projectId=${selectedProject.value.id}&userId=${user.id}`)
  showUsersDialog.value = false
}

// 行点击处理
const handleRowClick = (row) => {
  // 默认执行方案一：直接查看批次任务
  viewProjectTasks(row)
}

// 下拉菜单命令处理
const handleDropdownCommand = (command, row) => {
  switch (command) {
    case 'edit':
      editProject(row)
      break
    case 'statistics':
      viewProjectStatistics(row)
      break
    case 'export':
      exportProjectData(row)
      break
    case 'delete':
      deleteProject(row)
      break
  }
}

// 编辑项目
const editProject = (project) => {
  ElMessage.info('编辑功能开发中...')
}

// 查看项目统计
const viewProjectStatistics = (project) => {
  router.push(`/admin/annotation-analytics?projectId=${project.id}`)
}

// 导出项目数据
const exportProjectData = (project) => {
  router.push(`/admin/export?projectId=${project.id}`)
}

// 删除项目
const deleteProject = async (project) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除项目"${project.name}"吗？此操作不可恢复！`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const response = await projectApi.delete(project.id)
    
    if (response.success) {
      ElMessage.success('项目删除成功')
      fetchProjects()
    } else {
      ElMessage.error(response.message || '删除项目失败')
    }
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除项目失败:', error)
      ElMessage.error('删除项目失败')
    }
  }
}

// 辅助方法
const getProgressPercentage = (project) => {
  if (!project.totalTasks) return 0
  return Math.round((project.completedTasks || 0) / project.totalTasks * 100)
}

const getStatusType = (status) => {
  const statusMap = {
    'active': 'success',
    'inactive': 'danger',
    'completed': 'info',
    'paused': 'warning'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status) => {
  const statusMap = {
    'active': '进行中',
    'inactive': '已停用',
    'completed': '已完成',
    'paused': '暂停中'
  }
  return statusMap[status] || '未知'
}

const formatDate = (dateString) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleDateString('zh-CN')
}

// 监听创建对话框显示状态
watch(showCreateDialog, (newVal) => {
  if (newVal) {
    fetchLabelConfigs()
  }
})

onMounted(() => {
  fetchProjects()
  fetchLabelConfigs()
  
  // 临时解决方案：直接设置真实标签配置数据
  setTimeout(() => {
    if (labelConfigs.value.length === 0) {
      console.log('API获取失败，使用真实数据作为备份')
      labelConfigs.value = [
        { 
          id: 2, 
          name: '肾小管节段', 
          description: '肾小管节段标注配置' 
        }
      ]
    }
  }, 2000)
})
</script>

<style scoped>
.project-management {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-header h2 {
  margin: 0;
  color: #2c3e50;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.task-stats {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-stats {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.progress-text {
  font-size: 12px;
  color: #666;
  text-align: center;
  margin-top: 4px;
}

:deep(.project-row) {
  cursor: pointer;
  transition: background-color 0.3s;
}

:deep(.project-row:hover) {
  background-color: #f5f7fa;
}

/* 表格样式优化 */
:deep(.el-table__row) {
  transition: all 0.3s ease;
}

:deep(.el-table__row:hover) {
  transform: translateX(2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.el-button-group {
  display: flex;
  gap: 5px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: 15px;
    align-items: flex-start;
  }
  
  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }
  
  .task-stats,
  .user-stats {
    flex-direction: row;
    flex-wrap: wrap;
  }
}

/* 标签配置选择器样式 */
.label-config-option {
  display: flex;
  flex-direction: column;
  padding: 4px 0;
}

.config-name {
  font-weight: 600;
  color: #2c3e50;
}

.config-desc {
  font-size: 12px;
  color: #7f8c8d;
  margin-top: 2px;
}

.config-debug {
  margin-top: 8px;
}

/* 项目人员对话框样式 */
.dialog-header-actions {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

/* 添加用户对话框样式 */
.add-user-content {
  padding: 10px 0;
}

.project-info {
  margin-bottom: 20px;
  padding: 12px;
  background: #f0f9ff;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  color: #1e40af;
}

.user-option {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.user-name {
  font-weight: 600;
  color: #2c3e50;
}

.user-email {
  font-size: 12px;
  color: #7f8c8d;
}

/* 用户统计样式优化 */
.user-stats {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-stats .el-tag {
  margin: 1px 0;
}

/* 对话框优化 */
:deep(.el-dialog__header) {
  background: #f8f9fa;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

:deep(.el-dialog__body) {
  padding: 20px;
}

:deep(.el-dialog__footer) {
  background: #f8f9fa;
  padding: 15px 20px;
  border-top: 1px solid #eee;
}
</style>
