<template>
  <header class="nav-bar">
    <div class="nav-container">
      <div class="brand">
        <span class="logo-dot"></span>
        <span class="logo-text">Gemini</span>
      </div>

      <nav class="nav-links">
        <router-link to="/" class="nav-item">
          <span>首页</span>
        </router-link>
         <router-link to="/practice" class="nav-item">
          <span>速算</span>
        </router-link>
        <router-link to="/bank" class="nav-item">
          <span>选岗与备考策略</span>
        </router-link>
       
      </nav>

      <div class="nav-actions">
        <div class="avatar" title="ZHAO Wenyang">Z</div>
      </div>
    </div>
  </header>
  
  <div class="nav-spacer"></div>
</template>

<style scoped>
/* 核心：固定在顶部，使用毛玻璃背景 */
.nav-bar {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 64px;
  background-color: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  z-index: 1000;
  display: flex;
  justify-content: center;
  transition: all 0.3s ease;
}

/* 容器居中且限制最大宽度，与内容区对齐 */
.nav-container {
  width: 100%;
  max-width: 1200px;
  padding: 0 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

/* --- 左侧 Brand 设计 --- */
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

/* 摒弃复杂的图片，用纯粹的几何形和无衬线体作为 Logo */
.logo-dot {
  width: 10px;
  height: 10px;
  background-color: #111111;
  border-radius: 50%;
}

.logo-text {
  font-size: 1.125rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #111111;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}

/* --- 中间 Links 胶囊态设计 --- */
.nav-links {
  display: flex;
  gap: 8px; /* 间距拉开，增加呼吸感 */
}

.nav-item {
  text-decoration: none;
  color: #666666;
  font-size: 0.95rem;
  font-weight: 500;
  padding: 8px 16px;
  border-radius: 999px; /* 顶级官网最爱用的全圆角胶囊 */
  transition: all 0.2s cubic-bezier(0.25, 0.1, 0.25, 1);
  position: relative;
}

/* 悬浮态：极其微弱的灰色底 */
.nav-item:hover {
  color: #111111;
  background-color: rgba(0, 0, 0, 0.04);
}

/* 点击/按下态：带一点缩放效果 */
.nav-item:active {
  transform: scale(0.96);
}

/* 激活态：深色文字 + 略深的灰色背景，取代传统的蓝色高亮 */
.nav-item.router-link-active {
  color: #111111;
  background-color: rgba(0, 0, 0, 0.06);
  font-weight: 600;
}

/* --- 右侧头像设计 --- */
.nav-actions {
  display: flex;
  align-items: center;
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  border: 1px solid rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: #333;
  font-size: 0.95rem;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.avatar:hover {
  transform: scale(1.05);
}

/* 占位符，因为 nav 是 fixed 脱离文档流的，需要这个撑开高度 */
.nav-spacer {
  height: 64px;
  width: 100%;
}
</style>