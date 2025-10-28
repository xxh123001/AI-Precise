<template>
  <el-container class="admin-layout">
    <!-- 侧边栏 -->
    <el-aside width="260px">
      <div class="sidebar">
        <div class="logo">
          <img src="/Snipaste_2025-10-16_00-25-32.png" alt="AI-Precise" class="logo-img" />
          <h3>AI-Precise 标注系统</h3>
        </div>
        
        <el-menu
          :default-active="$route.path"
          router
          class="sidebar-menu"
          background-color="transparent"
          text-color="#64748b"
          active-text-color="#1e293b"
        >
          <el-menu-item index="/admin/dashboard">
            <el-icon><Odometer /></el-icon>
            <span>仪表盘</span>
          </el-menu-item>
          
          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          
          <el-menu-item index="/admin/images">
            <el-icon><Picture /></el-icon>
            <span>图像管理</span>
          </el-menu-item>
          
          <el-menu-item index="/admin/label-configs">
            <el-icon><Collection /></el-icon>
            <span>标签配置</span>
          </el-menu-item>
          
          <el-menu-item index="/admin/projects">
            <el-icon><FolderOpened /></el-icon>
            <span>大任务管理</span>
          </el-menu-item>
          
          <el-menu-item index="/admin/export">
            <el-icon><Download /></el-icon>
            <span>数据导出</span>
          </el-menu-item>
          
          <el-menu-item index="/admin/annotation-filter">
            <el-icon><Search /></el-icon>
            <span>标注筛选</span>
          </el-menu-item>
          
          <el-menu-item index="/admin/annotation-tasks">
            <el-icon><Setting /></el-icon>
            <span>标注任务管理</span>
          </el-menu-item>
          
          <el-menu-item index="/admin/annotation-analytics">
            <el-icon><DataAnalysis /></el-icon>
            <span>标注数据分析</span>
          </el-menu-item>
        </el-menu>
      </div>
    </el-aside>
    
    <!-- 主内容区 -->
    <el-container>
      <!-- 头部 -->
      <el-header class="header">
        <div class="header-content">
          <div class="breadcrumb">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item>管理后台</el-breadcrumb-item>
              <el-breadcrumb-item>{{ currentPageTitle }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          
          <div class="user-info">
            <el-dropdown @command="handleCommand">
              <span class="user-name">
                {{ authStore.user?.username }}
                <el-icon class="el-icon--right">
                  <arrow-down />
                </el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>
      
      <!-- 内容区 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'
import {
  Odometer,
  User,
  Picture,
  Collection,
  FolderOpened,
  Download,
  Search,
  Setting,
  DataAnalysis,
  ArrowDown
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 页面标题映射
const pageTitles = {
  '/admin/dashboard': '仪表盘',
  '/admin/users': '用户管理',
  '/admin/images': '图像管理',
  '/admin/label-configs': '标签配置',
  '/admin/export': '数据导出',
  '/admin/annotation-filter': '标注筛选',
  '/admin/annotation-tasks': '标注任务管理',
  '/admin/annotation-analytics': '标注数据分析'
}

const currentPageTitle = computed(() => {
  return pageTitles[route.path] || '管理后台'
})

const handleCommand = async (command) => {
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm(
        '确定要退出登录吗？',
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      
      await authStore.logout()
      ElMessage.success('已退出登录')
      router.push('/login')
    } catch (error) {
      // 用户取消操作
    }
  }
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
  background: #f8fafc;
}

.sidebar {
  height: 100vh;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 100%);
  border-right: 1px solid #e2e8f0;
  box-shadow: 4px 0 20px rgba(66, 211, 146, 0.1);
  position: relative;
}

.sidebar::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 1px;
  height: 100%;
  background: linear-gradient(180deg, #e2e8f0 0%, #f1f5f9 50%, #e2e8f0 100%);
}

.logo {
  height: 80px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
  background: var(--primary-gradient);
  position: relative;
  overflow: hidden;
  margin-bottom: 20px;
  box-shadow: 0 4px 20px rgba(66, 211, 146, 0.2);
}

.logo::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.1), transparent);
  animation: shimmer 3s infinite;
}

@keyframes shimmer {
  0% { left: -100%; }
  100% { left: 100%; }
}

.logo-img {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  margin-right: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  object-fit: cover;
}

.logo h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 0.025em;
}

.sidebar-menu {
  border: none;
  height: calc(100vh - 100px);
  padding: 0 16px;
  background: transparent;
}

:deep(.el-menu-item) {
  margin-bottom: 8px;
  border-radius: 12px;
  height: 48px;
  line-height: 48px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  font-weight: 500;
  color: #64748b;
}

:deep(.el-menu-item:hover) {
  background: linear-gradient(135deg, #f1f5f9 0%, #e2e8f0 100%);
  color: #1e293b;
  transform: translateX(4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

:deep(.el-menu-item.is-active) {
  background: var(--primary-gradient);
  color: white;
  transform: translateX(8px);
  box-shadow: 0 8px 24px rgba(66, 211, 146, 0.3);
}

:deep(.el-menu-item.is-active::before) {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  background: rgba(255, 255, 255, 0.8);
  border-radius: 0 2px 2px 0;
}

:deep(.el-menu-item .el-icon) {
  margin-right: 12px;
  font-size: 18px;
  transition: transform 0.3s ease;
}

:deep(.el-menu-item:hover .el-icon) {
  transform: scale(1.1);
}

.header {
  background: white;
  border-bottom: 1px solid #e2e8f0;
  padding: 0 32px;
  height: 80px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  position: relative;
  z-index: 10;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
}

.breadcrumb {
  flex: 1;
}

:deep(.el-breadcrumb) {
  font-size: 15px;
}

:deep(.el-breadcrumb__item) {
  color: #64748b;
  font-weight: 500;
}

:deep(.el-breadcrumb__item:last-child .el-breadcrumb__inner) {
  color: #1e293b;
  font-weight: 600;
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--spacing-md);
}

.theme-toggle-header {
  margin-right: var(--spacing-sm);
}

.user-name {
  cursor: pointer;
  color: #475569;
  display: flex;
  align-items: center;
  padding: 8px 16px;
  border-radius: 8px;
  transition: all 0.3s ease;
  font-weight: 500;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.user-name:hover {
  background: var(--primary-gradient-soft);
  color: var(--primary-green);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(66, 211, 146, 0.2);
  border-color: var(--primary-green);
}

.main-content {
  padding: 0;
  background: #f8fafc;
  min-height: calc(100vh - 80px);
  overflow-y: auto;
}

/* 滚动条美化 */
.main-content::-webkit-scrollbar {
  width: 6px;
}

.main-content::-webkit-scrollbar-track {
  background: #f1f5f9;
}

.main-content::-webkit-scrollbar-thumb {
  background: var(--primary-gradient);
  border-radius: 3px;
}

.main-content::-webkit-scrollbar-thumb:hover {
  background: var(--hover-gradient);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    width: 240px !important;
  }
  
  .logo h3 {
    font-size: 16px;
  }
  
  .header {
    padding: 0 16px;
  }
}

/* 统一使用白天模式，移除了暗黑模式适配 */
</style>
