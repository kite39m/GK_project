<template>
  <div class="login-container">
    <div class="bg-blur-blob"></div>
    
    <div class="login-card fade-in">
      <div class="login-header">
        <div class="brand">
          <span class="logo-dot"></span>
          <span class="logo-text">Gemini</span>
        </div>
        <h1 class="login-title">登录</h1>
        <p class="login-subtitle"></p>
      </div>

      <el-form :model="loginForm" class="login-form" @submit.prevent="handleLogin">
        <div class="form-item">
          <label>用户名</label>
          <el-input 
            v-model="loginForm.username" 
            placeholder="请输入用户名"
            size="large"
          />
        </div>

        <div class="form-item">
          <label>密码</label>
          <el-input 
            v-model="loginForm.password" 
            type="password" 
            placeholder="请输入密码"
            size="large"
            show-password
          />
        </div>

        <button class="btn-login" :disabled="isLoading" type="submit">
          <span v-if="!isLoading">点击登录</span>
          <span v-else class="loading-dot">登录中...</span>
        </button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const isLoading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    return ElMessage.warning('请填写完整账号密码')
  }

  isLoading.value = true
  try {
    // 真实调用我们刚刚写的 Spring Boot 后端登录接口
    const response = await axios.post('http://localhost:8080/api/auth/login', {
      username: loginForm.username,
      password: loginForm.password
    })

    if (response.data.code === 200) {
      // 登录成功，把后端给的 Token 存到浏览器的 localStorage 里
      localStorage.setItem('token', response.data.token)
      ElMessage.success('登录成功，欢迎赵书记')
      // 跳转到首页
      router.push('/')
    } else {
      ElMessage.error(response.data.message || '登录失败')
    }
  } catch (error) {
    ElMessage.error('网络错误或服务器未启动')
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #ffffff;
  position: relative;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

.bg-blur-blob {
  position: absolute;
  width: 600px;
  height: 600px;
  background: radial-gradient(circle, rgba(170, 59, 255, 0.08) 0%, rgba(255, 255, 255, 0) 70%);
  top: -100px;
  right: -100px;
  filter: blur(80px);
  z-index: 0;
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 48px;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 0, 0, 0.05);
  border-radius: 24px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.03);
  z-index: 1;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
}

.logo-dot {
  width: 12px;
  height: 12px;
  background-color: #111;
  border-radius: 50%;
}

.logo-text {
  font-weight: 700;
  font-size: 1.1rem;
  letter-spacing: -0.02em;
}

.login-title {
  font-size: 1.75rem;
  font-weight: 600;
  color: #111;
  margin-bottom: 8px;
}

.login-subtitle {
  color: #888;
  font-size: 0.95rem;
}

.form-item {
  margin-bottom: 24px;
}

.form-item label {
  display: block;
  font-size: 0.85rem;
  font-weight: 600;
  color: #444;
  margin-bottom: 8px;
  margin-left: 4px;
}

:deep(.el-input__wrapper) {
  background-color: #f9f9f9;
  box-shadow: none !important;
  border: 1px solid #eee;
  border-radius: 12px;
  padding: 8px 16px;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #111;
  background-color: #fff;
}

.btn-login {
  width: 100%;
  background: #111;
  color: #fff;
  border: none;
  padding: 16px;
  border-radius: 14px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  margin-top: 12px;
}

.btn-login:hover {
  background: #333;
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.fade-in {
  animation: fadeIn 0.8s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>