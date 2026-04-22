import { createRouter, createWebHistory } from 'vue-router'
// 导入三个页面组件
import HomeView from '../views/HomeView.vue'
import BankView from '../views/BankView.vue'
import PracticeView from '../views/PracticeView.vue'

const router = createRouter({
  // 使用 HTML5 模式的路由（网址中没有丑陋的 # 号）
  history: createWebHistory(import.meta.env.BASE_URL),
  // 定义路径和页面的映射关系
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/bank',
      name: 'bank',
      component: BankView
    },
    {
      path: '/practice',
      name: 'practice',
      component: PracticeView
    }
  ]
})

export default router