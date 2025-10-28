<template>
  <div class="label-config-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>标签配置管理</span>
          <el-button type="primary" @click="showAddDialog = true">
            <el-icon><Plus /></el-icon>
            创建配置
          </el-button>
        </div>
      </template>

      <!-- 配置列表 -->
      <el-table 
        :data="configList" 
        style="width: 100%"
        v-loading="loading"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="配置名称" />
        <el-table-column prop="description" label="描述" />
        <el-table-column prop="isActive" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isActive ? 'success' : 'danger'">
              {{ row.isActive ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="imageCount" label="使用图像数" width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" @click="handleView(row)">查看</el-button>
              <el-button size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button 
                size="small" 
                :type="row.isActive ? 'warning' : 'success'"
                @click="handleToggleStatus(row)"
              >
                {{ row.isActive ? '禁用' : '启用' }}
              </el-button>
              <el-button 
                size="small" 
                type="danger" 
                @click="handleDelete(row)"
                :disabled="row.imageCount > 0"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建/编辑配置对话框 -->
    <el-dialog 
      v-model="showAddDialog" 
      :title="editingConfig ? '编辑标签配置' : '创建标签配置'"
      :width="configMode === 'table' ? '1000px' : '800px'"
      :before-close="resetConfigForm"
    >
      <el-form 
        ref="configFormRef"
        :model="configForm" 
        :rules="configRules" 
        label-width="100px"
      >
        <el-form-item label="配置名称" prop="name">
          <el-input v-model="configForm.name" placeholder="请输入配置名称" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input 
            v-model="configForm.description" 
            type="textarea"
            rows="3"
            placeholder="请输入配置描述"
          />
        </el-form-item>
        <!-- 配置模式切换 -->
        <el-form-item label="配置模式">
          <el-radio-group v-model="configMode" @change="handleModeChange">
            <el-radio-button label="table">表格模式</el-radio-button>
            <el-radio-button label="json">JSON模式</el-radio-button>
          </el-radio-group>
          <div class="mode-tip">
            <el-text type="info" size="small">
              {{ configMode === 'table' ? '使用表格界面可视化配置标签' : '直接编辑JSON配置，适合高级用户' }}
            </el-text>
          </div>
        </el-form-item>

        <!-- 表格模式配置 -->
        <div v-if="configMode === 'table'">
          <el-form-item label="配置版本">
            <el-input v-model="tableConfig.version" placeholder="如: 1.0" style="width: 200px" />
          </el-form-item>
          
          <el-form-item label="标签分类">
            <div class="categories-config">
              <div class="categories-header">
                <span>分类列表</span>
                <el-button size="small" type="primary" @click="addCategory">
                  <el-icon><Plus /></el-icon>
                  添加分类
                </el-button>
              </div>
              
              <div v-if="tableConfig.categories.length === 0" class="empty-categories">
                <el-empty description="暂无分类配置，点击上方按钮添加" :image-size="60" />
              </div>
              
              <div v-else class="categories-list">
                <el-card 
                  v-for="(category, index) in tableConfig.categories" 
                  :key="category.id || index"
                  class="category-card"
                  shadow="hover"
                >
                  <template #header>
                    <div class="category-header">
                      <span>分类 {{ index + 1 }}</span>
                      <el-button 
                        size="small" 
                        type="danger" 
                        text 
                        @click="removeCategory(index)"
                      >
                        删除
                      </el-button>
                    </div>
                  </template>
                  
                  <el-row :gutter="16">
                    <el-col :span="12">
                      <el-form-item label="分类ID" required>
                        <el-input 
                          v-model="category.id" 
                          placeholder="如: category_1"
                          @input="syncToJSON"
                        />
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="分类名称" required>
                        <el-input 
                          v-model="category.name" 
                          placeholder="如: 肿瘤类型"
                          @input="syncToJSON"
                        />
                      </el-form-item>
                    </el-col>
                  </el-row>
                  
                  <el-row :gutter="16">
                    <el-col :span="12">
                      <el-form-item label="是否必填">
                        <el-switch 
                          v-model="category.required" 
                          @change="syncToJSON"
                        />
                      </el-form-item>
                    </el-col>
                    <el-col :span="12">
                      <el-form-item label="选择类型">
                        <el-select 
                          v-model="category.type" 
                          @change="syncToJSON"
                          style="width: 100%"
                        >
                          <el-option value="single_choice" label="单选" />
                          <el-option value="multiple_choice" label="多选" />
                          <el-option value="text" label="文本输入" />
                        </el-select>
                      </el-form-item>
                    </el-col>
                  </el-row>
                  
                  <!-- 选项配置（仅当类型为单选或多选时显示） -->
                  <div v-if="category.type === 'single_choice' || category.type === 'multiple_choice'">
                    <el-form-item label="选项配置">
                      <div class="options-config">
                        <div class="options-header">
                          <span>选项列表</span>
                          <el-button 
                            size="small" 
                            type="primary" 
                            text
                            @click="addOption(category)"
                          >
                            <el-icon><Plus /></el-icon>
                            添加选项
                          </el-button>
                        </div>
                        
                        <div v-if="!category.options || category.options.length === 0" class="empty-options">
                          <el-text type="info" size="small">暂无选项，点击上方按钮添加</el-text>
                        </div>
                        
                        <div v-else class="options-list">
                          <div 
                            v-for="(option, optionIndex) in category.options" 
                            :key="optionIndex"
                            class="option-item"
                          >
                            <el-input 
                              v-model="option.value" 
                              placeholder="选项值" 
                              style="width: 200px"
                              @input="syncToJSON"
                            />
                            <el-input 
                              v-model="option.label" 
                              placeholder="选项标签" 
                              style="width: 200px; margin-left: 8px"
                              @input="syncToJSON"
                            />
                            <el-button 
                              type="danger" 
                              text 
                              size="small"
                              @click="removeOption(category, optionIndex)"
                              style="margin-left: 8px"
                            >
                              删除
                            </el-button>
                          </div>
                        </div>
                      </div>
                    </el-form-item>
                  </div>
                </el-card>
              </div>
            </div>
          </el-form-item>
        </div>

        <!-- JSON模式配置 -->
        <div v-if="configMode === 'json'">
          <el-form-item label="配置JSON" prop="config">
            <el-input 
              v-model="configForm.config" 
              type="textarea"
              rows="10"
              placeholder="请输入标签配置JSON"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="info" @click="validateConfig">验证配置</el-button>
            <el-button type="primary" @click="useTemplate">使用模板</el-button>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveConfig">保存</el-button>
      </template>
    </el-dialog>

    <!-- 配置详情对话框 -->
    <el-dialog 
      v-model="showDetailDialog" 
      title="标签配置详情"
      width="600px"
    >
      <div v-if="selectedConfig">
        <h4>配置信息</h4>
        <p><strong>名称：</strong>{{ selectedConfig.name }}</p>
        <p><strong>描述：</strong>{{ selectedConfig.description || '无' }}</p>
        <p><strong>状态：</strong>
          <el-tag :type="selectedConfig.isActive ? 'success' : 'danger'">
            {{ selectedConfig.isActive ? '启用' : '禁用' }}
          </el-tag>
        </p>
        
        <h4>配置内容</h4>
        <pre class="config-json">{{ formatJson(selectedConfig.config) }}</pre>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { labelConfigApi } from '@/api/labelConfig'

const loading = ref(false)
const showAddDialog = ref(false)
const showDetailDialog = ref(false)
const editingConfig = ref(null)
const selectedConfig = ref(null)
const configFormRef = ref()
const configMode = ref('table') // 默认使用表格模式

const configList = ref([])

const configForm = reactive({
  name: '',
  description: '',
  config: ''
})

// 表格模式的配置数据
const tableConfig = reactive({
  version: '1.0',
  categories: []
})

const configRules = reactive({
  name: [
    { required: true, message: '配置名称不能为空', trigger: 'blur' }
  ],
  config: [
    { required: true, message: '配置JSON不能为空', trigger: 'blur' }
  ]
})

// 配置模板
const configTemplate = {
  version: "1.0",
  categories: [
    {
      id: "category_1",
      name: "肿瘤类型",
      required: true,
      type: "single_choice",
      options: [
        { value: "benign", label: "良性肿瘤" },
        { value: "malignant", label: "恶性肿瘤" },
        { value: "borderline", label: "边界性肿瘤" }
      ]
    },
    {
      id: "category_2",
      name: "病理特征",
      required: false,
      type: "multiple_choice",
      options: [
        { value: "inflammation", label: "炎症反应" },
        { value: "fibrosis", label: "纤维化" },
        { value: "necrosis", label: "坏死" }
      ]
    }
  ]
}

const fetchConfigList = async () => {
  try {
    loading.value = true
    const response = await labelConfigApi.getLabelConfigList()
    if (response.success) {
      configList.value = response.data.content || []
    }
  } catch (error) {
    ElMessage.error('获取配置列表失败')
    console.error(error)
  } finally {
    loading.value = false
  }
}

const handleView = (row) => {
  selectedConfig.value = row
  showDetailDialog.value = true
}

const handleEdit = (row) => {
  editingConfig.value = row
  Object.assign(configForm, {
    name: row.name,
    description: row.description || '',
    config: row.config
  })
  
  // 尝试解析为表格数据
  try {
    const parsedConfig = JSON.parse(row.config)
    Object.assign(tableConfig, {
      version: parsedConfig.version || '1.0',
      categories: parsedConfig.categories || []
    })
    configMode.value = 'table' // 默认使用表格模式编辑
  } catch (error) {
    configMode.value = 'json' // 如果解析失败，使用JSON模式
    resetTableConfig()
  }
  
  showAddDialog.value = true
}

const handleToggleStatus = async (row) => {
  const action = row.isActive ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确认${action}配置 ${row.name} 吗？`,
      '确认操作',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const response = row.isActive 
      ? await labelConfigApi.deactivateLabelConfig(row.id)
      : await labelConfigApi.activateLabelConfig(row.id)
      
    if (response.success) {
      ElMessage.success(`配置${action}成功`)
      fetchConfigList()
    }
  } catch (error) {
    console.error(`配置${action}失败:`, error)
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else {
      ElMessage.error(`配置${action}失败`)
    }
  }
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认删除配置 ${row.name} 吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const response = await labelConfigApi.deleteLabelConfig(row.id)
    if (response.success) {
      ElMessage.success('删除成功')
      fetchConfigList()
    }
  } catch (error) {
    console.error('删除配置失败:', error)
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else {
      ElMessage.error('删除配置失败')
    }
  }
}

const validateConfig = () => {
  try {
    JSON.parse(configForm.config)
    ElMessage.success('配置格式正确')
  } catch (error) {
    ElMessage.error('配置格式错误: ' + error.message)
  }
}

const useTemplate = () => {
  configForm.config = JSON.stringify(configTemplate, null, 2)
  
  // 如果当前是表格模式，同时更新表格数据
  if (configMode.value === 'table') {
    Object.assign(tableConfig, {
      version: configTemplate.version,
      categories: JSON.parse(JSON.stringify(configTemplate.categories)) // 深拷贝
    })
  }
  
  ElMessage.success('已填入模板配置')
}

const handleSaveConfig = async () => {
  if (!configFormRef.value) return
  
  try {
    await configFormRef.value.validate()
    
    // 如果是表格模式，先同步数据到JSON
    if (configMode.value === 'table') {
      syncToJSON()
    }
    
    // 验证JSON格式
    const parsedConfig = JSON.parse(configForm.config)
    
    const requestData = {
      name: configForm.name,
      description: configForm.description,
      config: JSON.stringify(parsedConfig),  // 发送JSON字符串而不是对象
      isActive: true
    }
    
    const response = editingConfig.value
      ? await labelConfigApi.updateLabelConfig(editingConfig.value.id, requestData)
      : await labelConfigApi.createLabelConfig(requestData)
    
    if (response.success) {
      ElMessage.success(editingConfig.value ? '更新成功' : '创建成功')
      showAddDialog.value = false
      editingConfig.value = null
      resetConfigForm()
      fetchConfigList()
    }
  } catch (error) {
    if (error instanceof SyntaxError) {
      ElMessage.error('配置JSON格式错误')
    } else {
      console.error('保存配置失败:', error)
      if (error.response?.data?.message) {
        ElMessage.error(error.response.data.message)
      } else {
        ElMessage.error('保存配置失败')
      }
    }
  }
}

const resetConfigForm = () => {
  Object.assign(configForm, {
    name: '',
    description: '',
    config: ''
  })
  resetTableConfig()
  configMode.value = 'table' // 重置为默认模式
}

const formatDate = (dateStr) => {
  return dateStr ? new Date(dateStr).toLocaleString() : ''
}

const formatJson = (jsonStr) => {
  try {
    return JSON.stringify(JSON.parse(jsonStr), null, 2)
  } catch (error) {
    return jsonStr
  }
}

// 表格模式相关方法
const handleModeChange = (mode) => {
  if (mode === 'table') {
    // 切换到表格模式时，尝试将JSON解析为表格数据
    if (configForm.config) {
      try {
        const parsedConfig = JSON.parse(configForm.config)
        Object.assign(tableConfig, {
          version: parsedConfig.version || '1.0',
          categories: parsedConfig.categories || []
        })
      } catch (error) {
        ElMessage.warning('JSON格式不正确，使用默认配置')
        resetTableConfig()
      }
    } else {
      resetTableConfig()
    }
  } else {
    // 切换到JSON模式时，将表格数据同步到JSON
    syncToJSON()
  }
}

const syncToJSON = () => {
  // 将表格数据同步到JSON配置
  const jsonConfig = {
    version: tableConfig.version,
    categories: tableConfig.categories
  }
  configForm.config = JSON.stringify(jsonConfig, null, 2)
}

const addCategory = () => {
  const newCategory = {
    id: `category_${tableConfig.categories.length + 1}`,
    name: '',
    required: true,
    type: 'single_choice',
    options: []
  }
  tableConfig.categories.push(newCategory)
  syncToJSON()
}

const removeCategory = (index) => {
  tableConfig.categories.splice(index, 1)
  syncToJSON()
}

const addOption = (category) => {
  if (!category.options) {
    category.options = []
  }
  category.options.push({
    value: '',
    label: ''
  })
  syncToJSON()
}

const removeOption = (category, optionIndex) => {
  category.options.splice(optionIndex, 1)
  syncToJSON()
}

const resetTableConfig = () => {
  Object.assign(tableConfig, {
    version: '1.0',
    categories: []
  })
}

onMounted(() => {
  fetchConfigList()
})
</script>

<style scoped>
.label-config-management {
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

.config-json {
  background: #f5f5f5;
  padding: 15px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
  max-height: 300px;
  overflow-y: auto;
}

/* 表格模式样式 */
.mode-tip {
  margin-top: 8px;
}

.categories-config {
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 16px;
  background: #fafafa;
}

.categories-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-weight: 600;
  color: #303133;
}

.empty-categories {
  text-align: center;
  padding: 20px;
}

.categories-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.category-card {
  background: white;
  border: 1px solid #e4e7ed;
}

.category-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  color: #303133;
}

.options-config {
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 12px;
  background: #f8f9fa;
}

.options-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-weight: 500;
  color: #606266;
}

.empty-options {
  text-align: center;
  padding: 12px;
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.option-item {
  display: flex;
  align-items: center;
  padding: 8px;
  background: white;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}
</style>
