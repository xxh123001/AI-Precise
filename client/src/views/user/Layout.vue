<template>
  <el-container class="user-layout">
    <!-- 头部 -->
    <el-header class="header" style="max-height:50px">
      <div class="header-content">
        <div class="logo">
          <img src="/AI_Precise.jpg" alt="AI-Precise" class="logo-img" />
          <h3>AI-Precise 标注系统</h3>
        </div>
        
        <div class="header-actions">
          <button class="logout-button" @click="handleLogout" title="退出登录">
            <svg class="logout-icon" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M16 17L21 12L16 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M21 12H9" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M13 22C13 22 3 22 3 22C3 22 3 12 3 12C3 2 3 2 3 2C3 2 13 2 13 2" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
      </div>
    </el-header>
    
    <!-- 内容区 -->
    <el-main :class="['main-content', { 'no-padding': isTaskPage }]">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { computed } from 'vue'
import { ElMessageBox, ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 检查是否是任务页面（需要无padding布局）
const isTaskPage = computed(() => {
  return route.path === '/user/tasks' || route.path.startsWith('/user/projects/') && route.path.includes('/tasks')
})

// 退出登录处理
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要退出登录吗？',
      '退出确认',
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
    if (error !== 'cancel') {
      console.error('退出登录失败:', error)
      ElMessage.error('退出登录失败')
    }
  }
}

</script>

<style scoped>
.user-layout {
  height: 100vh;
  background: var(--bg-primary);
}

.header {
  background: var(--primary-gradient);
  color: white;
  padding: 0 var(--spacing-xl);
  position: relative;
  box-shadow: var(--shadow-lg);
  overflow: hidden;
}

.header::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.1), transparent);
  animation: shimmer 4s infinite;
}

@keyframes shimmer {
  0% { left: -100%; }
  100% { left: 100%; }
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 100%;
  position: relative;
  z-index: 1;
  white-space: nowrap;
  flex-wrap: nowrap;
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  min-width: 0;
}

.logo-img {
  height: 40px;
  width: auto;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.logo h3 {
  margin: 0;
  color: white;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.025em;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  position: relative;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.logo h3::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 0;
  width: 0;
  height: 2px;
  background: rgba(255, 255, 255, 0.8);
  transition: width var(--transition-normal);
}

.logo:hover h3::after {
  width: 100%;
}

.nav-menu {
  flex: 1;
  display: flex;
  justify-content: center;
}

:deep(.el-menu--horizontal) {
  border-bottom: none;
  background: transparent;
}

:deep(.el-menu-item) {
  border-radius: var(--radius-md);
  margin: 0 var(--spacing-sm);
  transition: all var(--transition-normal);
  position: relative;
  overflow: hidden;
}

:deep(.el-menu-item::before) {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: rgba(255, 255, 255, 0.1);
  transition: left var(--transition-normal);
}

:deep(.el-menu-item:hover::before) {
  left: 0;
}

:deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.15);
  transform: translateY(-2px);
}

:deep(.el-menu-item.is-active) {
  background: rgba(255, 255, 255, 0.2);
  color: white;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

:deep(.el-menu-item .el-icon) {
  margin-right: var(--spacing-sm);
  transition: transform var(--transition-normal);
}

:deep(.el-menu-item:hover .el-icon) {
  transform: scale(1.1);
}

/* 头部右侧操作区域 */
.header-actions {
  display: flex;
  align-items: center;
}

/* 退出按钮样式 */
.logout-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  padding: 0;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 8px;
  color: white;
  cursor: pointer;
  transition: all 0.3s ease;
  backdrop-filter: blur(10px);
  outline: none;
  flex-shrink: 0;
}

.logout-button:hover {
  background: rgba(255, 255, 255, 0.2);
  border-color: rgba(255, 255, 255, 0.3);
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.logout-button:active {
  transform: translateY(0);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* 退出图标样式 */
.logout-icon {
  width: 20px;
  height: 20px;
  transition: transform 0.3s ease;
}

.logout-button:hover .logout-icon {
  transform: translateX(2px);
}

.main-content {
  padding: var(--spacing-xl);
  background: var(--bg-primary);
  min-height: calc(100vh - 60px);
  position: relative;
}

/* 为tasks页面移除padding，让图像容器能铺满 */
.main-content.no-padding {
  padding: 0;
}

.main-content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: var(--primary-gradient);
  opacity: 0.3;
}

/* 滚动条美化 */
.main-content::-webkit-scrollbar {
  width: 6px;
}

.main-content::-webkit-scrollbar-track {
  background: var(--bg-secondary);
}

.main-content::-webkit-scrollbar-thumb {
  background: var(--primary-gradient);
  border-radius: var(--radius-sm);
}

.main-content::-webkit-scrollbar-thumb:hover {
  background: var(--hover-gradient);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .header-content {
    flex-direction: row;
    justify-content: space-between;
    padding: var(--spacing-sm) 0;
  }
  
  .nav-menu {
    margin: var(--spacing-sm) 0;
  }
  
  .logo h3 {
    font-size: 18px;
  }
  
  .logout-button {
    width: 36px;
    height: 36px;
  }
  
  .logout-icon {
    width: 18px;
    height: 18px;
  }
  
  .main-content {
    padding: var(--spacing-md);
  }
}

/* 动画增强 */
.user-layout {
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

/* 统一使用白天模式，移除了暗黑模式适配 */
</style>
