<template>
  <div class="user-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="showAddDialog = true">
            <el-icon><Plus /></el-icon>
            添加用户
          </el-button>
        </div>
      </template>

      <!-- 搜索区域 -->
      <div class="search-section">
        <el-form :model="searchForm" inline>
          <el-form-item label="用户名">
            <el-input 
              v-model="searchForm.username" 
              placeholder="请输入用户名"
              clearable
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select 
              v-model="searchForm.status" 
              placeholder="请选择状态"
              clearable
            >
              <el-option label="活跃" value="ACTIVE" />
              <el-option label="禁用" value="INACTIVE" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 用户表格 -->
      <el-table 
        :data="userList" 
        style="width: 100%"
        v-loading="loading"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'">
              {{ row.role === 'ADMIN' ? '管理员' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'danger'">
              {{ row.status === 'ACTIVE' ? '活跃' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button 
                size="small" 
                type="primary"
                @click="viewUserProjects(row)"
              >
                查看任务
              </el-button>
              <el-button 
                size="small" 
                :type="row.status === 'ACTIVE' ? 'warning' : 'success'"
                @click="handleToggleStatus(row)"
              >
                {{ row.status === 'ACTIVE' ? '禁用' : '启用' }}
              </el-button>
              <el-button 
                size="small" 
                type="danger" 
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-section">
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
    </el-card>

    <!-- 添加/编辑用户对话框 -->
    <el-dialog 
      v-model="showAddDialog" 
      :title="editingUser ? '编辑用户' : '添加用户'"
      width="500px"
    >
      <el-form 
        ref="userFormRef"
        :model="userForm" 
        :rules="userRules" 
        label-width="100px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input 
            v-model="userForm.password" 
            type="password" 
            :placeholder="editingUser ? '留空则不修改密码' : '请输入密码'"
            show-password
          />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="userForm.role" placeholder="请选择角色">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="userForm.status" placeholder="请选择状态">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="禁用" value="INACTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveUser">确定</el-button>
      </template>
    </el-dialog>

    <!-- 用户项目/任务查看对话框 -->
    <el-dialog
      v-model="showUserProjectsDialog"
      :title="`${selectedUser?.username} - 大任务列表`"
      width="900px"
      destroy-on-close
    >
      <div v-if="selectedUser">
        <div class="user-projects-container">
          <el-table 
            :data="userProjects" 
            style="width: 100%"
            v-loading="loadingUserProjects"
          >
            <el-table-column prop="id" label="项目ID" width="80" />
            <el-table-column prop="name" label="项目名称" min-width="200" />
            <el-table-column prop="description" label="项目描述" min-width="250" />
            
            <el-table-column label="任务统计" width="180">
              <template #default="{ row }">
                <div class="task-stats-mini">
                  <el-tag size="small">总计: {{ row.totalTasks }}</el-tag>
                  <el-tag type="success" size="small">完成: {{ row.completedTasks }}</el-tag>
                  <el-tag type="warning" size="small">进行: {{ row.inProgressTasks }}</el-tag>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="进度" width="100">
              <template #default="{ row }">
                <el-progress 
                  :percentage="getProjectProgress(row)"
                  :stroke-width="4"
                  :show-text="false"
                />
                <div class="progress-text-mini">{{ getProjectProgress(row) }}%</div>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="120">
              <template #default="{ row }">
                <el-button 
                  size="small" 
                  type="primary"
                  @click="viewUserProjectTasks(row)"
                >
                  查看任务
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-empty 
            v-if="!loadingUserProjects && userProjects.length === 0"
            description="该用户暂无分配的大任务"
            :image-size="80"
          />
        </div>
      </div>

      <template #footer>
        <el-button @click="showUserProjectsDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { userApi } from '@/api/user'
import { projectApi } from '@/api/project'

const router = useRouter()

const loading = ref(false)
const showAddDialog = ref(false)
const editingUser = ref(null)
const userFormRef = ref()

// 用户项目查看相关状态
const showUserProjectsDialog = ref(false)
const selectedUser = ref(null)
const userProjects = ref([])
const loadingUserProjects = ref(false)

const searchForm = reactive({
  username: '',
  status: ''
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

const userList = ref([])

const userForm = reactive({
  username: '',
  password: '',
  role: 'USER',
  status: 'ACTIVE',
  email: '',
  phone: ''
})

const userRules = computed(() => ({
  username: [
    { required: true, message: '用户名不能为空', trigger: 'blur' }
  ],
  password: editingUser.value ? [
    { min: 1, message: '密码长度不少于1位', trigger: 'blur' }
  ] : [
    { required: true, message: '密码不能为空', trigger: 'blur' },
    { min: 1, message: '密码长度不少于1位', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ],
  status: [
    { required: true, message: '请选择状态', trigger: 'change' }
  ],
  email: [
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}))

const fetchUserList = async () => {
  try {
    loading.value = true
    const params = {
      page: pagination.page - 1, // 后端是从0开始
      size: pagination.size,
      username: searchForm.username || undefined,
      status: searchForm.status || undefined
    }
    
    const response = await userApi.getUserList(params)
    if (response.success) {
      userList.value = response.data.content || []
      pagination.total = response.data.totalElements || 0
    }
  } catch (error) {
    ElMessage.error('获取用户列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchUserList()
}

const handleReset = () => {
  Object.assign(searchForm, {
    username: '',
    status: ''
  })
  pagination.page = 1
  fetchUserList()
}

// 查看用户项目/大任务 - 直接跳转到第一个大任务的任务列表
const viewUserProjects = async (user) => {
  try {
    loading.value = true
    
    // 调用API获取用户的项目列表
    const response = await userApi.getUserProjects(user.id)
    
    if (response.success && response.data && response.data.length > 0) {
      // 直接跳转到第一个大任务的任务列表
      const firstProject = response.data[0]
      router.push(`/admin/annotation-tasks?projectId=${firstProject.id}&userId=${user.id}`)
    } else {
      ElMessage.warning('该用户暂无分配的大任务')
    }
    
  } catch (error) {
    console.error('获取用户项目失败:', error)
    ElMessage.error('获取用户项目失败')
  } finally {
    loading.value = false
  }
}

const fetchUserProjects = async (userId) => {
  try {
    loadingUserProjects.value = true
    
    // 调用API获取用户的项目列表
    const response = await userApi.getUserProjects(userId)
    
    if (response.success) {
      userProjects.value = response.data || []
      console.log(`用户 ${userId} 的项目列表:`, userProjects.value)
    } else {
      console.log(`用户 ${userId} 暂无项目`)
      userProjects.value = []
      // 不显示错误，因为用户可能确实没有分配项目
    }
    
  } catch (error) {
    console.error('获取用户项目失败:', error)
    ElMessage.error('获取用户项目失败')
    userProjects.value = []
  } finally {
    loadingUserProjects.value = false
  }
}

const viewUserProjectTasks = (project) => {
  // 跳转到任务管理页面，只显示该用户在该项目中的任务
  router.push(`/admin/annotation-tasks?projectId=${project.id}&userId=${selectedUser.value.id}`)
  showUserProjectsDialog.value = false
}

const getProjectProgress = (project) => {
  if (!project.totalTasks) return 0
  return Math.round((project.completedTasks || 0) / project.totalTasks * 100)
}

const handleEdit = (row) => {
  editingUser.value = row
  Object.assign(userForm, {
    username: row.username,
    password: '',
    role: row.role,
    status: row.status,
    email: row.email || '',
    phone: row.phone || ''
  })
  showAddDialog.value = true
}

const handleToggleStatus = async (row) => {
  const action = row.status === 'ACTIVE' ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确认${action}用户 ${row.username} 吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const response = row.status === 'ACTIVE' 
      ? await userApi.disableUser(row.id)
      : await userApi.enableUser(row.id)
      
    if (response.success) {
      ElMessage.success(`用户${action}成功`)
      fetchUserList()
    }
  } catch (error) {
    console.error(`用户${action}失败:`, error)
    ElMessage.error(`用户${action}失败`)
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认删除用户 ${row.username} 吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const response = await userApi.deleteUser(row.id)
    if (response.success) {
      ElMessage.success('删除成功')
      fetchUserList()
    }
  } catch (error) {
    console.error('删除用户失败:', error)
    ElMessage.error('删除用户失败')
  }
}

const handleSaveUser = async () => {
  if (!userFormRef.value) return
  
  try {
    await userFormRef.value.validate()
    
    // 准备发送的数据
    const formData = { ...userForm }
    
    // 如果是编辑模式且密码为空，则不发送密码字段
    if (editingUser.value && !formData.password) {
      delete formData.password
    }
    
    const response = editingUser.value
      ? await userApi.updateUser(editingUser.value.id, formData)
      : await userApi.createUser(formData)
    
    if (response.success) {
      ElMessage.success(editingUser.value ? '更新成功' : '创建成功')
      showAddDialog.value = false
      editingUser.value = null
      resetUserForm()
      fetchUserList()
    }
  } catch (error) {
    console.error('保存用户失败:', error)
    ElMessage.error('保存用户失败')
  }
}

const resetUserForm = () => {
  Object.assign(userForm, {
    username: '',
    password: '',
    role: 'USER',
    status: 'ACTIVE',
    email: '',
    phone: ''
  })
}

const handlePageChange = () => {
  fetchUserList()
}

const handleSizeChange = () => {
  pagination.page = 1
  fetchUserList()
}

const formatDate = (dateStr) => {
  return dateStr ? new Date(dateStr).toLocaleString() : ''
}

onMounted(() => {
  fetchUserList()
})
</script>

<style scoped>
.user-management {
  height: 100%;
}

/* 操作按钮多行排列 */
.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: stretch;
  min-width: 80px;
}

.action-buttons .el-button {
  margin: 0 !important;
  padding: 4px 8px;
  font-size: 11px;
  white-space: nowrap;
  width: 100%;
  text-align: center;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-section {
  margin-bottom: 20px;
  padding: 15px;
  background: #f5f5f5;
  border-radius: 4px;
}

.pagination-section {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

/* 用户项目查看对话框样式 */
.user-projects-container {
  max-height: 500px;
  overflow-y: auto;
}

.task-stats-mini {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.progress-text-mini {
  font-size: 11px;
  color: #666;
  text-align: center;
  margin-top: 2px;
}

.task-stats-mini .el-tag {
  margin: 1px 0;
}
</style>
