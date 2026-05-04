import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import BankView from '../views/BankView.vue'
import PracticeView from '../views/PracticeView.vue'
import LoginView from '../views/LoginView.vue'
import XingceView from '../views/XingceView.vue'
import XingceModuleView from '../views/XingceModuleView.vue'
import ShenlunView from '../views/ShenlunView.vue'
import ShenlunWriteView from '../views/ShenlunWriteView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: LoginView,
      // 🌟 新增：告诉系统，这个页面不需要显示导航栏
      meta: { hideNav: true } 
    },
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
    },
    {
      path: '/xingce',
      name: 'xingce',
      component: XingceView
    },
    {
      path: '/xingce/:module',
      name: 'xingce-module',
      component: XingceModuleView
    },
    {
      path: '/shenlun',
      name: 'shenlun',
      component: ShenlunView
    },
    {
      path: '/shenlun/write',
      name: 'shenlun-write',
      component: ShenlunWriteView
    }
  ]
})

// 路由守卫：登录拦截
router.beforeEach((to, from, next) => {
  if (to.name !== 'login') {
    const token = localStorage.getItem('token')
    if (!token) {
      next({ name: 'login' })
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router