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
          <span class="task-counter">{{ currentTaskIndex + 1 }} / {{ filteredTaskList.length }}</span>
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
              
      <!-- 图像显示区域 -->
      <div class="image-section">
        <div 
          class="image-container"
          ref="imageContainerRef"
          @mousedown="handleMouseDown"
          @mousemove="handleMouseMove"
          @mouseup="handleMouseUp"
          @touchstart="handleTouchStart"
          @touchmove="handleTouchMove"
          @touchend="handleTouchEnd"
          @wheel="handleWheel"
          @click="toggleFullscreen"
        >
          <!-- 左翻页按钮 -->
          <el-button
            @click.stop.prevent="navigateToTask('prev')"
            @mousedown.stop
            @touchstart.stop
            :disabled="!hasPrevTask"
            class="nav-btn-overlay nav-btn-left"
            circle
            type="primary"
          >
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          
          <!-- 右翻页按钮 -->
          <el-button
            @click.stop.prevent="navigateToTask('next')"
            @mousedown.stop
            @touchstart.stop
            :disabled="!hasNextTask"
            class="nav-btn-overlay nav-btn-right"
            circle
            type="primary"
          >
            <el-icon><ArrowRight /></el-icon>
          </el-button>
          
          <img 
            :src="getTaskImageUrl(currentTask)" 
                  :alt="currentTask.image.originalName" 
                  ref="imageRef"
                  :style="{
              transform: `translate(${imageTransform.x}px, ${imageTransform.y}px) scale(${imageTransform.scale})`
                  }"
                  @dragstart="$event.preventDefault()"
                />
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
               <div v-if="currentTask?.annotationId && !isTaskCompleted" class="existing-tip">
                <el-alert
                  title="正在编辑已有标注"
                  type="warning"
                  :closable="false"
                  show-icon
            size="small"
            style="max-height:28px;margin-top:5px"
          />
              </div>
            <el-tag :type="getStatusType(currentTask.status)" size="small">
              {{ getStatusText(currentTask.status) }}
            </el-tag>
            
                </div>
                
              </div>
            </div>

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

          <!-- 备注区域 - 现在作为普通分类项显示在网格中，固定在右列 -->
          <div class="category remark-category">
                  <div class="category-header">
              <span class="category-name" >标注备注</span>
              <el-tag size="small" type="info">备注</el-tag>
                  </div>
                  <el-input
                    v-model="annotationRemark"
                    type="textarea"
              placeholder="请输入标注备注（可选）"
              :rows="1"
              size="small"
              maxlength="200"
                    show-word-limit
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
          :disabled="!canSubmit || isTaskCompleted"
          size="large"
          class="submit-btn"
        >
          {{ currentTask?.annotationId ? '更新' : '提交' }}
        </el-button>
        <el-button 
          v-if="!isTaskCompleted"
          type="success" 
          @click="handleCompleteTask" 
          :loading="completing"
          size="large"
        >
          任务完成
        </el-button>
        <el-button @click="handleLogout" size="large" type="danger" plain>
          退出
        </el-button>
        </div>

      <!-- 联系信息栏 -->
      <div class="contact-info">
        <div class="contact-item">
          <el-icon><Phone /></el-icon>
          <span>TEL：19834514800</span>
        </div>
        <div class="contact-item beauty-badge">
          <el-icon><User /></el-icon>
          <span>辛晓红是大美女</span>
          <span class="sparkle">✨</span>
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
            v-if="currentTask"
            :src="getTaskImageUrl(currentTask)" 
            :alt="currentTask.image.originalName"
            class="fullscreen-image"
            :style="{
              transform: `translate(${imageTransform.x}px, ${imageTransform.y}px) scale(${imageTransform.scale})`
            }"
            @click="toggleFullscreen"
            @dragstart="$event.preventDefault()"
          />
        </div>
        
        <div class="fullscreen-footer">
          <div class="image-counter">
            {{ currentTaskIndex + 1 }} / {{ filteredTaskList.length }}
          </div>
          <div class="fullscreen-hint">
            📱 双指缩放图片，单指拖拽移动 | 💻 滚轮缩放 | 点击图片或按 ESC 退出全屏
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
import { useAuthStore } from '@/store/auth'

// 接收路由参数
const props = defineProps({
  projectId: String
})

const router = useRouter()
const loading = ref(false)
const authStore = useAuthStore()

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
const annotationRemark = ref('') // 标注备注

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
    if (error.message === '请求超时') {
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
      
      // 恢复备注信息
      if (response.data.remark) {
        annotationRemark.value = response.data.remark
        console.log('📝 成功恢复备注信息:', response.data.remark)
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
      annotationRemark.value = '' // 重置备注
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
  
  // 重置备注
  annotationRemark.value = ''
  
  // 立即重置图像变换状态
  imageTransform.scale = 1
  imageTransform.x = 0
  imageTransform.y = 0
  
  // 立即重置拖拽状态
  dragState.isDragging = false
  
  // 重置双指缩放状态
  pinchState.isPinching = false
  
  console.log('⚡ 图片切换完成，开始后台加载任务配置...')
  
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

// 标注相关方法
const handleSaveDraft = async () => {
  if (!currentTask.value) return
  
  try {
    saving.value = true
    
    const annotationData = {
      imageId: currentTask.value.image.id,
      labels: annotations,
      status: 'DRAFT',
      remark: annotationRemark.value || null // 包含备注信息
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
    ElMessage.warning('请完成所有必填项的标注')
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
      remark: annotationRemark.value || null // 包含备注信息
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
    // 单指拖拽
    if (!pinchState.isPinching) {
      event.preventDefault() // 只在单指时阻止
      const touch = event.touches[0]
      startDrag(touch.clientX, touch.clientY)
      console.log('👆 单指拖拽开始')
    }
  } else if (event.touches.length === 2) {
    // 双指缩放开始
    event.preventDefault() // 在双指时阻止默认缩放
    endDrag() // 结束可能的拖拽操作
    
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
    moveDrag(touch.clientX, touch.clientY)
  } else if (event.touches.length === 2 && pinchState.isPinching) {
    // 双指缩放
    event.preventDefault()
    
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
    
    // 清空备注
    annotationRemark.value = ''
    
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
  
  // 填充测试备注
  annotationRemark.value = `测试备注信息 - ${new Date().toLocaleString()}`
  
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
    ElMessage.error('获取任务列表失败，请稍后重试')
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
const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
  
  // 切换全屏时重置图像变换状态
  resetZoom()
  
  if (isFullscreen.value) {
    // 进入全屏时禁用body滚动
    document.body.style.overflow = 'hidden'
  } else {
    // 退出全屏时恢复body滚动
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
  fetchTaskList()
  
  // 添加键盘事件监听
  document.addEventListener('keydown', handleKeydown)
  
  console.log('🔄 移动端优化页面初始化完成')
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
/* 移动端优先的紧凑设计 */
.task-list {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f6fa;
  margin: 0;
  padding: 0;
  font-size: 16px;
  overflow: hidden;
}

/* 无任务状态 */
.no-task-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
}

/* 主任务容器 */
.task-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
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

/* 图像显示区域 */
.image-section {
  flex-shrink: 0;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
}

.image-container {
  height: 55vh;
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f8f9fa;
  cursor: grab;
  position: relative;
  overflow: hidden;
  touch-action: manipulation; /* 允许浏览器优化触摸延迟，同时支持多点触控 */
  -webkit-user-drag: none;
  -webkit-touch-callout: none;
  padding: 2px;
}

/* 悬浮翻页按钮 - 绿色主题 */
.nav-btn-overlay {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 100;
  width: 56px !important;
  height: 56px !important;
  min-width: 56px !important;
  min-height: 56px !important;
  padding: 0 !important;
  font-size: 28px !important;
  background: linear-gradient(135deg, #10b981 0%, #34d399 100%) !important;
  border: 3px solid white !important;
  border-radius: 50% !important;
  color: white !important;
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.6), 0 2px 8px rgba(0, 0, 0, 0.3);
  transition: all 0.3s ease;
  cursor: pointer;
  pointer-events: auto;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
}

.nav-btn-overlay:hover:not(:disabled) {
  background: linear-gradient(135deg, #059669 0%, #10b981 100%) !important;
  box-shadow: 0 6px 20px rgba(16, 185, 129, 0.8), 0 4px 12px rgba(0, 0, 0, 0.4);
  transform: translateY(-50%) scale(1.15);
  border-color: #ffd700;
}

.nav-btn-overlay:active:not(:disabled) {
  transform: translateY(-50%) scale(0.95);
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.5);
}

.nav-btn-overlay:disabled {
  background: linear-gradient(135deg, #c0c4cc 0%, #909399 100%) !important;
  cursor: not-allowed;
  opacity: 0.6;
  border-color: #e4e7ed;
}

.nav-btn-left {
  left: 20px;
}

.nav-btn-right {
  right: 20px;
}

/* 确保图标居中 */
.nav-btn-overlay .el-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
}

.image-container:active {
  cursor: grabbing;
}

.image-container img {
  width: 100%;
  max-width: 100%;
  height: auto;
  max-height: 100%;
  object-fit: contain;
  user-select: none;
  pointer-events: none;
  transform-origin: center;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

  .image-info {
    padding: 8px 12px;
    background: #fff;
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
  flex: 1;
  overflow-y: auto;
  background: #fff;
  padding: 8px 12px 12px 12px; /* 不再需要底部留空 */
}

.existing-tip {
  margin-bottom: 6px;
}

/* 分类容器 - 改成2列布局 */
.categories-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  grid-template-rows: auto auto; /* 2行自动高度 */
  gap: 6px;
  align-items: start;
  grid-auto-flow: row; /* 确保按行流动 */
}

/* 移除奇数个分类时最后一个跨两列的规则，让备注区域可以正常填入空位 */

.category {
  background: linear-gradient(145deg, #ffffff 0%, #f8fafc 100%);
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
  transition: all 0.3s ease;
  min-height: auto;
  position: relative;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

/* 第一个分类跨两行显示 */
.category:not(.remark-category):first-child {
  grid-row: 1 / 3; /* 第一个分类跨两行 */
  grid-column: 1; /* 固定在第一列 */
}

/* 其他普通分类正常显示 */
.category:not(.remark-category):not(:first-child) {
  grid-column: 2; /* 其他分类在第二列 */
  grid-row: 1; /* 在第一行 */
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

/* 选项容器 - 改成两行显示 */
.options-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 8px;
  align-items: start;
}

.option {
  margin: 0 !important;
  font-size: 16px !important;
  line-height: 1.5 !important;
  padding: 6px 8px !important;
  border-radius: 8px;
  transition: all 0.2s ease;
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

/* 备注分类 - 固定在右下角 */
.remark-category {
  grid-column: 2 / 3; /* 固定在第二列（右列） */
  grid-row: 2 / 3; /* 固定在第二行（下方） */
  order: 999; /* 确保在DOM顺序中排在最后 */
}

/* 底部操作栏 */
.bottom-actions {
  display: flex;
  justify-content: space-around;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: linear-gradient(180deg, #ffffff 0%, #f8f9fa 100%);
  border-top: 2px solid #e4e7ed;
  box-shadow: 
    0 -4px 12px rgba(0, 0, 0, 0.1),
    0 4px 12px rgba(0, 0, 0, 0.1),
    inset 0 1px 0 rgba(255, 255, 255, 0.5);
  flex-shrink: 0;
}

.bottom-actions .el-button {
  flex: 1;
  font-size: 18px !important;
  font-weight: 600 !important;
  height: 48px !important;
  border-radius: 8px !important;
  transition: all 0.3s ease;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
}

/* 联系信息栏 */
.contact-info {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 32px;
  padding: 16px 20px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 16px;
  font-weight: 500;
  border-radius: 0 0 12px 12px;
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
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

.submit-btn {
  flex: 1.2;
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

/* 响应式设计 - 移动端进一步优化 */
@media (max-width: 480px) {
  .task-list {
    font-size: 15px;
  }
  
  .image-container {
    height: 50vh;
    min-height: 280px;
    padding: 2px;
  }
  
  /* 移动端悬浮按钮调整 */
  .nav-btn-overlay {
    width: 48px !important;
    height: 48px !important;
    min-width: 48px !important;
    min-height: 48px !important;
    font-size: 24px !important;
    border-width: 2px !important;
  }
  
  .nav-btn-left {
    left: 12px;
  }
  
  .nav-btn-right {
    right: 12px;
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
    gap: 4px;
    grid-template-columns: 1fr 1fr;
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
  
  /* 移动端悬浮按钮触摸优化 */
  .nav-btn-overlay:hover:not(:disabled) {
    transform: translateY(-50%);
    background: linear-gradient(135deg, #10b981 0%, #34d399 100%) !important;
  }
  
  .nav-btn-overlay:active:not(:disabled) {
    transform: translateY(-50%) scale(0.9);
    background: linear-gradient(135deg, #059669 0%, #10b981 100%) !important;
    box-shadow: 0 2px 8px rgba(16, 185, 129, 0.8);
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
