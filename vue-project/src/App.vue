<template>
  <NavBar v-if="!$route.meta.hideNav" />
  
  <router-view />
</template>

<script setup>
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

#app {
  max-width: none;
  margin: 0;
  padding: 0;
  text-align: left;
}
</style>