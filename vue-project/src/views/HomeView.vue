<template>
  <div class="dashboard">
    <div class="dashboard-inner">

      <!-- Loading -->
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>正在加载数据看板...</p>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="error-state">
        <p>数据加载失败，请确认后端已启动</p>
        <button class="btn-retry" @click="fetchSummary">重试</button>
      </div>

      <!-- Dashboard Content -->
      <template v-else>
        <header class="hero">
          <h1 class="hero-title">欢迎回来</h1>
          <p class="hero-subtitle">遇见不一样的自己</p>
        </header>

        <!-- Stats Row -->
        <div class="stats-row">
          <div class="stat-card">
            <div class="stat-label">总题量</div>
            <div class="stat-value">{{ summary.totalQuestions }}</div>
          </div>
          <div class="stat-card">
            <div class="stat-label">正确率</div>
            <div class="stat-value" :class="accuracyClass">
              {{ summary.accuracy }}<span class="unit">%</span>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-label">平均用时</div>
            <div class="stat-value">{{ avgTimeDisplay }}<span class="unit">/题</span></div>
          </div>
          <div class="stat-card">
            <div class="stat-label">综合熟练度</div>
            <div class="stat-value" :class="masteryClass">
              {{ overallMastery }}<span class="unit">%</span>
            </div>
          </div>
        </div>

        <!-- Bento Grid -->
        <div class="bento-grid">
          <div class="bento-card chart-card">
            <h3 class="card-heading">能力画像</h3>
            <p class="card-desc">各模块熟练度雷达图</p>
            <div ref="chartRef" class="chart-container"></div>
          </div>

          <div class="bento-card actions-card">
            <h3 class="card-heading">快速操作</h3>
            <p class="card-desc">继续你的学习计划</p>
            <div class="action-list">
              <button class="action-btn primary" @click="$router.push('/practice')">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polygon points="5 3 19 12 5 21 5 3"></polygon></svg>
                开始随机模考
              </button>
              <button class="action-btn secondary" @click="$router.push('/practice?mode=mistake')">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2L2 7l10 5 10-5-10-5z"></path><path d="M2 17l10 5 10-5"></path><path d="M2 12l10 5 10-5"></path></svg>
                清理错题本
              </button>
              <button class="action-btn tertiary" @click="$router.push('/bank')">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 19.5A2.5 2.5 0 0 1 6.5 17H20"></path><path d="M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z"></path></svg>
                选岗与备考策略
              </button>
            </div>
          </div>
        </div>
      </template>

    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import axios from 'axios'
import * as echarts from 'echarts'

const chartRef = ref(null)
let chartInstance = null

const loading = ref(true)
const error = ref(false)
const summary = ref({
  totalQuestions: 0,
  accuracy: 0,
  avgTime: 0,
  skillValues: [100, 100, 100, 100, 100]
})

const overallMastery = computed(() => {
  const vals = summary.value.skillValues
  if (!vals || vals.length === 0) return 0
  return Math.round(vals.reduce((a, b) => a + b, 0) / vals.length)
})

const accuracyClass = computed(() => {
  const v = summary.value.accuracy
  if (v >= 80) return 'text-success'
  if (v >= 50) return 'text-warning'
  return 'text-danger'
})

const masteryClass = computed(() => {
  const v = overallMastery.value
  if (v >= 80) return 'text-success'
  if (v >= 50) return 'text-warning'
  return 'text-danger'
})

const avgTimeDisplay = computed(() => {
  return summary.value.avgTime > 0 ? summary.value.avgTime : '--'
})

const radarIndicators = [
  { name: '言语理解', max: 100 },
  { name: '数量关系', max: 100 },
  { name: '判断推理', max: 100 },
  { name: '资料分析', max: 100 },
  { name: '常识判断', max: 100 }
]

const CHART_COLOR = '#111111'

const initChart = () => {
  if (!chartRef.value) return
  if (chartInstance) chartInstance.dispose()

  chartInstance = echarts.init(chartRef.value)
  const option = {
    radar: {
      indicator: radarIndicators,
      center: ['50%', '50%'],
      radius: '70%',
      splitNumber: 4,
      axisName: {
        color: '#666',
        fontSize: 12,
        fontWeight: 500
      },
      splitArea: {
        areaStyle: {
          color: ['rgba(0,0,0,0.02)', 'rgba(0,0,0,0.04)']
        }
      },
      axisLine: {
        lineStyle: { color: 'rgba(0,0,0,0.08)' }
      },
      splitLine: {
        lineStyle: { color: 'rgba(0,0,0,0.08)' }
      }
    },
    series: [{
      type: 'radar',
      data: [{
        value: summary.value.skillValues,
        name: '熟练度',
        areaStyle: {
          color: 'rgba(0,0,0,0.08)'
        },
        lineStyle: {
          color: CHART_COLOR,
          width: 2
        },
        itemStyle: {
          color: CHART_COLOR
        }
      }],
      symbol: 'circle',
      symbolSize: 6
    }]
  }

  chartInstance.setOption(option)
  chartInstance.resize()
}

const handleResize = () => {
  chartInstance?.resize()
}

const fetchSummary = async () => {
  loading.value = true
  error.value = false
  try {
    const res = await axios.get('http://localhost:8080/api/stats/summary')
    summary.value = res.data
  } catch {
    error.value = true
    summary.value = { totalQuestions: 0, accuracy: 0, avgTime: 0, skillValues: [100, 100, 100, 100, 100] }
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchSummary()
  await nextTick()
  initChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  chartInstance?.dispose()
})

watch(() => summary.value.skillValues, () => {
  nextTick(() => initChart())
})
</script>

<style scoped>
/* ===== Layout ===== */
.dashboard {
  min-height: 100vh;
  background-color: #ffffff;
  display: flex;
  justify-content: center;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  color: #1a1a1a;
}

.dashboard-inner {
  width: 100%;
  max-width: 960px;
  padding: 10vh 24px 60px;
}

/* ===== Loading / Error ===== */
.loading-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120px 0;
  color: #888;
  gap: 16px;
}

.spinner {
  width: 32px; height: 32px;
  border: 3px solid #f3f3f3; border-top: 3px solid #111;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }

.btn-retry {
  padding: 10px 28px;
  border-radius: 999px;
  border: 1px solid #ddd;
  background: #fff;
  color: #333;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-retry:hover { background: #f5f5f5; }

/* ===== Hero ===== */
.hero {
  text-align: center;
  margin-bottom: 48px;
  animation: fadeIn 0.6s ease-out;
}

.hero-title {
  font-size: 2.25rem;
  font-weight: 600;
  letter-spacing: -0.02em;
  color: #111;
  margin-bottom: 8px;
  line-height: 1.2;
}

.hero-subtitle {
  font-size: 1.05rem;
  color: #888;
  font-weight: 400;
}

/* ===== Stats Row ===== */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 32px;
  animation: fadeIn 0.6s ease-out 0.1s both;
}

.stat-card {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 16px;
  padding: 24px 20px;
  text-align: center;
  transition: all 0.25s ease;
}

.stat-card:hover {
  border-color: #e0e0e0;
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.03);
}

.stat-label {
  font-size: 13px;
  font-weight: 500;
  color: #999;
  margin-bottom: 10px;
  letter-spacing: 0.02em;
}

.stat-value {
  font-size: 2rem;
  font-weight: 700;
  color: #111;
  letter-spacing: -0.02em;
  line-height: 1;
}

.stat-value .unit {
  font-size: 0.9rem;
  font-weight: 500;
  color: #bbb;
}

.text-success { color: #10b981; }
.text-warning { color: #f59e0b; }
.text-danger  { color: #ef4444; }

/* ===== Bento Grid ===== */
.bento-grid {
  display: grid;
  grid-template-columns: 1.6fr 1fr;
  gap: 20px;
  animation: fadeIn 0.6s ease-out 0.2s both;
}

.bento-card {
  background: #ffffff;
  border: 1px solid #eaeaea;
  border-radius: 20px;
  padding: 28px;
  transition: all 0.25s ease;
}

.bento-card:hover {
  border-color: #dcdcdc;
  box-shadow: 0 8px 32px rgba(0,0,0,0.03);
}

.card-heading {
  font-size: 1.15rem;
  font-weight: 600;
  color: #111;
  margin-bottom: 4px;
  letter-spacing: -0.01em;
}

.card-desc {
  font-size: 0.9rem;
  color: #aaa;
  margin-bottom: 20px;
}

/* ===== Radar Chart ===== */
.chart-container {
  width: 100%;
  height: 300px;
}

/* ===== Quick Actions ===== */
.action-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 16px 20px;
  border-radius: 14px;
  border: 1px solid #eaeaea;
  background: #fff;
  font-size: 15px;
  font-weight: 500;
  color: #333;
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
}

.action-btn:hover {
  transform: translateY(-1px);
}

.action-btn.primary {
  background: #111;
  color: #fff;
  border-color: #111;
}
.action-btn.primary:hover { background: #2a2a2a; }

.action-btn.secondary {
  background: #fef8f8;
  color: #dc2626;
  border-color: #fee2e2;
}
.action-btn.secondary:hover { background: #fef0f0; }

.action-btn.tertiary {
  background: #fafafa;
}
.action-btn.tertiary:hover { background: #f0f0f0; }

/* ===== Animation ===== */
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(10px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .dashboard-inner { padding: 6vh 16px 40px; }
  .hero-title { font-size: 1.75rem; }
  .stats-row { grid-template-columns: repeat(2, 1fr); gap: 12px; }
  .bento-grid { grid-template-columns: 1fr; }
  .stat-value { font-size: 1.6rem; }
}
</style>
