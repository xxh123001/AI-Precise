import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue'),
      meta: { requiresAuth: false }
    },
    {
      path: '/admin',
      component: () => import('@/views/admin/Layout.vue'),
      meta: { requiresAuth: true, roles: ['ADMIN'] },
      children: [
        {
          path: '',
          name: 'AdminLayout',
          redirect: '/admin/dashboard'
        },
        {
          path: 'dashboard',
          name: 'AdminDashboard',
          component: () => import('@/views/admin/Dashboard.vue')
        },
        {
          path: 'users',
          name: 'UserManagement',
          component: () => import('@/views/admin/UserManagement.vue')
        },
        {
          path: 'images',
          name: 'ImageManagement',
          component: () => import('@/views/admin/ImageManagement.vue')
        },
        {
          path: 'label-configs',
          name: 'LabelConfigManagement',
          component: () => import('@/views/admin/LabelConfigManagement.vue')
        },
        {
          path: 'projects',
          name: 'ProjectManagement',
          component: () => import('@/views/admin/ProjectManagement.vue')
        },
        {
          path: 'export',
          name: 'DataExport',
          component: () => import('@/views/admin/DataExport.vue')
        },
        {
          path: 'annotation-filter',
          name: 'AnnotationFilter',
          component: () => import('@/views/admin/AnnotationFilter.vue')
        },
        {
          path: 'annotation-tasks',
          name: 'AnnotationTaskManagement',
          component: () => import('@/views/admin/AnnotationTaskManagement.vue')
        },
        {
          path: 'user-tasks',
          name: 'UserTaskList',
          component: () => import('@/views/admin/UserTaskList.vue')
        },
        {
          path: 'annotation-analytics',
          name: 'AnnotationAnalytics',
          component: () => import('@/views/admin/AnnotationAnalytics.vue')
        }
      ]
    },
    {
      path: '/user',
      component: () => import('@/views/user/Layout.vue'),
      meta: { requiresAuth: true, roles: ['USER'] },
      children: [
        {
          path: '',
          name: 'UserLayout',
          redirect: '/user/projects'
        },
        {
          path: 'projects',
          name: 'ProjectList',
          component: () => import('@/views/user/ProjectList.vue')
        },
        {
          path: 'projects/:projectId/tasks',
          name: 'ProjectTaskList',
          component: () => import('@/views/user/TaskList.vue'),
          props: true
        },
        {
          path: 'tasks',
          name: 'TaskList',
          component: () => import('@/views/user/TaskList.vue')
        },
        {
          path: 'annotation/:id',
          name: 'ImageAnnotation',
          component: () => import('@/views/user/ImageAnnotation.vue')
        },
        {
          path: 'history',
          name: 'AnnotationHistory',
          component: () => import('@/views/user/AnnotationHistory.vue')
        }
      ]
    }
  ]
})

// 路由守卫 - 适配手机端，鉴权失败立即跳转
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  
  // 检查token是否有效（不仅仅检查是否存在）
  const hasValidToken = authStore.token && authStore.token.trim() !== ''
  const hasValidUser = authStore.user && authStore.user.role
  
  if (to.meta.requiresAuth) {
    // 需要鉴权的路由
    if (!hasValidToken || !hasValidUser) {
      // 立即清除无效的认证信息
      authStore.clearAuth()
      
      // 手机端和PC端都直接跳转，不弹框
      console.log('🔒 鉴权失败，跳转到登录页')
      next('/login')
    } else if (to.meta.roles && !to.meta.roles.includes(authStore.user?.role)) {
      // 角色不匹配，跳转到对应的默认页面
      console.log('⚠️ 角色不匹配，跳转到默认页面')
      if (authStore.user?.role === 'ADMIN') {
        next('/admin')
      } else {
        next('/user')
      }
    } else {
      next()
    }
  } else {
    // 不需要鉴权的路由（如登录页）
    if (hasValidToken && hasValidUser && to.path === '/login') {
      // 已登录用户访问登录页，跳转到对应的首页
      if (authStore.user?.role === 'ADMIN') {
        next('/admin')
      } else {
        next('/user')
      }
    } else {
      next()
    }
  }
})

// 全局错误处理 - 处理未捕获的导航错误
router.onError((error) => {
  console.error('路由错误:', error)
  // 不阻止导航，让用户可以继续操作
})

export default router
