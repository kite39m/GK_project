<template>
  <div id="app-root">
    <NavBar v-if="!$route.meta.hideNav" />
    <div v-if="!$route.meta.hideNav" class="nav-spacer"></div>
    <router-view />
  </div>
</template>

<script setup>
import NavBar from './components/NavBar.vue'
</script>

<style>
/* 全局重置 */
*, *::before, *::after {
  box-sizing: border-box;
}

html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  overflow: hidden;
  background-color: #ffffff;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif;
  -webkit-font-smoothing: antialiased;
}

#app {
  height: 100%;
  max-width: none;
  margin: 0;
  padding: 0;
  text-align: left;
}

#app-root {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.nav-spacer {
  flex: none;
  height: 64px;
  width: 100%;
}
</style>
