<template>
  <div class="image-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>图像管理</span>
          <div>
            <el-button type="primary" @click="showUploadDialog = true">
              <el-icon><Upload /></el-icon>
              上传图像
            </el-button>
            <el-button 
              type="info" 
              @click="handleGlobalCrop"
            >
              <el-icon><Setting /></el-icon>
              全局裁剪设置
            </el-button>
            <el-button 
              type="success" 
              @click="handleBatchAssign"
              :disabled="selectedImages.length === 0"
            >
              <el-icon><Collection /></el-icon>
              批量分配 ({{ selectedImages.length }})
            </el-button>
            <el-button 
              type="warning" 
              @click="handleAssignAll"
            >
              <el-icon><Collection /></el-icon>
              全量分配
            </el-button>
            <el-button 
              type="danger" 
              @click="handleBatchRevoke"
              :disabled="selectedAssignedImages.length === 0"
            >
              <el-icon><Remove /></el-icon>
              批量收回 ({{ selectedAssignedImages.length }})
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选区域 -->
      <div class="search-section">
        <el-card class="filter-card" shadow="hover">
          <template #header>
            <div class="filter-header">
              <span class="filter-title">
                <el-icon><Search /></el-icon>
                图像筛选
              </span>
              <el-button 
                type="primary" 
                size="small" 
                @click="toggleFilterExpand"
              >
                {{ filterExpanded ? '收起筛选' : '展开筛选' }}
              </el-button>
            </div>
          </template>
          
          <el-form :model="searchForm" label-width="100px">
            <!-- 基础筛选（始终显示） -->
            <el-row :gutter="20">
              <el-col :xs="24" :sm="12" :md="8" :lg="6">
                <el-form-item label="ID">
                  <el-input 
                    v-model="searchForm.id" 
                    placeholder="请输入图像ID"
                    clearable
                    type="number"
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12" :md="8" :lg="6">
                <el-form-item label="关键字">
                  <el-input 
                    v-model="searchForm.keyword" 
                    placeholder="搜索文件名"
                    clearable
                  />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12" :md="8" :lg="6">
                <el-form-item label="分配状态">
                  <el-select 
                    v-model="searchForm.isAssigned" 
                    placeholder="请选择分配状态"
                    clearable
                  >
                    <el-option label="全部" :value="null" />
                    <el-option label="已分配" :value="true" />
                    <el-option label="未分配" :value="false" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12" :md="8" :lg="6">
                <el-form-item>
                  <el-button type="primary" @click="handleSearch">
                    <el-icon><Search /></el-icon>
                    搜索
                  </el-button>
                  <el-button @click="handleReset">
                    <el-icon><RefreshLeft /></el-icon>
                    重置
                  </el-button>
                </el-form-item>
              </el-col>
            </el-row>

            <!-- 高级筛选（可展开/收起） -->
            <el-collapse-transition>
              <div v-show="filterExpanded" class="advanced-filters">
                <el-divider content-position="left">高级筛选</el-divider>
                <el-row :gutter="20">
                  <el-col :xs="24" :sm="12" :md="8" :lg="6">
                    <el-form-item label="原始文件名">
                      <el-input 
                        v-model="searchForm.originalName" 
                        placeholder="请输入原始文件名"
                        clearable
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12" :md="8" :lg="6">
                    <el-form-item label="标签配置">
                      <el-select 
                        v-model="searchForm.labelConfigId" 
                        placeholder="请选择标签配置"
                        clearable
                      >
                        <el-option 
                          v-for="config in labelConfigs" 
                          :key="config.id"
                          :label="config.name" 
                          :value="config.id" 
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12" :md="8" :lg="6">
                    <el-form-item label="分配用户">
                      <el-select 
                        v-model="searchForm.assignedUserId" 
                        placeholder="请选择分配用户"
                        clearable
                        filterable
                      >
                        <el-option 
                          v-for="user in userList" 
                          :key="user.id"
                          :label="user.username" 
                          :value="user.id" 
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :xs="24" :sm="12" :md="8" :lg="6">
                    <el-form-item label="用户名">
                      <el-input 
                        v-model="searchForm.assignedUsername" 
                        placeholder="请输入用户名"
                        clearable
                      />
                    </el-form-item>
                  </el-col>
                </el-row>
              </div>
            </el-collapse-transition>
          </el-form>
        </el-card>
      </div>

      <!-- 字段显示控制区域 -->
      <div class="column-control-section">
        <el-card class="control-card" shadow="hover">
          <template #header>
            <div class="control-header">
              <span class="control-title">
                <el-icon><Setting /></el-icon>
                字段显示控制
              </span>
              <el-button 
                type="primary" 
                size="small" 
                @click="activeCollapseNames.includes('columnControl') ? activeCollapseNames = [] : activeCollapseNames = ['columnControl']"
              >
                {{ activeCollapseNames.includes('columnControl') ? '收起' : '展开' }}
              </el-button>
            </div>
          </template>
          
          <el-collapse v-model="activeCollapseNames" class="control-collapse">
            <el-collapse-item name="columnControl">
              <div class="column-control-content">
                <div class="control-group">
                  <h4>
                    <el-icon><Document /></el-icon>
                    基础字段
                  </h4>
                  <el-checkbox-group v-model="visibleColumns.basic" class="checkbox-grid">
                    <el-checkbox label="preview">预览</el-checkbox>
                    <el-checkbox label="filename">文件名</el-checkbox>
                    <el-checkbox label="uploader">上传者</el-checkbox>
                    <el-checkbox label="labelConfig">标签模板</el-checkbox>
                    <el-checkbox label="status">状态</el-checkbox>
                  </el-checkbox-group>
                </div>
                
                <div class="control-group">
                  <h4>
                    <el-icon><DataAnalysis /></el-icon>
                    CSV数据字段
                    <el-tag size="small" type="info">{{ visibleColumns.csv.length }}/10</el-tag>
                  </h4>
                  <el-checkbox-group v-model="visibleColumns.csv" class="checkbox-grid">
                    <el-checkbox label="folder">文件夹</el-checkbox>
                    <el-checkbox label="reid">重命名ID</el-checkbox>
                    <el-checkbox label="imageClass">分类标签</el-checkbox>
                    <el-checkbox label="firstTrainLabel">第一次训练标签</el-checkbox>
                    <el-checkbox label="testLabel">测试标签</el-checkbox>
                    <el-checkbox label="model1">模型1</el-checkbox>
                    <el-checkbox label="pasConf">PAS置信度</el-checkbox>
                    <el-checkbox label="ifLabel">IF标签</el-checkbox>
                    <el-checkbox label="predictLabel">预测标签</el-checkbox>
                    <el-checkbox label="predictConfidence">预测置信度</el-checkbox>
                  </el-checkbox-group>
                </div>
                
                <div class="control-actions">
                  <el-button type="primary" size="small" @click="showAllColumns">
                    <el-icon><View /></el-icon>
                    显示全部
                  </el-button>
                  <el-button type="warning" size="small" @click="hideAllCsvColumns">
                    <el-icon><Hide /></el-icon>
                    隐藏CSV字段
                  </el-button>
                  <el-button size="small" @click="resetColumnSettings">
                    <el-icon><RefreshLeft /></el-icon>
                    重置
                  </el-button>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </el-card>
      </div>

      <!-- 操作按钮区域 -->
      <div class="action-buttons" v-if="selectedImages.length > 0">
        <el-button 
          type="primary" 
          @click="handleBatchSetLabelConfig"
          :disabled="selectedImages.length === 0"
        >
          <el-icon><Setting /></el-icon>
          批量设置模板 ({{ selectedImages.length }})
        </el-button>
        <el-button 
          type="danger" 
          @click="handleBatchDelete"
          :disabled="selectedImages.length === 0"
        >
          <el-icon><Delete /></el-icon>
          批量删除 ({{ selectedImages.length }})
        </el-button>
      </div>

      <!-- 图像表格 -->
      <el-table 
        ref="tableRef"
        :data="imageList" 
        v-loading="loading"
        stripe
        border
        @selection-change="handleSelectionChange"
        :row-class-name="getRowClassName"
        style="width: 100%"
        class="drag-select-table"
      >
        <el-table-column type="selection" width="55" />
        
        <el-table-column label="预览" width="100" align="center" v-if="visibleColumns.basic.includes('preview')">
          <template #default="{ row }">
            <div class="image-preview">
              <img :src="row.url" :alt="row.originalName" @click="handleView(row)" />
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="originalName" label="文件名" min-width="200" v-if="visibleColumns.basic.includes('filename')">
          <template #default="{ row }">
            <div class="file-info">
              <div class="file-name" :title="row.originalName">{{ row.originalName }}</div>
              <div class="file-meta">
                {{ formatFileSize(row.fileSize) }} • {{ formatDate(row.createdAt) }}
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="uploaderUsername" label="上传者" width="120" v-if="visibleColumns.basic.includes('uploader')" />
        
        <el-table-column label="标签模板" width="200" v-if="visibleColumns.basic.includes('labelConfig')">
          <template #default="{ row }">
            <div class="label-config-cell">
              <div v-if="row.labelConfigName" class="label-config-name">
                {{ row.labelConfigName }}
              </div>
              <div v-else class="no-label-config">
                未设置
              </div>
              <el-button 
                size="small" 
                type="primary" 
                link 
                @click="handleSetLabelConfig(row)"
              >
                {{ row.labelConfigName ? '修改' : '设置' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column label="状态" width="200" v-if="visibleColumns.basic.includes('status')">
          <template #default="{ row }">
            <div class="status-tags">
              <div v-if="row.isAssigned" class="assigned-status">
                <el-tag type="success" size="small" class="status-tag">
                  已分配
                </el-tag>
                <el-tooltip 
                  v-if="row.assignedUsers && row.assignedUsers.length > 0"
                  :content="`分配给: ${row.assignedUsers.join(', ')}`"
                  placement="top"
                  effect="dark"
                >
                  <div class="assigned-users">
                    <el-tag 
                      v-for="user in row.assignedUsers.slice(0, 2)" 
                      :key="user"
                      type="info" 
                      size="small"
                      class="user-tag"
                    >
                      {{ user }}
                    </el-tag>
                    <el-tag 
                      v-if="row.assignedUsers.length > 2"
                      type="info" 
                      size="small"
                      class="user-tag"
                    >
                      +{{ row.assignedUsers.length - 2 }}
                    </el-tag>
                  </div>
                </el-tooltip>
              </div>
              <el-tag v-if="row.isAnnotated" type="primary" size="small" class="status-tag">
                已标注
              </el-tag>
            </div>
          </template>
        </el-table-column>

        <!-- CSV相关字段列 -->
        <el-table-column prop="folder" label="文件夹" width="120" v-if="visibleColumns.csv.includes('folder')" />
        
        <el-table-column prop="reid" label="重命名ID" width="200" v-if="visibleColumns.csv.includes('reid')" show-overflow-tooltip />
        
        <el-table-column prop="imageClass" label="分类标签" width="300" v-if="visibleColumns.csv.includes('imageClass')" show-overflow-tooltip />
        
        <el-table-column prop="firstTrainLabel" label="第一次训练标签" width="120" v-if="visibleColumns.csv.includes('firstTrainLabel')" />
        
        <el-table-column prop="testLabel" label="测试标签" width="120" v-if="visibleColumns.csv.includes('testLabel')" />
        
        <el-table-column prop="model1" label="模型1" width="120" v-if="visibleColumns.csv.includes('model1')" />
        
        <el-table-column prop="pasConf" label="PAS置信度" width="120" v-if="visibleColumns.csv.includes('pasConf')">
          <template #default="{ row }">
            <span v-if="row.pasConf">{{ row.pasConf.toFixed(4) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="ifLabel" label="IF标签" width="120" v-if="visibleColumns.csv.includes('ifLabel')" />
        
        <el-table-column prop="predictLabel" label="预测标签" width="120" v-if="visibleColumns.csv.includes('predictLabel')" />
        
        <el-table-column prop="predictConfidence" label="预测置信度" width="120" v-if="visibleColumns.csv.includes('predictConfidence')">
          <template #default="{ row }">
            <span v-if="row.predictConfidence">{{ row.predictConfidence.toFixed(4) }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" @click="handleView(row)">查看</el-button>
              <el-button 
                size="small" 
                :type="row.isAssigned ? 'success' : 'primary'"
                @click="handleAssign(row)"
              >
                {{ row.isAssigned ? '追加分配' : '分配' }}
              </el-button>
              <el-button 
                v-if="row.isAssigned"
                size="small"
                type="warning"
                @click="handleRevoke(row)"
              >
                收回
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
      <div class="pagination-section" v-if="!pagination.isShowAll">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :total="pagination.total"
          :page-sizes="[100, 500, 1000, 999999]"
          :pager-count="7"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
          background
        />
        <!-- 单独的全量显示按钮 -->
        <el-button 
          size="small" 
          type="warning"
          @click="showAllRecords"
          class="show-all-btn"
        >
          全量显示
        </el-button>
      </div>
      
      <!-- 全量显示时的信息 -->
      <div class="pagination-section" v-else>
        <div class="total-info">
          <span>全量显示，共 {{ pagination.total }} 条记录</span>
          <el-button 
            size="small" 
            @click="() => { pagination.size = 100; pagination.isShowAll = false; handleSizeChange(100); }"
          >
            返回分页
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 上传对话框 -->
    <el-dialog 
      v-model="showUploadDialog" 
      title="上传图像" 
      width="600px"
    >
      <el-form :model="uploadForm" label-width="100px">
        <el-form-item label="标签配置">
          <el-select 
            v-model="uploadForm.labelConfigId" 
            placeholder="请选择标签配置（可选）"
            clearable
          >
            <el-option 
              v-for="config in labelConfigs" 
              :key="config.id"
              :label="config.name" 
              :value="config.id" 
            />
          </el-select>
        </el-form-item>
        <el-form-item label="选择文件">
          <el-upload
            ref="uploadRef"
            :action="uploadAction"
            :headers="uploadHeaders"
            :data="getUploadData()"
            multiple
            :show-file-list="true"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            accept="image/*"
          >
            <el-button type="primary">选择图像文件</el-button>
            <template #tip>
              <div class="el-upload__tip">
                支持 jpg/png/gif 文件，单个文件不超过 50MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 设置标签配置对话框 -->
    <el-dialog 
      v-model="showLabelConfigDialog" 
      title="设置标签配置" 
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="labelConfigForm" label-width="120px">
        <el-form-item label="图像信息">
          <div class="label-config-image-info">
            <div v-if="labelConfigForm.isBatch">
              共选择了 <strong>{{ labelConfigForm.imageIds.length }}</strong> 张图像
            </div>
            <div v-else-if="labelConfigForm.currentImage">
              {{ labelConfigForm.currentImage.originalName }}
            </div>
          </div>
        </el-form-item>
        <el-form-item label="标签配置">
          <el-select 
            v-model="labelConfigForm.labelConfigId" 
            placeholder="请选择标签配置（可为空）"
            clearable
            style="width: 100%"
          >
            <el-option 
              v-for="config in labelConfigs" 
              :key="config.id"
              :label="config.name" 
              :value="config.id" 
            />
          </el-select>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showLabelConfigDialog = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="confirmSetLabelConfig"
          :loading="settingLabelConfig"
        >
          {{ settingLabelConfig ? '设置中...' : '确认设置' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 分配图像对话框 -->
    <el-dialog 
      v-model="showAssignDialog" 
      title="分配图像" 
      width="600px"
      :close-on-click-modal="false"
    >
      <el-form :model="assignForm" label-width="100px">
        <!-- 已分配用户提示 -->
        <el-form-item 
          label="已分配用户" 
          v-if="assignImagePreviews.length === 1 && assignImagePreviews[0].isAssigned && assignImagePreviews[0].assignedUsers.length > 0"
        >
          <el-alert 
            type="info" 
            :closable="false"
            show-icon
          >
            <template #title>
              <span>此图像已分配给：</span>
              <el-tag 
                v-for="username in assignImagePreviews[0].assignedUsers" 
                :key="username"
                size="small"
                type="success"
                style="margin-left: 5px;"
              >
                {{ username }}
              </el-tag>
            </template>
          </el-alert>
        </el-form-item>
        
        <el-form-item label="选择用户">
          <el-select 
            v-model="assignForm.userIds" 
            multiple
            placeholder="请选择要分配的用户（可追加分配）"
            style="width: 100%"
            :loading="loadingUsers"
          >
            <el-option 
              v-for="user in userList" 
              :key="user.id"
              :label="`${user.username} (${user.role})`" 
              :value="user.id"
              :disabled="user.status !== 'ACTIVE'"
            />
          </el-select>
          <div style="margin-top: 8px; font-size: 12px; color: #909399;">
            <el-icon><InfoFilled /></el-icon>
            提示：可以在已分配的基础上追加新用户，或修改分配的用户列表
          </div>
        </el-form-item>
        <el-form-item label="分配图像">
          <div class="assign-image-info">
            <p>共选择了 <strong>{{ assignForm.imageIds.length }}</strong> 张图像
              <span v-if="isFullAssignment" class="all-assign-tip">（系统全量分配）</span>
              <span v-else-if="assignForm.imageIds.length === pagination.total && pagination.isShowAll" class="all-assign-tip">（当前页面全量）</span>
            </p>
            <div class="assign-image-preview" v-if="assignImagePreviews.length > 0">
              <div 
                v-for="image in assignImagePreviews.slice(0, 6)" 
                :key="image.id"
                class="mini-image"
              >
                <img :src="image.url" :alt="image.originalName" />
              </div>
              <div v-if="assignImagePreviews.length > 6" class="more-images">
                +{{ assignImagePreviews.length - 6 }}
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="cancelAssign">取消</el-button>
        <el-button 
          type="primary" 
          @click="confirmAssign"
          :loading="assigning"
          :disabled="assignForm.userIds.length === 0"
        >
          {{ assigning ? '分配中...' : '确认分配' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 全局裁剪设置对话框 -->
    <el-dialog 
      v-model="showCropDialog" 
      title="全局图片裁剪设置" 
      width="1000px"
      :close-on-click-modal="false"
    >
      <div class="crop-dialog-content">
        <div class="crop-info">
          <p><strong>说明：</strong>此设置将应用于选中的用户（图片尺寸：909 x 1080 px）</p>
          <p>
            <el-switch 
              v-model="cropForm.enabled" 
              active-text="启用全局裁剪"
              inactive-text="禁用全局裁剪"
            />
          </p>
          
          <!-- 用户权限选择 -->
          <div v-if="cropForm.enabled" style="margin: 15px 0;">
            <el-form-item label="生效用户：" label-width="90px">
              <el-select 
                v-model="cropForm.selectedUserIds" 
                multiple 
                placeholder="不选择表示对所有用户生效"
                style="width: 100%"
                clearable
              >
                <el-option 
                  v-for="user in userList" 
                  :key="user.id" 
                  :label="`${user.username} (${user.email || '无邮箱'})`" 
                  :value="user.id"
                />
              </el-select>
              <div class="user-hint" style="font-size: 12px; color: #909399; margin-top: 5px;">
                {{ cropForm.selectedUserIds.length === 0 ? '未选择用户，将对所有用户生效' : `已选择 ${cropForm.selectedUserIds.length} 个用户` }}
              </div>
            </el-form-item>
          </div>
          
          <p v-if="cropForm.enabled && cropForm.cropX !== null">
            <strong>裁剪区域：</strong>
            X={{ cropForm.cropX }}, Y={{ cropForm.cropY }}, 
            宽={{ cropForm.cropWidth }}, 高={{ cropForm.cropHeight }}
          </p>
          <p v-else-if="!cropForm.enabled" class="no-crop-tip">
            <el-icon><InfoFilled /></el-icon>
            当前未启用裁剪，用户将看到完整图片
          </p>
        </div>
        
        <div class="crop-container" v-if="cropForm.enabled">
          <div class="crop-preview" ref="cropPreviewRef">
            <img 
              :src="sampleImageUrl" 
              ref="cropImageRef"
              @load="onImageLoad"
              class="crop-image"
            />
            <div 
              v-if="cropBoxVisible"
              class="crop-box"
              :style="cropBoxStyle"
              @mousedown="startDrag"
            >
              <div class="crop-handle top-left" @mousedown.stop="(e) => startResize('top-left', e)"></div>
              <div class="crop-handle top-right" @mousedown.stop="(e) => startResize('top-right', e)"></div>
              <div class="crop-handle bottom-left" @mousedown.stop="(e) => startResize('bottom-left', e)"></div>
              <div class="crop-handle bottom-right" @mousedown.stop="(e) => startResize('bottom-right', e)"></div>
              <div class="crop-dimension">
                {{ Math.round(cropForm.cropWidth) }} × {{ Math.round(cropForm.cropHeight) }}
              </div>
            </div>
          </div>
        </div>

        <div class="crop-controls" v-if="cropForm.enabled">
          <el-button 
            type="primary" 
            @click="resetCropBox"
            size="small"
          >
            重置为全图
          </el-button>
          <div class="crop-tip">
            <el-icon><InfoFilled /></el-icon>
            拖动边框调整裁剪区域，拖动内部移动位置
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="showCropDialog = false">取消</el-button>
        <el-button 
          type="primary" 
          @click="confirmCrop"
          :loading="cropping"
        >
          {{ cropping ? '保存中...' : '确认保存' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 查看标注结果对话框 -->
    <el-dialog
      v-model="showAnnotationDialog"
      title="图像标注结果"
      width="90%"
      :before-close="handleCloseAnnotationDialog"
      v-loading="viewingAnnotations"
    >
      <div v-if="annotationData" class="annotation-viewer">
        <!-- 图像信息区域 -->
        <div class="image-section">
          <div class="image-info">
            <h3>图像信息</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="文件名">{{ annotationData.image.filename }}</el-descriptions-item>
              <el-descriptions-item label="原始名称">{{ annotationData.image.originalName }}</el-descriptions-item>
              <el-descriptions-item label="文件大小">{{ formatFileSize(annotationData.image.fileSize) }}</el-descriptions-item>
              <el-descriptions-item label="MIME类型">{{ annotationData.image.mimeType }}</el-descriptions-item>
              <el-descriptions-item label="上传者">{{ annotationData.image.uploaderUsername }}</el-descriptions-item>
              <el-descriptions-item label="上传时间">{{ formatDate(annotationData.image.createdAt) }}</el-descriptions-item>
            </el-descriptions>
          </div>
          <div class="image-preview-large">
            <img :src="annotationData.image.url" :alt="annotationData.image.originalName" />
          </div>
        </div>

        <!-- 标注结果区域 -->
        <div class="annotations-section">
          <h3>标注结果 ({{ annotationData.annotations.length }} 条)</h3>
          
          <div v-if="annotationData.annotations.length === 0" class="no-annotations">
            <el-empty description="该图像暂无标注记录" :image-size="100" />
          </div>
          
          <div v-else class="annotations-list">
            <el-card 
              v-for="annotation in annotationData.annotations" 
              :key="annotation.id"
              class="annotation-card"
              shadow="hover"
            >
              <template #header>
                <div class="annotation-header">
                  <div class="annotation-user">
                    <el-tag type="info" size="small">{{ annotation.user.username }}</el-tag>
                    <span class="annotation-status">
                      <el-tag :type="getAnnotationStatusType(annotation.status)" size="small">
                        {{ getAnnotationStatusText(annotation.status) }}
                      </el-tag>
                    </span>
                  </div>
                  <div class="annotation-time">
                    <span>创建: {{ formatDate(annotation.createdAt) }}</span>
                    <span v-if="annotation.updatedAt !== annotation.createdAt">
                      更新: {{ formatDate(annotation.updatedAt) }}
                    </span>
                  </div>
                </div>
              </template>
              
              <div class="annotation-content">
                <div v-if="annotation.labels && annotation.labels !== '{}'" class="labels-content">
                  <div v-for="(value, key) in parseLabels(annotation.labels)" :key="key" class="label-item">
                    <div class="label-category">{{ key }}:</div>
                    <div class="label-value">
                      <span v-if="Array.isArray(value)">{{ value.join(', ') }}</span>
                      <span v-else>{{ value }}</span>
                    </div>
                  </div>
                </div>
                <div v-else class="no-content">
                  <el-text type="info">暂无标注内容</el-text>
                </div>
              </div>
            </el-card>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="handleCloseAnnotationDialog">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, computed, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Collection, Setting, Delete, Document, DataAnalysis, View, Hide, RefreshLeft, Remove, Search, InfoFilled } from '@element-plus/icons-vue'
import { useAuthStore } from '@/store/auth'
import { imageApi } from '@/api/image'
import { labelConfigApi } from '@/api/labelConfig'
import { userApi } from '@/api/user'
import { systemConfigApi } from '@/api/systemConfig'

const authStore = useAuthStore()
const loading = ref(false)
const loadingUsers = ref(false)
const assigning = ref(false)
const settingLabelConfig = ref(false)
const showUploadDialog = ref(false)
const showAssignDialog = ref(false)
const showLabelConfigDialog = ref(false)
const showAnnotationDialog = ref(false)
const showCropDialog = ref(false)
const uploadRef = ref()
const tableRef = ref()
const cropImageRef = ref()
const cropPreviewRef = ref()

// 查看标注相关状态
const viewingAnnotations = ref(false)
const annotationData = ref(null)

// 选择和分配相关状态
const selectedImages = ref([])
const userList = ref([])
const assignImagePreviews = ref([])
const isFullAssignment = ref(false) // 标记是否为系统全量分配

// 收回相关状态
const revoking = ref(false)

// 已分配的选中图像（用于批量收回）
const selectedAssignedImages = computed(() => {
  return selectedImages.value.filter(imageId => {
    const image = imageList.value.find(img => img.id === imageId)
    return image && image.isAssigned
  })
})

// 拖拽选择状态
const dragSelectState = reactive({
  isDragging: false,
  startRowIndex: -1,
  currentRowIndex: -1,
  startSelection: []
})

const searchForm = reactive({
  id: '',
  keyword: '',
  originalName: '',
  labelConfigId: '',
  isAssigned: null,
  assignedUserId: '',
  assignedUsername: ''
})

// 筛选控制
const filterExpanded = ref(false)

// 字段显示控制
const activeCollapseNames = ref(['columnControl']) // 默认展开
const visibleColumns = reactive({
  basic: ['preview', 'filename', 'uploader', 'labelConfig', 'status'],
  csv: [] // 默认不显示CSV字段
})

const pagination = reactive({
  page: 1,
  size: 100,
  total: 0,
  isShowAll: false // 标记是否显示全量
})

const uploadForm = reactive({
  labelConfigId: ''
})

const assignForm = reactive({
  imageIds: [],
  userIds: []
})

// 设置标签配置表单
const labelConfigForm = reactive({
  imageIds: [],
  labelConfigId: null,
  currentImage: null,
  isBatch: false
})

// 裁剪表单
const cropForm = reactive({
  enabled: false,
  cropX: null,
  cropY: null,
  cropWidth: null,
  cropHeight: null,
  selectedUserIds: [] // 选中的用户ID列表，空数组表示对所有用户生效
})

// 裁剪相关状态
const cropping = ref(false)
const cropBoxVisible = ref(false)
const imageNaturalWidth = ref(909)
const imageNaturalHeight = ref(1080)
const imageDisplayWidth = ref(0)
const imageDisplayHeight = ref(0)
const cropBoxState = reactive({
  isDragging: false,
  isResizing: false,
  resizeHandle: '',
  startX: 0,
  startY: 0,
  startCropX: 0,
  startCropY: 0,
  startCropWidth: 0,
  startCropHeight: 0
})

const imageList = ref([])
const labelConfigs = ref([])

const uploadAction = '/api/admin/images/upload'
const uploadHeaders = computed(() => ({
  'Authorization': `Bearer ${authStore.token}`
}))

const getUploadData = () => {
  const data = {}
  // 只有当 labelConfigId 有值时才发送
  if (uploadForm.labelConfigId && uploadForm.labelConfigId !== '') {
    data.labelConfigId = uploadForm.labelConfigId
  }
  return data
}

// 示例图片URL（用于裁剪预览）
const sampleImageUrl = computed(() => {
  // 使用第一张图片作为示例，如果没有则使用占位图
  if (imageList.value && imageList.value.length > 0) {
    return imageList.value[0].url
  }
  return ''
})

const fetchImageList = async () => {
  try {
    loading.value = true
    console.log('📡 开始获取图像列表...')
    
    // 处理全量显示逻辑
    const isShowAll = pagination.isShowAll || pagination.size === -1 || pagination.size === 999999
    pagination.isShowAll = isShowAll
    
    const params = {
      page: isShowAll ? 0 : pagination.page - 1,
      size: isShowAll ? 10000 : pagination.size, // 全量时使用大数值
      id: searchForm.id || undefined,
      keyword: searchForm.keyword || undefined,
      originalName: searchForm.originalName || undefined,
      labelConfigId: searchForm.labelConfigId && searchForm.labelConfigId !== '' ? searchForm.labelConfigId : undefined,
      isAssigned: searchForm.isAssigned,
      assignedUserId: searchForm.assignedUserId || undefined,
      assignedUsername: searchForm.assignedUsername || undefined
    }
    
    console.log('📋 发送请求参数:', params)
    const response = await imageApi.getImageList(params)
    console.log('📨 API响应:', response)
    if (response.success) {
      imageList.value = response.data.content || []
      // 修复分页数据结构匹配问题
      if (response.data.pagination) {
        pagination.total = response.data.pagination.totalElements || 0
      } else {
        pagination.total = response.data.totalElements || 0
      }
      console.log('✅ 图像列表更新成功，共', imageList.value.length, '张图像，总数:', pagination.total)
      console.log('📋 分页信息:', response.data.pagination || response.data)
      
      // 数据更新后重新初始化拖拽选择
      nextTick(() => {
        initDragSelect()
      })
    } else {
      console.error('❌ API返回失败:', response)
      ElMessage.error('获取图像列表失败: ' + (response.message || '未知错误'))
    }
  } catch (error) {
    ElMessage.error('获取图像列表失败')
    console.error('❌ 请求异常:', error)
  } finally {
    loading.value = false
  }
}

const fetchLabelConfigs = async () => {
  try {
    const response = await labelConfigApi.getLabelConfigList()
    if (response.success) {
      labelConfigs.value = response.data.content || []
    }
  } catch (error) {
    console.error('获取标签配置失败:', error)
  }
}

const fetchUserList = async () => {
  try {
    console.log('🔍 开始获取用户列表...')
    const response = await userApi.getUserList({ page: 0, size: 1000 })
    console.log('📋 用户列表API响应:', response)
    if (response.success) {
      userList.value = response.data.content || []
      console.log('✅ 用户列表加载成功，共', userList.value.length, '个用户')
    } else {
      console.error('❌ 用户列表API返回失败:', response)
    }
  } catch (error) {
    console.error('❌ 获取用户列表失败:', error)
  }
}

const handleSearch = () => {
  console.log('🔍 执行搜索，当前筛选条件:', searchForm)
  pagination.page = 1
  fetchImageList()
}

const handleReset = () => {
  Object.assign(searchForm, {
    id: '',
    keyword: '',
    originalName: '',
    labelConfigId: '',
    isAssigned: null,
    assignedUserId: '',
    assignedUsername: ''
  })
  pagination.page = 1
  fetchImageList()
}

const toggleFilterExpand = () => {
  filterExpanded.value = !filterExpanded.value
}


const handleAssign = async (image) => {
  // 单个图像分配
  assignForm.imageIds = [image.id]
  assignImagePreviews.value = [image]
  isFullAssignment.value = false
  
  // 先加载用户列表
  await fetchUserListForAssign()
  
  // 如果图像已分配，预选已分配的用户
  if (image.isAssigned && image.assignedUserIds && image.assignedUserIds.length > 0) {
    assignForm.userIds = [...image.assignedUserIds]
  } else if (image.isAssigned && image.assignedUsers && image.assignedUsers.length > 0) {
    // 如果没有assignedUserIds，通过用户名匹配
    const assignedUserIds = userList.value
      .filter(user => image.assignedUsers.includes(user.username))
      .map(user => user.id)
    assignForm.userIds = assignedUserIds
  } else {
    assignForm.userIds = []
  }
  
  showAssignDialog.value = true
}

const handleDelete = async (image) => {
  try {
    await ElMessageBox.confirm(
      `确认删除图像 ${image.originalName} 吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    const response = await imageApi.deleteImage(image.id)
    if (response.success) {
      ElMessage.success('删除成功')
      fetchImageList()
    }
  } catch (error) {
    console.error('删除图像失败:', error)
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else {
      ElMessage.error('删除失败')
    }
  }
}

// ========== 裁剪相关方法 ==========
const handleGlobalCrop = async () => {
  try {
    // 获取当前全局裁剪配置
    const response = await systemConfigApi.getGlobalCropConfig()
    if (response.success) {
      const config = response.data
      cropForm.enabled = config.enabled || false
      cropForm.cropX = config.cropX
      cropForm.cropY = config.cropY
      cropForm.cropWidth = config.cropWidth
      cropForm.cropHeight = config.cropHeight
      
      // 解析用户ID列表
      try {
        const userIds = JSON.parse(config.userIds || '[]')
        cropForm.selectedUserIds = Array.isArray(userIds) ? userIds : []
      } catch (e) {
        cropForm.selectedUserIds = []
      }
    }
  } catch (error) {
    console.error('获取全局裁剪配置失败:', error)
    ElMessage.error('获取配置失败')
  }
  
  showCropDialog.value = true
  
  nextTick(() => {
    // 重置裁剪框可见性
    cropBoxVisible.value = false
    
    // 等待图片加载
    if (cropImageRef.value) {
      if (cropImageRef.value.complete) {
        onImageLoad()
      }
    }
  })
}

const onImageLoad = () => {
  if (!cropImageRef.value) return
  
  // 获取图片显示尺寸
  imageDisplayWidth.value = cropImageRef.value.clientWidth
  imageDisplayHeight.value = cropImageRef.value.clientHeight
  
  // 如果启用了裁剪，始终显示裁剪框
  if (cropForm.enabled) {
    // 如果有裁剪配置，使用配置的值
    if (cropForm.cropX !== null && cropForm.cropY !== null && 
        cropForm.cropWidth !== null && cropForm.cropHeight !== null) {
      cropBoxVisible.value = true
    } else {
      // 没有裁剪配置，初始化为全图
      resetCropBox()
    }
  }
}

const resetCropBox = () => {
  cropForm.cropX = 0
  cropForm.cropY = 0
  cropForm.cropWidth = imageNaturalWidth.value
  cropForm.cropHeight = imageNaturalHeight.value
  cropBoxVisible.value = true
}


// 计算裁剪框样式
const cropBoxStyle = computed(() => {
  if (!cropBoxVisible.value || !imageDisplayWidth.value) return {}
  
  const scale = imageDisplayWidth.value / imageNaturalWidth.value
  
  return {
    left: (cropForm.cropX || 0) * scale + 'px',
    top: (cropForm.cropY || 0) * scale + 'px',
    width: (cropForm.cropWidth || 0) * scale + 'px',
    height: (cropForm.cropHeight || 0) * scale + 'px'
  }
})

const startDrag = (e) => {
  e.preventDefault()
  e.stopPropagation()
  
  cropBoxState.isDragging = true
  cropBoxState.startX = e.clientX
  cropBoxState.startY = e.clientY
  cropBoxState.startCropX = cropForm.cropX || 0
  cropBoxState.startCropY = cropForm.cropY || 0
  
  document.addEventListener('mousemove', onDrag)
  document.addEventListener('mouseup', stopDrag)
}

const onDrag = (e) => {
  if (!cropBoxState.isDragging) return
  
  const scale = imageNaturalWidth.value / imageDisplayWidth.value
  const dx = (e.clientX - cropBoxState.startX) * scale
  const dy = (e.clientY - cropBoxState.startY) * scale
  
  let newX = cropBoxState.startCropX + dx
  let newY = cropBoxState.startCropY + dy
  
  // 限制边界
  newX = Math.max(0, Math.min(newX, imageNaturalWidth.value - (cropForm.cropWidth || 0)))
  newY = Math.max(0, Math.min(newY, imageNaturalHeight.value - (cropForm.cropHeight || 0)))
  
  cropForm.cropX = Math.round(newX)
  cropForm.cropY = Math.round(newY)
}

const stopDrag = () => {
  cropBoxState.isDragging = false
  document.removeEventListener('mousemove', onDrag)
  document.removeEventListener('mouseup', stopDrag)
}

const startResize = (handle, e) => {
  e.preventDefault()
  e.stopPropagation()
  
  cropBoxState.isResizing = true
  cropBoxState.resizeHandle = handle
  cropBoxState.startX = e.clientX
  cropBoxState.startY = e.clientY
  cropBoxState.startCropX = cropForm.cropX || 0
  cropBoxState.startCropY = cropForm.cropY || 0
  cropBoxState.startCropWidth = cropForm.cropWidth || 0
  cropBoxState.startCropHeight = cropForm.cropHeight || 0
  
  document.addEventListener('mousemove', onResize)
  document.addEventListener('mouseup', stopResize)
}

const onResize = (e) => {
  if (!cropBoxState.isResizing) return
  
  const scale = imageNaturalWidth.value / imageDisplayWidth.value
  const dx = (e.clientX - cropBoxState.startX) * scale
  const dy = (e.clientY - cropBoxState.startY) * scale
  
  let newX = cropBoxState.startCropX
  let newY = cropBoxState.startCropY
  let newWidth = cropBoxState.startCropWidth
  let newHeight = cropBoxState.startCropHeight
  
  switch (cropBoxState.resizeHandle) {
    case 'top-left':
      newX += dx
      newY += dy
      newWidth -= dx
      newHeight -= dy
      break
    case 'top-right':
      newY += dy
      newWidth += dx
      newHeight -= dy
      break
    case 'bottom-left':
      newX += dx
      newWidth -= dx
      newHeight += dy
      break
    case 'bottom-right':
      newWidth += dx
      newHeight += dy
      break
  }
  
  // 限制最小尺寸和边界
  newWidth = Math.max(50, newWidth)
  newHeight = Math.max(50, newHeight)
  
  newX = Math.max(0, Math.min(newX, imageNaturalWidth.value - newWidth))
  newY = Math.max(0, Math.min(newY, imageNaturalHeight.value - newHeight))
  
  if (newX + newWidth > imageNaturalWidth.value) {
    newWidth = imageNaturalWidth.value - newX
  }
  if (newY + newHeight > imageNaturalHeight.value) {
    newHeight = imageNaturalHeight.value - newY
  }
  
  cropForm.cropX = Math.round(newX)
  cropForm.cropY = Math.round(newY)
  cropForm.cropWidth = Math.round(newWidth)
  cropForm.cropHeight = Math.round(newHeight)
}

const stopResize = () => {
  cropBoxState.isResizing = false
  document.removeEventListener('mousemove', onResize)
  document.removeEventListener('mouseup', stopResize)
}

const confirmCrop = async () => {
  try {
    cropping.value = true
    
    const params = {
      enabled: cropForm.enabled,
      cropX: cropForm.cropX,
      cropY: cropForm.cropY,
      cropWidth: cropForm.cropWidth,
      cropHeight: cropForm.cropHeight,
      userIds: JSON.stringify(cropForm.selectedUserIds) // 转为JSON字符串
    }
    
    const response = await systemConfigApi.updateGlobalCropConfig(params)
    
    if (response.success) {
      const userCount = cropForm.selectedUserIds.length
      const message = userCount === 0 
        ? '全局裁剪配置已保存（对所有用户生效）' 
        : `全局裁剪配置已保存（对 ${userCount} 个用户生效）`
      ElMessage.success(message)
      showCropDialog.value = false
    }
  } catch (error) {
    console.error('保存全局裁剪配置失败:', error)
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else {
      ElMessage.error('保存失败')
    }
  } finally {
    cropping.value = false
  }
}

const handleUploadSuccess = (response, file) => {
  ElMessage.success(`${file.name} 上传成功`)
  fetchImageList()
}

const handleUploadError = (error, file) => {
  ElMessage.error(`${file.name} 上传失败`)
  console.error(error)
}

const handlePageChange = () => {
  fetchImageList()
}

const handleSizeChange = (newSize) => {
  pagination.size = newSize
  pagination.page = 1
  
  // 如果是最大值，当作全量处理
  if (newSize === 999999) {
    pagination.isShowAll = true
  } else {
    pagination.isShowAll = false
  }
  
  fetchImageList()
}

// 显示全量记录
const showAllRecords = () => {
  pagination.isShowAll = true
  pagination.size = -1
  pagination.page = 1
  fetchImageList()
}


// 图像选择相关方法
const handleSelectionChange = (selection) => {
  if (!dragSelectState.isDragging) {
    selectedImages.value = selection.map(item => item.id)
  }
}

// 获取表格行类名
const getRowClassName = ({ rowIndex }) => {
  return `table-row-${rowIndex}`
}

// 拖拽选择相关方法
const handleMouseDown = (event, rowIndex) => {
  // 只处理左键点击
  if (event.button !== 0) return
  
  // 防止选中文本
  event.preventDefault()
  
  dragSelectState.isDragging = true
  dragSelectState.startRowIndex = rowIndex
  dragSelectState.currentRowIndex = rowIndex
  
  // 保存当前选中状态
  dragSelectState.startSelection = [...selectedImages.value]
  
  // 添加全局监听器
  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
  document.body.style.userSelect = 'none' // 防止选中文本
}

const handleMouseMove = (event) => {
  if (!dragSelectState.isDragging) return
  
  // 获取当前鼠标位置下的表格行
  const element = document.elementFromPoint(event.clientX, event.clientY)
  if (!element) return
  
  const row = element.closest('tr')
  if (!row) return
  
  // 获取行索引
  const rowClass = row.className
  const match = rowClass.match(/table-row-(\d+)/)
  if (!match) return
  
  const rowIndex = parseInt(match[1])
  if (rowIndex >= 0 && rowIndex < imageList.value.length) {
    dragSelectState.currentRowIndex = rowIndex
    updateRangeSelection()
  }
}

const handleMouseUp = () => {
  if (dragSelectState.isDragging) {
    dragSelectState.isDragging = false
    
    // 清除视觉反馈
    clearDragSelectVisual()
  }
  
  // 清理全局监听器
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
  document.body.style.userSelect = '' // 恢复文本选中
}

const updateRangeSelection = () => {
  const startIndex = Math.min(dragSelectState.startRowIndex, dragSelectState.currentRowIndex)
  const endIndex = Math.max(dragSelectState.startRowIndex, dragSelectState.currentRowIndex)
  
  // 清除之前的拖拽选择视觉反馈
  clearDragSelectVisual()
  
  // 获取范围内的所有图像 ID
  const rangeIds = []
  for (let i = startIndex; i <= endIndex; i++) {
    if (imageList.value[i]) {
      rangeIds.push(imageList.value[i].id)
    }
  }
  
  // 添加拖拽选择的视觉反馈
  addDragSelectVisual(startIndex, endIndex)
  
  // 更新选中状态：结合初始选中和范围选中
  const newSelection = new Set(dragSelectState.startSelection)
  rangeIds.forEach(id => newSelection.add(id))
  
  selectedImages.value = Array.from(newSelection)
  
  // 同步更新表格的选中状态
  nextTick(() => {
    updateTableSelection()
  })
}

// 添加拖拽选择视觉反馈
const addDragSelectVisual = (startIndex, endIndex) => {
  if (!tableRef.value) return
  
  const tableElement = tableRef.value.$el
  const rows = tableElement.querySelectorAll('tbody tr')
  
  for (let i = startIndex; i <= endIndex; i++) {
    if (rows[i]) {
      rows[i].classList.add('drag-selecting')
    }
  }
  
  // 给表格添加拖拽状态类
  tableElement.classList.add('selecting')
}

// 清除拖拽选择视觉反馈
const clearDragSelectVisual = () => {
  if (!tableRef.value) return
  
  const tableElement = tableRef.value.$el
  const rows = tableElement.querySelectorAll('tbody tr')
  
  rows.forEach(row => {
    row.classList.remove('drag-selecting')
  })
  
  tableElement.classList.remove('selecting')
}

const updateTableSelection = () => {
  if (!tableRef.value) return
  
  // 清空当前选中
  tableRef.value.clearSelection()
  
  // 重新选中对应的行
  imageList.value.forEach((item, index) => {
    if (selectedImages.value.includes(item.id)) {
      tableRef.value.toggleRowSelection(item, true)
    }
  })
}

// 批量分配
const handleBatchAssign = () => {
  if (selectedImages.value.length === 0) {
    ElMessage.warning('请先选择要分配的图像')
    return
  }
  
  assignForm.imageIds = [...selectedImages.value]
  assignForm.userIds = []
  
  // 获取选中图像的预览信息
  assignImagePreviews.value = imageList.value.filter(img => 
    selectedImages.value.includes(img.id)
  )
  
  isFullAssignment.value = false
  showAssignDialog.value = true
  fetchUserListForAssign()
}

// 全量分配
const handleAssignAll = async () => {
  try {
    // 首先获取系统中所有图像的总数（模拟fetchImageList的参数格式）
    console.log('开始获取系统图像总数...')
    const countParams = {
      page: 0,
      size: 1,
      originalName: '' || undefined, // 模拟空搜索条件，和fetchImageList格式一致
      labelConfigId: ('' && '' !== '') ? '' : undefined // 模拟空搜索条件，和fetchImageList格式一致
    }
    console.log('获取总数请求参数:', countParams)
    
    const countResponse = await imageApi.getImageList(countParams)
    
    console.log('获取图像总数响应:', countResponse)
    
    if (!countResponse.success) {
      ElMessage.error('获取图像总数失败: ' + (countResponse.message || '未知错误'))
      // 如果当前页面有图像，提供备用选择
      if (imageList.value.length > 0) {
        try {
          await ElMessageBox.confirm(
            `无法获取系统总数，但当前页面有 ${imageList.value.length} 张图像。是否分配当前页面的图像？`,
            '备用分配方案',
            {
              confirmButtonText: '分配当前页面',
              cancelButtonText: '取消',
              type: 'info'
            }
          )
          // 使用当前页面的图像进行分配
          assignForm.imageIds = imageList.value.map(img => img.id)
          assignForm.userIds = []
          assignImagePreviews.value = [...imageList.value]
          isFullAssignment.value = false
          showAssignDialog.value = true
          fetchUserListForAssign()
          return
        } catch {
          // 用户取消了备用方案
        }
      }
      return
    }
    
    // 尝试不同的方式读取总数
    let totalCount = countResponse.data?.totalElements || 
                    countResponse.data?.pagination?.totalElements || 
                    countResponse.data?.pagination?.total || 0
    
    console.log('系统图像总数:', totalCount)
    console.log('完整的data对象:', countResponse.data)
    console.log('pagination对象:', countResponse.data?.pagination)
    
    // 如果API返回的总数为0，但当前页面有数据，尝试使用页面总数
    if (totalCount === 0 && imageList.value.length > 0 && pagination.total > 0) {
      console.warn('API返回总数为0，但当前页面有', imageList.value.length, '张图像，页面总数为', pagination.total)
      
      await ElMessageBox.confirm(
        `系统API返回总数为0，但当前页面显示有 ${pagination.total} 张图像。是否使用页面总数进行全量分配？`,
        '使用页面总数',
        {
          confirmButtonText: '使用页面总数',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      
      totalCount = pagination.total
      console.log('使用页面总数:', totalCount)
    } else if (totalCount === 0) {
      ElMessage.warning('系统中没有可分配的图像')
      return
    }
    
    await ElMessageBox.confirm(
      `确认要将系统中所有 ${totalCount} 张图像进行全量分配吗？`,
      '全量分配确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // 获取所有图像数据
    ElMessage.info('正在获取所有图像数据，请稍候...')
    const allImagesParams = {
      page: 0,
      size: Math.max(totalCount, 10000), // 使用总数或大数值确保获取全部
      originalName: '' || undefined, // 空搜索条件，保持和fetchImageList一致
      labelConfigId: ('' && '' !== '') ? '' : undefined // 空搜索条件，保持和fetchImageList一致
    }
    console.log('获取所有图像请求参数:', allImagesParams)
    
    const allImagesResponse = await imageApi.getImageList(allImagesParams)
    
    console.log('获取所有图像响应:', allImagesResponse)
    
    if (!allImagesResponse.success) {
      ElMessage.error('获取所有图像数据失败: ' + (allImagesResponse.message || '未知错误'))
      return
    }
    
    const allImages = allImagesResponse.data?.content || []
    console.log('获取到的图像数量:', allImages.length)
    const allImageIds = allImages.map(img => img.id)
    
    if (allImageIds.length === 0) {
      ElMessage.warning('没有可分配的图像')
      // 如果当前页面有图像，提供备用选择
      if (imageList.value.length > 0) {
        try {
          await ElMessageBox.confirm(
            `全量获取失败，但当前页面有 ${imageList.value.length} 张图像。是否分配当前页面的图像？`,
            '备用分配方案',
            {
              confirmButtonText: '分配当前页面',
              cancelButtonText: '取消',
              type: 'info'
            }
          )
          // 使用当前页面的图像进行分配
          assignForm.imageIds = imageList.value.map(img => img.id)
          assignForm.userIds = []
          assignImagePreviews.value = [...imageList.value]
          isFullAssignment.value = false
          showAssignDialog.value = true
          fetchUserListForAssign()
          return
        } catch {
          // 用户取消了备用方案
        }
      }
      return
    }
    
    assignForm.imageIds = allImageIds
    assignForm.userIds = []
    
    // 设置预览信息（显示前几张作为预览）
    assignImagePreviews.value = allImages.slice(0, 20) // 只显示前20张作为预览
    
    isFullAssignment.value = true
    showAssignDialog.value = true
    fetchUserListForAssign()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('全量分配失败:', error)
      ElMessage.error('全量分配操作失败')
    }
  }
}

// 获取用户列表用于分配
const fetchUserListForAssign = async () => {
  try {
    loadingUsers.value = true
    const response = await userApi.getUserList({ page: 0, size: 1000 })
    if (response.success) {
      // 只显示激活状态的普通用户，排除管理员
      userList.value = (response.data.content || []).filter(user => 
        user.status === 'ACTIVE' && user.role === 'USER'
      )
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
    ElMessage.error('获取用户列表失败')
  } finally {
    loadingUsers.value = false
  }
}

// 取消分配
const cancelAssign = () => {
  showAssignDialog.value = false
  isFullAssignment.value = false
}

// 确认分配
const confirmAssign = async () => {
  if (assignForm.userIds.length === 0) {
    ElMessage.warning('请至少选择一个用户')
    return
  }
  
  if (assignForm.imageIds.length === 0) {
    ElMessage.warning('请至少选择一张图像')
    return
  }
  
  try {
    assigning.value = true
    
    const response = await imageApi.assignImages({
      imageIds: assignForm.imageIds,
      userIds: assignForm.userIds
    })
    
    if (response.success) {
      const assignTypeText = isFullAssignment.value ? '系统全量' : '批量'
      ElMessage.success(
        `${assignTypeText}分配成功：${assignForm.imageIds.length} 张图像已分配给 ${assignForm.userIds.length} 个用户`
      )
      
      // 关闭对话框
      showAssignDialog.value = false
      
      // 清空选中状态
      selectedImages.value = []
      
      // 重置分配标记
      isFullAssignment.value = false
      
      // 刷新图像列表
      fetchImageList()
    }
  } catch (error) {
    console.error('分配图像失败:', error)
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else {
      ElMessage.error('分配图像失败')
    }
  } finally {
    assigning.value = false
  }
}

// 收回单个图像分配
const handleRevoke = async (image) => {
  try {
    await ElMessageBox.confirm(
      `确认要收回图像 "${image.originalName || image.filename}" 的分配吗？`,
      '收回确认',
      {
        confirmButtonText: '确定收回',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    revoking.value = true
    
    // 根据图像ID收回所有分配
    const response = await imageApi.revokeAssignmentByImage(image.id)
    
    if (response.success) {
      ElMessage.success('分配收回成功')
      // 刷新图像列表
      fetchImageList()
    } else {
      ElMessage.error('收回失败: ' + (response.message || '未知错误'))
    }
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('收回分配失败:', error)
      ElMessage.error('收回分配失败')
    }
  } finally {
    revoking.value = false
  }
}

// 批量收回图像分配
const handleBatchRevoke = async () => {
  if (selectedAssignedImages.value.length === 0) {
    ElMessage.warning('请先选择已分配的图像')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确认要收回 ${selectedAssignedImages.value.length} 张图像的分配吗？`,
      '批量收回确认',
      {
        confirmButtonText: '确定收回',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    revoking.value = true
    
    // 获取图像ID列表进行批量收回
    const imageIds = selectedAssignedImages.value
    
    const response = await imageApi.batchRevokeAssignmentsByImage(imageIds)
    
    if (response.success) {
      const result = response.data
      if (result.failedCount > 0 || result.completedCount > 0) {
        let message = `收回完成：成功 ${result.successCount} 个`
        if (result.failedCount > 0) {
          message += `，失败 ${result.failedCount} 个`
        }
        if (result.completedCount > 0) {
          message += `，${result.completedCount} 个已完成无法收回`
        }
        ElMessage.warning(message)
        console.log('收回失败的原因:', result.errors)
      } else {
        ElMessage.success(`成功收回 ${result.successCount} 个图像分配`)
      }
      
      // 清空选中状态
      selectedImages.value = []
      
      // 刷新图像列表
      fetchImageList()
    } else {
      ElMessage.error('批量收回失败: ' + (response.message || '未知错误'))
    }
    
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量收回失败:', error)
      ElMessage.error('批量收回失败')
    }
  } finally {
    revoking.value = false
  }
}

// 查看图像标注结果
const handleView = async (image) => {
  try {
    viewingAnnotations.value = true
    
    const response = await imageApi.getImageAnnotations(image.id)
    if (response.success) {
      annotationData.value = response.data
      showAnnotationDialog.value = true
    } else {
      ElMessage.error(response.message || '获取标注结果失败')
    }
  } catch (error) {
    console.error('获取标注结果失败:', error)
    ElMessage.error('获取标注结果失败')
  } finally {
    viewingAnnotations.value = false
  }
}

// 关闭查看标注对话框
const handleCloseAnnotationDialog = () => {
  showAnnotationDialog.value = false
  annotationData.value = null
}

// 解析JSON标注内容
const parseLabels = (labelsStr) => {
  if (!labelsStr || labelsStr === '{}') return {}
  
  try {
    const parsed = JSON.parse(labelsStr)
    return parsed || {}
  } catch (error) {
    console.error('解析标注内容失败:', error)
    return {}
  }
}

// 获取标注状态类型
const getAnnotationStatusType = (status) => {
  const typeMap = {
    'DRAFT': 'warning',
    'COMPLETED': 'success'
  }
  return typeMap[status] || 'info'
}

// 获取标注状态文本
const getAnnotationStatusText = (status) => {
  const textMap = {
    'DRAFT': '草稿',
    'COMPLETED': '已完成'
  }
  return textMap[status] || status
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 格式化时间
const formatDate = (dateStr) => {
  return dateStr ? new Date(dateStr).toLocaleString() : ''
}

// 字段显示控制方法
const showAllColumns = () => {
  visibleColumns.basic = ['preview', 'filename', 'uploader', 'labelConfig', 'status']
  visibleColumns.csv = ['folder', 'reid', 'imageClass', 'firstTrainLabel', 'testLabel', 'model1', 'pasConf', 'ifLabel', 'predictLabel', 'predictConfidence']
}

const hideAllCsvColumns = () => {
  visibleColumns.csv = []
}

const resetColumnSettings = () => {
  visibleColumns.basic = ['preview', 'filename', 'uploader', 'labelConfig', 'status']
  visibleColumns.csv = []
}

// 设置单个图像标签配置
const handleSetLabelConfig = (image) => {
  labelConfigForm.imageIds = [image.id]
  labelConfigForm.labelConfigId = image.labelConfigId
  labelConfigForm.currentImage = image
  labelConfigForm.isBatch = false
  showLabelConfigDialog.value = true
}

// 批量设置标签配置
const handleBatchSetLabelConfig = () => {
  if (selectedImages.value.length === 0) {
    ElMessage.warning('请先选择要设置的图像')
    return
  }
  
  labelConfigForm.imageIds = [...selectedImages.value]
  labelConfigForm.labelConfigId = null
  labelConfigForm.currentImage = null
  labelConfigForm.isBatch = true
  showLabelConfigDialog.value = true
}

// 确认设置标签配置
const confirmSetLabelConfig = async () => {
  try {
    settingLabelConfig.value = true
    
    if (labelConfigForm.isBatch) {
      // 批量设置
      const response = await imageApi.batchSetImageLabelConfig({
        imageIds: labelConfigForm.imageIds,
        labelConfigId: labelConfigForm.labelConfigId
      })
      
      if (response.success) {
        ElMessage.success(`批量设置成功，影响 ${labelConfigForm.imageIds.length} 张图像`)
        showLabelConfigDialog.value = false
        selectedImages.value = [] // 清空选中状态
        fetchImageList()
      }
    } else {
      // 单个设置
      const response = await imageApi.setImageLabelConfig(
        labelConfigForm.imageIds[0], 
        labelConfigForm.labelConfigId
      )
      
      if (response.success) {
        ElMessage.success('标签配置设置成功')
        showLabelConfigDialog.value = false
        fetchImageList()
      }
    }
  } catch (error) {
    console.error('设置标签配置失败:', error)
    if (error.response?.data?.message) {
      ElMessage.error(error.response.data.message)
    } else {
      ElMessage.error('设置标签配置失败')
    }
  } finally {
    settingLabelConfig.value = false
  }
}

// 批量删除
const handleBatchDelete = async () => {
  if (selectedImages.value.length === 0) {
    ElMessage.warning('请先选择要删除的图像')
    return
  }
  
  try {
    await ElMessageBox.confirm(
      `确认删除选中的 ${selectedImages.value.length} 张图像吗？`,
      '批量删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    // TODO: 需要后端支持批量删除接口
    for (const imageId of selectedImages.value) {
      await imageApi.deleteImage(imageId)
    }
    
    ElMessage.success(`批量删除成功，已删除 ${selectedImages.value.length} 张图像`)
    selectedImages.value = []
    fetchImageList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
      ElMessage.error('批量删除失败')
    }
  }
}

// 初始化事件监听器
const initDragSelect = async () => {
  await nextTick()
  
  if (!tableRef.value) return
  
  const tableElement = tableRef.value.$el
  if (!tableElement) return
  
  // 为每个表格行添加鼠标事件监听
  const addRowListeners = () => {
    const rows = tableElement.querySelectorAll('tbody tr')
    rows.forEach((row, index) => {
      // 移除旧的监听器（如果存在）
      row.removeEventListener('mousedown', row._mouseDownHandler)
      
      // 添加新的监听器
      const mouseDownHandler = (event) => handleMouseDown(event, index)
      row.addEventListener('mousedown', mouseDownHandler)
      
      // 保存引用以便后续移除
      row._mouseDownHandler = mouseDownHandler
    })
  }
  
  // 初始化监听器
  addRowListeners()
  
  // 监听表格数据变化，重新添加监听器
  const observer = new MutationObserver(() => {
    setTimeout(addRowListeners, 100) // 延迟执行以确保 DOM 更新完成
  })
  
  const tbody = tableElement.querySelector('tbody')
  if (tbody) {
    observer.observe(tbody, {
      childList: true,
      subtree: true
    })
  }
  
  // 保存 observer 以便清理
  tableRef.value._observer = observer
}

onMounted(() => {
  fetchImageList()
  fetchLabelConfigs()
  fetchUserList()
  initDragSelect()
})

onUnmounted(() => {
  // 清理事件监听器
  document.removeEventListener('mousemove', handleMouseMove)
  document.removeEventListener('mouseup', handleMouseUp)
  
  // 清理 MutationObserver
  if (tableRef.value?._observer) {
    tableRef.value._observer.disconnect()
  }
  
  // 恢复文本选中
  document.body.style.userSelect = ''
})
</script>

<style scoped>
.image-management {
  height: 100%;
}

/* 操作按钮多行排列 */
.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: stretch;
  min-width: 100px;
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

.action-buttons {
  margin-bottom: 16px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}

.action-buttons .el-button {
  margin-right: 12px;
}

.image-preview {
  width: 60px;
  height: 60px;
  overflow: hidden;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #e4e7ed;
}

.image-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.file-info {
  display: flex;
  flex-direction: column;
}

.file-name {
  font-weight: 500;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 180px;
}

.file-meta {
  font-size: 12px;
  color: #909399;
}

.label-config-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.label-config-name {
  font-size: 12px;
  color: #409eff;
  font-weight: 500;
}

.no-label-config {
  font-size: 12px;
  color: #c0c4cc;
}

.status-tags {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.status-tags {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}

.status-tags .el-tag {
  width: fit-content;
}

.assigned-status {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}

.assigned-users {
  display: flex;
  flex-wrap: wrap;
  gap: 2px;
  margin-top: 2px;
}

.user-tag {
  font-size: 11px !important;
  padding: 1px 4px !important;
  height: 18px !important;
  line-height: 16px !important;
}

.status-tag {
  margin-bottom: 2px;
}

.pagination-section {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 20px;
  gap: 15px;
}

.pagination-size-select {
  width: 120px;
}

.show-all-btn {
  margin-left: 15px;
}

.total-info {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  color: #606266;
  font-size: 14px;
}

.all-assign-tip {
  color: #f56c6c;
  font-size: 12px;
  font-weight: normal;
}

/* 分配对话框样式 */
.assign-image-info {
  background: #f5f5f5;
  padding: 15px;
  border-radius: 4px;
}

.assign-image-info p {
  margin: 0 0 10px 0;
  color: #666;
}

.assign-image-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.mini-image {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  overflow: hidden;
  border: 1px solid #ddd;
}

.mini-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.more-images {
  width: 40px;
  height: 40px;
  background: #e0e0e0;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #666;
  font-weight: bold;
}

/* 设置标签配置对话框样式 */
.label-config-image-info {
  padding: 10px;
  background: #f5f7fa;
  border-radius: 4px;
  color: #606266;
  font-size: 14px;
}

/* 裁剪对话框样式 */
.crop-dialog-content {
  padding: 10px;
}

.crop-info {
  margin-bottom: 20px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.crop-info p {
  margin: 8px 0;
  font-size: 14px;
  color: #606266;
}

.no-crop-tip {
  color: #909399;
  display: flex;
  align-items: center;
  gap: 5px;
}

.crop-container {
  margin-bottom: 20px;
  display: flex;
  justify-content: center;
}

.crop-preview {
  position: relative;
  display: inline-block;
  max-width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  overflow: hidden;
}

.crop-image {
  display: block;
  max-width: 100%;
  height: auto;
  max-height: 600px;
  user-select: none;
}

.crop-box {
  position: absolute;
  border: 2px solid #409eff;
  background: rgba(64, 158, 255, 0.1);
  cursor: move;
  box-sizing: border-box;
}

.crop-handle {
  position: absolute;
  width: 10px;
  height: 10px;
  background: #409eff;
  border: 2px solid #fff;
  border-radius: 50%;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.crop-handle.top-left {
  top: -5px;
  left: -5px;
  cursor: nwse-resize;
}

.crop-handle.top-right {
  top: -5px;
  right: -5px;
  cursor: nesw-resize;
}

.crop-handle.bottom-left {
  bottom: -5px;
  left: -5px;
  cursor: nesw-resize;
}

.crop-handle.bottom-right {
  bottom: -5px;
  right: -5px;
  cursor: nwse-resize;
}

.crop-dimension {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0, 0, 0, 0.7);
  color: #fff;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  pointer-events: none;
  white-space: nowrap;
}

.crop-controls {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 15px;
  background: #f5f7fa;
  border-radius: 8px;
}

.crop-tip {
  margin-left: auto;
  color: #909399;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 5px;
}

/* 拖拽选择样式 */
.drag-select-table {
  position: relative;
}

.drag-select-table tbody tr {
  position: relative;
  cursor: pointer;
  transition: background-color 0.2s ease;
}

.drag-select-table tbody tr:hover {
  background-color: #f5f7fa !important;
}

/* 防止拖拽时选中文本 */
.drag-select-table.selecting {
  user-select: none;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
}

.drag-select-table.selecting tbody tr {
  pointer-events: none;
}

.drag-select-table.selecting .el-checkbox {
  pointer-events: auto;
}

/* 拖拽状态下的视觉反馈 */
.drag-select-table tbody tr.drag-selecting {
  background-color: rgba(64, 158, 255, 0.1) !important;
  border-color: #409eff;
}

/* 鼠标按下时的效果 */
.drag-select-table tbody tr:active {
  background-color: rgba(64, 158, 255, 0.05) !important;
}

/* 查看标注结果对话框样式 */
.annotation-viewer {
  max-height: 80vh;
  overflow-y: auto;
}

.annotation-viewer h3 {
  margin-bottom: 15px;
  color: #333;
  border-bottom: 2px solid #409eff;
  padding-bottom: 8px;
}

.image-section {
  display: flex;
  gap: 20px;
  margin-bottom: 30px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
}

.image-info {
  flex: 1;
}

.image-preview-large {
  flex: 1;
  text-align: center;
}

.image-preview-large img {
  width: 100%;
  max-width: 100%;
  height: auto;
  max-height: 350px;
  object-fit: contain;
  border: 1px solid #ddd;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.annotations-section {
  margin-top: 20px;
}

.no-annotations {
  text-align: center;
  padding: 40px;
  color: #909399;
}

.annotations-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.annotation-card {
  border-radius: 8px;
  transition: all 0.3s ease;
}

.annotation-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

.annotation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.annotation-user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.annotation-time {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  font-size: 12px;
  color: #666;
  gap: 4px;
}

.annotation-content {
  margin-top: 10px;
}

.labels-content {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 12px;
}

.label-item {
  display: flex;
  align-items: flex-start;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 6px;
  border-left: 3px solid #409eff;
}

.label-category {
  font-weight: bold;
  color: #409eff;
  min-width: 80px;
  flex-shrink: 0;
}

.label-value {
  color: #606266;
  line-height: 1.5;
  word-break: break-all;
}

.no-content {
  text-align: center;
  padding: 20px;
  color: #909399;
  font-style: italic;
}

/* 统一使用白天模式，移除了暗黑模式适配 */
</style>
