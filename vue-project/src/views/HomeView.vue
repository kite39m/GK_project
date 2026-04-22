<template>
  <div class="page-container">
    <div class="content-wrapper">
      <header class="hero-section">
        <h1 class="title">欢迎来到你的专属题库。</h1>
        <p class="subtitle">目标 2027 年国考，稳扎稳打，步步为营。</p >
      </header>

      <main class="dashboard-container">
        <div class="dashboard-card">
          <div class="card-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 12V7H5a2 2 0 0 1 0-4h14v4" />
              <path d="M3 5v14a2 2 0 0 0 2 2h16v-5" />
              <path d="M18 12H9" />
            </svg>
          </div>
          <h2 class="card-title">训练数据概览</h2>
          <p class="card-desc">这里未来将展示你的刷题总数据、正确率趋势与成长轨迹。</p >
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
/* 引入类似 Claude 的无衬线字体体系 */
.page-container {
  min-height: 100vh;
  background-color: #ffffff; /* 纯白背景 */
  display: flex;
  justify-content: center;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  color: #1a1a1a;
}

.content-wrapper {
  width: 100%;
  max-width: 860px; /* 限制最大宽度，保证阅读舒适度 */
  padding: 12vh 24px 60px;
}

/* --- 排版呼吸感 --- */
.hero-section {
  text-align: center;
  margin-bottom: 64px;
  animation: fadeIn 0.8s ease-out;
}

.title {
  font-size: 2.5rem;
  font-weight: 600;
  letter-spacing: -0.02em; /* 轻微收缩字间距，提升高级感 */
  color: #111111;
  margin-bottom: 16px;
  line-height: 1.2;
}

.subtitle {
  font-size: 1.125rem;
  font-weight: 400;
  color: #666666; /* 柔和的次要文本颜色 */
  letter-spacing: 0.01em;
  line-height: 1.6;
}

/* --- 极简卡片设计 --- */
.dashboard-container {
  display: flex;
  justify-content: center;
  animation: fadeIn 1s ease-out 0.2s both; /* 错落出现的动画，增加质感 */
}

.dashboard-card {
  width: 100%;
  max-width: 640px;
  background-color: #ffffff;
  border: 1px solid #eaeaea; /* 极浅的边框代替生硬的背景色 */
  border-radius: 16px;
  padding: 48px;
  text-align: center;
  transition: all 0.3s ease;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.02); /* 几乎察觉不到的弥散阴影 */
}

.dashboard-card:hover {
  border-color: #dcdcdc;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.04);
  transform: translateY(-2px);
}

.card-icon {
  width: 48px;
  height: 48px;
  margin: 0 auto 24px;
  color: #1a1a1a;
  opacity: 0.8;
}

.card-title {
  font-size: 1.25rem;
  font-weight: 500;
  color: #111111;
  margin-bottom: 12px;
  letter-spacing: -0.01em;
}

.card-desc {
  font-size: 1rem;
  color: #888888;
  line-height: 1.6;
  max-width: 80%;
  margin: 0 auto;
}

/* 优雅的入场动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式调整 */
@media (max-width: 640px) {
  .title {
    font-size: 2rem;
  }
  .dashboard-card {
    padding: 32px 20px;
  }
}
</style>