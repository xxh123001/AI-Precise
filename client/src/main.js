import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import './assets/css/variables.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import { useAuthStore } from '@/store/auth'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(ElementPlus)

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 全局错误处理 - 确保任何未捕获的鉴权错误都能处理
app.config.errorHandler = (err, instance, info) => {
  console.error('全局错误:', err, info)
  
  // 如果是鉴权相关错误，清空store并跳转
  if (err?.response?.status === 401 || err?.response?.status === 403) {
    const authStore = useAuthStore()
    authStore.clearAuth()
    router.push('/login')
  }
}

// 监听storage变化 - 如果token被删除，立即跳转登录
window.addEventListener('storage', (e) => {
  if (e.key === 'token' && !e.newValue) {
    console.log('🔒 检测到token被删除，跳转登录页')
    const authStore = useAuthStore()
    authStore.clearAuth()
    router.push('/login')
  }
})

app.mount('#app')
