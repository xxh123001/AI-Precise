<template>
  <div class="task-list">
    <div v-if="!currentTask" class="no-task-container">
      <el-empty description="暂无标注任务" :image-size="100" />
          </div>

    <div v-else class="task-container">
      <!-- 顶部操作栏 -->
      <div class="top-bar">
        <div class="task-info">
          <div v-if="isProjectMode && projectInfo" class="project-info">
            <el-button 
              @click="backToProjects" 
              size="small" 
              type="primary" 
              plain
              class="back-btn"
            >
              <el-icon><ArrowLeft /></el-icon>
              返回项目
            </el-button>
            <span class="project-name">{{ projectInfo.name }}</span>
          </div>
          <span class="task-counter">{{ currentTaskIndex + 1 }} / {{ taskIdList.length }}</span>
          <span class="user-info">{{ currentUser?.username || '未知' }}</span>
        </div>
        <div class="nav-controls">
          <el-button 
            size="small" 
            type="warning"
            @click="openTaskNavigation"
            class="nav-btn-top"
          >
            <el-icon><List /></el-icon>
                </el-button>
        </div>
              </div>
      
      <!-- 左右布局内容区域 -->
      <div class="content-wrapper">
        <!-- 左侧：图像显示区域 -->
        <div class="left-panel">
      <div class="image-section">
        <!-- 图像容器 -->
        <div 
          class="image-container"
          ref="imageContainerRef"
          :style="getImageContainerStyle()"
          @mousedown="handleMouseDown"
          @mousemove="handleMouseMove"
          @mouseup="handleMouseUp"
          @touchstart="handleTouchStart"
          @touchmove="handleTouchMove"
          @touchend="handleTouchEnd"
          @wheel="handleWheel"
          @click="toggleFullscreen"
        >
          <img 
            v-if="currentImageUrl"
            :src="currentImageUrl" 
            :alt="currentTask.image.originalName" 
            ref="imageRef"
            :style="getImageStyle()"
            @dragstart="$event.preventDefault()"
          />
          
        </div>
        
        <!-- 翻页导航区域 -->
        <div class="image-navigation">
          <button 
            @click.stop.prevent="navigateToTask('prev')"
            :disabled="!hasPrevTask"
            class="nav-button nav-prev"
            :class="{ 'disabled': !hasPrevTask }"
          >
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M15 18L9 12L15 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span>上一张</span>
          </button>
          
          <div class="nav-counter">
            <span class="current">{{ currentTaskIndex + 1 }}</span>
            <span class="separator">/</span>
            <span class="total">{{ taskIdList.length }}</span>
          </div>
          
          <button 
            @click.stop.prevent="resetImageView"
            class="nav-button nav-reset"
            title="重置视图"
          >
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <span>重置</span>
          </button>
          
          <button 
            @click.stop.prevent="navigateToTask('next')"
            :disabled="!hasNextTask"
            class="nav-button nav-next"
            :class="{ 'disabled': !hasNextTask }"
          >
            <span>下一张</span>
            <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M9 18L15 12L9 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
        
        <!-- 图片信息 -->
        <div class="image-info">
          <div v-if="isTaskCompleted" class="readonly-alert">
            <el-alert
              title="任务已完成 - 只读模式"
              type="success"
              :closable="false"
              show-icon
              size="small"
              style="margin-bottom: 8px;"
            >
              <template #default>
                此任务已标记为完成，所有标注内容为只读状态
              </template>
            </el-alert>
          </div>
          <div class="image-meta" style="max-height:20px">
            <span class="filename-display">{{ currentTask.image?.originalName || currentTask.image?.filename }}</span>
            <el-tag :type="getStatusType(currentTask.status)" size="small">
              {{ getStatusText(currentTask.status) }}
            </el-tag>
            
                </div>
                
              </div>
            </div>
        </div>
        
        <!-- 右侧：标注表单和操作区域 -->
        <div class="right-panel">
            <!-- 标注表单区域 -->
      <div class="annotation-section" >
              <!-- 已有标注提示 -->
     

        <!-- 标注分类 -->
        <div v-if="labelConfig?.categories" class="categories-container">
                <div 
                  v-for="category in labelConfig.categories" 
                  :key="category.id"
            class="category"
                  :class="getCategoryClasses(category)"
                >
                  <div class="category-header">
              <span class="category-name">
                      {{ category.name }}
                      <span v-if="category.required" class="required">*</span>
                      <el-tag 
                        v-if="currentTask?.annotationId && !isTaskCompleted && hasCategoryValue(category.id)" 
                        size="small" 
                        type="warning"
                        class="edited-tag"
                      >
                        编辑已有标注
                      </el-tag>
              </span>
              <el-tag size="small" class="category-type-tag">{{ getCategoryTypeText(category.type) }}</el-tag>
                  </div>

            <!-- 单选 -->
                  <el-radio-group 
                    v-if="category.type === 'single_choice'"
                    v-model="annotations[category.id]"
              class="options-container"
                    :disabled="isTaskCompleted"
                  >
                    <el-radio 
                      v-for="option in category.options"
                      :key="option.value"
                      :value="option.value"
                class="option"
                      @click.native="!isTaskCompleted && handleRadioClick(category.id, option.value, $event)"
                    >
                      {{ option.label }}
                    </el-radio>
                  </el-radio-group>

            <!-- 多选 -->
                  <el-checkbox-group 
                    v-else-if="category.type === 'multiple_choice'"
                    v-model="annotations[category.id]"
              class="options-container"
                    :disabled="isTaskCompleted"
                  >
                    <el-checkbox 
                      v-for="option in category.options"
                      :key="option.value"
                      :value="option.value"
                class="option"
                    >
                      {{ option.label }}
                    </el-checkbox>
                  </el-checkbox-group>

            <!-- 文本输入 -->
                  <el-input
                    v-else-if="category.type === 'text'"
                    v-model="annotations[category.id]"
                    type="textarea"
                    :placeholder="`请输入${category.name}`"
              :rows="2"
              size="small"
                    :disabled="isTaskCompleted"
                  />
              </div>

              </div>

              <div v-else class="no-config">
          <el-empty description="没有可用的标签配置" :image-size="60" />
              </div>
            </div>

      <!-- 底部操作栏 -->
      <div class="bottom-actions">
        <el-button 
          type="primary" 
          @click="handleSubmit"
          :loading="submitting"
          :disabled="isTaskCompleted"
          size="large"
          class="action-btn"
        >
          {{ currentTask?.annotationId ? '更新' : '提交' }}
        </el-button>
        <el-button 
          v-if="!isTaskCompleted"
          type="success" 
          @click="handleCompleteTask" 
          :loading="completing"
          size="large"
          class="action-btn"
        >
          任务完成
        </el-button>
        <el-button 
          @click="handleLogout" 
          size="large" 
          type="danger" 
          plain
          class="action-btn"
        >
          退出
        </el-button>
        </div>

      <!-- 联系信息栏 -->
      <div class="contact-info">
       
        <div class="contact-item beauty-badge">
          <el-icon><User /></el-icon>
          <span>YangLab</span>
          <span class="sparkle">✨</span>
        </div>
      </div>
        </div>
      </div>
    </div>

    <!-- 全屏图片查看器 -->
    <div v-if="isFullscreen" class="fullscreen-viewer" @click="toggleFullscreen">
      <div class="fullscreen-container">
        <div class="fullscreen-header">
          <div class="fullscreen-title">{{ currentTask?.image?.originalName }}</div>
          <div class="fullscreen-controls">
            <el-button 
              size="default" 
              @click.stop="navigateToTask('prev')" 
              :disabled="!hasPrevTask"
              title="上一张"
            >
              <el-icon><ArrowLeft /></el-icon>
            </el-button>
            <el-button 
              size="default" 
              @click.stop="navigateToTask('next')" 
              :disabled="!hasNextTask"
              title="下一张"
            >
              <el-icon><ArrowRight /></el-icon>
            </el-button>
            <el-button 
              size="default" 
              @click.stop="resetImageView"
              title="重置视图"
              v-if="isShowingHDImage"
            >
              <el-icon><Refresh /></el-icon>
            </el-button>
            <el-button 
              size="default" 
              @click.stop="toggleFullscreen"
              title="退出全屏"
            >
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
      </div>

        <div 
          class="fullscreen-image-container" 
          ref="fullscreenImageContainer"
          @click.stop
          @touchstart="handleTouchStart"
          @touchmove="handleTouchMove"
          @touchend="handleTouchEnd"
          @wheel="handleWheel"
        >
          <img 
            v-if="currentTask && currentFullscreenImageUrl"
            :src="currentFullscreenImageUrl" 
            :alt="currentTask.image.originalName"
            class="fullscreen-image"
            :style="{
              transform: isShowingHDImage 
                ? `translate(${imageTransform.x}px, ${imageTransform.y}px) scale(${imageTransform.scale}) rotate(-90deg)`
                : `translate(${imageTransform.x}px, ${imageTransform.y}px) scale(${imageTransform.scale})`
            }"
            @click="toggleFullscreen"
            @dragstart="$event.preventDefault()"
          />
          
          <!-- 指向质心的红色箭头（仅在显示高清图且处于初始状态时显示） -->
          <div class="centroid-arrow" v-if="currentTask && currentFullscreenImageUrl && isShowingHDImage && isInitialViewState">
            <svg width="100" height="100" viewBox="0 0 100 100">
              <defs>
                <!-- 定义箭头头部 -->
                <marker id="arrowhead" markerWidth="10" markerHeight="10" 
                        refX="9" refY="3" orient="auto">
                  <polygon points="0 0, 10 3, 0 6" fill="#ff0000" />
                </marker>
              </defs>
              <!-- 箭头线条，自动添加箭头头部 -->
              <line x1="10" y1="90" x2="85" y2="15" 
                    stroke="#ff0000" 
                    stroke-width="4" 
                    marker-end="url(#arrowhead)" />
            </svg>
            <div class="arrow-label">小图区域</div>
          </div>
        </div>
        
        <!-- 缩略图切换区域 -->
        <div class="thumbnail-switcher" @click.stop>
          <div class="thumbnail-container">
            <div 
              class="thumbnail-item"
              :class="{ active: isShowingHDImage }"
              @click="switchToHDImage"
            >
              <img :src="getHDImageUrl(currentTask)" alt="高清大图" />
              <span class="thumbnail-label">高清大图</span>
            </div>
            <div 
              class="thumbnail-item"
              :class="{ active: !isShowingHDImage }"
              @click="switchToOriginalImage"
            >
              <img :src="croppedOriginalImageUrl" alt="原图（裁剪后）" />
              <span class="thumbnail-label">原图</span>
            </div>
          </div>
        </div>
        
        <div class="fullscreen-footer">
          <div class="image-counter">
            {{ currentTaskIndex + 1 }} / {{ filteredTaskList.length }}
          </div>
          <div class="fullscreen-hint">
            📱 双指缩放图片，单指拖拽移动 | 💻 滚轮缩放 | 点击缩略图切换 | 点击图片或按 ESC 退出全屏
          </div>
        </div>
      </div>
    </div>

    <!-- 查看标注结果对话框 -->
    <el-dialog
      v-model="showAnnotationDialog"
      title="标注结果"
      width="80%"
      :before-close="handleCloseAnnotationDialog"
    >
      <div v-if="annotationResult" class="annotation-result">
        <!-- 图像信息 -->
        <div class="image-section">
          <div class="image-info">
            <h3>图像信息</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="文件名">{{ annotationResult.image.filename }}</el-descriptions-item>
              <el-descriptions-item label="原始名称">{{ annotationResult.image.originalName }}</el-descriptions-item>
              <el-descriptions-item label="文件大小">{{ formatFileSize(annotationResult.image.fileSize) }}</el-descriptions-item>
              <el-descriptions-item label="MIME类型">{{ annotationResult.image.mimeType }}</el-descriptions-item>
            </el-descriptions>
          </div>
          <div class="image-preview">
            <img :src="getTaskImageUrl(annotationResult)" :alt="annotationResult.image.originalName" />
          </div>
        </div>

        <!-- 标注信息 -->
        <div class="annotation-section">
          <h3>标注信息</h3>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="标注状态">
              <el-tag :type="getStatusType(annotationResult.status)">
                {{ getStatusText(annotationResult.status) }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatDate(annotationResult.createdAt) }}</el-descriptions-item>
            <el-descriptions-item label="更新时间">{{ formatDate(annotationResult.updatedAt) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 标注内容 -->
        <div class="labels-section">
          <h3>标注内容</h3>
          <div v-if="parsedLabels && Object.keys(parsedLabels).length > 0" class="labels-content">
            <el-card v-for="(value, key) in parsedLabels" :key="key" class="label-item">
              <template #header>
                <span class="label-category">{{ key }}</span>
              </template>
              <div class="label-value">
                <span v-if="Array.isArray(value)">{{ value.join(', ') }}</span>
                <span v-else>{{ value }}</span>
              </div>
            </el-card>
          </div>
          <el-empty v-else description="暂无标注内容" :image-size="80" />
        </div>
      </div>

      <template #footer>
        <el-button @click="handleCloseAnnotationDialog">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 任务导航弹窗 -->
    <el-dialog
      v-model="showTaskNavigation"
      width="80%"
      max-width="1000px"
      :before-close="() => showTaskNavigation = false"
    >
      <template #header>
        <div class="task-nav-dialog-header">
          <span class="dialog-title">任务快速导航</span>
          <span class="annotation-count-info">
            已标注：{{ completedTaskCount }} / {{ taskIdList.length }}
          </span>
        </div>
      </template>
      <div class="task-navigation-content">
        <div class="task-nav-header">
          <div class="task-nav-info">
            <span>共 {{ taskIdList.length }} 个任务</span>
            <span v-if="currentTask">当前：任务 {{ currentTask.id }}</span>
  </div>
          <div class="task-nav-controls">
            <el-input
              v-model="taskSearchKeyword"
              placeholder="搜索任务ID或文件名"
              style="width: 250px"
              clearable
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
        </div>

        <div v-loading="loadingTaskList" class="task-grid-container">
          <div class="task-grid">
            <div
              v-for="task in filteredTaskIdList"
              :key="task.id"
              class="task-item"
              :class="{
                'current': currentTask && currentTask.id === task.id,
                'completed': task.status === 'COMPLETED',
                'in-progress': task.status === 'IN_PROGRESS',
                'assigned': task.status === 'ASSIGNED'
              }"
              @click="jumpToTask(task.id)"
            >
              <div class="task-id">{{ task.id }}</div>
              <div class="task-status">
                <el-tag 
                  :type="getTaskStatusType(task.status)" 
                  size="small"
                >
                  {{ getTaskStatusText(task.status) }}
                </el-tag>
              </div>
              <div class="task-filename" :title="task.filename">
                {{ task.filename.length > 20 ? task.filename.substring(0, 20) + '...' : task.filename }}
              </div>
            </div>
          </div>
        </div>

        <div class="task-nav-footer">
          <el-button @click="showTaskNavigation = false">关闭</el-button>
        </div>
      </div>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  DocumentCopy, 
  Check, 
  ZoomIn, 
  ZoomOut, 
  Refresh,
  Edit,
  ArrowLeft,
  ArrowRight,
  Close,
  List,
  Search,
  ArrowDown,
  Switch,
  Phone,
  User
} from '@element-plus/icons-vue'
import { userTaskApi } from '@/api/userTask'
import { systemConfigApi } from '@/api/systemConfig'
import { useAuthStore } from '@/store/auth'

// 接收路由参数
const props = defineProps({
  projectId: String
})

const router = useRouter()
const loading = ref(false)
const authStore = useAuthStore()

// 全局裁剪配置
const globalCropConfig = ref({
  enabled: false,
  cropX: 0,
  cropY: 0,
  cropWidth: 909,
  cropHeight: 1080
})

// 项目相关状态
const projectInfo = ref(null)
const isProjectMode = computed(() => !!props.projectId)

// 当前用户信息
const currentUser = computed(() => authStore.user)

// 检查当前任务是否已完成（只读模式）
const isTaskCompleted = computed(() => {
  return currentTask.value?.isCompleted || false
})

// Tab切换
const activeTab = ref('incomplete')

// 当前选中的任务
const currentTask = ref(null)

// 全屏显示状态
const isFullscreen = ref(false)
const isShowingHDImage = ref(true) // 是否显示高清图，默认为true
const currentFullscreenImageUrl = ref('') // 当前全屏显示的图片URL
const croppedOriginalImageUrl = ref('') // 裁剪后的原图URL（用于缩略图显示）
const isInitialViewState = ref(true) // 是否处于初始查看状态（用于控制箭头显示）

// 标注相关状态
const saving = ref(false)
const submitting = ref(false)
const completing = ref(false) // 任务完成状态
const zoomLevel = ref(1)
const imageRef = ref()
const imageContainerRef = ref()
const fullscreenImageContainer = ref()
const labelConfig = ref(null)
const annotations = reactive({})

// 图像拖拽和平移状态
const imageTransform = reactive({
  x: 0,
  y: 0,
  scale: 1
})

const dragState = reactive({
  isDragging: false,
  startX: 0,
  startY: 0,
  startTransformX: 0,
  startTransformY: 0
})

// 双指缩放状态
const pinchState = reactive({
  isPinching: false,
  initialDistance: 0,
  initialScale: 1,
  initialCenterX: 0,
  initialCenterY: 0
})

// 触摸点击检测状态
const tapState = reactive({
  startX: 0,
  startY: 0,
  startTime: 0,
  moved: false
})

// 查看标注结果对话框
const showAnnotationDialog = ref(false)
const annotationResult = ref(null)
const viewLoading = ref(false)

const pagination = reactive({
  page: 1,
  size: 10, // 每次加载10个任务
  total: 0,
  hasMore: true // 是否还有更多数据
})

const taskList = ref([])
const loadingMore = ref(false)

// 任务导航相关
const taskIdList = ref([]) // 所有任务ID列表
const showTaskNavigation = ref(false) // 是否显示任务导航
const loadingTaskList = ref(false) // 加载任务列表状态
const taskSearchKeyword = ref('') // 任务搜索关键词

// 预加载配置
const PRELOAD_THRESHOLD = 3 // 剩余3个任务时开始预加载下一页
let IMAGE_PRELOAD_COUNT = 3 // 预加载后续3张图片（动态调整）

// 图片预加载缓存
const imageCache = new Map() // 存储已加载的图片
const preloadingImages = new Set() // 正在预加载的图片URL
const imageLoadTimes = [] // 记录图片加载时间，用于网络质量评估
let preloadDebounceTimer = null // 预加载防抖计时器

// 裁切后的图片缓存
const croppedImageCache = new Map() // 存储裁切后的图片 Data URL

const stats = computed(() => {
  return {
    assigned: taskList.value.filter(task => task.status === 'ASSIGNED').length,
    inProgress: taskList.value.filter(task => task.status === 'IN_PROGRESS').length,
    completed: taskList.value.filter(task => task.status === 'COMPLETED').length
  }
})

// 已完成任务数量（用于任务导航标题显示）
const completedTaskCount = computed(() => {
  return taskIdList.value.filter(task => task.status === 'COMPLETED').length
})

// 显示所有任务，不再过滤已标注的任务
const filteredTaskList = computed(() => {
  // 返回所有任务，让用户可以在所有任务间导航
  return taskList.value
})

// 当前任务索引（基于过滤后的列表）
const currentTaskIndex = computed(() => {
  if (!currentTask.value) return -1
  return filteredTaskList.value.findIndex(task => task.id === currentTask.value.id)
})

// 当前任务在未完成任务列表中的索引
const currentIncompleteTaskIndex = computed(() => {
  if (!currentTask.value) return -1
  const incompleteTasks = taskList.value.filter(task => 
    task.status === 'ASSIGNED' || task.status === 'IN_PROGRESS'
  )
  const index = incompleteTasks.findIndex(task => task.id === currentTask.value.id)
  
  // 调试信息（简化）
  if (index === -1 && currentTask.value) {
    console.log('索引计算警告 - 当前任务不在未完成列表中:', {
      taskId: currentTask.value.id,
      status: currentTask.value.status,
      incompleteCount: incompleteTasks.length
    })
  }
  
  return index
})

// 是否有上一张（基于所有任务）
const hasPrevTask = computed(() => {
  return currentTaskIndex.value > 0
})

// 是否有下一张（基于所有任务）
const hasNextTask = computed(() => {
  const hasNext = currentTaskIndex.value >= 0 && currentTaskIndex.value < filteredTaskList.value.length - 1
  
  // 调试信息（简化）
  if (!hasNext && currentTaskIndex.value >= 0) {
    console.log('导航提示 - 已达到最后一张:', {
      currentIndex: currentTaskIndex.value,
      totalTasks: filteredTaskList.value.length,
      hasMore: pagination.hasMore
    })
  }
  
  return hasNext
})

// 是否可以提交 - 检查所有必填项是否已完成
const canSubmit = computed(() => {
  if (!labelConfig.value?.categories) return false
  
  const requiredCategories = labelConfig.value.categories.filter(cat => cat.required)
  if (requiredCategories.length === 0) return true
  
  return requiredCategories.every(cat => {
    const value = annotations[cat.id]
    if (cat.type === 'multiple_choice') {
      return Array.isArray(value) && value.length > 0
    }
    return value && value.trim() !== ''
  })
})

// 获取未填写的必选项列表
const getMissingRequiredFields = () => {
  if (!labelConfig.value?.categories) return []
  
  return labelConfig.value.categories
    .filter(cat => cat.required)
    .filter(cat => {
      const value = annotations[cat.id]
      if (cat.type === 'multiple_choice') {
        return !Array.isArray(value) || value.length === 0
      }
      return !value || value.trim() === ''
    })
    .map(cat => `• ${cat.name}`)
}

// 解析标注内容
const parsedLabels = computed(() => {
  if (!annotationResult.value || !annotationResult.value.labels) {
    return null
  }
  
  try {
    const labelsStr = annotationResult.value.labels
    if (typeof labelsStr === 'string') {
      return JSON.parse(labelsStr)
    }
    return labelsStr
  } catch (error) {
    console.error('解析标注内容失败:', error)
    return null
  }
})

// 过滤后的任务ID列表
const filteredTaskIdList = computed(() => {
  if (!taskSearchKeyword.value.trim()) {
    return taskIdList.value
  }
  
  const keyword = taskSearchKeyword.value.toLowerCase()
  return taskIdList.value.filter(task => 
    task.id.toString().includes(keyword) ||
    task.filename.toLowerCase().includes(keyword)
  )
})

// 获取任务列表（支持分页）
const fetchTaskList = async (page = 1, append = false) => {
  const isLoadingMore = append
  try {
    if (isLoadingMore) {
      loadingMore.value = true
    } else {
    loading.value = true
    }
    
    console.log(`🔄 正在${isLoadingMore ? '加载更多' : '初始加载'}任务，第${page}页，每页${pagination.size}个任务...`)
    
    // 添加超时控制，避免长时间等待
    const params = {
      page: page,
      size: pagination.size
    }
    console.log(`📡 发送API请求:`, params)
    
    const response = await Promise.race([
      userTaskApi.getAssignments(params),
      new Promise((_, reject) => 
        setTimeout(() => reject(new Error('请求超时')), 30000) // 缩短超时时间到30秒
      )
    ])
    
    if (response.success) {
      const receivedTasks = response.data || []
      console.log(`✅ 第${page}页API响应成功: 收到 ${receivedTasks.length} 个任务`)
      console.log(`📋 任务数据概览:`, receivedTasks.map(t => ({ 
        id: t.id, 
        status: t.status,
        imageId: t.image?.id,
        imageName: t.image?.filename?.substring(0, 30) + '...'
      })))
      
      if (append) {
        // 追加到现有列表
        taskList.value.push(...receivedTasks)
        console.log('🔗 追加任务后总数:', taskList.value.length)
        
        // 检查是否还有更多数据
        pagination.hasMore = receivedTasks.length === pagination.size
        console.log(`📊 分页状态: hasMore=${pagination.hasMore} (收到${receivedTasks.length}个，期望${pagination.size}个)`)
      } else {
        // 初始加载，替换整个列表
        taskList.value = receivedTasks
        pagination.hasMore = receivedTasks.length === pagination.size
        console.log(`🎯 初始加载完成: hasMore=${pagination.hasMore} (收到${receivedTasks.length}个，期望${pagination.size}个)`)
      }
      
      pagination.page = page
      pagination.total = taskList.value.length // 当前已加载的任务数
      
      // 最终状态汇总
      const incompleteTasks = taskList.value.filter(task => task.status === 'ASSIGNED' || task.status === 'IN_PROGRESS')
      const completedTasks = taskList.value.filter(task => task.status === 'COMPLETED')
      
      console.log(`📈 分页加载汇总:`, {
        currentPage: pagination.page,
        totalLoaded: taskList.value.length,
        hasMore: pagination.hasMore,
        incomplete: incompleteTasks.length,
        completed: completedTasks.length
      })
      
    } else {
      ElMessage.error('获取任务列表失败: ' + (response.message || '未知错误'))
      if (!append) {
      taskList.value = []
      }
    }
  } catch (error) {
    console.error('获取任务列表失败:', error)
    
    // 检查是否是鉴权错误，如果是则由axios拦截器处理跳转
    // 这里只处理其他类型的错误
    if (error.response?.status === 401 || error.response?.status === 403) {
      // 鉴权错误，不显示额外消息，让拦截器处理
      console.log('🔒 鉴权错误，将由拦截器处理跳转')
    } else if (error.message === '请求超时') {
      ElMessage.error('网络请求超时，请刷新页面重试')
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络连接')
    } else {
      ElMessage.error('获取任务列表失败，请稍后重试')
    }
    
    if (!append) {
    taskList.value = []
    }
  } finally {
    if (isLoadingMore) {
      loadingMore.value = false
    } else {
    loading.value = false
    }
  }
}

// 加载下一页任务
const loadMoreTasks = async () => {
  if (loadingMore.value || !pagination.hasMore) {
    console.log('跳过加载更多：', { loadingMore: loadingMore.value, hasMore: pagination.hasMore })
    return
  }
  
  console.log('开始加载下一页任务...')
  await fetchTaskList(pagination.page + 1, true)
}

// 图片预加载机制
const preloadImage = (url) => {
  return new Promise((resolve, reject) => {
    // 如果已经在缓存中，直接返回
    if (imageCache.has(url)) {
      console.log('🎯 图片已在缓存中:', url.split('/').pop())
      resolve(imageCache.get(url))
      return
    }
    
    // 如果正在预加载中，避免重复请求
    if (preloadingImages.has(url)) {
      console.log('⏳ 图片正在预加载中:', url.split('/').pop())
      resolve(null)
      return
    }
    
    preloadingImages.add(url)
    const startTime = performance.now()
    
    const img = new Image()
    
    img.onload = () => {
      const loadTime = performance.now() - startTime
      const fileName = url.split('/').pop()
      
      console.log(`✅ 图片预加载成功: ${fileName} (${loadTime.toFixed(0)}ms)`)
      
      // 记录加载时间用于网络质量评估
      imageLoadTimes.push(loadTime)
      if (imageLoadTimes.length > 10) {
        imageLoadTimes.shift() // 只保留最近10次的记录
      }
      
      imageCache.set(url, img)
      preloadingImages.delete(url)
      
      // 根据加载时间调整预加载策略
      adjustPreloadStrategy()
      
      resolve(img)
    }
    
    img.onerror = (error) => {
      const loadTime = performance.now() - startTime
      console.warn(`❌ 图片预加载失败: ${url.split('/').pop()} (${loadTime.toFixed(0)}ms)`, error)
      preloadingImages.delete(url)
      reject(error)
    }
    
    img.src = url
  })
}

// 根据网络质量自适应调整预加载策略
const adjustPreloadStrategy = () => {
  if (imageLoadTimes.length < 3) return // 至少需要3个样本
  
  const avgLoadTime = imageLoadTimes.reduce((sum, time) => sum + time, 0) / imageLoadTimes.length
  
  let newPreloadCount = 3 // 默认值
  
  if (avgLoadTime < 500) {
    // 网络很快，预加载5张图片
    newPreloadCount = 5
    console.log('🚀 网络状况良好，增加预加载数量到5张')
  } else if (avgLoadTime < 1500) {
    // 网络一般，预加载3张图片  
    newPreloadCount = 3
    console.log('📡 网络状况一般，保持预加载3张图片')
  } else {
    // 网络较慢，只预加载2张图片
    newPreloadCount = 2
    console.log('🐌 网络状况较慢，减少预加载到2张图片')
  }
  
  if (IMAGE_PRELOAD_COUNT !== newPreloadCount) {
    IMAGE_PRELOAD_COUNT = newPreloadCount
    console.log(`⚙️ 预加载策略已调整为: ${IMAGE_PRELOAD_COUNT}张图片 (平均加载时间: ${avgLoadTime.toFixed(0)}ms)`)
  }
}

// 预加载接下来的几张图片
const preloadNextImages = async () => {
  const incompleteTasks = taskList.value.filter(task => 
    task.status === 'ASSIGNED' || task.status === 'IN_PROGRESS'
  )
  
  const currentIndex = currentIncompleteTaskIndex.value
  if (currentIndex === -1) return
  
  console.log(`🚀 开始预加载图片，当前索引: ${currentIndex}，总未完成任务: ${incompleteTasks.length}`)
  
  // 预加载接下来的几张图片
  const preloadPromises = []
  for (let i = 1; i <= IMAGE_PRELOAD_COUNT; i++) {
    const nextIndex = currentIndex + i
    if (nextIndex < incompleteTasks.length) {
      const nextTask = incompleteTasks[nextIndex]
      if (nextTask?.image?.url) {
        const fullUrl = nextTask.image.url.startsWith('http') 
          ? nextTask.image.url 
          : `http://62.234.94.65:8080${nextTask.image.url}`
        
        console.log(`📥 预加载第${i}张后续图片:`, nextTask.image.filename)
        preloadPromises.push(preloadImage(fullUrl))
      }
    }
  }
  
  // 并行预加载，不阻塞主流程
  if (preloadPromises.length > 0) {
    Promise.allSettled(preloadPromises).then(results => {
      const successful = results.filter(r => r.status === 'fulfilled').length
      const failed = results.filter(r => r.status === 'rejected').length
      console.log(`📊 预加载完成: 成功${successful}张，失败${failed}张`)
    })
  }
}

// 获取当前图片的完整URL
const getCurrentImageUrl = () => {
  if (!currentTask.value?.image?.url) return null
  
  const url = currentTask.value.image.url
  return url.startsWith('http') ? url : `http://62.234.94.65:8080${url}`
}

// 加载已有标注数据
const loadExistingAnnotation = async (imageId) => {
  if (!imageId) {
    console.warn('⚠️ imageId为空，无法加载已有标注')
    return
  }
  
  console.log('🔍 查找图片已有标注数据，imageId:', imageId)
  
  try {
    // 获取用户对该图片的已有标注
    const response = await userTaskApi.getImageAnnotation(imageId)
    
    if (response.success && response.data) {
      console.log('✅ 找到已有标注数据:', response.data)
      
      // 解析已有的标注数据
      const existingLabels = typeof response.data.labels === 'string' 
        ? JSON.parse(response.data.labels)
        : response.data.labels
      
      console.log('📋 解析的标注数据:', existingLabels)
      
      // 将已有数据填充到标注表单中
      if (existingLabels && typeof existingLabels === 'object') {
        console.log('🔍 准备填充数据，当前annotations状态:', Object.keys(annotations))
        console.log('🔍 需要填充的数据:', existingLabels)
        
        Object.keys(existingLabels).forEach(categoryId => {
          if (annotations.hasOwnProperty(categoryId)) {
            annotations[categoryId] = existingLabels[categoryId]
            console.log(`✅ 成功恢复标注 ${categoryId}:`, annotations[categoryId])
          } else {
            console.warn(`⚠️ 跳过未知的category: ${categoryId}，当前可用的categories:`, Object.keys(annotations))
          }
        })
        
        console.log('📊 最终annotations状态:', annotations)
      } else {
        console.warn('⚠️ 无有效的标注数据:', existingLabels)
      }
      
      
      // 保存原始标注ID，用于后续更新
      currentTask.value.annotationId = response.data.id
      currentTask.value.annotationStatus = response.data.status
      
      console.log('💾 已有标注数据已加载并填充到表单')
      
    } else {
      console.log('ℹ️ 该图片暂无已有标注数据')
      // 重置标注ID和备注
      if (currentTask.value) {
        currentTask.value.annotationId = null
        currentTask.value.annotationStatus = null
      }
    }
    
  } catch (error) {
    console.warn('⚠️ 加载已有标注数据失败:', error.message)
    // 失败时也重置标注ID和备注
    if (currentTask.value) {
      currentTask.value.annotationId = null
      currentTask.value.annotationStatus = null
    }
    annotationRemark.value = '' // 重置备注
  }
}

// 清理过期的图片缓存
const cleanupImageCache = () => {
  const MAX_CACHE_SIZE = 20 // 最大缓存20张图片
  
  if (imageCache.size > MAX_CACHE_SIZE) {
    console.log('🧹 清理图片缓存，当前大小:', imageCache.size)
    const entries = Array.from(imageCache.entries())
    const entriesToRemove = entries.slice(0, entries.length - MAX_CACHE_SIZE + 5) // 保留15张
    
    entriesToRemove.forEach(([url, img]) => {
      imageCache.delete(url)
      img.src = '' // 释放内存
    })
    
    console.log('✅ 缓存清理完成，剩余:', imageCache.size)
  }
}

// 选择任务（在新布局中直接显示标注面板） - 优化版：立即切换图片
const selectTask = async (task) => {
  console.log('🎯 selectTask called with task:', { id: task.id, imageId: task.image?.id })
  
  // 立即切换当前任务，不等待任何加载过程
  currentTask.value = task
  
  // 立即重置UI状态，确保界面响应迅速
  Object.keys(annotations).forEach(key => {
    delete annotations[key]
  })
  
  
  // 立即重置图像变换状态
  imageTransform.scale = 1
  imageTransform.x = 0
  imageTransform.y = 0
  
  // 立即重置拖拽状态
  dragState.isDragging = false
  
  // 重置双指缩放状态
  pinchState.isPinching = false
  
  // 标记为初始状态，显示箭头
  isInitialViewState.value = true
  
  console.log('⚡ 图片切换完成，开始后台加载任务配置...')
  
  // 加载并裁切图片（高优先级）
  const originalUrl = getTaskImageUrl(task)
  if (originalUrl) {
    getCroppedImageUrl(originalUrl).then(croppedUrl => {
      currentImageUrl.value = croppedUrl
      console.log('✅ 图片裁切完成')
    }).catch(error => {
      console.error('❌ 图片裁切失败，使用原始图片:', error)
      currentImageUrl.value = originalUrl
    })
  }
  
  // 后台异步加载标注配置和已有标注（不阻塞UI）
  loadTaskAnnotation(task).then(() => {
    console.log('✅ Task annotation loading completed in background')
  }).catch(error => {
    console.error('❌ Background task annotation loading failed:', error)
  })
  
  // 低优先级的图片预加载（更长的延迟，确保不影响当前图片显示）
  if (preloadDebounceTimer) {
    clearTimeout(preloadDebounceTimer)
  }
  
  preloadDebounceTimer = setTimeout(() => {
    console.log('🖼️ 开始后台预加载后续图片...')
    preloadNextImages()
    cleanupImageCache() // 定期清理缓存
  }, 1000) // 延迟1000ms，优先保证当前图片显示流畅
}

// 加载任务的标注配置和已有标注
const loadTaskAnnotation = async (task) => {
  if (!task?.image?.id) {
    console.warn('⚠️ 任务或图像ID无效:', task)
    return
  }

  console.log('📡 检查任务数据中的labelConfig:', !!task.image?.labelConfig)

  try {
    // 如果任务数据中已有完整的labelConfig，直接使用
    if (task.image?.labelConfig?.config) {
      console.log('✅ 使用任务数据中的labelConfig，跳过API调用')
      const configObj = typeof task.image.labelConfig.config === 'string' 
        ? JSON.parse(task.image.labelConfig.config)
        : task.image.labelConfig.config
      
      labelConfig.value = configObj
      console.log('📋 标签配置已设置:', labelConfig.value)
      
      // 初始化标注对象（关键修复：确保两个路径都初始化annotations）
      if (labelConfig.value?.categories) {
        labelConfig.value.categories.forEach(category => {
          if (category.type === 'multiple_choice') {
            annotations[category.id] = []
          } else {
            annotations[category.id] = ''
          }
        })
        console.log('📝 标注对象已初始化:', Object.keys(annotations))
      }
      
      // 加载已有标注数据
      await loadExistingAnnotation(task.image.id)
      return
    }

    console.log('📡 任务数据中无完整labelConfig，发起API请求, imageId:', task.image.id)

    // 获取图像的标注配置 - 设置较短的超时时间
    const response = await Promise.race([
      userTaskApi.getImageDetail(task.image.id),
      new Promise((_, reject) => 
        setTimeout(() => reject(new Error('请求超时')), 300000)
      )
    ])
    
    console.log('📦 图像详情API响应:', response.success)
    
    if (response.success) {
      const imageData = response.data
      
      // 解析标签配置
      if (imageData?.labelConfig?.config) {
        const configObj = typeof imageData.labelConfig.config === 'string' 
          ? JSON.parse(imageData.labelConfig.config)
          : imageData.labelConfig.config

        labelConfig.value = configObj
        
        // 初始化标注对象
        if (labelConfig.value?.categories) {
          labelConfig.value.categories.forEach(category => {
            if (category.type === 'multiple_choice') {
              annotations[category.id] = []
            } else {
              annotations[category.id] = ''
            }
          })
        }
        
        // 加载已有标注数据
        await loadExistingAnnotation(task.image.id)
        
      } else {
        console.warn('该图像没有配置标签')
        labelConfig.value = null
      }
      
      // 移除旧的注释，现在统一使用loadExistingAnnotation方法
    } else {
      console.error('获取图像详情失败:', response.message)
      ElMessage.warning('获取图像配置失败，但您仍可以查看图像')
      labelConfig.value = null
    }
  } catch (error) {
    console.error('加载任务标注失败:', error)
    if (error.message === '请求超时') {
      ElMessage.error('网络请求超时，请稍后重试')
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络连接')
    } else {
      ElMessage.warning('加载标注配置失败，但您仍可以查看图像')
    }
    // 即使加载失败也要清空标签配置，允许用户查看图像
    labelConfig.value = null
  }
}

const handleStartAnnotation = (task) => {
  if (task.status === 'COMPLETED') {
    // 查看标注结果
    handleViewAnnotation(task)
    return
  }
  
  // 在新布局中直接选择任务
  selectTask(task)
}

const handleViewAnnotation = async (task) => {
  try {
    viewLoading.value = true
    const response = await userTaskApi.getImageAnnotation(task.image.id)
    
    if (response.success) {
      annotationResult.value = response.data
      showAnnotationDialog.value = true
    } else {
      ElMessage.error(response.message || '获取标注结果失败')
    }
  } catch (error) {
    console.error('获取标注结果失败:', error)
    ElMessage.error('获取标注结果失败')
  } finally {
    viewLoading.value = false
  }
}

const handleCloseAnnotationDialog = () => {
  showAnnotationDialog.value = false
  annotationResult.value = null
}

const handleTabClick = (tab) => {
  // Tab切换时不需要重新获取数据，只需要过滤显示
  console.log('切换到tab:', tab.props.name)
}

const handleRefresh = () => {
  fetchTaskList()
}

// 移除分页处理方法，使用导航方式

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

const getActionText = (status) => {
  const textMap = {
    'ASSIGNED': '开始标注',
    'IN_PROGRESS': '继续标注',
    'COMPLETED': '查看结果'
  }
  return textMap[status] || '开始标注'
}

const getEmptyDescription = () => {
  if (activeTab.value === 'completed') {
    return '暂无已完成的标注任务'
  } else {
    return '暂无未完成的标注任务'
  }
}

const formatDate = (dateStr) => {
  return dateStr ? new Date(dateStr).toLocaleString() : ''
}

const formatFileSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 获取全局裁剪配置
const loadGlobalCropConfig = async () => {
  try {
    const response = await systemConfigApi.getUserGlobalCropConfig()
    if (response.success && response.data) {
      globalCropConfig.value = {
        enabled: response.data.enabled || false,
        cropX: response.data.cropX || 0,
        cropY: response.data.cropY || 0,
        cropWidth: response.data.cropWidth || 909,
        cropHeight: response.data.cropHeight || 1080
      }
      console.log('✅ 全局裁剪配置已加载:', globalCropConfig.value)
    }
  } catch (error) {
    console.error('获取全局裁剪配置失败:', error)
    // 静默失败，使用默认配置
  }
}

// 使用 Canvas 裁切图片
const cropImageWithCanvas = async (imageUrl, cropConfig) => {
  return new Promise((resolve, reject) => {
    const img = new Image()
    
    // 只有跨域时才设置 crossOrigin
    // 判断是否跨域
    const urlObj = new URL(imageUrl, window.location.href)
    const isCrossOrigin = urlObj.origin !== window.location.origin
    
    if (isCrossOrigin) {
      img.crossOrigin = 'anonymous'
      console.log('🌐 检测到跨域图片，设置 crossOrigin:', imageUrl)
    }
    
    img.onload = () => {
      try {
        const canvas = document.createElement('canvas')
        const ctx = canvas.getContext('2d')
        
        const { cropX, cropY, cropWidth, cropHeight } = cropConfig
        
        // 设置 canvas 尺寸为裁切后的尺寸
        canvas.width = cropWidth
        canvas.height = cropHeight
        
        // 绘制裁切后的图片
        ctx.drawImage(
          img,
          cropX, cropY, cropWidth, cropHeight, // 源图片的裁切区域
          0, 0, cropWidth, cropHeight // 目标 canvas 的绘制区域
        )
        
        // 转换为 Data URL
        const croppedDataUrl = canvas.toDataURL('image/jpeg', 0.95)
        
        console.log('✅ 图片裁切成功:', {
          original: `${img.width}x${img.height}`,
          cropped: `${cropWidth}x${cropHeight}`,
          dataUrlSize: `${(croppedDataUrl.length / 1024).toFixed(1)}KB`
        })
        
        resolve(croppedDataUrl)
      } catch (error) {
        console.error('❌ Canvas 裁切失败（可能是 CORS 污染）:', error)
        reject(error)
      }
    }
    
    img.onerror = (error) => {
      console.error('❌ 图片加载失败:', error)
      reject(error)
    }
    
    img.src = imageUrl
  })
}

// 获取裁切后的图片 URL（带缓存）
const getCroppedImageUrl = async (originalUrl) => {
  // 如果没有启用裁切，直接返回原始 URL
  if (!globalCropConfig.value.enabled) {
    return originalUrl
  }
  
  // 检查缓存
  const cacheKey = `${originalUrl}_${globalCropConfig.value.cropX}_${globalCropConfig.value.cropY}_${globalCropConfig.value.cropWidth}_${globalCropConfig.value.cropHeight}`
  
  if (croppedImageCache.has(cacheKey)) {
    console.log('🎯 使用裁切缓存:', originalUrl.split('/').pop())
    return croppedImageCache.get(cacheKey)
  }
  
  try {
    console.log('✂️ 开始裁切图片:', originalUrl.split('/').pop())
    const croppedUrl = await cropImageWithCanvas(originalUrl, globalCropConfig.value)
    
    // 存入缓存
    croppedImageCache.set(cacheKey, croppedUrl)
    
    return croppedUrl
  } catch (error) {
    console.error('裁切图片失败，使用原始图片:', error)
    return originalUrl // 失败时返回原始图片
  }
}

// 当前显示的图片 URL（裁切后的）
const currentImageUrl = ref('')

// 计算图片样式（仅缩放和位移）
const getImageStyle = () => {
  // 基础样式：缩放和位移
  return {
    transform: `translate(${imageTransform.x}px, ${imageTransform.y}px) scale(${imageTransform.scale})`
  }
}

// 计算图片容器样式（根据裁切后的尺寸设置宽高比）
const getImageContainerStyle = () => {
  if (!globalCropConfig.value.enabled) {
    return {}
  }
  
  const { cropWidth, cropHeight } = globalCropConfig.value
  
  // 设置容器高度为裁切后的比例
  return {
    aspectRatio: `${cropWidth} / ${cropHeight}`
  }
}

// 获取任务图像URL
const getTaskImageUrl = (task) => {
  if (!task?.image) return ''
  
  console.log('🖼️ [TaskList] 获取任务图像URL')
  console.log('任务图像数据:', task.image)
  
  // 优先使用后端返回的 url 字段
  if (task.image.url) {
    const url = task.image.url.startsWith('http') ? task.image.url : `${window.location.origin}${task.image.url}`
    console.log('✓ 使用后端返回的URL:', url)
    return url
  }
  
  // 兼容：如果没有 url 字段，使用旧的拼接方式
  if (task.image.filename) {
    const url = `${window.location.origin}/uploads/${task.image.filename}`
    console.warn('⚠️ 使用兼容模式拼接URL:', url)
    return url
  }
  
  console.error('❌ 无法获取任务图像URL')
  return ''
}

// 获取高清图片URL
const getHDImageUrl = (task) => {
  if (!task?.image) return ''
  
  // 从原始文件名中提取部分，构造高清图URL
  // 格式: K2023-1409_8c3f358b0efe45d5a621f2f7dad05434.jpg
  // 需要拆分成: K2023-1409 和 8c3f358b0efe45d5a621f2f7dad05434
  const originalName = task.image.originalName || task.image.filename
  if (!originalName) return ''
  
  // 移除文件扩展名
  const nameWithoutExt = originalName.replace(/\.(jpg|jpeg|png|gif)$/i, '')
  
  // 按下划线分割
  const parts = nameWithoutExt.split('_')
  if (parts.length < 2) return getTaskImageUrl(task) // 如果格式不对，返回原图
  
  const folder = parts[0] // K2023-1409
  const filePrefix = parts.slice(1).join('_') // 8c3f358b0efe45d5a621f2f7dad05434
  
  // 构造高清图URL
  const hdUrl = `https://cos.linfelix.cn/annotation_edge_image/${folder}/${filePrefix}_8192.jpg`
  console.log('🖼️ 构造高清图URL:', hdUrl)
  
  return hdUrl
}

// 切换到高清图
const switchToHDImage = () => {
  if (!currentTask.value) return
  
  isShowingHDImage.value = true
  currentFullscreenImageUrl.value = getHDImageUrl(currentTask.value)
  
  // 重置缩放和位置，并设置初始放大
  imageTransform.scale = 1.5 // 初始放大1.5倍
  imageTransform.x = 0
  imageTransform.y = 0
  
  // 切换回高清图时，恢复初始状态，显示箭头
  isInitialViewState.value = true
  
  console.log('✅ 切换到高清图')
}

// 切换到原图（裁剪后的）
const switchToOriginalImage = () => {
  if (!currentTask.value) return
  
  isShowingHDImage.value = false
  
  // 使用已经裁剪好的原图URL
  currentFullscreenImageUrl.value = croppedOriginalImageUrl.value
  
  // 重置缩放和位置
  imageTransform.scale = 1
  imageTransform.x = 0
  imageTransform.y = 0
  
  console.log('✅ 切换到原图（裁剪后）')
}

// 标注相关方法
const handleSaveDraft = async () => {
  if (!currentTask.value) return
  
  try {
    saving.value = true
    
    const annotationData = {
      imageId: currentTask.value.image.id,
      labels: annotations,
      status: 'DRAFT',
      remark: null
    }
    
    // 添加超时控制
    const response = await Promise.race([
      userTaskApi.saveAnnotation(annotationData),
      new Promise((_, reject) => 
        setTimeout(() => reject(new Error('保存超时')), 300000)
      )
    ])
    
    if (response.success) {
      ElMessage.success('草稿保存成功')
      // 更新任务状态
      if (currentTask.value.status !== 'IN_PROGRESS') {
        currentTask.value.status = 'IN_PROGRESS'
        
        // 同步更新taskIdList中对应任务的状态
        const taskIdIndex = taskIdList.value.findIndex(task => task.id === currentTask.value.id)
        if (taskIdIndex !== -1) {
          taskIdList.value[taskIdIndex].status = 'IN_PROGRESS'
        }
      }
    } else {
      ElMessage.error('保存失败: ' + (response.message || '未知错误'))
    }
  } catch (error) {
    console.error('保存草稿失败:', error)
    if (error.message === '保存超时') {
      ElMessage.error('保存超时，请检查网络连接')
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后重试')
    } else {
      ElMessage.error('保存失败，请稍后重试')
    }
  } finally {
    saving.value = false
  }
}

const handleSubmit = async () => {
  console.log('🚀 提交按钮被点击了！')
  console.log('当前任务:', currentTask.value)
  console.log('canSubmit:', canSubmit.value)
  console.log('当前标注数据:', annotations)
  
  if (!currentTask.value) {
    console.warn('❌ 没有当前任务')
    return
  }
  
  // 验证必填项
  if (!canSubmit.value) {
    console.warn('⚠️ 必填项未完成')
    
    // 获取未填写的必选项列表
    const missingFields = getMissingRequiredFields()
    
    // 使用更明显的对话框提示（使用HTML换行）
    ElMessageBox.alert(
      `<div style="text-align: left; line-height: 1.8;">
        <p style="margin-bottom: 10px; font-weight: 600;">请完成以下必填项的标注：</p>
        ${missingFields.map(field => `<p style="margin: 5px 0;">${field}</p>`).join('')}
      </div>`,
      '标注未完成',
      {
        confirmButtonText: '我知道了',
        type: 'warning',
        center: true,
        dangerouslyUseHTMLString: true
      }
    )
    return
  }
  
  try {
    const isUpdate = currentTask.value?.annotationId
    console.log('📝 准备提交，是否更新:', isUpdate)
    await ElMessageBox.confirm(
      isUpdate 
        ? '确认更新标注结果吗？' 
        : '确认提交标注结果吗？',
      isUpdate ? '更新确认' : '提交确认',
      {
        confirmButtonText: isUpdate ? '确定更新' : '确定提交',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    submitting.value = true
    
    const annotationData = {
      imageId: currentTask.value.image.id,
      labels: annotations,
      status: 'COMPLETED',
      remark: null
    }
    
    // 在更新状态前先找到下一个未完成的任务
    const incompleteTasksBeforeUpdate = taskList.value.filter(task => 
      task.status === 'ASSIGNED' || task.status === 'IN_PROGRESS'
    )
    const currentIndexInIncomplete = incompleteTasksBeforeUpdate.findIndex(task => 
      task.id === currentTask.value.id
    )
    const nextTask = incompleteTasksBeforeUpdate[currentIndexInIncomplete + 1]
    
    // 添加超时控制
    const response = await Promise.race([
      userTaskApi.saveAnnotation(annotationData),
      new Promise((_, reject) => 
        setTimeout(() => reject(new Error('提交超时')), 300000)
      )
    ])
    
    if (response.success) {
      ElMessage.success(isUpdate ? '标注更新成功' : '标注提交成功')
      
      // 更新任务状态
      currentTask.value.status = 'COMPLETED'
      
      // 同步更新taskIdList中对应任务的状态
      const taskIdIndex = taskIdList.value.findIndex(task => task.id === currentTask.value.id)
      if (taskIdIndex !== -1) {
        taskIdList.value[taskIdIndex].status = 'COMPLETED'
      }
      
      // 如果有下一个未完成的任务，自动跳转
      if (nextTask) {
        setTimeout(async () => {
          await selectTask(nextTask)
          ElMessage.success('已自动跳转到下一张图片')
          
          // 检查是否需要预加载
          await checkPreloadTasks()
        }, 1000) // 延迟1秒让用户看到提交成功的消息
      } else {
        // 当前批次没有下一个任务，尝试加载下一页
        if (pagination.hasMore && !loadingMore.value) {
          console.log('当前批次已完成，尝试加载下一页...')
          setTimeout(async () => {
            await loadMoreTasks()
            
            // 重新检查是否有未完成任务
            const newIncompleteTasks = taskList.value.filter(task => 
              task.status === 'ASSIGNED' || task.status === 'IN_PROGRESS'
            )
            
            if (newIncompleteTasks.length > 0) {
              await selectTask(newIncompleteTasks[0])
              ElMessage.success('已自动跳转到下一张图片')
      } else {
              ElMessage.info('🎉 所有未完成任务已完成！')
            }
          }, 1000)
        } else {
          // 真的没有更多任务了
          setTimeout(() => {
            ElMessage.info('🎉 所有未完成任务已完成！')
          }, 1000)
        }
      }
    }
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交标注失败:', error)
      if (error.message === '提交超时') {
        ElMessage.error('提交超时，请检查网络连接')
      } else if (error.code === 'ECONNABORTED') {
        ElMessage.error('请求超时，请稍后重试')
      } else {
        ElMessage.error('提交失败，请稍后重试')
      }
    }
  } finally {
    submitting.value = false
  }
}

// 处理任务完成
const handleCompleteTask = async () => {
  if (!currentTask.value) return
  
  try {
    await ElMessageBox.confirm(
      '确认完成此任务吗？完成后任务将变为只读模式，无法再修改标注。',
      '任务完成确认',
      {
        confirmButtonText: '确定完成',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    completing.value = true
    
    const response = await userTaskApi.completeTask(currentTask.value.id)
    
    if (response.success) {
      // 更新任务状态
      currentTask.value.isCompleted = true
      
      // 同步更新taskIdList中对应任务的状态
      const taskIdIndex = taskIdList.value.findIndex(task => task.id === currentTask.value.id)
      if (taskIdIndex !== -1) {
        taskIdList.value[taskIdIndex].isCompleted = true
      }
      
      ElMessage.success('任务已完成！现在处于只读模式')
    }
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('完成任务失败:', error)
      ElMessage.error('完成任务失败，请稍后重试')
    }
  } finally {
    completing.value = false
  }
}

// 返回项目列表
const backToProjects = () => {
  router.push('/user/projects')
}

// 退出登录处理
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确认要退出登录吗？', '退出确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch (error) {
    if (error !== 'cancel') {
      console.error('退出登录失败:', error)
      ElMessage.error('退出登录失败')
    }
  }
}

// 图片缩放和平移方法
const zoomIn = () => {
  imageTransform.scale = Math.min(imageTransform.scale * 1.2, 5)
}

const zoomOut = () => {
  imageTransform.scale = Math.max(imageTransform.scale * 0.8, 0.1)
}

const resetZoom = () => {
  imageTransform.scale = 1
  imageTransform.x = 0
  imageTransform.y = 0
  // 重置双指缩放状态
  pinchState.isPinching = false
}

// 重置图像视图到初始状态（用于全屏查看和电脑端）
const resetImageView = () => {
  // 全屏模式下，只对高清图进行重置
  if (isFullscreen.value && !isShowingHDImage.value) return
  
  // 电脑端：重置到初始状态（1倍缩放）
  // 全屏高清图：重置到初始放大状态（1.5倍缩放）
  if (isFullscreen.value && isShowingHDImage.value) {
    imageTransform.scale = 1.5
  } else {
    imageTransform.scale = 1
  }
  
  imageTransform.x = 0
  imageTransform.y = 0
  
  // 标记为初始状态，显示箭头
  isInitialViewState.value = true
  
  console.log('🔄 重置图像视图到初始状态')
}

// 鼠标和触摸事件处理
const handleMouseDown = (event) => {
  if (event.button !== 0) return
  startDrag(event.clientX, event.clientY)
}
  
const handleMouseMove = (event) => {
  if (dragState.isDragging) {
  event.preventDefault()
    moveDrag(event.clientX, event.clientY)
  }
}

const handleMouseUp = (event) => {
  endDrag()
}

// 计算两个触摸点之间的距离
const getTouchDistance = (touch1, touch2) => {
  const dx = touch1.clientX - touch2.clientX
  const dy = touch1.clientY - touch2.clientY
  return Math.sqrt(dx * dx + dy * dy)
}

// 计算两个触摸点的中心
const getTouchCenter = (touch1, touch2) => {
  return {
    x: (touch1.clientX + touch2.clientX) / 2,
    y: (touch1.clientY + touch2.clientY) / 2
  }
}

const handleTouchStart = (event) => {
  // 不要阻止默认行为，让浏览器处理多点触控
  // event.preventDefault()
  
  console.log(`📱 触摸开始: ${event.touches.length}个触摸点`)
  
  if (event.touches.length === 1) {
    // 单指操作
    const touch = event.touches[0]
    
    // 记录触摸开始状态，用于检测点击
    tapState.startX = touch.clientX
    tapState.startY = touch.clientY
    tapState.startTime = Date.now()
    tapState.moved = false
    
    if (!pinchState.isPinching) {
      event.preventDefault() // 只在单指时阻止
      startDrag(touch.clientX, touch.clientY)
      console.log('👆 单指拖拽开始')
    }
  } else if (event.touches.length === 2) {
    // 双指缩放开始
    event.preventDefault() // 在双指时阻止默认缩放
    endDrag() // 结束可能的拖拽操作
    tapState.moved = true // 标记为已移动，不触发点击
    
    const touch1 = event.touches[0]
    const touch2 = event.touches[1]
    const distance = getTouchDistance(touch1, touch2)
    const center = getTouchCenter(touch1, touch2)
    
    console.log(`🤏 双指缩放开始: 初始距离=${distance.toFixed(0)}px`)
    
    // 获取容器的位置信息（根据是否全屏选择容器）
    const container = isFullscreen.value ? fullscreenImageContainer.value : imageContainerRef.value
    if (!container) {
      console.warn('⚠️ 容器元素不存在')
      return
    }
    
    const containerRect = container.getBoundingClientRect()
    const centerX = center.x - containerRect.left - containerRect.width / 2
    const centerY = center.y - containerRect.top - containerRect.height / 2
    
    pinchState.isPinching = true
    pinchState.initialDistance = distance
    pinchState.initialScale = imageTransform.scale
    pinchState.initialCenterX = centerX
    pinchState.initialCenterY = centerY
  }
}

const handleTouchMove = (event) => {
  if (event.touches.length === 1 && dragState.isDragging && !pinchState.isPinching) {
    // 单指拖拽
    event.preventDefault()
    const touch = event.touches[0]
    
    // 检测是否移动了（超过10px视为移动）
    const deltaX = Math.abs(touch.clientX - tapState.startX)
    const deltaY = Math.abs(touch.clientY - tapState.startY)
    if (deltaX > 10 || deltaY > 10) {
      tapState.moved = true
    }
    
    moveDrag(touch.clientX, touch.clientY)
  } else if (event.touches.length === 2 && pinchState.isPinching) {
    // 双指缩放
    event.preventDefault()
    tapState.moved = true // 标记为已移动
    
    const touch1 = event.touches[0]
    const touch2 = event.touches[1]
    const distance = getTouchDistance(touch1, touch2)
    const center = getTouchCenter(touch1, touch2)
    
    // 计算缩放比例
    const scale = distance / pinchState.initialDistance
    const newScale = Math.max(0.1, Math.min(5, pinchState.initialScale * scale))
    
    // 获取容器位置信息（根据是否全屏选择容器）
    const container = isFullscreen.value ? fullscreenImageContainer.value : imageContainerRef.value
    if (!container) return
    
    const containerRect = container.getBoundingClientRect()
    const centerX = center.x - containerRect.left - containerRect.width / 2
    const centerY = center.y - containerRect.top - containerRect.height / 2
    
    // 计算缩放变化比例
    const scaleChange = newScale / imageTransform.scale
    
    // 以双指中心为缩放中心
    imageTransform.x = centerX + (imageTransform.x - centerX) * scaleChange
    imageTransform.y = centerY + (imageTransform.y - centerY) * scaleChange
    imageTransform.scale = newScale
    
    // 用户双指缩放时，标记为非初始状态（隐藏箭头）
    isInitialViewState.value = false
    
    console.log(`🔍 缩放中: scale=${newScale.toFixed(2)}x, 距离=${distance.toFixed(0)}px`)
  } else if (event.touches.length === 2) {
    // 如果是双指但没有启动缩放，阻止默认行为
    event.preventDefault()
  }
}

const handleTouchEnd = (event) => {
  console.log(`📱 触摸结束: 剩余${event.touches.length}个触摸点`)
  
  if (event.touches.length === 0) {
    // 所有手指都离开了屏幕
    if (pinchState.isPinching) {
      console.log('✅ 双指缩放结束')
    } else if (!tapState.moved) {
      // 检测是否是点击（没有移动且时间短）
      const touchDuration = Date.now() - tapState.startTime
      if (touchDuration < 300) { // 300ms内视为点击
        console.log('👆 检测到点击，切换全屏')
        toggleFullscreen()
      }
    }
    endDrag()
    pinchState.isPinching = false
  } else if (event.touches.length === 1 && pinchState.isPinching) {
    // 从双指变为单指
    console.log('🔄 从双指切换到单指')
    pinchState.isPinching = false
    // 可以选择开始单指拖拽
    const touch = event.touches[0]
    startDrag(touch.clientX, touch.clientY)
  }
}

const startDrag = (x, y) => {
  dragState.isDragging = true
  dragState.startX = x
  dragState.startY = y
  dragState.startTransformX = imageTransform.x
  dragState.startTransformY = imageTransform.y
  document.body.style.userSelect = 'none'
}

const moveDrag = (x, y) => {
  const deltaX = x - dragState.startX
  const deltaY = y - dragState.startY
  imageTransform.x = dragState.startTransformX + deltaX
  imageTransform.y = dragState.startTransformY + deltaY
  
  // 用户拖拽图片时，标记为非初始状态（隐藏箭头）
  if (Math.abs(deltaX) > 5 || Math.abs(deltaY) > 5) {
    isInitialViewState.value = false
  }
}

const endDrag = () => {
  if (dragState.isDragging) {
    dragState.isDragging = false
    document.body.style.userSelect = ''
  }
}

// 鼠标滚轮缩放事件处理
const handleWheel = (event) => {
  event.preventDefault()
  
  // 计算缩放因子，提供更平滑的缩放体验
  const scaleFactor = event.deltaY > 0 ? 0.9 : 1.1
  const newScale = Math.max(0.1, Math.min(5, imageTransform.scale * scaleFactor))
  
  if (newScale !== imageTransform.scale) {
    // 获取鼠标在容器中的相对位置（根据是否全屏选择容器）
    const container = showFullscreen.value ? fullscreenImageContainer.value : imageContainerRef.value
    const containerRect = container.getBoundingClientRect()
    const mouseX = event.clientX - containerRect.left - containerRect.width / 2
    const mouseY = event.clientY - containerRect.top - containerRect.height / 2
    
    // 计算缩放比例
    const scale = newScale / imageTransform.scale
    
    // 以鼠标位置为中心进行缩放
    imageTransform.x = mouseX + (imageTransform.x - mouseX) * scale
    imageTransform.y = mouseY + (imageTransform.y - mouseY) * scale
    imageTransform.scale = newScale
    
    // 用户缩放图片时，标记为非初始状态（隐藏箭头）
    isInitialViewState.value = false
  }
}

// 标注辅助方法
// 处理单选框点击（支持点击取消选中）
const handleRadioClick = (categoryId, optionValue, event) => {
  event.preventDefault() // 阻止默认的单选框行为
  
  // 如果当前选中的就是点击的选项，则取消选中
  if (annotations[categoryId] === optionValue) {
    annotations[categoryId] = ''
  } else {
    // 否则选中点击的选项
    annotations[categoryId] = optionValue
  }
}

const clearAllAnnotations = async () => {
  try {
    await ElMessageBox.confirm(
      '确认要清空所有标注内容吗？',
      '清空确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 重置所有标注
    if (labelConfig.value?.categories) {
      labelConfig.value.categories.forEach(category => {
        if (category.type === 'multiple_choice') {
          annotations[category.id] = []
        } else {
          annotations[category.id] = ''
        }
      })
    }
    
    
    ElMessage.success('已清空所有标注')
  } catch (error) {
    // 用户取消操作
  }
}

const autoFillTestData = () => {
  if (!labelConfig.value?.categories) {
    ElMessage.warning('没有可用的标签配置')
    return
  }
  
  labelConfig.value.categories.forEach(category => {
    if (category.type === 'single_choice' && category.options?.length > 0) {
      // 随机选择一个选项
      const randomIndex = Math.floor(Math.random() * category.options.length)
      annotations[category.id] = category.options[randomIndex].value
    } else if (category.type === 'multiple_choice' && category.options?.length > 0) {
      // 随机选择1-3个选项
      const count = Math.min(Math.ceil(Math.random() * 3), category.options.length)
      const selectedOptions = []
      for (let i = 0; i < count; i++) {
        const randomIndex = Math.floor(Math.random() * category.options.length)
        const optionValue = category.options[randomIndex].value
        if (!selectedOptions.includes(optionValue)) {
          selectedOptions.push(optionValue)
        }
      }
      annotations[category.id] = selectedOptions
    } else if (category.type === 'text') {
      // 填充示例文本
      annotations[category.id] = `测试文本内容 - ${category.name} - ${Date.now()}`
    }
  })
  
  
  ElMessage.success('已填充测试数据')
}

const getCategoryTypeText = (type) => {
  const typeMap = {
    'single_choice': '单选',
    'multiple_choice': '多选',
    'text': '文本'
  }
  return typeMap[type] || type
}

// 检查某个分类是否有值
const hasCategoryValue = (categoryId) => {
  const value = annotations[categoryId]
  if (!value) return false
  
  if (Array.isArray(value)) {
    return value.length > 0
  }
  
  return value.toString().trim() !== ''
}

// 获取分类的CSS类
const getCategoryClasses = (category) => {
  const classes = []
  
  // 必填项
  if (category.required) {
    classes.push('required-category')
  }
  
  // 已完成项
  const value = annotations[category.id]
  let isCompleted = false
  
  if (category.type === 'multiple_choice') {
    isCompleted = Array.isArray(value) && value.length > 0
  } else {
    isCompleted = value && value.toString().trim() !== ''
  }
  
  if (isCompleted) {
    classes.push('completed-category')
  }
  
  // 根据选项数量添加压缩类
  if (category.options) {
    if (category.options.length === 1) {
      classes.push('single-option')
    } else if (category.options.length <= 2) {
      classes.push('few-options')
    }
  }
  
  return classes
}

// 检查是否需要预加载更多任务
const checkPreloadTasks = async () => {
  const incompleteTasks = taskList.value.filter(task => 
    task.status === 'ASSIGNED' || task.status === 'IN_PROGRESS'
  )
  const currentIndex = currentIncompleteTaskIndex.value
  const remainingTasks = incompleteTasks.length - currentIndex - 1
  
  console.log('预加载检查 - 剩余未完成任务数:', remainingTasks)
  console.log('预加载检查 - 预加载阈值:', PRELOAD_THRESHOLD)
  console.log('预加载检查 - 是否还有更多数据:', pagination.hasMore)
  
  if (remainingTasks <= PRELOAD_THRESHOLD && pagination.hasMore && !loadingMore.value) {
    console.log('触发预加载条件，开始加载下一页...')
    await loadMoreTasks()
  }
}

// 导航到上一张或下一张图片（基于所有任务） - 优化版：立即切换，后台加载
const navigateToTask = async (direction) => {
  const allTasks = filteredTaskList.value
  const currentIndex = currentTaskIndex.value
  let targetIndex = -1
  
  console.log('⚡ 导航函数 - 方向:', direction, '当前索引:', currentIndex, '总任务数:', allTasks.length)
  
  if (direction === 'prev' && hasPrevTask.value) {
    targetIndex = currentIndex - 1
  } else if (direction === 'next' && hasNextTask.value) {
    targetIndex = currentIndex + 1
  }
  
  console.log('导航函数调试 - 目标索引:', targetIndex)
  
  if (targetIndex >= 0 && targetIndex < allTasks.length) {
    const targetTask = allTasks[targetIndex]
    console.log('⚡ 立即切换到任务ID:', targetTask.id)
    
    // 立即切换图片，不等待任何加载过程
    await selectTask(targetTask)
    
    // 后台处理预加载任务（不阻塞用户操作）
    checkPreloadTasks().then(() => {
      console.log('📊 后台预加载任务检查完成')
    }).catch(error => {
      console.warn('⚠️ 后台预加载任务检查失败:', error)
    })
  } else {
    console.log('导航函数调试 - 无法导航，索引超出范围')
    
    // 如果是向下导航且没有更多任务，尝试加载下一页
    if (direction === 'next' && pagination.hasMore && !loadingMore.value) {
      console.log('📄 尝试加载下一页以继续导航...')
      
      try {
        await loadMoreTasks()
        
        // 重新获取任务列表
        const newAllTasks = filteredTaskList.value
        
        if (currentIndex + 1 < newAllTasks.length) {
          const targetTask = newAllTasks[currentIndex + 1]
          console.log('📄 加载新数据后立即导航到任务ID:', targetTask.id)
          
          // 立即切换到新任务
          await selectTask(targetTask)
          
        } else {
          ElMessage.info('已到达最后一张图片')
        }
      } catch (error) {
        console.error('❌ 加载下一页失败:', error)
        ElMessage.error('加载下一页失败')
      }
    } else if (direction === 'prev') {
      ElMessage.info('已到达第一张图片')
    } else if (direction === 'next') {
      ElMessage.info('已到达最后一张图片')
    }
  }
}

// 快速跳转到标注表单
const scrollToAnnotationForm = () => {
  const annotationSection = document.querySelector('.annotation-form-section')
  if (annotationSection) {
    annotationSection.scrollIntoView({ 
      behavior: 'smooth', 
      block: 'start' 
    })
    // 添加高亮效果
    annotationSection.style.background = 'rgba(64, 158, 255, 0.1)'
    setTimeout(() => {
      annotationSection.style.background = ''
    }, 2000)
  }
}

// 获取任务ID列表
const fetchTaskIdList = async () => {
  if (loadingTaskList.value) return
  
  try {
    loadingTaskList.value = true
    
    if (isProjectMode.value) {
      console.log(`🔄 正在获取项目 ${props.projectId} 的任务列表...`)
      
      // 先获取项目信息
      const projectResponse = await userTaskApi.getProjectDetail(props.projectId)
      if (projectResponse.success) {
        projectInfo.value = projectResponse.data
      }
      
      // 获取项目任务列表
      const response = await userTaskApi.getProjectTasks(props.projectId)
      
      if (response.success) {
        taskIdList.value = response.data || []
        console.log(`✅ 获取到项目 ${projectInfo.value?.name} 的 ${taskIdList.value.length} 个任务`)
      } else {
        console.error('❌ 获取项目任务列表失败:', response.message)
        ElMessage.error('获取项目任务列表失败')
      }
    } else {
      console.log('🔄 正在获取任务ID列表...')
      
      const response = await userTaskApi.getTaskIdList()
      
      if (response.success) {
        taskIdList.value = response.data || []
        console.log(`✅ 获取到 ${taskIdList.value.length} 个任务（已随机排序）`)
      } else {
        console.error('❌ 获取任务列表失败:', response.message)
        ElMessage.error('获取任务列表失败')
      }
    }
  } catch (error) {
    console.error('❌ 获取任务列表异常:', error)
    
    // 检查是否是鉴权错误
    if (error.response?.status === 401 || error.response?.status === 403) {
      console.log('🔒 鉴权错误，将由拦截器处理跳转')
    } else {
      ElMessage.error('获取任务列表失败，请稍后重试')
    }
  } finally {
    loadingTaskList.value = false
  }
}

// 强制刷新随机顺序
const refreshRandomOrder = async () => {
  try {
    console.log('🎲 强制刷新随机顺序...')
    ElMessage.info('正在刷新随机顺序...')
    
    // 清除缓存
    taskList.value = []
    taskIdList.value = []
    currentTask.value = null
    
    // 重新获取数据
    await fetchTaskList()
    
    ElMessage.success('随机顺序已刷新！')
  } catch (error) {
    console.error('❌ 刷新失败:', error)
    ElMessage.error('刷新失败，请稍后重试')
  }
}

// 根据任务ID跳转到指定任务
const jumpToTask = async (taskId) => {
  try {
    console.log(`🎯 跳转到任务 ${taskId}`)
    const loadingMessage = ElMessage.info({
      message: '正在加载任务...',
      duration: 0, // 不自动关闭
      showClose: false
    })
    
    // 获取任务详情
    const response = await userTaskApi.getTaskById(taskId)
    
    // 关闭loading消息
    loadingMessage.close()
    
    if (response.success) {
      const task = response.data
      console.log('✅ 任务详情加载成功:', task)
      
      // 关闭任务导航
      showTaskNavigation.value = false
      
      // 直接选择这个任务
      await selectTask(task)
      
      ElMessage.success(`已跳转到任务 ${taskId}`)
      
    } else {
      ElMessage.error('任务加载失败: ' + (response.message || '未知错误'))
    }
  } catch (error) {
    console.error('❌ 跳转任务失败:', error)
    ElMessage.error('跳转失败，请稍后重试')
  }
}

// 打开任务导航
const openTaskNavigation = async () => {
  showTaskNavigation.value = true
  
  // 如果还没有加载任务列表，则加载
  if (taskIdList.value.length === 0) {
    await fetchTaskIdList()
  }
}

// 获取任务状态类型（用于Element UI标签颜色）
const getTaskStatusType = (status) => {
  switch (status) {
    case 'COMPLETED':
      return 'success'
    case 'IN_PROGRESS':
      return 'warning'
    case 'ASSIGNED':
      return 'info'
    default:
      return 'info'
  }
}

// 获取任务状态文本
const getTaskStatusText = (status) => {
  switch (status) {
    case 'COMPLETED':
      return '已完成'
    case 'IN_PROGRESS':
      return '进行中'
    case 'ASSIGNED':
      return '已分配'
    default:
      return '未知'
  }
}

// 切换全屏显示
const toggleFullscreen = async () => {
  isFullscreen.value = !isFullscreen.value
  
  if (isFullscreen.value) {
    // 进入全屏时，默认显示高清图并放大
    isShowingHDImage.value = true
    currentFullscreenImageUrl.value = getHDImageUrl(currentTask.value)
    
    // 预加载裁剪后的原图（用于缩略图和切换）
    const originalUrl = getTaskImageUrl(currentTask.value)
    croppedOriginalImageUrl.value = await getCroppedImageUrl(originalUrl)
    
    // 设置初始放大比例（放大中心）
    imageTransform.scale = 1.5
    imageTransform.x = 0
    imageTransform.y = 0
    
    // 标记为初始状态，显示箭头
    isInitialViewState.value = true
    
    // 禁用body滚动
    document.body.style.overflow = 'hidden'
    
    console.log('🖼️ 进入全屏，显示高清图，初始放大1.5倍')
  } else {
    // 退出全屏时重置图像变换状态
    resetZoom()
    
    // 恢复body滚动
    document.body.style.overflow = ''
  }
}

// 键盘快捷键处理
const handleKeydown = (event) => {
  // ESC 键退出全屏
  if (event.key === 'Escape' && isFullscreen.value) {
    event.preventDefault()
    toggleFullscreen()
    return
  }
  
  if (!currentTask.value) return
  
  // 全局导航快捷键（不需要焦点）
  switch (event.key) {
    case 'ArrowLeft':
      event.preventDefault()
      if (hasPrevTask.value) {
        navigateToTask('prev')
      }
      break
    case 'ArrowRight':
      event.preventDefault()
      if (hasNextTask.value) {
        navigateToTask('next')
      }
      break
  }
  
  // 只有在图像容器获得焦点时才处理缩放快捷键
  if (document.activeElement === imageContainerRef.value) {
    switch (event.key) {
      case '+':
      case '=':
        event.preventDefault()
        zoomIn()
        break
      case '-':
        event.preventDefault()
        zoomOut()
        break
      case '0':
        event.preventDefault()
        resetZoom()
        break
    }
  }
}

onMounted(() => {
  // 强制清除可能的缓存，确保获取最新的随机化数据
  taskList.value = []
  taskIdList.value = []
  currentTask.value = null
  
  console.log('🔄 组件初始化，强制刷新任务数据...')
  
  // 加载全局裁剪配置
  loadGlobalCropConfig()
  
  // 加载任务ID列表（用于显示总任务数）
  fetchTaskIdList()
  
  // 加载任务详情列表
  fetchTaskList()
  
  // 添加键盘事件监听
  document.addEventListener('keydown', handleKeydown)
  
  console.log('🔄 页面初始化完成')
})

onUnmounted(() => {
  // 清理键盘事件监听
  document.removeEventListener('keydown', handleKeydown)
  
  // 恢复用户选择状态和滚动状态
  document.body.style.userSelect = ''
  document.body.style.overflow = ''
  
  // 清理图片缓存，释放内存
  console.log('🧹 组件卸载，清理图片缓存:', imageCache.size)
  imageCache.forEach((img, url) => {
    img.src = '' // 释放图片内存
  })
  imageCache.clear()
  preloadingImages.clear()
  imageLoadTimes.length = 0
  
  // 清理裁切后的图片缓存
  console.log('🧹 清理裁切图片缓存:', croppedImageCache.size)
  croppedImageCache.clear()
})

// 监听任务列表变化，自动选择第一个任务
watch(filteredTaskList, (newTasks) => {
  console.log('🔍 filteredTaskList changed:', {
    newTasksLength: newTasks.length,
    hasCurrentTask: !!currentTask.value,
    firstTaskId: newTasks[0]?.id
  })
  
  if (newTasks.length > 0 && !currentTask.value) {
    console.log('✅ Auto-selecting first task:', newTasks[0].id)
    // 自动选择第一个任务
    selectTask(newTasks[0])
  }
}, { immediate: true })

// 监听tab切换，重新选择第一个任务
watch(activeTab, () => {
  if (filteredTaskList.value.length > 0) {
    selectTask(filteredTaskList.value[0])
  }
})

</script>

<style scoped>
/* 电脑端优先的左右布局设计 */
.task-list {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f6fa;
  margin: 0;
  padding: 0;
  font-size: 14px;
  overflow-y: auto;
  overflow-x: hidden;
}

/* 无任务状态 */
.no-task-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}

/* 主任务容器 - 电脑端左右布局 */
.task-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

/* 内容区域 - 左右布局容器 */
.content-wrapper {
  display: flex;
  flex-direction: row;
  flex: 1;
  gap: 0;
}

/* 顶部操作栏 */
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
  flex-shrink: 0;
}

.task-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.task-counter {
  font-size: 16px;
  font-weight: 600;
  color: #409eff;
  background: #ecf5ff;
  padding: 4px 8px;
  border-radius: 12px;
  white-space: nowrap; /* 禁止换行 */
}

.user-info {
  font-size: 15px;
  color: #666;
}

.nav-controls {
  display: flex;
  gap: 4px;
}

.nav-btn-top {
  width: 32px !important;
  height: 32px !important;
  min-width: 32px !important;
  padding: 0 !important;
  font-size: 14px !important;
}

/* 左侧面板 - 图像区域 */
.left-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f8f9fa;
  position: relative;
}

/* 右侧面板 - 标注和操作区域 */
.right-panel {
  width: 480px;
  min-width: 420px;
  max-width: 550px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-left: 2px solid #e4e7ed;
}

/* 图像显示区域 */
.image-section {
  flex: 1;
  background: #fff;
  display: flex;
  flex-direction: column;
}

.image-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #2c3e50;
  cursor: grab;
  position: relative;
  overflow: hidden;
  touch-action: manipulation;
  -webkit-user-drag: none;
  -webkit-touch-callout: none;
  padding: 0;
}

.image-container img {
  max-width: 100%;
  max-height: 100%;
  width: 100%;
  height: auto;
  object-fit: contain;
  display: block;
  margin: auto;
}

/* 翻页导航区域 */
.image-navigation {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  flex-shrink: 0;
}

.nav-button {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: rgba(255, 255, 255, 0.15);
  border: 1.5px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  color: white;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.2);
}

.nav-button svg {
  width: 16px;
  height: 16px;
  transition: transform 0.3s ease;
}

.nav-button:hover:not(.disabled) {
  background: rgba(255, 255, 255, 0.25);
  border-color: rgba(255, 255, 255, 0.5);
  transform: translateY(-2px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.3);
}

.nav-button:hover:not(.disabled) svg {
  transform: scale(1.2);
}

.nav-button:active:not(.disabled) {
  transform: translateY(0);
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
}

.nav-button.disabled {
  opacity: 0.4;
  cursor: not-allowed;
  background: rgba(255, 255, 255, 0.1);
  border-color: rgba(255, 255, 255, 0.15);
}

.nav-counter {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 5px 14px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 16px;
  color: white;
  font-weight: 700;
  font-size: 14px;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.nav-counter .current {
  color: #ffd700;
  font-size: 16px;
}

.nav-counter .separator {
  color: rgba(255, 255, 255, 0.6);
  font-size: 13px;
}

.nav-counter .total {
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
}

.image-container:active {
  cursor: grabbing;
}

.image-info {
  padding: 10px 15px;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  flex-shrink: 0;
}

  .touch-hint {
    margin-bottom: 8px;
    padding: 6px 10px;
    background: rgba(64, 158, 255, 0.06);
    border-left: 3px solid #409eff;
    border-radius: 4px;
    font-size: 15px;
  }

.filename {
  font-size: 15px;
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.image-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  color: #666;
}

.filename-display {
  font-weight: 600;
  color: #333;
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 标注区域 - 优化空间 */
.annotation-section {
  background: #f8f9fa;
  padding: 15px;
}

.existing-tip {
  margin-bottom: 6px;
}

/* 分类容器 - 电脑端单列布局 */
.categories-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: stretch;
}

.category {
  background: linear-gradient(145deg, #ffffff 0%, #f8fafc 100%);
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  padding: 15px;
  transition: all 0.3s ease;
  min-height: auto;
  position: relative;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.category::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, #06b6d4 0%, #10b981 100%);
  border-radius: 12px 12px 0 0;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.category:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(6, 182, 212, 0.15);
  border-color: #cbd5e1;
}

.category:hover::before {
  opacity: 1;
}

/* 进一步压缩选项少的分类空间 */
.category.single-option {
  padding: 10px;
}

.category.few-options {
  padding: 11px;
}

.category.completed-category {
  border-color: #10b981;
  background: linear-gradient(145deg, #ecfdf5 0%, #f0fdf4 100%);
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.1);
}

.category.completed-category::before {
  background: linear-gradient(90deg, #10b981 0%, #059669 100%);
  opacity: 1;
}

.category.required-category {
  border-color: #06b6d4;
  background: linear-gradient(145deg, #ecfeff 0%, #f0fdfa 100%);
}

.category.required-category::before {
  background: linear-gradient(90deg, #06b6d4 0%, #0891b2 100%);
}

.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.category-name {
  font-size: 17px;
  font-weight: 700;
  color: #1e293b;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.category-name::before {
  content: '';
  width: 8px;
  height: 8px;
  background: linear-gradient(135deg, #06b6d4 0%, #10b981 100%);
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(6, 182, 212, 0.3);
}

.required {
  color: #06b6d4;
  margin-left: 4px;
  font-weight: 700;
  text-shadow: 0 1px 2px rgba(6, 182, 212, 0.2);
}

.edited-tag {
  margin-left: 8px !important;
  font-size: 11px !important;
  padding: 2px 8px !important;
  font-weight: 600 !important;
  border-radius: 10px !important;
}

本来以前是没有这么宽布局/* 选项本来以前是没有这么宽布局呀，现在直接变丑了，你看这个也行
.options-container {
  display: flex;
  flex-direction: row;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.option {
  margin: 0 !important;
  font-size: 15px !important;
  line-height: 1.6 !important;
  padding: 10px 12px !important;
  border-radius: 8px;
  transition: all 0.2s ease;
  background: rgba(248, 250, 252, 0.5);
  border: 1px solid transparent;
}

.option:hover {
  background: rgba(6, 182, 212, 0.08);
  transform: translateX(2px);
}

/* Element Plus组件样式覆盖 */
.option :deep(.el-radio__label),
.option :deep(.el-checkbox__label) {
  font-size: 16px !important;
  line-height: 1.5 !important;
  padding-left: 8px !important;
  color: #374151;
  font-weight: 500;
}

.option :deep(.el-radio__input),
.option :deep(.el-checkbox__input) {
  margin-right: 8px !important;
}

/* 选中状态的美化 */
.option :deep(.el-radio__input.is-checked .el-radio__inner),
.option :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
  background: linear-gradient(135deg, #06b6d4 0%, #10b981 100%);
  border-color: #06b6d4;
  box-shadow: 0 2px 6px rgba(6, 182, 212, 0.4);
}

.option :deep(.el-radio__input.is-checked + .el-radio__label),
.option :deep(.el-checkbox__input.is-checked + .el-checkbox__label) {
  color: #06b6d4;
  font-weight: 600;
}

/* 分类类型标签美化 */
.category-type-tag {
  background: linear-gradient(135deg, #06b6d4 0%, #10b981 100%) !important;
  color: white !important;
  border: none !important;
  font-size: 13px !important;
  font-weight: 600 !important;
  padding: 4px 10px !important;
  border-radius: 12px !important;
  box-shadow: 0 2px 6px rgba(6, 182, 212, 0.3);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

/* 底部操作栏 */
.bottom-actions {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 10px;
  padding: 15px;
  background: linear-gradient(180deg, #ffffff 0%, #f8f9fa 100%);
  border-top: 2px solid #e4e7ed;
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.08);
}

.bottom-actions .el-button {
  width: 100%;
  font-size: 16px !important;
  font-weight: 600 !important;
  height: 44px !important;
  border-radius: 8px !important;
  transition: all 0.3s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

/* 联系信息栏 */
.contact-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 10px;
  padding: 12px 15px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 13px;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 8px;
  animation: fadeInUp 0.6s ease-out;
}

.contact-item .el-icon {
  font-size: 20px;
  color: #ffd700;
}

.beauty-badge {
  position: relative;
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 20px;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.sparkle {
  margin-left: 4px;
  animation: sparkle 1.5s ease-in-out infinite;
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes sparkle {
  0%, 100% {
    transform: scale(1) rotate(0deg);
    opacity: 1;
  }
  50% {
    transform: scale(1.2) rotate(180deg);
    opacity: 0.8;
  }
}

.bottom-actions .el-button:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.bottom-actions .el-button:active:not(:disabled) {
  transform: translateY(0);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

/* 移动端按钮统一宽度 */
.action-btn {
  width: 100%;
  min-width: 0;
  flex: 1;
}

/* 全屏图片查看器 - 移动端优化 */
.fullscreen-viewer {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.95);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  cursor: pointer;
}

.fullscreen-container {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.fullscreen-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: rgba(0, 0, 0, 0.7);
}

.fullscreen-title {
  color: white;
  font-size: 17px;
  font-weight: 500;
  max-width: 60%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.fullscreen-controls {
  display: flex;
  gap: 8px;
}

.fullscreen-controls .el-button {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: white;
  width: 36px;
  height: 36px;
  padding: 0;
  font-size: 12px;
}

.fullscreen-image-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  overflow: hidden;
  touch-action: manipulation; /* 允许多点触控 */
  -webkit-user-drag: none;
  -webkit-touch-callout: none;
  cursor: grab;
  position: relative; /* 允许绝对定位的子元素 */
}

.fullscreen-image-container:active {
  cursor: grabbing;
}

.fullscreen-image {
  width: 100%;
  max-width: 100%;
  height: auto;
  max-height: 100%;
  object-fit: contain;
  cursor: pointer;
  border-radius: 4px;
  user-select: none;
  pointer-events: none;
  transform-origin: center;
}

/* 质心箭头指示器 */
.centroid-arrow {
  position: absolute;
  /* 箭头终点(85,15)对准质心(50%,50%) */
  left: calc(50% - 85px); /* 质心左侧85px */
  top: calc(50% - 15px); /* 质心上方15px */
  pointer-events: none;
  z-index: 100;
  display: flex;
  flex-direction: column;
  align-items: center;
  animation: pulse-arrow 2s ease-in-out infinite;
}

.centroid-arrow svg {
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.5));
}

.arrow-label {
  color: #ff0000;
  font-size: 16px;
  font-weight: bold;
  margin-top: -10px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.8),
               0 0 10px rgba(255, 0, 0, 0.5);
  background: rgba(0, 0, 0, 0.6);
  padding: 4px 8px;
  border-radius: 4px;
  white-space: nowrap;
}

/* 箭头脉动动画 */
@keyframes pulse-arrow {
  0%, 100% {
    opacity: 0.8;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.1);
  }
}

.fullscreen-footer {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 12px;
  background: rgba(0, 0, 0, 0.7);
}

.image-counter {
  color: white;
  font-size: 17px;
  padding: 6px 12px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 16px;
}

.fullscreen-hint {
  color: rgba(255, 255, 255, 0.7);
    font-size: 15px;
  margin-top: 8px;
    text-align: center;
}

/* 缩略图切换器 */
.thumbnail-switcher {
  position: absolute;
  bottom: 80px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1000;
  padding: 10px;
  background: rgba(0, 0, 0, 0.8);
  border-radius: 12px;
  backdrop-filter: blur(10px);
}

.thumbnail-container {
  display: flex;
  gap: 15px;
  align-items: center;
}

.thumbnail-item {
  cursor: pointer;
  transition: all 0.3s ease;
  border: 3px solid transparent;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}

.thumbnail-item:hover {
  transform: scale(1.05);
  border-color: rgba(255, 255, 255, 0.5);
}

.thumbnail-item.active {
  border-color: #409eff;
  box-shadow: 0 0 15px rgba(64, 158, 255, 0.6);
}

.thumbnail-item img {
  width: 120px;
  height: 80px;
  object-fit: cover;
  display: block;
}

.thumbnail-label {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  font-size: 12px;
  text-align: center;
  padding: 4px;
}

/* 标注结果对话框 */
.annotation-result {
  max-height: 70vh;
  overflow-y: auto;
  font-size: 16px;
}

.annotation-result h3 {
  margin-bottom: 12px;
  color: #333;
  font-size: 16px;
  border-bottom: 2px solid #409eff;
  padding-bottom: 6px;
}

.labels-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.label-item {
  margin-bottom: 8px;
}

.label-category {
  font-weight: 600;
  color: #409eff;
  font-size: 16px;
}

.label-value {
  font-size: 15px;
  color: #666;
  line-height: 1.4;
}

/* 任务导航对话框 */
.task-navigation-content {
  display: flex;
  flex-direction: column;
  height: 70vh;
  max-height: 600px;
}

.task-nav-header {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid #e4e7ed;
  margin-bottom: 12px;
}

.task-nav-info {
  display: flex;
  gap: 16px;
  font-weight: 500;
  color: #606266;
  font-size: 16px;
  flex-wrap: wrap;
}

.task-nav-controls {
  width: 100%;
}

.task-nav-info span:last-child {
  color: #409EFF;
  font-weight: 600;
}

.task-grid-container {
  flex: 1;
  overflow-y: auto;
  padding: 4px 0;
  max-height: 500px;
}

.task-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  padding: 4px;
  width: 100%;
}

.task-item {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: white;
}

.task-item:hover {
  border-color: #409EFF;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.task-item.current {
  border-color: #409EFF;
  background: rgba(64, 158, 255, 0.1);
}

.task-item.completed {
  border-color: #67C23A;
  background: rgba(103, 194, 58, 0.05);
}

.task-item.in-progress {
  border-color: #E6A23C;
  background: rgba(230, 162, 60, 0.05);
}

.task-id {
  font-size: 19px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.task-status {
  margin-bottom: 6px;
}

.task-filename {
  font-size: 14px;
  color: #909399;
  line-height: 1.3;
  word-break: break-all;
}

.task-nav-footer {
  padding: 12px 0;
  border-top: 1px solid #e4e7ed;
  text-align: center;
  margin-top: 12px;
}

/* 任务导航对话框标题样式 */
.task-nav-dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.dialog-title {
  font-size: 19px;
  font-weight: 600;
  color: #303133;
}

.annotation-count-info {
  font-size: 15px;
  color: #606266;
  background: #f5f7fa;
  padding: 4px 8px;
  border-radius: 10px;
  border: 1px solid #e4e7ed;
}

/* 响应式设计 - 平板和移动端恢复上下布局 */
@media (max-width: 1024px) {
  /* 恢复上下布局 */
  .content-wrapper {
    flex-direction: column;
  }
  
  .left-panel {
    flex: 0 0 auto;
    width: 100%;
  }
  
  .right-panel {
    width: 100%;
    min-width: auto;
    max-width: none;
    flex: 1;
    border-left: none;
    border-top: 2px solid #e4e7ed;
  }
  
  .annotation-section {
    padding: 12px;
  }
  
  .categories-container {
    display: flex;
    flex-direction: column; /* 垂直排列 */
    gap: 8px;
  }
  
  .bottom-actions {
    flex-direction: row;
    padding: 12px;
    gap: 12px;
  }
  
  .bottom-actions .el-button {
    flex: 1;
    min-width: 0;
    max-width: none;
  }
  
  .action-btn {
    flex: 1 1 0;
    width: auto;
  }
  
  .contact-info {
    flex-direction: row;
    gap: 20px;
    padding: 14px;
    font-size: 14px;
  }
}

/* 响应式设计 - 移动端进一步优化 */
@media (max-width: 480px) {
  .task-list {
    font-size: 15px;
  }
  
  .left-panel {
    width: 100%;
    flex: 0 0 auto;
  }
  
  /* 移动端导航按钮优化 */
  .image-navigation {
    padding: 6px 10px;
  }
  
  .nav-button {
    padding: 5px 10px;
    font-size: 12px;
    gap: 4px;
  }
  
  .nav-button svg {
    width: 14px;
    height: 14px;
  }
  
  .nav-button span {
    font-size: 12px;
  }
  
  .nav-counter {
    padding: 4px 10px;
    font-size: 12px;
  }
  
  .nav-counter .current {
    font-size: 14px;
  }
  
  .nav-counter .separator {
    font-size: 11px;
  }
  
  .nav-counter .total {
    font-size: 12px;
  }
  
  .category {
    padding: 8px;
  }
  
  .category.single-option {
    padding: 6px 8px;
  }
  
  .category.few-options {
    padding: 7px 8px;
  }
  
  .category-name {
    font-size: 15px;
  }
  
  .category-name::before {
    width: 6px;
    height: 6px;
  }
  
  .category-header {
    margin-bottom: 6px;
  }
  
  .option {
    font-size: 14px !important;
    padding: 4px 6px !important;
  }
  
  .option :deep(.el-radio__label),
  .option :deep(.el-checkbox__label) {
    font-size: 14px !important;
    padding-left: 6px !important;
  }
  
  .task-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
    gap: 10px;
  }
  
  .top-bar {
    padding: 6px 10px;
  }
  
  .task-counter {
    font-size: 15px;
    padding: 3px 6px;
  }
  
  .bottom-actions {
    padding: 8px 10px;
    gap: 8px;
  }
  
  .bottom-actions .el-button {
    font-size: 16px !important;
    height: 44px !important;
  }
  
  .annotation-section {
    padding: 4px 8px 8px 8px;
  }
  
  .categories-container {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }
}

/* 移动端触摸优化 */
@media (hover: none) and (pointer: coarse) {
  .category:hover {
    transform: none;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  }
  
  .category:hover::before {
    opacity: 0;
  }
  
  .option:hover {
    background: transparent;
    transform: none;
  }
  
  .category:active {
    transform: scale(0.98);
    background: linear-gradient(145deg, #f8fafc 0%, #f1f5f9 100%);
  }
  
  .option:active {
    background: rgba(6, 182, 212, 0.1);
  }
  
  .image-container {
    cursor: default;
  }
  
  /* 移动端导航按钮触摸优化 */
  .nav-button:hover:not(.disabled) {
    transform: none;
    background: rgba(255, 255, 255, 0.15);
  }
  
  .nav-button:active:not(.disabled) {
    transform: scale(0.95);
    background: rgba(255, 255, 255, 0.25);
  }
  
  /* 移动端底部按钮触摸优化 */
  .bottom-actions .el-button:hover:not(:disabled) {
    transform: none;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
  }
  
  .bottom-actions .el-button:active:not(:disabled) {
    transform: scale(0.95);
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
  }

  /* 移动端联系信息栏 */
  .contact-info {
    flex-direction: column;
    gap: 12px;
    padding: 12px 16px;
    font-size: 14px;
  }

  .contact-item {
    gap: 6px;
  }

  .contact-item .el-icon {
    font-size: 18px;
  }

  .beauty-badge {
    padding: 6px 12px;
  }
}

/* 项目模式样式 */
.project-info {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 5px 10px;
  background: rgba(64, 158, 255, 0.1);
  border-radius: 8px;
  border: 1px solid rgba(64, 158, 255, 0.2);
  margin-right: 15px;
}

.project-name {
  font-weight: 600;
  color: #409eff;
  font-size: 17px;
}

.back-btn {
  height: 28px;
  padding: 0 10px;
}

/* 只读模式样式 */
.readonly-alert {
  margin-bottom: 8px;
}

.readonly-alert :deep(.el-alert__content) {
  font-size: 12px;
}

/* 已完成任务的表单项禁用样式 */
.options-container:disabled,
.options-container.is-disabled {
  opacity: 0.6;
  pointer-events: none;
}

@media (max-width: 768px) {
  .project-info {
    margin-right: 10px;
    padding: 3px 8px;
  }
  
  .project-name {
    font-size: 12px;
  }
  
  .back-btn {
    height: 24px;
    padding: 0 8px;
    font-size: 12px;
  }
}
</style>

<style>
/* 全局样式：ElMessage 白色背景 */
.el-message {
  background-color: #ffffff !important;
  background: #ffffff !important;
}
</style>
