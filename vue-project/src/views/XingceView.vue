<template>
  <div class="xingce-page">
    <div class="xingce-header">
      <h1 class="page-title">行测训练</h1>
      <p class="page-desc">选择模块开始练习，AI 将实时诊断你的薄弱环节</p>
    </div>
    <div class="module-grid">
      <div
        v-for="mod in modules"
        :key="mod.code"
        class="module-card"
        @click="$router.push(`/xingce/${mod.code}`)"
      >
        <div class="module-icon-wrapper">
          <svg class="module-icon" width="48" height="48" viewBox="0 0 24 24" fill="none"
            stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
            v-html="mod.iconPath"></svg>
        </div>
        <h3 class="module-name">{{ mod.name }}</h3>
        <p class="module-desc">{{ mod.desc }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
const modules = [
  {
    code: 'CHANGSHI',
    name: '常识',
    desc: '政治、经济、法律、科技、历史',
    iconPath: '<path d="M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z"/><path d="M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z"/>'
  },
  {
    code: 'YUYU',
    name: '言语理解',
    desc: '主旨概括、细节判断、语句排序',
    iconPath: '<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>'
  },
  {
    code: 'ZILIAO',
    name: '资料分析',
    desc: '增长率、比重、倍数、基期量',
    iconPath: '<line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/>'
  },
  {
    code: 'TUILI',
    name: '推理判断',
    desc: '图形推理、逻辑判断、定义判断',
    iconPath: '<polygon points="12 2 22 8.5 22 15.5 12 22 2 15.5 2 8.5 12 2"/><line x1="12" y1="22" x2="12" y2="15.5"/><polyline points="22 8.5 12 15.5 2 8.5"/>'
  },
  {
    code: 'SHULIANG',
    name: '数量关系',
    desc: '工程问题、行程问题、排列组合',
    iconPath: '<rect x="4" y="2" width="16" height="20" rx="2"/><line x1="8" y1="6" x2="16" y2="6"/><line x1="8" y1="10" x2="16" y2="10"/><line x1="8" y1="14" x2="12" y2="14"/>'
  }
]
</script>

<style scoped>
.xingce-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: var(--space-5) var(--space-3);
  background: var(--color-bg-page);
  min-height: 100%;
}

.xingce-header {
  margin-bottom: var(--space-4);
}

.page-title {
  font-size: 1.8rem;
  font-weight: 600;
  color: var(--color-text-title);
  margin: 0 0 var(--space-1);
  letter-spacing: -0.02em;
}

.page-desc {
  color: var(--color-text-caption);
  margin: 0;
  font-size: 0.95rem;
}

/* 2x2 Grid */
.module-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-3);
}

/* Card — borderless, diffuse shadow */
.module-card {
  background: var(--color-bg-card);
  border: none;
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  padding: var(--space-3);
  cursor: pointer;
  transition: all 150ms cubic-bezier(0.4, 0, 0.2, 1);
}

.module-card:hover {
  box-shadow: var(--shadow-card-hover);
  transform: translateY(-2px);
}

.module-card:active {
  transform: scale(0.98);
}

/* Icon with blob background */
.module-icon-wrapper {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(0, 0, 0, 0.03) 0%, transparent 70%);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--space-2);
}

.module-icon {
  color: var(--color-text-title);
}

/* Text hierarchy */
.module-name {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-text-title);
  margin: 0 0 4px;
}

.module-desc {
  font-size: 0.82rem;
  font-weight: 400;
  color: var(--color-text-caption);
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* Responsive */
@media (max-width: 767px) {
  .xingce-page {
    padding: var(--space-3) var(--space-2);
  }
  .module-grid {
    grid-template-columns: 1fr;
    gap: var(--space-2);
  }
}

@media (max-width: 640px) {
  .module-card {
    padding: var(--space-2);
  }
}
</style>
