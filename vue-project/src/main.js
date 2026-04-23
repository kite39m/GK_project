import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

// 引入 axios
import axios from 'axios'

// 核心：配置 Axios 拦截器
// 1. 发送请求前，自动把 token 塞进请求头
axios.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 2. 接收响应时，如果后端说 token 错误或过期 (401)，直接跳回登录页
axios.interceptors.response.use(
  response => response,
  error => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token') // 清除失效的 token
      router.push('/login') // 回到登录页
    }
    return Promise.reject(error)
  }
)

const app = createApp(App)

app.use(router)
app.use(ElementPlus)

app.mount('#app')