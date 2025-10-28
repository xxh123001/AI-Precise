<template>
  <div class="annotation-filter">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>标注数据筛选</span>
          <el-button type="text" @click="showHelp">
            <el-icon><QuestionFilled /></el-icon>
            使用说明
          </el-button>
        </div>
      </template>

      <el-form :model="filterForm" label-width="120px" ref="filterFormRef">
        <!-- 基础筛选条件 -->
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="筛选用户">
              <el-select 
                v-model="filterForm.userIds" 
                placeholder="选择用户"
                multiple
                clearable
                filterable
                style="width: 100%"
              >
                <el-option 
                  v-for="user in userList" 
                  :key="user.id"
                  :label="`${user.username} (${user.annotationCount}条)`" 
                  :value="user.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="标注状态">
              <el-select v-model="filterForm.annotationStatus" clearable>
                <el-option label="已完成" value="COMPLETED" />
                <el-option label="草稿" value="DRAFT" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="时间范围">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                @change="handleDateRangeChange"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 标签筛选 -->
        <el-form-item label="标签筛选">
          <el-row :gutter="10">
            <el-col :span="8">
              <el-select
                v-model="filterForm.requiredTags"
                placeholder="必须包含的标签"
                multiple
                filterable
                allow-create
                style="width: 100%"
              >
                <el-option
                  v-for="tag in commonTags"
                  :key="tag"
                  :label="tag"
                  :value="tag"
                />
              </el-select>
            </el-col>
            <el-col :span="4">
              <el-select v-model="filterForm.tagMatchMode">
                <el-option label="包含所有" value="ALL" />
                <el-option label="包含任意" value="ANY" />
              </el-select>
            </el-col>
            <el-col :span="8">
              <el-select
                v-model="filterForm.excludeTags"
                placeholder="排除的标签"
                multiple
                filterable
                allow-create
                style="width: 100%"
              >
                <el-option
                  v-for="tag in commonTags"
                  :key="tag"
                  :label="tag"
                  :value="tag"
                />
              </el-select>
            </el-col>
            <el-col :span="4">
              <el-button @click="showQuickTags = !showQuickTags">
                快捷标签
              </el-button>
            </el-col>
          </el-row>
        </el-form-item>

        <!-- 快捷标签选择 -->
        <el-form-item v-if="showQuickTags" label="">
          <div class="quick-tags">
            <el-tag
              v-for="tag in commonTags.slice(0, 20)"
              :key="tag"
              :type="filterForm.requiredTags.includes(tag) ? 'success' : ''"
              @click="toggleQuickTag(tag)"
              style="margin: 2px; cursor: pointer;"
            >
              {{ tag }}
            </el-tag>
          </div>
        </el-form-item>

        <!-- 双标签快速筛选 -->
        <el-form-item label="双标签筛选">
          <div class="double-tag-section">
            <div class="quick-buttons">
              <el-button 
                :type="isDoubleTagActive(['近端小管', '远端小管']) ? 'primary' : ''" 
                @click="setDoubleTags(['近端小管', '远端小管'])"
                size="small"
                :loading="searching"
              >
                近端+远端
              </el-button>
              <el-button 
                :type="isDoubleTagActive(['近端小管', '萎缩小管']) ? 'primary' : ''" 
                @click="setDoubleTags(['近端小管', '萎缩小管'])"
                size="small"
                :loading="searching"
              >
                近端+萎缩
              </el-button>
              <el-button 
                :type="isDoubleTagActive(['远端小管', '萎缩小管']) ? 'primary' : ''" 
                @click="setDoubleTags(['远端小管', '萎缩小管'])"
                size="small"
                :loading="searching"
              >
                远端+萎缩
              </el-button>
              <el-button 
                :type="isDoubleTagActive(['近端小管', '集合管']) ? 'primary' : ''" 
                @click="setDoubleTags(['近端小管', '集合管'])"
                size="small"
                :loading="searching"
              >
                近端+集合管
              </el-button>
              <el-button 
                :type="isDoubleTagActive(['远端小管', '集合管']) ? 'primary' : ''" 
                @click="setDoubleTags(['远端小管', '集合管'])"
                size="small"
                :loading="searching"
              >
                远端+集合管
              </el-button>
            </div>
            <div class="custom-tags-input">
              <el-input 
                v-model="customTagInput"
                placeholder="输入自定义标签组合（逗号分隔）"
                @keyup.enter="addCustomTags"
                size="small"
              >
                <template #suffix>
                  <el-button 
                    type="text" 
                    @click="addCustomTags"
                    :loading="searching"
                    size="small"
                  >
                    应用
                  </el-button>
                </template>
              </el-input>
            </div>
          </div>
        </el-form-item>

        <!-- 操作按钮 -->
        <el-form-item>
          <div class="main-actions">
            <el-button 
              type="primary" 
              @click="handleSearch"
              :loading="searching"
            >
              <el-icon><Search /></el-icon>
              搜索
            </el-button>
            <el-button 
              @click="handlePreview"
              :loading="previewing"
            >
              <el-icon><View /></el-icon>
              预览结果
            </el-button>
            <el-button 
              type="success" 
              @click="handleExportImages"
              :loading="exporting"
              :disabled="!hasResults"
            >
              <el-icon><Download /></el-icon>
              导出图像
            </el-button>
            <el-button @click="handleReset">
              <el-icon><Refresh /></el-icon>
              重置
            </el-button>
            <el-dropdown>
              <el-button type="info">
                快速筛选<el-icon class="el-icon--right"><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="quickFilter('all')">所有已完成</el-dropdown-item>
                  <el-dropdown-item @click="quickFilter('近端')">包含近端小管</el-dropdown-item>
                  <el-dropdown-item @click="quickFilter('远端')">包含远端小管</el-dropdown-item>
                  <el-dropdown-item @click="quickFilter('萎缩')">包含萎缩小管</el-dropdown-item>
                  <el-dropdown-item @click="quickFilter('集合管')">包含集合管</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 搜索结果 -->
    <el-card v-if="hasResults" style="margin-top: 20px;">
      <template #header>
        <div class="card-header">
          <span>搜索结果 (共 {{ totalCount }} 条)</span>
          <div>
            <el-tag v-if="filterSummary.userCount" type="info">
              {{ filterSummary.userCount }} 个用户
            </el-tag>
            <el-tag v-if="filterSummary.tagCount" type="success">
              {{ filterSummary.tagCount }} 个标签条件
            </el-tag>
          </div>
        </div>
      </template>

      <!-- 标签组合统计 -->
      <div v-if="tagCombinations && Object.keys(tagCombinations).length > 0" class="tag-combinations">
        <h4>标签组合分布：</h4>
        <el-row :gutter="10">
          <el-col 
            v-for="(count, combination) in tagCombinations"
            :key="combination"
            :span="6"
          >
            <el-card shadow="hover" class="combination-card">
              <div class="combination-text">{{ combination }}</div>
              <div class="combination-count">{{ count }} 条</div>
            </el-card>
          </el-col>
        </el-row>
      </div>

      <!-- 结果表格 -->
      <div class="table-container">
        <el-table 
          :data="searchResults" 
          style="width: 100%;"
          max-height="500"
          stripe
          @row-click="handleRowClick"
          :row-style="{ cursor: 'pointer' }"
        >
        <el-table-column prop="id" label="标注ID" width="90" />
        <el-table-column prop="imageId" label="图像ID" width="90" />
        <el-table-column prop="user.username" label="用户" width="100" />
        <el-table-column label="图像" min-width="350">
          <template #default="{ row }">
            <div v-if="row.image">
              <div class="original-name">{{ row.image.originalName }}</div>
              <div class="image-info">{{ row.image.filename }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="labelsText" label="标签" width="200" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'COMPLETED' ? 'success' : 'warning'" size="small">
              {{ row.status === 'COMPLETED' ? '已完成' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button 
              type="primary" 
              size="small" 
              @click.stop="handleRowClick(row)"
              :icon="View"
            >
              查看
            </el-button>
          </template>
        </el-table-column>
        </el-table>
      </div>

      <!-- 分页 -->
      <el-pagination
        v-if="totalCount > pageSize"
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[20, 50, 100]"
        :total="totalCount"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 20px; text-align: center;"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>

    <!-- 预览对话框 -->
    <el-dialog 
      v-model="showPreviewDialog" 
      title="预览筛选结果" 
      width="90%"
      :close-on-click-modal="false"
    >
      <div v-if="previewData.length > 0">
        <div class="preview-stats">
          <el-tag type="info">预览前 {{ previewData.length }} 条</el-tag>
          <el-tag type="success">总共 {{ previewTotalCount }} 条符合条件</el-tag>
        </div>
        
        <el-table 
          :data="previewData" 
          style="width: 100%; margin-top: 10px;"
          max-height="400"
        >
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="user.username" label="用户" width="100" />
          <el-table-column label="图像文件" min-width="300">
            <template #default="{ row }">
              <div class="preview-filename">{{ row.image ? row.image.originalName : '无图像' }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="labelsText" label="标签" width="200" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="时间" width="140">
            <template #default="{ row }">
              {{ formatDate(row.createdAt) }}
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div v-else>
        <el-empty description="无符合条件的数据" />
      </div>
      
      <template #footer>
        <div class="dialog-actions">
          <el-button @click="showPreviewDialog = false">关闭</el-button>
          <el-button 
            v-if="previewData.length > 0"
            type="primary" 
            @click="confirmExport"
            :loading="exporting"
          >
            确认导出 ({{ previewTotalCount }} 条)
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 帮助对话框 -->
    <el-dialog 
      v-model="showHelpDialog" 
      title="使用说明" 
      width="60%"
    >
      <div class="help-content">
        <h3>筛选功能说明</h3>
        <el-steps direction="vertical" :active="4">
          <el-step title="选择用户" description="可选择一个或多个用户进行筛选" />
          <el-step title="设置时间范围" description="选择标注创建的时间范围" />
          <el-step title="配置标签条件" description="设置必须包含或排除的标签" />
          <el-step title="执行筛选" description="点击搜索按钮查看结果" />
          <el-step title="导出数据" description="将筛选结果导出为压缩包" />
        </el-steps>
        
        <h3>双标签筛选</h3>
        <p>双标签筛选可以找到同时包含多个指定标签的标注记录，特别适用于：</p>
        <ul>
          <li><strong>近端+远端：</strong>同时标注了近端小管和远端小管的图像</li>
          <li><strong>近端+萎缩：</strong>同时包含近端小管和萎缩小管标签的记录</li>
          <li><strong>自定义组合：</strong>可以输入任意标签组合进行筛选</li>
        </ul>
        
        <h3>导出功能</h3>
        <p>导出的压缩包包含：</p>
        <ul>
          <li>符合条件的所有图像文件</li>
          <li>详细的汇总报告（export_summary.txt）</li>
          <li>文件命名格式：[图像ID]_[原始文件名]</li>
        </ul>
      </div>
      
      <template #footer>
        <el-button type="primary" @click="showHelpDialog = false">知道了</el-button>
      </template>
    </el-dialog>

    <!-- 图片查看对话框 -->
    <el-dialog 
      v-model="showImageDialog" 
      :title="`图片详情 - 标注ID: ${selectedAnnotation?.id || ''}`" 
      width="80%"
      :close-on-click-modal="false"
    >
      <div v-if="selectedAnnotation" class="image-detail">
        <el-row :gutter="20">
          <el-col :span="16">
            <div class="image-container-wrapper">
              <div class="image-container" ref="imageContainer">
                <el-image 
                  :src="getImageUrl(selectedAnnotation.image)"
                  fit="none"
                  :style="imageStyle"
                  class="zoomable-image"
                  @mousedown="startDrag"
                  @mousemove="drag"
                  @mouseup="endDrag"
                  @wheel="handleWheel"
                >
                  <template #error>
                    <div class="image-error">
                      <el-icon><Picture /></el-icon>
                      <div>图片加载失败</div>
                      <div class="error-path">{{ selectedAnnotation.image?.filePath }}</div>
                    </div>
                  </template>
                </el-image>
              </div>
              <div class="image-controls">
                <el-button-group>
                  <el-button size="small" @click="zoomIn">
                    <el-icon><ZoomIn /></el-icon>
                  </el-button>
                  <el-button size="small" @click="zoomOut">
                    <el-icon><ZoomOut /></el-icon>
                  </el-button>
                  <el-button size="small" @click="resetZoom">重置</el-button>
                </el-button-group>
                <div class="zoom-info">{{ Math.round(imageZoom * 100) }}%</div>
              </div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="annotation-info">
              <h3>标注信息</h3>
              <el-descriptions :column="1" border>
                <el-descriptions-item label="标注ID">
                  {{ selectedAnnotation.id }}
                </el-descriptions-item>
                <el-descriptions-item label="图像ID">
                  {{ selectedAnnotation.imageId }}
                </el-descriptions-item>
                <el-descriptions-item label="标注用户">
                  {{ selectedAnnotation.user?.username || '未知' }}
                </el-descriptions-item>
                <el-descriptions-item label="状态">
                  <el-tag :type="selectedAnnotation.status === 'COMPLETED' ? 'success' : 'warning'">
                    {{ selectedAnnotation.status === 'COMPLETED' ? '已完成' : '草稿' }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="创建时间">
                  {{ formatDateTime(selectedAnnotation.createdAt) }}
                </el-descriptions-item>
                <el-descriptions-item label="更新时间">
                  {{ formatDateTime(selectedAnnotation.updatedAt) }}
                </el-descriptions-item>
              </el-descriptions>

              <h3 style="margin-top: 20px;">图像信息</h3>
              <el-descriptions :column="1" border v-if="selectedAnnotation.image">
                <el-descriptions-item label="文件名">
                  {{ selectedAnnotation.image.filename }}
                </el-descriptions-item>
                <el-descriptions-item label="原始名称">
                  {{ selectedAnnotation.image.originalName }}
                </el-descriptions-item>
                <el-descriptions-item label="文件路径">
                  <div class="file-path">{{ selectedAnnotation.image.filePath }}</div>
                </el-descriptions-item>
              </el-descriptions>

              <h3 style="margin-top: 20px;">标注标签</h3>
              <div class="labels-display">
                <el-tag 
                  v-for="(tag, index) in parseLabels(selectedAnnotation.labelsText)"
                  :key="index"
                  type="success"
                  style="margin: 5px;"
                  size="large"
                >
                  {{ tag }}
                </el-tag>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
      
      <template #footer>
        <el-button @click="showImageDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Search, 
  View, 
  Download, 
  Refresh, 
  QuestionFilled,
  Picture,
  ZoomIn,
  ZoomOut,
  ArrowDown
} from '@element-plus/icons-vue'
import { annotationFilterApi } from '@/api/annotationFilter'

// 响应式数据
const searching = ref(false)
const previewing = ref(false)
const exporting = ref(false)
const showPreviewDialog = ref(false)
const showHelpDialog = ref(false)
const showQuickTags = ref(false)
const showImageDialog = ref(false)
const selectedAnnotation = ref(null)

// 图像缩放和拖拽相关
const imageZoom = ref(1)
const imagePosition = ref({ x: 0, y: 0 })
const isDragging = ref(false)
const dragStart = ref({ x: 0, y: 0 })
const imageContainer = ref(null)

const filterFormRef = ref()
const filterForm = reactive({
  userIds: [],
  usernames: [],
  annotationStatus: 'COMPLETED',
  requiredTags: [],
  optionalTags: [],
  excludeTags: [],
  tagMatchMode: 'ALL',
  pageSize: 50,
  pageNumber: 0,
  sortBy: 'createdAt',
  sortDirection: 'DESC'
})

const dateRange = ref([])
const customTagInput = ref('')

// 数据
const userList = ref([])
const commonTags = ref([])
const searchResults = ref([])
const previewData = ref([])
const tagCombinations = ref({})

// 分页
const currentPage = ref(1)
const pageSize = ref(50)
const totalCount = ref(0)
const previewTotalCount = ref(0)

// 计算属性
const hasResults = computed(() => searchResults.value.length > 0)

const filterSummary = computed(() => {
  return {
    userCount: filterForm.userIds.length,
    tagCount: filterForm.requiredTags.length + filterForm.excludeTags.length
  }
})

// 判断双标签按钮是否应该被激活
const isDoubleTagActive = computed(() => {
  return (tags) => {
    if (!filterForm.requiredTags || filterForm.requiredTags.length !== tags.length) {
      return false
    }
    return tags.every(tag => filterForm.requiredTags.includes(tag)) && 
           filterForm.requiredTags.every(tag => tags.includes(tag))
  }
})

// 图像样式计算属性
const imageStyle = computed(() => {
  return {
    transform: `scale(${imageZoom.value}) translate(${imagePosition.value.x}px, ${imagePosition.value.y}px)`,
    cursor: isDragging.value ? 'grabbing' : 'grab',
    transition: isDragging.value ? 'none' : 'transform 0.1s ease-out'
  }
})

// 监听器
watch(() => filterForm.requiredTags, (newTags) => {
  // 自动设置标签匹配模式
  if (newTags.length >= 2) {
    filterForm.tagMatchMode = 'ALL'
  }
}, { deep: true })

// 监听图片对话框状态，重置缩放
watch(showImageDialog, (newVal) => {
  if (newVal) {
    resetZoom()
  }
})

// 方法
const fetchUserList = async () => {
  try {
    const response = await annotationFilterApi.getAnnotationUsers()
    if (response.success) {
      userList.value = response.data || []
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败')
  }
}

const fetchCommonTags = async () => {
  try {
    const response = await annotationFilterApi.getCommonTags()
    if (response.success) {
      commonTags.value = response.data || []
    }
  } catch (error) {
    console.error('获取常见标签失败:', error)
  }
}

const handleDateRangeChange = (dates) => {
  if (dates && dates.length === 2) {
    filterForm.startDate = dates[0]
    filterForm.endDate = dates[1]
  } else {
    filterForm.startDate = null
    filterForm.endDate = null
  }
}

const toggleQuickTag = (tag) => {
  const index = filterForm.requiredTags.indexOf(tag)
  if (index > -1) {
    filterForm.requiredTags.splice(index, 1)
  } else {
    filterForm.requiredTags.push(tag)
  }
}

const setDoubleTags = async (tags) => {
  filterForm.requiredTags = [...tags]
  filterForm.tagMatchMode = 'ALL'
  ElMessage.success(`已设置双标签筛选：${tags.join(' + ')}`)
  
  // 自动执行搜索
  await handleSearch()
}

const addCustomTags = async () => {
  if (customTagInput.value.trim()) {
    const tags = customTagInput.value.split(',').map(tag => tag.trim()).filter(tag => tag)
    filterForm.requiredTags = [...new Set([...filterForm.requiredTags, ...tags])]
    customTagInput.value = ''
    ElMessage.success(`已添加标签：${tags.join(', ')}`)
    
    // 如果是多个标签，设置为ALL模式并自动搜索
    if (tags.length >= 2) {
      filterForm.tagMatchMode = 'ALL'
      await handleSearch()
    }
  }
}

// 快速筛选功能
const quickFilter = async (type) => {
  // 先重置筛选条件（不显示重置消息）
  Object.assign(filterForm, {
    userIds: [],
    usernames: [],
    annotationStatus: 'COMPLETED',
    requiredTags: [],
    optionalTags: [],
    excludeTags: [],
    tagMatchMode: 'ALL',
    pageSize: 50,
    pageNumber: 0,
    sortBy: 'createdAt',
    sortDirection: 'DESC'
  })
  dateRange.value = []
  customTagInput.value = ''
  
  switch (type) {
    case 'all':
      // 显示所有已完成的标注
      filterForm.annotationStatus = 'COMPLETED'
      break
    case '近端':
      filterForm.requiredTags = ['近端小管']
      filterForm.tagMatchMode = 'ANY'
      break
    case '远端':
      filterForm.requiredTags = ['远端小管']
      filterForm.tagMatchMode = 'ANY'
      break
    case '萎缩':
      filterForm.requiredTags = ['萎缩小管']
      filterForm.tagMatchMode = 'ANY'
      break
    case '集合管':
      filterForm.requiredTags = ['集合管']
      filterForm.tagMatchMode = 'ANY'
      break
  }
  
  // 自动执行搜索
  await handleSearch()
  ElMessage.success(`已应用快速筛选：${type === 'all' ? '所有已完成' : type}`)
}

const handleSearch = async () => {
  try {
    searching.value = true
    filterForm.pageNumber = 0
    currentPage.value = 1
    
    // 根据标签数量选择合适的API
    let response
    if (filterForm.requiredTags && filterForm.requiredTags.length >= 2 && filterForm.tagMatchMode === 'ALL') {
      console.log('使用双标签筛选API:', filterForm.requiredTags)
      response = await annotationFilterApi.filterDoubleTags(filterForm)
    } else {
      console.log('使用普通搜索API')
      response = await annotationFilterApi.searchAnnotations(filterForm)
    }
    
    if (response.success) {
      searchResults.value = response.data.content || []
      totalCount.value = response.data.totalElements || 0
      tagCombinations.value = response.data.tagCombinations || {}
      
      const searchType = filterForm.requiredTags && filterForm.requiredTags.length >= 2 && filterForm.tagMatchMode === 'ALL' ? '双标签筛选' : '搜索'
      ElMessage.success(`${searchType}完成，找到 ${totalCount.value} 条符合条件的记录`)
    } else {
      ElMessage.error('搜索失败: ' + (response.message || ''))
    }
  } catch (error) {
    console.error('搜索失败:', error)
    ElMessage.error('搜索失败: ' + error.message)
  } finally {
    searching.value = false
  }
}

const handlePreview = async () => {
  try {
    previewing.value = true
    
    const previewRequest = { ...filterForm, pageSize: 20, pageNumber: 0 }
    
    // 根据标签数量选择合适的API进行预览
    let response
    if (filterForm.requiredTags && filterForm.requiredTags.length >= 2 && filterForm.tagMatchMode === 'ALL') {
      console.log('使用双标签筛选API预览:', filterForm.requiredTags)
      response = await annotationFilterApi.filterDoubleTags(previewRequest)
      if (response.success) {
        previewData.value = response.data.content || []
        previewTotalCount.value = response.data.totalElements || 0
      }
    } else {
      console.log('使用预览API')
      response = await annotationFilterApi.previewFilterResults(previewRequest)
      if (response.success) {
        previewData.value = response.data.previewData || []
        previewTotalCount.value = response.data.totalCount || 0
      }
    }
    
    if (response.success) {
      showPreviewDialog.value = true
    } else {
      ElMessage.error('预览失败: ' + (response.message || ''))
    }
  } catch (error) {
    console.error('预览失败:', error)
    ElMessage.error('预览失败: ' + error.message)
  } finally {
    previewing.value = false
  }
}

const handleExportImages = async () => {
  if (!hasResults.value) {
    ElMessage.warning('请先搜索数据')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确认导出 ${totalCount.value} 条记录对应的图像文件？这可能需要一些时间。`,
      '确认导出',
      {
        confirmButtonText: '确认导出',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await confirmExport()
  } catch {
    // 用户取消
  }
}

const confirmExport = async () => {
  try {
    exporting.value = true
    
    const exportRequest = { ...filterForm, exportFiles: true, exportFormat: 'ZIP' }
    
    // 确保使用与搜索相同的逻辑来导出
    console.log('导出筛选结果，标签数量:', filterForm.requiredTags?.length)
    const response = await annotationFilterApi.exportImages(exportRequest)
    
    // 处理文件下载
    const blob = new Blob([response.data], { type: 'application/zip' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    
    // 根据筛选类型命名文件
    const isDoubleTag = filterForm.requiredTags && filterForm.requiredTags.length >= 2 && filterForm.tagMatchMode === 'ALL'
    const filename = isDoubleTag 
      ? `双标签筛选_${filterForm.requiredTags.join('_')}_${new Date().toISOString().split('T')[0]}.zip`
      : `筛选结果_${new Date().toISOString().split('T')[0]}.zip`
    link.download = filename
    
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('图像文件导出成功')
    showPreviewDialog.value = false
    
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败: ' + error.message)
  } finally {
    exporting.value = false
  }
}

const handleReset = () => {
  Object.assign(filterForm, {
    userIds: [],
    usernames: [],
    annotationStatus: 'COMPLETED',
    requiredTags: [],
    optionalTags: [],
    excludeTags: [],
    tagMatchMode: 'ALL',
    pageSize: 50,
    pageNumber: 0,
    sortBy: 'createdAt',
    sortDirection: 'DESC'
  })
  dateRange.value = []
  customTagInput.value = ''
  searchResults.value = []
  totalCount.value = 0
  currentPage.value = 1
  ElMessage.info('筛选条件已重置')
}

const handleSizeChange = (newSize) => {
  pageSize.value = newSize
  filterForm.pageSize = newSize
  filterForm.pageNumber = 0
  currentPage.value = 1
  handleSearch()
}

const handleCurrentChange = (newPage) => {
  currentPage.value = newPage
  filterForm.pageNumber = newPage - 1
  handleSearch()
}

const showHelp = () => {
  showHelpDialog.value = true
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString()
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString()
}

const handleRowClick = (row) => {
  selectedAnnotation.value = row
  showImageDialog.value = true
}

const getImageUrl = (image) => {
  if (!image || !image.filename) {
    return ''
  }
  // 构建图片URL，假设后端API提供图片访问
  return `http://62.234.94.65:8080/uploads/${image.filename}`
}

const parseLabels = (labelsText) => {
  if (!labelsText) return []
  // 如果是逗号分隔的字符串，直接分割
  if (typeof labelsText === 'string') {
    return labelsText.split(',').map(tag => tag.trim()).filter(tag => tag)
  }
  return []
}

// 图像缩放和拖拽方法
const zoomIn = () => {
  imageZoom.value = Math.min(imageZoom.value * 1.2, 5)
}

const zoomOut = () => {
  imageZoom.value = Math.max(imageZoom.value / 1.2, 0.1)
}

const resetZoom = () => {
  imageZoom.value = 1
  imagePosition.value = { x: 0, y: 0 }
}

const handleWheel = (event) => {
  event.preventDefault()
  const delta = event.deltaY > 0 ? -1 : 1
  const zoomFactor = 1.1
  
  if (delta > 0) {
    imageZoom.value = Math.min(imageZoom.value * zoomFactor, 5)
  } else {
    imageZoom.value = Math.max(imageZoom.value / zoomFactor, 0.1)
  }
}

const startDrag = (event) => {
  event.preventDefault()
  isDragging.value = true
  
  const containerRect = imageContainer.value.getBoundingClientRect()
  dragStart.value = {
    x: event.clientX - containerRect.left - imagePosition.value.x,
    y: event.clientY - containerRect.top - imagePosition.value.y
  }
  
  document.addEventListener('mousemove', drag)
  document.addEventListener('mouseup', endDrag)
}

const drag = (event) => {
  if (!isDragging.value) return
  event.preventDefault()
  event.stopPropagation()
  
  const containerRect = imageContainer.value.getBoundingClientRect()
  imagePosition.value = {
    x: event.clientX - containerRect.left - dragStart.value.x,
    y: event.clientY - containerRect.top - dragStart.value.y
  }
}

const endDrag = (event) => {
  event.preventDefault()
  event.stopPropagation()
  isDragging.value = false
  document.removeEventListener('mousemove', drag)
  document.removeEventListener('mouseup', endDrag)
}

// 生命周期
onMounted(async () => {
  // 先加载基础数据
  await Promise.all([
    fetchUserList(),
    fetchCommonTags()
  ])
  
  // 延迟执行初始搜索，确保数据加载完成
  setTimeout(async () => {
    // 进入页面时自动加载所有已完成的标注
    filterForm.annotationStatus = 'COMPLETED'
    await handleSearch()
    console.log('页面初始化完成，已加载所有已完成的标注')
  }, 500)
})
</script>

<style scoped>
.annotation-filter {
  height: 100%;
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.quick-tags {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 10px;
  max-height: 120px;
  overflow-y: auto;
}

.tag-combinations {
  margin-bottom: 20px;
}

.combination-card {
  text-align: center;
  margin-bottom: 10px;
}

.combination-text {
  font-weight: bold;
  margin-bottom: 5px;
}

.combination-count {
  color: #409eff;
  font-size: 18px;
}

.image-info {
  font-size: 11px;
  color: #909399;
  line-height: 1.3;
  margin-top: 2px;
  opacity: 0.8;
}

.preview-stats {
  margin-bottom: 10px;
}

.preview-stats .el-tag {
  margin-right: 10px;
}

.help-content {
  line-height: 1.6;
}

.help-content h3 {
  color: #409eff;
  margin-top: 20px;
  margin-bottom: 10px;
}

.help-content ul {
  padding-left: 20px;
}

.help-content li {
  margin-bottom: 5px;
}

.image-detail {
  padding: 10px;
}

.image-container-wrapper {
  position: relative;
  width: 100%;
  height: 600px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #f5f7fa;
}

.image-container {
  width: 100%;
  height: 560px;
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  background: 
    radial-gradient(circle, #ccc 1px, transparent 1px),
    radial-gradient(circle, #ccc 1px, transparent 1px);
  background-size: 20px 20px;
  background-position: 0 0, 10px 10px;
}

.zoomable-image {
  user-select: none;
  transform-origin: center center;
  width: 100%;
  height: auto;
  max-width: none !important;
  max-height: none !important;
  object-fit: contain;
}

.image-controls {
  position: absolute;
  bottom: 10px;
  left: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(255, 255, 255, 0.9);
  padding: 5px 10px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.zoom-info {
  font-size: 12px;
  color: #666;
  min-width: 40px;
  text-align: center;
}

.image-error {
  text-align: center;
  color: #909399;
  padding: 40px;
}

.image-error .el-icon {
  font-size: 64px;
  margin-bottom: 10px;
}

.error-path {
  font-size: 12px;
  margin-top: 10px;
  word-break: break-all;
}

.annotation-info h3 {
  color: #409eff;
  margin: 10px 0;
  font-size: 16px;
}

.file-path {
  font-size: 12px;
  color: #606266;
  word-break: break-all;
}

.labels-display {
  margin-top: 10px;
  padding: 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  min-height: 60px;
  background-color: #f5f7fa;
}

/* 文件名样式优化 */
.original-name {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
  line-height: 1.4;
  word-break: break-all;
}

.preview-filename {
  font-weight: 500;
  color: #303133;
  line-height: 1.4;
  word-break: break-all;
}

/* 双标签筛选区域样式 */
.double-tag-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.quick-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.custom-tags-input {
  width: 100%;
}

/* 主要操作按钮行排列 */
.main-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.main-actions .el-button {
  margin: 0;
  white-space: nowrap;
}

.main-actions .el-dropdown {
  margin-left: 4px;
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

/* 表格容器样式 */
.table-container {
  margin-top: 20px;
  width: 100%;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 表格样式优化 - 确保完全填充 */
.table-container .el-table {
  width: 100% !important;
  table-layout: auto !important;
}

.table-container .el-table__header-wrapper,
.table-container .el-table__body-wrapper {
  width: 100% !important;
}

.table-container .el-table__header,
.table-container .el-table__body {
  width: 100% !important;
  table-layout: auto !important;
}

/* 确保表格在小屏幕上的响应式 */
@media (max-width: 1200px) {
  .table-container {
    overflow-x: auto;
  }
}
</style>

