<template>
  <div class="xingce-page">
    <h1 class="page-title">行测训练</h1>
    <p class="page-desc">选择模块开始练习，AI 将实时诊断你的薄弱环节</p>
    <div class="module-grid">
      <div
        v-for="mod in modules"
        :key="mod.code"
        class="module-card"
        @click="$router.push(`/xingce/${mod.code}`)"
      >
        <div class="module-icon">{{ mod.icon }}</div>
        <h3>{{ mod.name }}</h3>
        <p>{{ mod.desc }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
const modules = [
  { code: 'CHANGSHI', name: '常识', icon: '📚', desc: '政治、经济、法律、科技、历史' },
  { code: 'YUYU', name: '言语理解', icon: '💬', desc: '主旨概括、细节判断、语句排序' },
  { code: 'ZILIAO', name: '资料分析', icon: '📊', desc: '增长率、比重、倍数、基期量' },
  { code: 'TUILI', name: '推理判断', icon: '🧩', desc: '图形推理、逻辑判断、定义判断' },
  { code: 'SHULIANG', name: '数量关系', icon: '🔢', desc: '工程问题、行程问题、排列组合' }
]
</script>

<style scoped>
.xingce-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 40px 24px;
}
.page-title {
  font-size: 1.8rem;
  font-weight: 700;
  color: #111;
  margin: 0 0 8px;
}
.page-desc {
  color: #888;
  margin: 0 0 32px;
}
.module-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}
.module-card {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 16px;
  padding: 28px 24px;
  cursor: pointer;
  transition: all 0.2s;
}
.module-card:hover {
  border-color: #111;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  transform: translateY(-2px);
}
.module-icon {
  font-size: 2rem;
  margin-bottom: 12px;
}
.module-card h3 {
  margin: 0 0 6px;
  font-size: 1.1rem;
  color: #111;
}
.module-card p {
  margin: 0;
  font-size: 0.85rem;
  color: #999;
}
</style>
