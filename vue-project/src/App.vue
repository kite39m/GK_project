<template>
  <NavBar />
  
  <router-view />
</template>

<script setup>
// 引入导航栏组件
import NavBar from './components/NavBar.vue'
</script>

<style>
/* 全局基础样式重置 */
html, body {
  margin: 0;
  padding: 0;
  background-color: #ffffff;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
}

/* 隐藏自带的那个导致页面偏向中间的默认 #app 样式 */
#app {
  max-width: none;
  margin: 0;
  padding: 0;
  text-align: left;
}
</style>