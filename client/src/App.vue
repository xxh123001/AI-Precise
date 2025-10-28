<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
// Vue 3 Composition API with script setup
</script>

<style>
@import '@/assets/css/variables.css';

#app {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  background: var(--bg-primary);
  height: 100vh;
  margin: 0;
  padding: 0;
  overflow: hidden;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100%;
  color: var(--text-primary);
}

/* 隐藏滚动条但保留滚动功能 */
::-webkit-scrollbar {
  display: none; /* WebKit浏览器 (Chrome, Safari, Opera) */
}

/* Firefox */
html {
  scrollbar-width: none;
}

/* IE and Edge */
body {
  -ms-overflow-style: none;
}

/* 确保所有元素都隐藏滚动条 */
* {
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */
}

*::-webkit-scrollbar {
  display: none; /* WebKit浏览器 */
}

/* Element Plus 组件样式覆盖 */
.el-button--primary {
  background: var(--primary-gradient) !important;
  border: none !important;
  box-shadow: var(--shadow-md) !important;
  transition: all var(--transition-normal) !important;
}

.el-button--primary:hover {
  background: var(--hover-gradient) !important;
  transform: translateY(-2px) !important;
  box-shadow: var(--shadow-lg) !important;
}

.el-input__wrapper {
  border-radius: var(--radius-md) !important;
  transition: all var(--transition-normal) !important;
}

.el-input__wrapper:hover {
  box-shadow: var(--shadow-sm) !important;
}

.el-input__wrapper.is-focus {
  box-shadow: 0 0 0 2px rgba(66, 211, 146, 0.3) !important;
}

.el-card {
  border: none !important;
  border-radius: var(--radius-lg) !important;
  box-shadow: var(--shadow-md) !important;
  transition: all var(--transition-normal) !important;
}

.el-card:hover {
  box-shadow: var(--shadow-lg) !important;
  transform: translateY(-2px) !important;
}

/* 加载动画美化 */
.el-loading-mask {
  background: rgba(255, 255, 255, 0.8) !important;
  backdrop-filter: blur(10px) !important;
}

.el-loading-spinner .path {
  stroke: var(--primary-green) !important;
}

/* 消息提示美化 */
.el-message--success {
  background: var(--primary-gradient) !important;
  border: none !important;
  color: white !important;
}

.el-message--info {
  background: linear-gradient(135deg, var(--primary-blue), var(--primary-blue-light)) !important;
  border: none !important;
  color: white !important;
}

/* 统一使用白天模式，移除了暗黑模式适配 */
</style>
