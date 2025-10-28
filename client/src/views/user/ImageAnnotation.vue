<template>
  <div class="image-annotation">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="title">
            <el-button 
              type="text" 
              @click="$router.back()"
              class="back-btn"
            >
              <el-icon><ArrowLeft /></el-icon>
              返回任务列表
            </el-button>
            <span class="image-title">{{ imageInfo?.originalName }}</span>
          </div>
          <div class="actions">
            <el-button @click="handleSaveDraft" :loading="saving">
              <el-icon><DocumentCopy /></el-icon>
              保存草稿 (Ctrl+S)
            </el-button>
            <el-button 
              type="primary"
              @click="handleSubmit"
              :loading="submitting"
              :disabled="!canSubmit"
            >
              <el-icon><Check /></el-icon>
              {{ hasExistingAnnotation ? '更新标注 (Ctrl+Enter)' : '提交标注 (Ctrl+Enter)' }}
            </el-button>
            <div class="auto-save-status" v-if="autoSaveEnabled">
              <el-icon v-if="autoSaving" class="rotating"><Loading /></el-icon>
              <span :class="{ 'text-success': lastAutoSaveSuccess, 'text-danger': !lastAutoSaveSuccess }">
                {{ autoSaveStatus }}
              </span>
            </div>
          </div>
        </div>
      </template>

      <div class="annotation-content">
        <div class="image-section">
          <div class="image-container">
            <div 
              class="image-wrapper clickable-container" 
              v-if="imageInfo"
              @click="handleImageClick"
              @touchend.prevent="handleImageClick"
              title="点击查看大图"
            >
              <img 
                :src="imageInfo.url" 
                :alt="imageInfo.originalName"
                ref="imageRef"
                @load="handleImageLoad"
              />
              <div class="fullscreen-hint">
                <el-icon><ZoomIn /></el-icon>
                <span>点击查看大图</span>
              </div>
            </div>
            
            <div class="image-tools" @click.stop>
              <el-button-group>
                <el-button @click="zoomIn">
                  <el-icon><ZoomIn /></el-icon>
                </el-button>
                <el-button @click="zoomOut">
                  <el-icon><ZoomOut /></el-icon>
                </el-button>
                <el-button @click="resetZoom">
                  <el-icon><Refresh /></el-icon>
                </el-button>
              </el-button-group>
              <span class="zoom-level">{{ Math.round(zoomLevel * 100) }}%</span>
            </div>
          </div>
        </div>

        <div class="annotation-panel">
          <div class="panel-header">
            <h3>标注面板</h3>
            <div class="progress-info">
              <span>进度: {{ annotationProgress }}%</span>
              <el-progress 
                :percentage="annotationProgress" 
                :stroke-width="6"
                :show-text="false"
              />
            </div>
            <div class="quick-actions">
              <el-button size="small" @click="clearAllAnnotations">清空标注</el-button>
              <el-button size="small" type="success" @click="autoFillTestData">填充测试</el-button>
            </div>
            
            <!-- 操作提示 -->
            <div class="help-tips" v-if="showHelpTips">
              <el-alert
                title="快速上手"
                type="info"
                :closable="true"
                @close="showHelpTips = false"
              >
                <template #default>
                  <div class="tips-content">
                    <p><strong>快捷键:</strong></p>
                    <ul>
                      <li>Ctrl+S: 保存草稿</li>
                      <li>Ctrl+Enter: {{ hasExistingAnnotation ? '更新标注' : '提交标注' }}</li>
                      <li>ESC: 退出文本编辑</li>
                    </ul>
                    <p><strong>自动保存:</strong> 修改后3秒自动保存草稿</p>
                  </div>
                </template>
              </el-alert>
            </div>
          </div>

          <!-- 可滚动的标签区域 -->
          <div class="label-categories-wrapper">
            <div class="label-categories" v-if="labelConfig">
              <div 
                v-for="category in labelConfig.categories" 
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
                v-model="annotations[category.id]"
                class="option-group"
              >
                <el-radio 
                  v-for="option in category.options"
                  :key="option.value"
                  :value="option.value"
                  class="option-item"
                  @click.native="handleRadioClick(category.id, option.value, $event)"
                >
                  {{ option.label }}
                </el-radio>
              </el-radio-group>

              <!-- 多选类型 -->
              <el-checkbox-group 
                v-else-if="category.type === 'multiple_choice'"
                v-model="annotations[category.id]"
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
                v-model="annotations[category.id]"
                type="textarea"
                :placeholder="`请输入${category.name}`"
                :rows="3"
              />
            </div>
          </div>
          </div>

          <!-- 标注摘要区域（固定在底部） -->
          <div class="annotation-summary">
            <h4>标注摘要</h4>
            <div class="summary-content">
              <div 
                v-for="(value, key) in annotations" 
                :key="key"
                class="summary-item"
                v-if="value && value !== ''"
              >
                <span class="label">{{ getCategoryName(key) }}:</span>
                <span class="value">{{ formatAnnotationValue(key, value) }}</span>
              </div>
              <div v-if="Object.keys(getValidAnnotations()).length === 0" class="empty-summary">
                暂无标注内容
              </div>
            </div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 图片全屏预览对话框 -->
    <el-dialog 
      v-model="imagePreviewVisible" 
      :fullscreen="true"
      :show-close="true"
      class="image-preview-dialog"
      append-to-body
    >
      <div 
        class="preview-container" 
        @click="imagePreviewVisible = false"
        @touchend.prevent="imagePreviewVisible = false"
      >
        <img 
          :src="imageInfo?.url" 
          :alt="imageInfo?.originalName"
          class="preview-image"
          @click.stop
          @touchend.stop
        />
        <div class="preview-hint">点击任意处关闭</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  DocumentCopy,
  Check,
  ZoomIn,
  ZoomOut,
  Refresh,
  Loading
} from '@element-plus/icons-vue'
import { userTaskApi } from '@/api/userTask'

const route = useRoute()
const router = useRouter()

const imageRef = ref()
const saving = ref(false)
const submitting = ref(false)
const zoomLevel = ref(1)

// 自动保存相关状态
const autoSaveEnabled = ref(true)
const autoSaving = ref(false)
const lastAutoSaveSuccess = ref(true)
const autoSaveStatus = ref('已保存')
const autoSaveTimer = ref(null)
const hasUnsavedChanges = ref(false)

// UI状态
const showHelpTips = ref(true)
const imagePreviewVisible = ref(false)

const imageInfo = ref(null)
const labelConfig = ref(null)
const hasExistingAnnotation = ref(false) // 标识是否有已有标注
const annotations = reactive({})
const annotationRemark = ref('') // 标注备注

const annotationProgress = computed(() => {
  if (!labelConfig.value) return 0
  
  const totalRequired = labelConfig.value.categories.filter(cat => cat.required).length
  const completedRequired = labelConfig.value.categories
    .filter(cat => cat.required)
    .filter(cat => annotations[cat.id] && annotations[cat.id] !== '').length
  
  if (totalRequired === 0) return 100
  return Math.round((completedRequired / totalRequired) * 100)
})

// 是否可以提交
const canSubmit = computed(() => {
  return annotationProgress.value === 100
})

// 监听标注变化，触发自动保存
const triggerAutoSave = () => {
  if (!autoSaveEnabled.value) return
  
  hasUnsavedChanges.value = true
  autoSaveStatus.value = '有未保存更改'
  
  // 清除之前的计时器
  if (autoSaveTimer.value) {
    clearTimeout(autoSaveTimer.value)
  }
  
  // 3秒后自动保存
  autoSaveTimer.value = setTimeout(() => {
    performAutoSave()
  }, 3000)
}

// 执行自动保存
const performAutoSave = async () => {
  if (!hasUnsavedChanges.value || autoSaving.value) return
  
  try {
    autoSaving.value = true
    autoSaveStatus.value = '保存中...'
    
    const annotationData = {
      imageId: imageInfo.value.id,
      labels: annotations,
      status: 'DRAFT',
      remark: annotationRemark.value || null
    }
    
    const response = await userTaskApi.saveAnnotation(annotationData)
    
    if (response.success) {
      hasUnsavedChanges.value = false
      lastAutoSaveSuccess.value = true
      autoSaveStatus.value = `已保存 ${new Date().toLocaleTimeString()}`
    } else {
      throw new Error(response.message || '保存失败')
    }
  } catch (error) {
    lastAutoSaveSuccess.value = false
    autoSaveStatus.value = '保存失败'
    console.error('自动保存失败:', error)
  } finally {
    autoSaving.value = false
  }
}

const loadImageAndConfig = async () => {
  try {
    const imageId = route.params.id
    
    // 重置状态
    hasExistingAnnotation.value = false
    
    // 获取图像信息
    const imageResponse = await userTaskApi.getImageDetail(imageId)
    if (imageResponse.success) {
      imageInfo.value = imageResponse.data
      console.log('获取到的图像信息:', imageInfo.value)
    }
    
    // 初始化annotations对象
    if (imageInfo.value?.labelConfig?.config) {
      console.log('原始标签配置:', imageInfo.value.labelConfig.config)
      
      // 解析标签配置JSON
      const configObj = typeof imageInfo.value.labelConfig.config === 'string' 
        ? JSON.parse(imageInfo.value.labelConfig.config)
        : imageInfo.value.labelConfig.config

      labelConfig.value = configObj
      console.log('解析后的标签配置:', labelConfig.value)
      
      if (labelConfig.value?.categories) {
        labelConfig.value.categories.forEach(category => {
          if (category.type === 'multiple_choice') {
            annotations[category.id] = []
          } else {
            annotations[category.id] = ''
          }
        })
        console.log('初始化的标注对象:', annotations)
      }
      
      // 加载已有的标注数据（包括备注）
      await loadExistingAnnotationData(imageId)
    } else {
      console.warn('图像没有关联的标签配置')
      if (imageInfo.value) {
        console.log('图像信息:', {
          id: imageInfo.value.id,
          labelConfigId: imageInfo.value.labelConfigId,
          labelConfigName: imageInfo.value.labelConfigName,
          hasLabelConfig: !!imageInfo.value.labelConfig
        })
      }
    }
    
  } catch (error) {
    ElMessage.error('加载数据失败')
    console.error(error)
  }
}

const handleImageLoad = () => {
  console.log('图像加载完成')
}

// 处理图片点击 - 全屏预览
const handleImageClick = (event) => {
  console.log('图片容器被点击/触摸', event?.type)
  if (event) {
    event.stopPropagation()
  }
  imagePreviewVisible.value = true
  console.log('全屏预览已打开:', imagePreviewVisible.value)
}

// 加载已有标注数据（包括备注）
const loadExistingAnnotationData = async (imageId) => {
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
      
      // 标识存在已有标注
      hasExistingAnnotation.value = true
      
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
      } else {
        annotationRemark.value = ''
        console.log('📝 该标注暂无备注信息')
      }
      
      console.log('💾 已有标注数据已加载并填充到表单')
      
    } else {
      console.log('ℹ️ 该图片暂无已有标注数据')
      // 确保备注也被重置
      annotationRemark.value = ''
      // 重置已有标注标识
      hasExistingAnnotation.value = false
    }
    
  } catch (error) {
    console.warn('⚠️ 加载已有标注数据失败:', error.message)
    // 失败时重置备注和已有标注标识
    annotationRemark.value = ''
    hasExistingAnnotation.value = false
  }
}

const zoomIn = () => {
  zoomLevel.value = Math.min(zoomLevel.value + 0.2, 3)
  updateImageZoom()
}

const zoomOut = () => {
  zoomLevel.value = Math.max(zoomLevel.value - 0.2, 0.2)
  updateImageZoom()
}

const resetZoom = () => {
  zoomLevel.value = 1
  updateImageZoom()
}

const updateImageZoom = () => {
  if (imageRef.value) {
    imageRef.value.style.transform = `scale(${zoomLevel.value})`
  }
}

const handleSaveDraft = async () => {
  try {
    saving.value = true
    
    const annotationData = {
      imageId: imageInfo.value.id,
      labels: annotations,
      status: 'DRAFT',
      remark: annotationRemark.value || null
    }
    
    const response = await userTaskApi.saveAnnotation(annotationData)
    if (response.success) {
      ElMessage.success('草稿保存成功')
    }
  } catch (error) {
    console.error('保存草稿失败:', error)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

const handleSubmit = async () => {
  // 验证必填项
  if (!validateRequiredFields()) {
    // 获取未填写的必选项
    const missingFields = getMissingRequiredFields()
    
    // 更明显的错误提示
    ElMessageBox.alert(
      `请完成以下必填项的标注：\n${missingFields.join('\n')}`,
      '标注未完成',
      {
        confirmButtonText: '我知道了',
        type: 'warning',
        center: true
      }
    )
    return
  }
  
  try {
    const isUpdate = hasExistingAnnotation.value
    await ElMessageBox.confirm(
      isUpdate 
        ? '确认更新标注结果吗？' 
        : '确认提交标注结果吗？提交后将无法修改。',
      isUpdate ? '更新确认' : '提交确认',
      {
        confirmButtonText: isUpdate ? '确定更新' : '确定提交',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    submitting.value = true
    
    const annotationData = {
      imageId: imageInfo.value.id,
      labels: annotations,
      status: 'COMPLETED',
      remark: annotationRemark.value || null
    }
    
    const response = await userTaskApi.saveAnnotation(annotationData)
    if (response.success) {
      ElMessage.success(isUpdate ? '标注更新成功' : '标注提交成功')
      router.push('/user/tasks')
    }
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('提交标注失败:', error)
      ElMessage.error('提交失败')
    }
  } finally {
    submitting.value = false
  }
}

const validateRequiredFields = () => {
  if (!labelConfig.value) return false
  
  return labelConfig.value.categories
    .filter(cat => cat.required)
    .every(cat => {
      const value = annotations[cat.id]
      if (cat.type === 'multiple_choice') {
        return Array.isArray(value) && value.length > 0
      }
      return value && value !== ''
    })
}

// 获取未填写的必选项列表
const getMissingRequiredFields = () => {
  if (!labelConfig.value) return []
  
  return labelConfig.value.categories
    .filter(cat => cat.required)
    .filter(cat => {
      const value = annotations[cat.id]
      if (cat.type === 'multiple_choice') {
        return !Array.isArray(value) || value.length === 0
      }
      return !value || value === ''
    })
    .map(cat => `• ${cat.name}`)
}

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

const getCategoryTypeText = (type) => {
  const typeMap = {
    'single_choice': '单选',
    'multiple_choice': '多选',
    'text': '文本'
  }
  return typeMap[type] || type
}

const getCategoryName = (categoryId) => {
  if (!labelConfig.value) return categoryId
  const category = labelConfig.value.categories.find(cat => cat.id === categoryId)
  return category ? category.name : categoryId
}

const formatAnnotationValue = (categoryId, value) => {
  if (!labelConfig.value) return value
  
  const category = labelConfig.value.categories.find(cat => cat.id === categoryId)
  if (!category) return value
  
  if (category.type === 'multiple_choice' && Array.isArray(value)) {
    return value.map(v => {
      const option = category.options.find(opt => opt.value === v)
      return option ? option.label : v
    }).join(', ')
  }
  
  if (category.type === 'single_choice') {
    const option = category.options.find(opt => opt.value === value)
    return option ? option.label : value
  }
  
  return value
}

// 获取有效的标注（不为空的标注）
const getValidAnnotations = () => {
  const validAnnotations = {}
  Object.keys(annotations).forEach(key => {
    const value = annotations[key]
    if (value && value !== '' && !(Array.isArray(value) && value.length === 0)) {
      validAnnotations[key] = value
    }
  })
  return validAnnotations
}

// 清空所有标注
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

// 自动填充测试数据
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

// 监听标注变化
watch(annotations, () => {
  triggerAutoSave()
}, { deep: true })

// 监听备注变化，也触发自动保存
watch(annotationRemark, () => {
  triggerAutoSave()
})

// 键盘快捷键处理
const handleKeydown = (event) => {
  // Ctrl+S 保存草稿
  if (event.ctrlKey && event.key === 's') {
    event.preventDefault()
    handleSaveDraft()
  }
  
  // Ctrl+Enter 提交标注
  if (event.ctrlKey && event.key === 'Enter') {
    event.preventDefault()
    if (canSubmit.value) {
      handleSubmit()
    }
  }
  
  // Esc 清空当前标注项（如果正在编辑文本框）
  if (event.key === 'Escape') {
    const activeElement = document.activeElement
    if (activeElement && activeElement.tagName === 'TEXTAREA') {
      activeElement.blur()
    }
  }
}

// 页面离开前提醒
const handleBeforeUnload = (event) => {
  if (hasUnsavedChanges.value) {
    const message = '您有未保存的更改，确定要离开页面吗？'
    event.returnValue = message
    return message
  }
}

onMounted(() => {
  loadImageAndConfig()
  
  // 添加键盘事件监听
  document.addEventListener('keydown', handleKeydown)
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onUnmounted(() => {
  // 清理定时器和事件监听
  if (autoSaveTimer.value) {
    clearTimeout(autoSaveTimer.value)
  }
  
  document.removeEventListener('keydown', handleKeydown)
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>

<style scoped>
.image-annotation {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  display: flex;
  align-items: center;
}

.back-btn {
  margin-right: 15px;
  color: #666;
}

.image-title {
  font-weight: bold;
  font-size: 16px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 15px;
}

.auto-save-status {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  padding: 5px 10px;
  background: #f8f9fa;
  border-radius: 4px;
  border: 1px solid #e9ecef;
}

.text-success {
  color: #28a745;
}

.text-danger {
  color: #dc3545;
}

.rotating {
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.annotation-content {
  display: flex;
  height: calc(100vh - 200px);
  gap: 20px;
}

.image-section {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.image-container {
  flex: 1;
  position: relative;
  border: 1px solid #ddd;
  border-radius: 8px;
  overflow: auto;
  background: #f9f9f9;
}

.image-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100%;
  padding: 20px;
  position: relative;
  z-index: 1;
}

.image-wrapper.clickable-container {
  cursor: pointer;
  transition: background-color 0.2s ease;
  -webkit-tap-highlight-color: rgba(0, 0, 0, 0.1);
  touch-action: manipulation;
  user-select: none;
  -webkit-user-select: none;
}

.image-wrapper.clickable-container:hover {
  background-color: rgba(0, 0, 0, 0.02);
}

.image-wrapper.clickable-container:active {
  background-color: rgba(0, 0, 0, 0.05);
}

.fullscreen-hint {
  position: absolute;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  border-radius: 25px;
  font-size: 14px;
  opacity: 0;
  transition: opacity 0.3s ease;
  pointer-events: none;
  z-index: 2;
}

.image-wrapper.clickable-container:hover .fullscreen-hint {
  opacity: 1;
}

.image-wrapper img {
  width: 100%;
  max-width: 100%;
  height: auto;
  max-height: 100%;
  object-fit: contain;
  transition: all 0.3s ease;
  pointer-events: none; /* 让图片本身不响应点击，只由容器响应 */
}

.image-wrapper.clickable-container img {
  opacity: 1;
  transition: opacity 0.3s ease;
}

.image-wrapper.clickable-container:hover img {
  opacity: 0.9;
}

.image-tools {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(255, 255, 255, 0.9);
  padding: 8px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 10px;
  z-index: 10;
  pointer-events: auto;
}

.zoom-level {
  font-size: 12px;
  color: #666;
  min-width: 40px;
}

.annotation-panel {
  width: 400px;
  border-left: 1px solid #eee;
  padding-left: 20px;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 200px);
}

.panel-header {
  margin-bottom: 20px;
  flex-shrink: 0;
}

.quick-actions {
  margin-top: 15px;
  display: flex;
  gap: 10px;
}

.label-categories-wrapper {
  flex: 1;
  overflow-y: auto;
  padding-right: 5px;
  margin-right: -5px;
}

.label-categories {
  padding-bottom: 20px;
}

.panel-header h3 {
  margin: 0 0 10px 0;
  color: #333;
}

.progress-info {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #666;
}

.progress-info .el-progress {
  flex: 1;
}

.category-item {
  margin-bottom: 25px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.category-item:last-child {
  border-bottom: none;
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

.annotation-summary {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 2px solid #eee;
  flex-shrink: 0;
  max-height: 200px;
  overflow-y: auto;
}

.annotation-summary h4 {
  margin: 0 0 15px 0;
  color: #333;
}

.summary-content {
  font-size: 14px;
}

.summary-item {
  margin-bottom: 8px;
  display: flex;
  align-items: flex-start;
}

.summary-item .label {
  font-weight: bold;
  color: #666;
  width: 100px;
  flex-shrink: 0;
}

.summary-item .value {
  flex: 1;
  color: #333;
  word-break: break-word;
}

.empty-summary {
  color: #999;
  text-align: center;
  padding: 20px;
}

/* 自定义滚动条样式 */
.label-categories-wrapper::-webkit-scrollbar,
.annotation-summary::-webkit-scrollbar {
  width: 6px;
}

.label-categories-wrapper::-webkit-scrollbar-track,
.annotation-summary::-webkit-scrollbar-track {
  background: #f5f5f5;
  border-radius: 3px;
}

.label-categories-wrapper::-webkit-scrollbar-thumb,
.annotation-summary::-webkit-scrollbar-thumb {
  background: #c0c0c0;
  border-radius: 3px;
}

.label-categories-wrapper::-webkit-scrollbar-thumb:hover,
.annotation-summary::-webkit-scrollbar-thumb:hover {
  background: #a0a0a0;
}

/* 优化标注项间距 */
.category-item {
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
  transition: background-color 0.2s ease;
}

.category-item:hover {
  background-color: #fafafa;
  border-radius: 6px;
  padding: 15px;
  margin-left: -15px;
  margin-right: -15px;
}

.category-item:last-child {
  border-bottom: none;
  margin-bottom: 0;
}

/* 快捷操作按钮样式 */
.quick-actions .el-button {
  flex: 1;
  font-size: 12px;
}

/* 帮助提示样式 */
.help-tips {
  margin-top: 15px;
}

.tips-content {
  font-size: 13px;
}

.tips-content p {
  margin: 8px 0;
}

.tips-content ul {
  margin: 5px 0;
  padding-left: 20px;
}

.tips-content li {
  margin: 2px 0;
}

/* 进度信息样式优化 */
.progress-info {
  padding: 10px;
  background: #f8f9fa;
  border-radius: 6px;
  margin-top: 10px;
}

/* 标注摘要内容优化 */
.summary-content {
  max-height: 150px;
  overflow-y: auto;
}

/* 响应式优化 */
@media (max-width: 1200px) {
  .annotation-panel {
    width: 350px;
  }
}

@media (max-width: 768px) {
  .annotation-content {
    flex-direction: column;
    height: auto;
  }
  
  .annotation-panel {
    width: 100%;
    border-left: none;
    border-top: 1px solid #eee;
    padding-left: 0;
    padding-top: 20px;
    margin-top: 20px;
    height: 400px;
  }
}

/* 图片全屏预览样式 */
.image-preview-dialog {
  background: rgba(0, 0, 0, 0.95);
}

.image-preview-dialog :deep(.el-dialog__header) {
  display: none;
}

.image-preview-dialog :deep(.el-dialog__body) {
  padding: 0;
  height: 100vh;
  width: 100vw;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.image-preview-dialog :deep(.el-overlay) {
  background: rgba(0, 0, 0, 0.95);
}

.preview-container {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  cursor: pointer;
}

.preview-image {
  max-width: 95%;
  max-height: 95vh;
  object-fit: contain;
  cursor: default;
  user-select: none;
}

.preview-hint {
  position: absolute;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  background: rgba(0, 0, 0, 0.5);
  padding: 8px 20px;
  border-radius: 20px;
  pointer-events: none;
}

/* 移动端优化 */
@media (max-width: 768px) {
  .image-wrapper.clickable-container {
    /* 确保移动端点击区域足够大 */
    min-height: 300px;
  }
  
  .preview-image {
    max-width: 100%;
    max-height: 100vh;
  }
  
  .preview-hint {
    bottom: 20px;
    font-size: 14px;
    padding: 8px 18px;
  }
  
  /* 移动端始终显示全屏提示 */
  .fullscreen-hint {
    opacity: 0.85;
    font-size: 13px;
    animation: fadeInOut 3s ease-in-out infinite;
  }
  
  @keyframes fadeInOut {
    0%, 100% { opacity: 0.7; }
    50% { opacity: 1; }
  }
  
  /* 确保全屏对话框在移动端正常显示 */
  .image-preview-dialog :deep(.el-dialog__body) {
    height: 100vh !important;
    width: 100vw !important;
  }
}
</style>
