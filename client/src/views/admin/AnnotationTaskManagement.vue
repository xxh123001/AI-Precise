<template>
  <div class="annotation-task-management">
    <!-- 页面头部 -->
    <el-card>
      <template #header>
        <div class="page-header">
          <div class="title-section">
            <h2>
              {{ isProjectMode ? '批次任务管理' : '标注任务管理' }}
              <span v-if="projectInfo" class="project-subtitle">
                - {{ projectInfo.name }}
              </span>
              <span v-if="selectedUserInfo" class="user-subtitle">
                - {{ selectedUserInfo.username }}
              </span>
            </h2>
            <div v-if="isProjectMode || isUserMode" class="breadcrumb">
              <el-button 
                size="small" 
                type="primary" 
                plain 
                @click="goBack"
              >
                <el-icon><ArrowLeft /></el-icon>
                {{ isUserMode ? '返回用户管理' : '返回大任务管理' }}
              </el-button>
              
              <!-- 用户模式下的大任务切换按钮 -->
              <div v-if="isUserMode && userProjectsList.length > 0" class="project-navigation">
                <el-button 
                  size="small" 
                  :disabled="!canSwitchPrevious"
                  @click="switchToPreviousProject"
                >
                  <el-icon><ArrowLeft /></el-icon>
                  上一个大任务
                </el-button>
                <span class="project-indicator">
                  {{ currentProjectIndex + 1 }} / {{ userProjectsList.length }}
                </span>
                <el-button 
                  size="small" 
                  :disabled="!canSwitchNext"
                  @click="switchToNextProject"
                >
                  下一个大任务
                  <el-icon><ArrowRight /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
          <div class="header-actions">
            <el-button-group>
              <el-button 
                :type="groupByUser ? 'primary' : 'default'"
                @click="toggleGroupMode(true)"
                size="small"
              >
                <el-icon><User /></el-icon>
                按人员分组
              </el-button>
              <el-button 
                :type="!groupByUser ? 'primary' : 'default'"
                @click="toggleGroupMode(false)"
                size="small"
              >
                <el-icon><List /></el-icon>
                列表视图
              </el-button>
            </el-button-group>
            <el-button type="primary" @click="refreshData" :loading="loading">
              <el-icon><Refresh /></el-icon>
              刷新数据
            </el-button>
            <el-button @click="fetchStatistics" :loading="loading" type="info">
              <el-icon><DataBoard /></el-icon>
              更新统计
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选条件 -->
      <div class="filter-section">
        <el-row :gutter="20">
          <el-col :span="4">
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
          <el-col :span="4">
            <el-select 
              v-model="filters.status" 
              placeholder="选择状态"
              clearable
              @change="handleFilterChange"
            >
              <el-option label="已分配" value="ASSIGNED" />
              <el-option label="进行中" value="IN_PROGRESS" />
              <el-option label="已完成" value="COMPLETED" />
            </el-select>
          </el-col>
          <el-col :span="4">
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
          <el-col :span="4">
            <el-input
              v-model="filters.keyword"
              placeholder="搜索文件名"
              clearable
              @input="handleSearchInput"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-col>
          <el-col :span="4">
            <el-button @click="resetFilters">重置筛选</el-button>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <!-- 统计信息 -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-icon assigned">
              <el-icon><Document /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ statistics.assigned || 0 }}</div>
              <div class="stat-label">已分配</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-icon in-progress">
              <el-icon><Timer /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ statistics.inProgress || 0 }}</div>
              <div class="stat-label">进行中</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-icon completed">
              <el-icon><Collection /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ statistics.completed || 0 }}</div>
              <div class="stat-label">已完成</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-item">
            <div class="stat-icon total">
              <el-icon><DataBoard /></el-icon>
            </div>
            <div class="stat-content">
              <div class="stat-value">{{ statistics.total || 0 }}</div>
              <div class="stat-label">总任务数</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 任务列表 -->
    <el-card style="margin-top: 20px;">
      <template #header>
        <span>任务列表 ({{ pagination.total }} 条)</span>
      </template>

      <!-- 按人员分组视图 -->
      <div v-if="groupByUser" class="user-groups-layout">
        <!-- 左侧用户导航 -->
        <div class="user-navigation">
          <div class="nav-header">
            <h4>👥 用户列表</h4>
            <el-tag type="info" size="small">{{ userTaskGroups.length }} 人</el-tag>
          </div>
          <div class="user-nav-list">
            <div 
              v-for="userGroup in userTaskGroups" 
              :key="userGroup.userId"
              class="user-nav-item"
              :class="{ 'active': selectedUserId === userGroup.userId }"
              @click="scrollToUser(userGroup.userId)"
            >
              <div class="nav-user-info">
                <el-avatar :size="28">{{ userGroup.username?.charAt(0) }}</el-avatar>
                <div class="nav-user-details">
                  <div class="nav-username">{{ userGroup.username }}</div>
                  <div class="nav-user-stats">
                    <span class="total">{{ userGroup.tasks.length }}个</span>
                    <span class="completed">完成{{ userGroup.completedCount }}</span>
                  </div>
                </div>
              </div>
              <div class="nav-progress">
                <el-progress 
                  :percentage="getUserProgressPercentage(userGroup)"
                  :stroke-width="4"
                  :show-text="false"
                  :color="getProgressColor(getUserProgressPercentage(userGroup))"
                />
                <span class="nav-progress-text">{{ getUserProgressPercentage(userGroup) }}%</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧用户表格区域 -->
        <div class="user-tables-content" ref="tablesContainer">
          <div 
            v-for="userGroup in userTaskGroups" 
            :key="userGroup.userId"
            :id="`user-section-${userGroup.userId}`"
            class="user-section"
          >
            <div class="user-section-header">
              <div class="user-info-large">
                <el-avatar :size="40">{{ userGroup.username?.charAt(0) }}</el-avatar>
                <div class="user-details-large">
                  <h3 class="username-large">{{ userGroup.username }}</h3>
                  <div class="user-stats-large">
                    总任务: {{ userGroup.tasks.length }} | 
                    完成: {{ userGroup.completedCount }} | 
                    进行: {{ userGroup.inProgressCount }}
                  </div>
                </div>
              </div>
              <div class="user-progress-large">
                <el-progress 
                  :percentage="getUserProgressPercentage(userGroup)"
                  :stroke-width="8"
                  :show-text="false"
                />
                <span class="progress-text-large">{{ getUserProgressPercentage(userGroup) }}%</span>
              </div>
            </div>

            <div class="user-table-wrapper">
              <el-table 
                :data="userGroup.tasks" 
                style="width: 100%"
                size="small"
                max-height="500"
                scrollbar-always-on
              >
                <el-table-column prop="id" label="任务ID" width="80" />
                <el-table-column label="图片信息" min-width="250">
                  <template #default="{ row }">
                    <div class="image-info-compact">
                      <img 
                        :src="getImageUrl(row.image)" 
                        :alt="row.image?.filename"
                        class="thumbnail-small"
                        @click="previewImage(row.image)"
                      />
                      <div class="image-details-compact">
                        <div class="filename-small">{{ row.image?.originalName }}</div>
                        <div class="filename-id">{{ row.image?.filename }}</div>
                      </div>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="getStatusType(row.status)" size="small">
                      {{ getStatusText(row.status) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="assignedAt" label="分配时间" width="120">
                  <template #default="{ row }">
                    {{ formatDateTime(row.assignedAt) }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="150">
                  <template #default="{ row }">
                    <el-button-group>
                      <el-button size="small" @click="viewTaskDetail(row)">
                        详情
                      </el-button>
                      <el-button size="small" type="primary" @click="editTask(row)">
                        编辑
                      </el-button>
                    </el-button-group>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </div>
      </div>

      <!-- 传统列表视图 -->
      <div v-else class="table-container">
        <el-table 
          :data="taskList" 
          style="width: 100%"
          v-loading="loading"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="50" />
          <el-table-column prop="id" label="任务ID" width="80" />
          <el-table-column label="图片信息" min-width="280">
            <template #default="{ row }">
              <div class="image-info">
                <img 
                  :src="getImageUrl(row.image)" 
                  :alt="row.image?.filename"
                  class="thumbnail"
                  @click="previewImage(row.image)"
                />
                <div class="image-details">
                  <div class="filename" :title="row.image?.originalName">
                    {{ row.image?.originalName }}
                  </div>
                  <div class="image-size">{{ row.image?.filename?.substring(0, 25) }}{{ row.image?.filename?.length > 25 ? '...' : '' }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="标注人" width="100">
            <template #default="{ row }">
              <el-tag type="info" size="small">{{ row.user?.username || '未知' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="分配时间" width="130">
            <template #default="{ row }">
              <span class="date-text">{{ formatDate(row.assignedAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="完成时间" width="130">
            <template #default="{ row }">
              <span class="date-text">{{ row.completedAt ? formatDate(row.completedAt) : '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="进度" width="110">
            <template #default="{ row }">
              <div v-if="row.hasAnnotation || row.status === 'COMPLETED'" class="progress-cell">
                <el-progress 
                  :percentage="row.status === 'COMPLETED' ? 100 : (row.hasAnnotation ? 75 : 50)"
                  :status="row.status === 'COMPLETED' ? 'success' : 'warning'"
                  :stroke-width="4"
                  :show-text="false"
                />
                <div class="progress-text">{{ getProgressText(row) }}</div>
              </div>
              <div v-else class="no-annotation">未开始</div>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <div class="action-buttons">
                <el-button size="small" type="primary" @click="viewTask(row)">
                  查看详情
                </el-button>
                <el-button 
                  size="small" 
                  type="success" 
                  @click="editAnnotation(row)"
                  v-if="row.hasAnnotation"
                >
                  编辑标注
                </el-button>
                <el-dropdown trigger="click">
                  <el-button size="small" type="info">
                    更多<el-icon class="el-icon--right"><ArrowDown /></el-icon>
                  </el-button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item @click="reassignTask(row)">
                        重新分配
                      </el-dropdown-item>
                      <el-dropdown-item @click="deleteTask(row)" class="danger">
                        删除任务
                      </el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 分页（仅在列表视图显示） -->
      <div v-if="!groupByUser" class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 任务详情对话框 -->
    <el-dialog
      v-model="showTaskDialog"
      title="任务详情"
      width="80%"
      :close-on-click-modal="false"
    >
      <div v-if="currentTask" class="task-detail">
        <el-row :gutter="20">
          <el-col :span="12">
            <div class="task-info">
              <h3>任务信息</h3>
              <el-descriptions :column="1" border>
                <el-descriptions-item label="任务ID">{{ currentTask.id }}</el-descriptions-item>
                <el-descriptions-item label="状态">
                  <el-tag :type="getStatusType(currentTask.status)">
                    {{ getStatusText(currentTask.status) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="标注人">{{ currentTask.user?.username || '未知' }}</el-descriptions-item>
                <el-descriptions-item label="分配者">{{ currentTask.assigner?.username || '未知' }}</el-descriptions-item>
                <el-descriptions-item label="分配时间">{{ formatDate(currentTask.assignedAt) }}</el-descriptions-item>
                <el-descriptions-item label="完成时间">{{ currentTask.completedAt ? formatDate(currentTask.completedAt) : '未完成' }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="image-preview">
              <h3>图片预览</h3>
              <img 
                :src="getImageUrl(currentTask.image)" 
                :alt="currentTask.image?.filename"
                class="detail-image"
              />
            </div>
          </el-col>
        </el-row>

        <!-- 标注信息 -->
        <div v-if="currentTask.hasAnnotation && currentTask.annotationStatus" class="annotation-info">
          <h3>标注信息</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="标注状态">
              <el-tag :type="getAnnotationStatusType(currentTask.annotationStatus)">
                {{ getAnnotationStatusText(currentTask.annotationStatus) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="任务状态">
              <el-tag :type="getStatusType(currentTask.status)">
                {{ getStatusText(currentTask.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="分配时间">{{ formatDate(currentTask.assignedAt) }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ currentTask.completedAt ? formatDate(currentTask.completedAt) : '未完成' }}</el-descriptions-item>
          </el-descriptions>

          <!-- 标注内容 -->
          <div class="annotation-content">
            <h4>标注内容</h4>
            <el-alert 
              title="标注详情" 
              type="info" 
              description="该任务已有标注数据，可以通过编辑功能查看详细内容。"
              show-icon
              :closable="false"
            />
          </div>
        </div>
        <div v-else class="no-annotation-info">
          <el-empty description="该任务暂无标注信息" :image-size="100" />
        </div>
      </div>

      <template #footer>
        <div class="dialog-actions">
          <el-button @click="showTaskDialog = false">关闭</el-button>
          <el-button type="primary" @click="editAnnotation(currentTask)" v-if="currentTask?.hasAnnotation">
            编辑标注
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 标注编辑对话框 -->
    <el-dialog
      v-model="showEditDialog"
      title="编辑标注"
      width="90%"
      :close-on-click-modal="false"
    >
      <div v-if="editingTask" class="annotation-editor">
        <el-row :gutter="20">
          <el-col :span="12">
            <div class="image-section">
              <h3>图片预览</h3>
              <img 
                :src="editingTask.image?.url" 
                :alt="editingTask.image?.filename"
                class="edit-image"
              />
            </div>
          </el-col>
          <el-col :span="12">
            <div class="edit-form">
              <h3>标注编辑</h3>
              
              <!-- 动态生成标注表单 -->
              <div v-if="editingLabelConfig" class="label-categories">
                <div 
                  v-for="category in editingLabelConfig.categories" 
                  :key="category.id"
                  class="category-item"
                >
                  <div class="category-header">
                    <label class="category-label">
                      {{ category.name }}
                      <span v-if="category.required" class="required">*</span>
                    </label>
                    <div class="category-type">{{ getCategoryTypeText(category.type) }}</div>
                  </div>

                  <!-- 单选类型 -->
                  <el-radio-group 
                    v-if="category.type === 'single_choice'"
                    v-model="editingAnnotations[category.id]"
                    class="option-group"
                  >
                    <el-radio 
                      v-for="option in category.options"
                      :key="option.value"
                      :value="option.value"
                      class="option-item"
                    >
                      {{ option.label }}
                    </el-radio>
                  </el-radio-group>

                  <!-- 多选类型 -->
                  <el-checkbox-group 
                    v-else-if="category.type === 'multiple_choice'"
                    v-model="editingAnnotations[category.id]"
                    class="option-group"
                  >
                    <el-checkbox 
                      v-for="option in category.options"
                      :key="option.value"
                      :value="option.value"
                      class="option-item"
                    >
                      {{ option.label }}
                    </el-checkbox>
                  </el-checkbox-group>

                  <!-- 文本输入类型 -->
                  <el-input
                    v-else-if="category.type === 'text'"
                    v-model="editingAnnotations[category.id]"
                    type="textarea"
                    :placeholder="`请输入${category.name}`"
                    :rows="3"
                  />
                </div>
              </div>

              <!-- 备注编辑 -->
              <div class="remark-section">
                <div class="category-item remark-item">
                  <div class="category-header">
                    <label class="category-label">标注备注</label>
                    <div class="category-type">备注</div>
                  </div>
                  <el-input
                    v-model="editingRemark"
                    type="textarea"
                    placeholder="请输入标注备注信息（可选）"
                    :rows="3"
                    maxlength="500"
                    show-word-limit
                  />
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>

      <template #footer>
        <div class="dialog-actions">
          <el-button @click="showEditDialog = false">取消</el-button>
          <el-button type="primary" @click="saveAnnotationEdit" :loading="saving">
            保存修改
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 图片预览对话框 -->
    <el-dialog
      v-model="showImageDialog"
      title="图片预览"
      width="60%"
      :close-on-click-modal="true"
    >
      <div v-if="previewImageData" class="image-preview-dialog">
        <img 
          :src="previewImageData.url" 
          :alt="previewImageData.filename"
          class="preview-image"
        />
        <div class="image-info-detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="文件名">{{ previewImageData.filename }}</el-descriptions-item>
            <el-descriptions-item label="文件大小">{{ formatFileSize(previewImageData.fileSize) }}</el-descriptions-item>
            <el-descriptions-item label="MIME类型">{{ previewImageData.mimeType }}</el-descriptions-item>
            <el-descriptions-item label="上传时间">{{ formatDate(previewImageData.createdAt) }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Refresh, 
  Search, 
  Document, 
  Timer, 
  Collection, 
  DataBoard,
  ArrowDown,
  ArrowLeft,
  ArrowRight,
  User,
  List
} from '@element-plus/icons-vue'
import { userApi } from '@/api/user'
import { adminTaskApi } from '@/api/adminTask'

const route = useRoute()
const router = useRouter()

// 路由参数
const routeParams = computed(() => ({
  projectId: route.query.projectId,
  userId: route.query.userId,
  groupBy: route.query.groupBy
}))

// 响应式数据
const loading = ref(false)
const saving = ref(false)
const taskList = ref([])
const userList = ref([])
const selectedTasks = ref([])

// 新增：项目和用户模式相关数据
const projectInfo = ref(null)
const selectedUserInfo = ref(null)
const groupByUser = ref(false)
const userTaskGroups = ref([])

// 用户导航相关
const selectedUserId = ref(null)
const tablesContainer = ref(null)

// 用户大任务列表和切换
const userProjectsList = ref([])
const currentProjectIndex = ref(0)
const loadingProjects = ref(false)

// 筛选条件
const filters = reactive({
  userId: null,
  status: null,
  dateRange: null,
  keyword: ''
})

// 分页信息
const pagination = reactive({
  current: 1,
  size: 20,
  total: 0
})

// 统计数据
const statistics = reactive({
  assigned: 0,
  inProgress: 0,
  completed: 0,
  total: 0
})

// 对话框状态
const showTaskDialog = ref(false)
const showEditDialog = ref(false)
const showImageDialog = ref(false)

// 当前任务数据
const currentTask = ref(null)

// 计算属性
const isProjectMode = computed(() => !!routeParams.value.projectId)
const isUserMode = computed(() => !!routeParams.value.userId)
const pageTitle = computed(() => {
  if (isUserMode.value && selectedUserInfo.value) {
    return `${selectedUserInfo.value.username} - 任务管理`
  }
  if (isProjectMode.value && projectInfo.value) {
    return `${projectInfo.value.name} - 批次任务`
  }
  return '标注任务管理'
})
const editingTask = ref(null)
const editingLabelConfig = ref(null)
const editingAnnotations = reactive({})
const editingRemark = ref('')
const previewImageData = ref(null)

// 搜索防抖
let searchTimer = null

// 计算属性
const parsedLabels = computed(() => {
  if (!currentTask.value?.annotation?.labels) return null
  
  try {
    const labelsStr = currentTask.value.annotation.labels
    if (typeof labelsStr === 'string') {
      return JSON.parse(labelsStr)
    }
    return labelsStr
  } catch (error) {
    console.error('解析标注内容失败:', error)
    return null
  }
})

// 新增方法：项目和用户模式支持
const initializePageMode = async () => {
  // 根据路由参数初始化页面模式
  if (routeParams.value.groupBy === 'user') {
    groupByUser.value = true
  }
  
  // 如果有projectId，获取项目信息
  if (routeParams.value.projectId) {
    await fetchProjectInfo(routeParams.value.projectId)
  }
  
  // 如果有userId，获取用户信息
  if (routeParams.value.userId) {
    await fetchUserInfo(routeParams.value.userId)
    filters.userId = parseInt(routeParams.value.userId)
  }
}

const fetchProjectInfo = async (projectId) => {
  try {
    // 这里调用API获取项目信息
    projectInfo.value = {
      id: projectId,
      name: `标注项目 ${projectId}`,
      description: '项目描述'
    }
  } catch (error) {
    console.error('获取项目信息失败:', error)
  }
}

const fetchUserInfo = async (userId) => {
  try {
    const response = await userApi.getUserById(userId)
    if (response.success) {
      selectedUserInfo.value = response.data
      
      // 获取用户的所有大任务列表
      await fetchUserProjectsList(userId)
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

const fetchUserProjectsList = async (userId) => {
  try {
    loadingProjects.value = true
    const response = await userApi.getUserProjects(userId)
    
    if (response.success && response.data) {
      userProjectsList.value = response.data
      
      // 找到当前项目在列表中的索引
      const currentProjectId = parseInt(routeParams.value.projectId)
      const index = userProjectsList.value.findIndex(p => p.id === currentProjectId)
      currentProjectIndex.value = index >= 0 ? index : 0
      
      // 更新项目信息
      if (userProjectsList.value.length > 0 && currentProjectIndex.value < userProjectsList.value.length) {
        projectInfo.value = userProjectsList.value[currentProjectIndex.value]
      }
    }
  } catch (error) {
    console.error('获取用户大任务列表失败:', error)
  } finally {
    loadingProjects.value = false
  }
}

// 切换到上一个大任务
const switchToPreviousProject = () => {
  if (currentProjectIndex.value > 0) {
    const previousProject = userProjectsList.value[currentProjectIndex.value - 1]
    router.push(`/admin/annotation-tasks?projectId=${previousProject.id}&userId=${routeParams.value.userId}`)
  }
}

// 切换到下一个大任务
const switchToNextProject = () => {
  if (currentProjectIndex.value < userProjectsList.value.length - 1) {
    const nextProject = userProjectsList.value[currentProjectIndex.value + 1]
    router.push(`/admin/annotation-tasks?projectId=${nextProject.id}&userId=${routeParams.value.userId}`)
  }
}

// 计算属性：是否可以切换到上一个
const canSwitchPrevious = computed(() => {
  return isUserMode.value && currentProjectIndex.value > 0
})

// 计算属性：是否可以切换到下一个
const canSwitchNext = computed(() => {
  return isUserMode.value && currentProjectIndex.value < userProjectsList.value.length - 1
})

const toggleGroupMode = (groupMode) => {
  groupByUser.value = groupMode
  if (groupMode) {
    organizeTasksByUser()
  }
}

const organizeTasksByUser = () => {
  // 按用户分组任务
  const userGroups = {}
  
  taskList.value.forEach(task => {
    const userId = task.user?.id || 'unknown'
    const username = task.user?.username || '未知用户'
    
    if (!userGroups[userId]) {
      userGroups[userId] = {
        userId: userId,
        username: username,
        tasks: [],
        completedCount: 0,
        inProgressCount: 0
      }
    }
    
    userGroups[userId].tasks.push(task)
    
    if (task.status === 'COMPLETED') {
      userGroups[userId].completedCount++
    } else if (task.status === 'IN_PROGRESS') {
      userGroups[userId].inProgressCount++
    }
  })
  
  // 按完成任务数量排序，完成多的排在前面
  userTaskGroups.value = Object.values(userGroups).sort((a, b) => {
    return b.completedCount - a.completedCount || b.tasks.length - a.tasks.length
  })
  
  // 自动选中第一个用户
  if (userTaskGroups.value.length > 0) {
    selectedUserId.value = userTaskGroups.value[0].userId
  }
}

const getUserProgressPercentage = (userGroup) => {
  if (!userGroup.tasks.length) return 0
  return Math.round((userGroup.completedCount / userGroup.tasks.length) * 100)
}

// 滚动到指定用户的表格
const scrollToUser = (userId) => {
  selectedUserId.value = userId
  
  const targetElement = document.getElementById(`user-section-${userId}`)
  const container = tablesContainer.value
  
  if (targetElement && container) {
    targetElement.scrollIntoView({
      behavior: 'smooth',
      block: 'start'
    })
  }
}

// 获取进度条颜色
const getProgressColor = (percentage) => {
  if (percentage >= 90) return '#52c41a'  // 绿色
  if (percentage >= 70) return '#1890ff'  // 蓝色
  if (percentage >= 50) return '#faad14'  // 橙色
  if (percentage >= 30) return '#fa8c16'  // 深橙色
  return '#ff4d4f' // 红色
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return ''
  return new Date(dateTime).toLocaleString('zh-CN', {
    month: 'numeric',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const goBack = () => {
  if (isUserMode.value) {
    router.push('/admin/users')
  } else if (isProjectMode.value) {
    router.push('/admin/projects')
  }
}

// 监听路由参数变化
watch(() => routeParams.value, async (newParams) => {
  await initializePageMode()
  await fetchTaskList()
}, { immediate: true })

// 方法定义
const fetchTaskList = async () => {
  try {
    loading.value = true
    
    const params = {
      page: groupByUser.value ? 0 : pagination.current - 1, // 分组模式下不分页
      size: groupByUser.value ? 10000 : pagination.size,   // 分组模式下获取所有数据
      userId: filters.userId || routeParams.value.userId,
      status: filters.status,
      startDate: filters.dateRange?.[0],
      endDate: filters.dateRange?.[1],
      keyword: filters.keyword?.trim(),
      projectId: routeParams.value.projectId // 新增：项目筛选
    }

    // 使用新的管理员任务API
    const response = await adminTaskApi.getTaskAssignments(params)
    
    if (response.success) {
      taskList.value = response.data.content || []
      pagination.total = response.data.totalElements || 0
      
      // 如果是按人员分组模式，组织数据
      if (groupByUser.value) {
        organizeTasksByUser()
      }
      
      // 获取真实统计数据
      await fetchStatistics()
    }
  } catch (error) {
    console.error('获取任务列表失败:', error)
    ElMessage.error('获取任务列表失败')
  } finally {
    loading.value = false
  }
}

const fetchUserList = async () => {
  try {
    const response = await adminTaskApi.getTaskUsers()
    if (response.success) {
      userList.value = response.data || []
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
  }
}

const fetchStatistics = async () => {
  try {
    // 根据当前模式传递不同的参数
    const params = {
      userId: routeParams.value.userId || undefined,
      projectId: routeParams.value.projectId || undefined
    }
    
    const response = await adminTaskApi.getTaskStatistics(params)
    if (response.success) {
      const data = response.data
      statistics.assigned = data.assignedCount || 0
      statistics.inProgress = data.inProgressCount || 0
      statistics.completed = data.completedCount || 0
      statistics.total = data.totalAssignments || 0
      console.log('统计数据更新:', statistics, '筛选条件:', params)
    } else {
      console.error('获取统计数据失败:', response.message)
      // 失败时使用本地数据作为备用
      updateLocalStatistics()
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
    // 失败时使用本地数据作为备用
    updateLocalStatistics()
  }
}

const updateLocalStatistics = () => {
  statistics.assigned = taskList.value.filter(task => task.status === 'ASSIGNED').length
  statistics.inProgress = taskList.value.filter(task => task.status === 'IN_PROGRESS').length
  statistics.completed = taskList.value.filter(task => task.status === 'COMPLETED').length
  statistics.total = taskList.value.length
}

const handleFilterChange = async () => {
  pagination.current = 1
  await fetchTaskList()
  // 筛选条件改变后也要更新统计数据
  await fetchStatistics()
}

const handleSearchInput = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    handleFilterChange()
  }, 500)
}

const resetFilters = async () => {
  filters.userId = null
  filters.status = null
  filters.dateRange = null
  filters.keyword = ''
  await handleFilterChange()
  ElMessage.success('筛选条件已重置')
}

const handleSelectionChange = (selection) => {
  selectedTasks.value = selection
}

const handleSizeChange = async (newSize) => {
  pagination.size = newSize
  pagination.current = 1
  await fetchTaskList()
  // 数据更新后刷新统计
  await fetchStatistics()
}

const handleCurrentChange = async (newPage) => {
  pagination.current = newPage
  await fetchTaskList()
}

const refreshData = async () => {
  await Promise.all([
    fetchTaskList(),
    fetchUserList(),
    fetchStatistics()
  ])
  ElMessage.success('数据已刷新')
}

const viewTask = async (task) => {
  try {
    currentTask.value = {
      ...task,
      assigner: { username: task.assignerUsername }
    }
    showTaskDialog.value = true
  } catch (error) {
    console.error('查看任务详情失败:', error)
    ElMessage.error('查看任务详情失败')
  }
}

const editAnnotation = async (task) => {
  try {
    console.log('编辑标注:', task)
    editingTask.value = task
    
    // 获取标注数据
    const response = await adminTaskApi.getAnnotation(task.image.id, task.user.id)
    
    if (response.success && response.data) {
      const annotation = response.data
      
      // 解析标注数据
      const labels = typeof annotation.labels === 'string' 
        ? JSON.parse(annotation.labels)
        : annotation.labels
      
      // 设置编辑数据
      editingLabelConfig.value = task.image?.labelConfig || {}
      Object.assign(editingAnnotations, labels || {})
      editingRemark.value = annotation.remark || ''
      
      // 保存标注ID用于后续更新
      editingTask.value.annotationId = annotation.id
      
      showEditDialog.value = true
      console.log('标注数据加载成功')
    } else {
      ElMessage.error('未找到标注数据')
    }
  } catch (error) {
    console.error('加载标注数据失败:', error)
    ElMessage.error('加载标注数据失败: ' + error.message)
  }
}

const saveAnnotationEdit = async () => {
  try {
    saving.value = true
    
    if (!editingTask.value?.annotationId) {
      ElMessage.error('缺少标注ID')
      return
    }
    
    const updateData = {
      labels: JSON.stringify(editingAnnotations),
      remark: editingRemark.value || null,
      status: 'COMPLETED'
    }
    
    console.log('保存标注修改:', updateData)
    const response = await adminTaskApi.updateAnnotation(editingTask.value.annotationId, updateData)
    
    if (response.success) {
      ElMessage.success('标注修改成功')
      showEditDialog.value = false
      
      // 刷新任务列表
      await fetchTaskList()
      
      // 如果任务详情对话框也在显示，关闭它
      if (showTaskDialog.value) {
        showTaskDialog.value = false
      }
    } else {
      ElMessage.error('保存失败: ' + (response.message || ''))
    }
  } catch (error) {
    console.error('保存标注修改失败:', error)
    ElMessage.error('保存失败: ' + error.message)
  } finally {
    saving.value = false
  }
}

const reassignTask = async (task) => {
  try {
    await ElMessageBox.confirm('确认要重新分配此任务吗？', '重新分配确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    // TODO: 实现重新分配逻辑
    ElMessage.info('重新分配功能开发中...')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('重新分配任务失败:', error)
    }
  }
}

const deleteTask = async (task) => {
  try {
    await ElMessageBox.confirm('确认要删除此任务吗？删除后无法恢复！', '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'error'
    })
    
    // TODO: 实现删除任务逻辑
    ElMessage.info('删除功能开发中...')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除任务失败:', error)
    }
  }
}

const previewImage = (image) => {
  previewImageData.value = {
    ...image,
    url: getImageUrl(image)
  }
  showImageDialog.value = true
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

const getAnnotationStatusType = (status) => {
  const typeMap = {
    'DRAFT': 'warning',
    'COMPLETED': 'success'
  }
  return typeMap[status] || 'info'
}

const getAnnotationStatusText = (status) => {
  const textMap = {
    'DRAFT': '草稿',
    'COMPLETED': '已完成'
  }
  return textMap[status] || status
}

const getCategoryTypeText = (type) => {
  const typeMap = {
    'single_choice': '单选',
    'multiple_choice': '多选',
    'text': '文本'
  }
  return typeMap[type] || type
}

const formatDate = (dateStr) => {
  return dateStr ? new Date(dateStr).toLocaleString() : ''
}

const getImageUrl = (image) => {
  if (!image || !image.filename) return ''
  return `http://62.234.94.65:8080/uploads/${image.filename}`
}

const getProgressText = (task) => {
  if (task.status === 'COMPLETED') return '已完成'
  if (task.hasAnnotation) return '进行中'
  if (task.status === 'IN_PROGRESS') return '进行中'
  return '未开始'
}


const formatFileSize = (bytes) => {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}


// 生命周期
// 监听路由参数变化
watch(() => routeParams.value, async (newParams) => {
  await initializePageMode()
  fetchTaskList()
}, { immediate: false })

onMounted(async () => {
  // 并行获取所有初始数据
  await Promise.all([
    initializePageMode(),
    fetchTaskList(),
    fetchUserList(),
    fetchStatistics()
  ])
  console.log('页面初始化完成，已加载任务列表、用户列表和统计数据')
})
</script>

<style scoped>
.annotation-task-management {
  padding: 20px;
  background: #f5f5f5;
  min-height: 100vh;
}

/* 页面头部样式 */
.title-section h2 {
  margin: 0;
  color: #2c3e50;
}

.project-subtitle,
.user-subtitle {
  color: #409eff;
  font-weight: normal;
  font-size: 16px;
}

.breadcrumb {
  margin-top: 8px;
  display: flex;
  align-items: center;
  gap: 15px;
}

.project-navigation {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: 15px;
  padding-left: 15px;
  border-left: 2px solid #e4e7ed;
}

.project-indicator {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
  padding: 0 10px;
  background: #f5f7fa;
  border-radius: 4px;
  line-height: 28px;
}

/* 按人员分组视图 - 左右分栏布局 */
.user-groups-layout {
  display: flex;
  gap: 20px;
  height: calc(100vh - 400px);
  min-height: 600px;
}

/* 左侧用户导航 */
.user-navigation {
  width: 280px;
  flex-shrink: 0;
  background: #fafafa;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  max-height: 100%;
}

.nav-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e4e7ed;
  background: #f8f9fa;
  border-radius: 8px 8px 0 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nav-header h4 {
  margin: 0;
  color: #2c3e50;
  font-size: 14px;
}

.user-nav-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.user-nav-item {
  padding: 12px;
  margin: 4px 0;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.user-nav-item:hover {
  background: #e8f4fd;
  border-color: #409eff;
  transform: translateX(2px);
}

.user-nav-item.active {
  background: #409eff;
  color: white;
  border-color: #409eff;
}

.nav-user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.nav-user-details {
  flex: 1;
  min-width: 0;
}

.nav-username {
  font-size: 13px;
  font-weight: 600;
  color: inherit;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-nav-item.active .nav-username {
  color: white;
}

.nav-user-stats {
  font-size: 11px;
  color: #7f8c8d;
  margin-top: 2px;
}

.user-nav-item.active .nav-user-stats {
  color: rgba(255, 255, 255, 0.8);
}

.nav-user-stats .total {
  margin-right: 8px;
}

.nav-progress {
  display: flex;
  align-items: center;
  gap: 6px;
}

.nav-progress-text {
  font-size: 11px;
  color: #666;
  min-width: 30px;
  text-align: right;
}

.user-nav-item.active .nav-progress-text {
  color: rgba(255, 255, 255, 0.9);
}

/* 右侧内容区域 */
.user-tables-content {
  flex: 1;
  overflow-y: auto;
  padding-right: 4px;
}

.user-section {
  margin-bottom: 30px;
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.user-section:last-child {
  margin-bottom: 0;
}

.user-section-header {
  padding: 16px 20px;
  background: #f8f9fa;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-info-large {
  display: flex;
  align-items: center;
  gap: 15px;
}

.user-details-large {
  display: flex;
  flex-direction: column;
}

.username-large {
  margin: 0;
  font-size: 18px;
  color: #2c3e50;
}

.user-stats-large {
  font-size: 13px;
  color: #7f8c8d;
  margin-top: 4px;
}

.user-progress-large {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 120px;
}

.progress-text-large {
  font-size: 14px;
  color: #409eff;
  font-weight: 600;
  margin-top: 6px;
}

.user-table-wrapper {
  padding: 0;
  max-height: 500px;
  overflow: hidden;
}

.user-table-wrapper .el-table {
  border: none;
}

.user-table-wrapper .el-table__body-wrapper {
  max-height: 450px;
  overflow-y: auto;
}

.user-group-card {
  border-radius: 8px;
  overflow: hidden;
}

.user-group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-details {
  display: flex;
  flex-direction: column;
}

.username {
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.user-stats {
  font-size: 12px;
  color: #7f8c8d;
  margin-top: 2px;
}

.user-progress {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 80px;
}

.progress-text {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
}

/* 紧凑图片信息样式 */
.image-info-compact {
  display: flex;
  align-items: center;
  gap: 8px;
}

.thumbnail-small {
  width: 40px;
  height: 30px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #ddd;
}

.image-details-compact {
  flex: 1;
  min-width: 0;
}

.filename-small {
  font-size: 12px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 2px;
}

.filename-id {
  font-size: 10px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 用户表格容器样式 */
.user-table-container {
  max-height: 400px;
  overflow: hidden;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  position: relative;
}

.user-table-container::-webkit-scrollbar {
  width: 6px;
}

.user-table-container::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.user-table-container::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.user-table-container::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 用户组卡片间距优化 */
.user-group-card {
  margin-bottom: 20px;
}

.user-group-card:last-child {
  margin-bottom: 0;
}

/* 表格内的滚动条优化 */
.user-table-container .el-table {
  max-height: 400px;
}

.user-table-container .el-table__body-wrapper {
  max-height: 350px;
  overflow-y: auto;
  overflow-x: hidden;
}

.user-table-container .el-table__body-wrapper::-webkit-scrollbar {
  width: 6px;
}

.user-table-container .el-table__body-wrapper::-webkit-scrollbar-track {
  background: #f5f7fa;
  border-radius: 3px;
}

.user-table-container .el-table__body-wrapper::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
  transition: background 0.3s;
}

.user-table-container .el-table__body-wrapper::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}

/* 确保表格头部始终可见 */
.user-table-container .el-table__header-wrapper {
  position: sticky;
  top: 0;
  z-index: 10;
  background: #fafafa;
  border-bottom: 1px solid #ebeef5;
}

/* 导航列表滚动条样式 */
.user-nav-list::-webkit-scrollbar {
  width: 4px;
}

.user-nav-list::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.user-nav-list::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 2px;
}

.user-nav-list::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 右侧内容区滚动条样式 */
.user-tables-content::-webkit-scrollbar {
  width: 8px;
}

.user-tables-content::-webkit-scrollbar-track {
  background: #f5f7fa;
  border-radius: 4px;
}

.user-tables-content::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 4px;
}

.user-tables-content::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}

/* 移动端优化 */
@media (max-width: 768px) {
  .user-groups-layout {
    flex-direction: column;
    height: auto;
    min-height: auto;
  }
  
  .user-navigation {
    width: 100%;
    max-height: 200px;
    margin-bottom: 20px;
  }
  
  .user-tables-content {
    overflow-y: visible;
  }
  
  .user-section {
    margin-bottom: 20px;
  }
  
  .user-table-wrapper {
    max-height: 400px;
  }
  
  .user-table-wrapper .el-table__body-wrapper {
    max-height: 350px;
  }
  
  .user-section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .user-progress-large {
    min-width: auto;
    width: 100%;
  }
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

.stat-icon.assigned {
  background: linear-gradient(135deg, #909399 0%, #606266 100%);
}

.stat-icon.in-progress {
  background: linear-gradient(135deg, #e6a23c 0%, #f56c6c 100%);
}

.stat-icon.completed {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.stat-icon.total {
  background: linear-gradient(135deg, #409eff 0%, #337ecc 100%);
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

.image-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.thumbnail {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #ddd;
}

.image-details {
  flex: 1;
  min-width: 0;
}

.filename {
  font-weight: 500;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.image-size {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

.no-annotation {
  color: #999;
  font-size: 12px;
}

.pagination-wrapper {
  margin-top: 20px;
  text-align: center;
}

.task-detail {
  padding: 20px 0;
}

.task-info h3,
.image-preview h3 {
  margin: 0 0 15px 0;
  color: #333;
}

.detail-image {
  width: 100%;
  max-width: 100%;
  height: auto;
  max-height: 300px;
  object-fit: contain;
  border: 1px solid #ddd;
  border-radius: 8px;
}

.progress-text {
  font-size: 10px;
  color: #666;
  text-align: center;
  margin-top: 2px;
}

.progress-cell {
  padding: 2px;
}

.date-text {
  font-size: 12px;
  color: #606266;
}

/* 表格容器样式 - 确保填充满容器 */
.table-container {
  width: 100%;
  overflow-x: auto;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.table-container .el-table {
  width: 100% !important;
  table-layout: auto !important;
  min-width: 1000px; /* 确保表格有最小宽度 */
}

.table-container .el-table__header-wrapper,
.table-container .el-table__body-wrapper {
  width: 100% !important;
}

.table-container .el-table__header,
.table-container .el-table__body {
  width: 100% !important;
  min-width: 1000px !important;
}

/* 在大屏幕上让表格完全填充 */
@media (min-width: 1400px) {
  .table-container .el-table {
    min-width: 100%;
  }
  
  .table-container .el-table__header,
  .table-container .el-table__body {
    min-width: 100% !important;
  }
}

/* 图片信息列样式优化 */
.image-info {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.image-details {
  flex: 1;
  min-width: 0;
}

.filename {
  font-size: 12px;
  color: #303133;
  font-weight: 500;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.image-size {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 操作按钮多行排列 */
.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: stretch;
  min-width: 120px;
}

.action-buttons .el-button {
  margin: 0 !important;
  padding: 4px 8px;
  font-size: 11px;
  white-space: nowrap;
  width: 100%;
  text-align: center;
}

.action-buttons .el-dropdown {
  width: 100%;
}

.action-buttons .el-dropdown .el-button {
  padding: 4px 8px;
  width: 100%;
  text-align: center;
}

/* 对话框操作按钮行排列 */
.dialog-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: flex-end;
}

.dialog-actions .el-button {
  margin: 0;
}

/* 操作列允许换行显示多行按钮 */
.el-table .cell {
  white-space: normal;
  overflow: visible;
  padding: 8px 4px;
}

.el-table td.el-table-fixed-column--right {
  padding: 8px 4px;
}

/* 统计卡片样式 */
.stat-card {
  border-radius: 12px;
  overflow: hidden;
  border: none;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  background: linear-gradient(145deg, #ffffff 0%, #f8fafc 100%);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12);
}

.stat-item {
  display: flex;
  align-items: center;
  padding: 16px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  font-size: 20px;
  color: white;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.stat-icon.assigned {
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
}

.stat-icon.in-progress {
  background: linear-gradient(135deg, #f59e0b, #d97706);
}

.stat-icon.completed {
  background: linear-gradient(135deg, #10b981, #059669);
}

.stat-icon.total {
  background: linear-gradient(135deg, #6366f1, #4338ca);
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1;
  margin-bottom: 6px;
  font-family: 'SF Pro Display', -apple-system, sans-serif;
}

.stat-label {
  font-size: 14px;
  color: #6b7280;
  font-weight: 500;
  letter-spacing: 0.25px;
}

/* 页面头部样式 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0;
}

.page-header h2 {
  margin: 0;
  color: #1f2937;
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #3b82f6, #10b981);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.annotation-task-management {
  padding: 16px;
  background: #f8fafc;
  min-height: 100vh;
}

.annotation-info {
  margin-top: 30px;
}

.annotation-info h3 {
  margin: 0 0 15px 0;
  color: #333;
}

.annotation-content {
  margin-top: 20px;
}

.annotation-content h4 {
  margin: 0 0 15px 0;
  color: #666;
}

.labels-display {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 15px;
}

.label-card {
  margin-bottom: 10px;
}

.label-category {
  font-weight: bold;
  color: #409eff;
}

.label-value {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

.no-annotation-info {
  margin-top: 30px;
  text-align: center;
}

.annotation-editor {
  padding: 20px 0;
}

.image-section h3,
.edit-form h3 {
  margin: 0 0 15px 0;
  color: #333;
}

.edit-image {
  width: 100%;
  max-width: 100%;
  height: auto;
  max-height: 400px;
  object-fit: contain;
  border: 1px solid #ddd;
  border-radius: 8px;
}

.label-categories {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.category-item {
  padding: 20px;
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.category-label {
  font-weight: bold;
  color: #333;
}

.required {
  color: #f56c6c;
  margin-left: 2px;
}

.category-type {
  font-size: 12px;
  color: #999;
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 3px;
}

.option-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.option-item {
  margin: 0;
}

.remark-section {
  margin-top: 20px;
}

.remark-item {
  border-color: #d1ecf1;
  background: #f8f9fa;
}

.image-preview-dialog {
  text-align: center;
}

.preview-image {
  width: 100%;
  max-width: 100%;
  height: auto;
  max-height: 500px;
  object-fit: contain;
  border: 1px solid #ddd;
  border-radius: 8px;
}

.image-info-detail {
  margin-top: 20px;
}

.danger {
  color: #f56c6c;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .annotation-task-management {
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
}
</style>
