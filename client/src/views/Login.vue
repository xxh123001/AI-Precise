<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <img src="/Snipaste_2025-10-16_00-25-32.png" alt="AI-Precise" class="login-logo" />
        <h2>AI-Precise 标注系统</h2>
        <p>请登录您的账户</p>
      </div>
      
      <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            size="large"
            prefix-icon="User"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            size="large"
            prefix-icon="Lock"
            show-password
          />
        </el-form-item>
        
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            @click="handleLogin"
            class="login-button"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>
      
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const authStore = useAuthStore()

const loginFormRef = ref()
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 1, message: '密码长度不少于1位', trigger: 'blur' }
  ]
})

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  try {
    // 登录前先清空所有认证信息，确保干净状态
    authStore.clearAuth()
    
    // 表单验证
    const isValid = await loginFormRef.value.validate()
    if (!isValid) {
      ElMessage.error('请检查输入信息')
      return
    }
    
    loading.value = true
    
    const result = await authStore.login(loginForm)
    
    if (result.success) {
      ElMessage.success('登录成功')
      
      // 跳转到用户页面
      router.push('/user')
    } else {
      ElMessage.error(result.message)
      // 登录失败，确保清空认证信息
      authStore.clearAuth()
    }
  } catch (error) {
    console.error('登录失败:', error)
    
    // 登录失败，清空认证信息
    authStore.clearAuth()
    
    // 处理表单验证错误
    if (typeof error === 'object' && error !== null) {
      const errorMessages = Object.values(error).flat()
      if (errorMessages.length > 0) {
        ElMessage.error(errorMessages[0])
      } else {
        ElMessage.error('登录失败，请检查用户名和密码')
      }
    } else {
      ElMessage.error('登录失败，请稍后重试')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: var(--primary-gradient);
  position: relative;
  overflow: hidden;
}

.login-container::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 20"><defs><radialGradient id="a" cx="50%" cy="40%" r="50%"><stop offset="0%" stop-color="rgba(255,255,255,.1)"/><stop offset="100%" stop-color="rgba(255,255,255,0)"/></radialGradient></defs><rect width="100%" height="100%" fill="url(%23a)"/></svg>');
  animation: float 6s ease-in-out infinite;
}

.login-card {
  width: 420px;
  padding: var(--spacing-2xl);
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: var(--radius-xl);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: var(--shadow-xl);
  position: relative;
  z-index: 1;
  transform: perspective(1000px) rotateX(2deg);
  transition: all var(--transition-slow);
}

.login-card:hover {
  transform: perspective(1000px) rotateX(0deg) translateY(-8px);
  box-shadow: 0 20px 60px rgba(66, 211, 146, 0.3);
}

.login-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  background: var(--primary-gradient);
  border-radius: var(--radius-xl) var(--radius-xl) 0 0;
}

.login-header {
  text-align: center;
  margin-bottom: var(--spacing-xl);
  position: relative;
}

.login-logo {
  width: 80px;
  height: 80px;
  border-radius: 16px;
  margin-bottom: var(--spacing-md);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  object-fit: cover;
}

.login-header h2 {
  color: var(--text-primary);
  margin-bottom: var(--spacing-sm);
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
  background: var(--primary-gradient);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  position: relative;
}

.login-header h2::after {
  content: '';
  position: absolute;
  bottom: -8px;
  left: 50%;
  transform: translateX(-50%);
  width: 60px;
  height: 3px;
  background: var(--primary-gradient);
  border-radius: var(--radius-sm);
}

.login-header p {
  color: var(--text-secondary);
  font-size: 15px;
  font-weight: 500;
  margin-top: var(--spacing-md);
}

.login-form {
  margin-bottom: var(--spacing-lg);
}

.login-form :deep(.el-form-item) {
  margin-bottom: var(--spacing-lg);
}

.login-form :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.95);
  border: 2px solid rgba(229, 231, 235, 0.8);
  border-radius: var(--radius-md);
  padding: 12px 16px;
  transition: all var(--transition-normal);
  backdrop-filter: blur(10px);
}

.login-form :deep(.el-input__wrapper:hover) {
  background: rgba(255, 255, 255, 1);
  border-color: rgba(66, 211, 146, 0.3);
  box-shadow: var(--shadow-sm);
}

.login-form :deep(.el-input__wrapper.is-focus) {
  background: rgba(255, 255, 255, 1);
  border-color: var(--primary-green);
  box-shadow: 0 0 0 3px rgba(66, 211, 146, 0.15);
}

.login-form :deep(.el-input__inner) {
  font-size: 15px;
  font-weight: 500;
  color: #2c3e50 !important;
}

.login-form :deep(.el-input__inner::placeholder) {
  color: #94a3b8 !important;
  font-weight: 400;
}

.login-button {
  width: 100%;
  height: 50px;
  font-size: 16px;
  font-weight: 600;
  background: var(--primary-gradient);
  border: none;
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  transition: all var(--transition-normal);
  position: relative;
  overflow: hidden;
}

.login-button::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: left 0.5s ease;
}

.login-button:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
  background: var(--hover-gradient);
}

.login-button:hover::before {
  left: 100%;
}

.login-button:active {
  transform: translateY(0);
}

/* 动画效果 */
@keyframes float {
  0%, 100% { transform: translateY(0px) rotate(0deg); }
  33% { transform: translateY(-10px) rotate(1deg); }
  66% { transform: translateY(5px) rotate(-1deg); }
}

@keyframes slideInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-card {
  animation: slideInUp 0.6s ease-out;
}

/* 响应式设计 */
@media (max-width: 480px) {
  .login-card {
    width: 90%;
    padding: var(--spacing-lg);
    margin: var(--spacing-md);
  }
  
  .login-header h2 {
    font-size: 24px;
  }
}
</style>
