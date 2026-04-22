import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// 【新增部分】：引入 Element Plus 及其样式
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'

const app = createApp(App)

app.use(router)
// 【新增部分】：告诉 Vue 使用 Element Plus
app.use(ElementPlus)

app.mount('#app')