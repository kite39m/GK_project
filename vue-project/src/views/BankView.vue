<template>
  <div class="page-container">
    <h2>专项题库中心</h2>
    <p class="desc">请选择你要攻克的模块：</p >
    <div class="module-list">
      
      <div 
        class="module-card mistake-card" 
        @click="$router.push({ path: '/practice', query: { mode: 'mistake' } })"
      >
        ⚔️ 清理我的错题本
      </div>

      <div class="module-card">行测：资料分析（速算）</div>
      <div class="module-card">行测：言语理解</div>
      <div class="module-card">申论：金句素材背诵</div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  padding: 40px;
  max-width: 800px;
  margin: 0 auto;
}
.desc {
  color: #606266;
  margin-bottom: 20px;
}
.module-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}
.module-card {
  padding: 20px;
  background-color: #f0f9eb;
  border: 1px solid #67c23a;
  border-radius: 8px;
  color: #67c23a;
  font-weight: bold;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.2s;
}
.module-card:hover {
  background-color: #67c23a;
  color: white;
  transform: translateX(10px);
}
.mistake-card {
  background-color: #fef0f0;
  border-color: #f56c6c;
  color: #f56c6c;
}
.mistake-card:hover {
  background-color: #f56c6c;
  color: white;
}
</style>