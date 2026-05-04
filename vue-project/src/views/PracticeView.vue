<template>
  <div class="practice-container">
    <Transition name="fade-up" mode="out-in">
      <!-- ========== 阶段一：设置 ========== -->
      <div v-if="practiceState === 'setup'" key="setup" class="setup-phase">
        <div class="bg-blob"></div>
        <div class="setup-card fade-in">
          <div class="setup-header">
            <div class="brand">
              <span class="logo-dot"></span>
              <span class="logo-text">SpeedCalc</span>
            </div>
            <h1 class="setup-title">赵文阳的专属备考智库</h1>
            <p class="setup-subtitle">精准攻克行测速算</p>
          </div>

          <div class="setup-form">
            <div class="form-item">
              <label class="form-label">题型选择</label>
              <div class="select-wrapper">
                <select v-model="selectedType" class="setup-select">
                  <option value="资料分析基期量计算">资料分析基期量计算</option>
                  <option value="截位直除">截位直除</option>
                  <option value="增长量计算">增长量计算</option>
                  <option value="多位数乘除">多位数乘除</option>
                  <option value="分数比较大小">分数比较大小</option>
                  <option value="比重计算">比重计算</option>
                </select>
                <svg class="select-arrow" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#999" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <polyline points="6 9 12 15 18 9"/>
                </svg>
              </div>
            </div>

            <button class="btn-start" @click="startPractice" :disabled="loading">
              <span v-if="loading" class="spinner"></span>
              <template v-else>开始生成</template>
            </button>
          </div>
        </div>
      </div>

      <!-- ========== 阶段二：答题 ========== -->
      <div v-else-if="practiceState === 'practicing'" key="practicing" class="practice-phase">
        <!-- 顶部状态栏 -->
        <div class="topbar">
          <div class="topbar-left">
            <span class="topbar-label">第 <b>{{ currentIndex + 1 }}</b> / {{ questions.length }} 题</span>
            <div class="progress-track">
              <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
            </div>
          </div>
          <div class="topbar-right">
            <div class="timer-chip">
              <span class="timer-label">本题</span>
              <span class="timer-val">{{ currentQuestionTime }}s</span>
            </div>
            <div class="timer-chip timer-chip--dark">
              <span class="timer-label">总用时</span>
              <span class="timer-val">{{ totalTimeFormatted }}</span>
            </div>
          </div>
        </div>

        <!-- 题目卡片 -->
        <Transition name="slide-fade" mode="out-in">
          <div :key="currentIndex" class="question-card">
            <span class="q-badge">Q{{ currentIndex + 1 }}</span>
            <p class="q-text">{{ currentQuestion.question }}</p>

            <div class="options">
              <button
                v-for="(opt, idx) in currentQuestion.options"
                :key="idx"
                class="opt-btn"
                :class="{ 'opt-btn--active': userAnswer === opt }"
                @click="selectOption(opt)"
              >
                <span class="opt-letter" :class="{ 'opt-letter--active': userAnswer === opt }">{{ ['A','B','C','D'][idx] }}</span>
                <span class="opt-text">{{ opt }}</span>
                <Transition name="check-pop">
                  <span v-if="userAnswer === opt" class="opt-check">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#111" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                      <polyline points="20 6 9 17 4 12"/>
                    </svg>
                  </span>
                </Transition>
              </button>
            </div>
          </div>
        </Transition>

        <!-- 底部按钮 -->
        <div class="bottom-bar">
          <button class="btn-next" :class="{ 'btn-next--active': userAnswer }" :disabled="!userAnswer" @click="nextQuestion">
            {{ currentIndex === questions.length - 1 ? '提交并查看报告' : '下一题' }}
          </button>
        </div>
      </div>

      <!-- ========== 阶段三：复盘 ========== -->
      <div v-else-if="practiceState === 'result'" key="result" class="result-phase">
        <!-- 数据看板 -->
        <div class="stats-row">
          <div class="stat-card">
            <div class="stat-value">{{ accuracy }}<span class="stat-unit">%</span></div>
            <div class="stat-label">正确率</div>
            <div class="stat-sub">{{ correctCount }}/{{ questions.length }} 题</div>
          </div>
          <div class="stat-card">
            <div class="stat-value stat-value--mono">{{ totalTimeFormatted }}</div>
            <div class="stat-label">总耗时</div>
          </div>
          <div class="stat-card">
            <div class="stat-value stat-value--mono">{{ avgTimePerQuestion }}<span class="stat-unit">s</span></div>
            <div class="stat-label">平均用时</div>
          </div>
        </div>

        <!-- 解析列表 -->
        <div class="review-list">
          <div
            v-for="(q, idx) in questions"
            :key="idx"
            class="review-card"
            :class="q.isCorrect ? 'review-card--ok' : 'review-card--fail'"
          >
            <div class="review-head">
              <span class="review-idx">Q{{ idx + 1 }}</span>
              <span class="review-badge" :class="q.isCorrect ? 'badge-ok' : 'badge-fail'">
                {{ q.isCorrect ? '正确' : '错误' }}
              </span>
              <span class="review-time">{{ q.timeSpent }}s</span>
            </div>

            <p class="review-question">{{ q.question }}</p>

            <div class="review-opts">
              <div
                v-for="(opt, oi) in q.options"
                :key="oi"
                class="review-opt"
                :class="{
                  'review-opt--correct': opt === q.exactAnswer,
                  'review-opt--wrong': opt === q.userAnswer && !q.isCorrect,
                  'review-opt--dim': opt !== q.exactAnswer && opt !== q.userAnswer
                }"
              >
                <span class="review-opt-l">{{ ['A','B','C','D'][oi] }}</span>
                <span class="review-opt-t">{{ opt }}</span>
                <span v-if="opt === q.exactAnswer" class="review-opt-icon review-opt-icon--ok">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
                </span>
                <span v-else-if="opt === q.userAnswer && !q.isCorrect" class="review-opt-icon review-opt-icon--fail">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
                </span>
              </div>
            </div>

            <div class="review-analysis">
              <span class="skill-tag">{{ q.fastCalcSkill }}</span>
              <div class="analysis-body">
                <span class="analysis-label">解析</span>
                <p class="analysis-text">{{ q.analysis }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- 底部 -->
        <div class="result-bottom">
          <button class="btn-restart" @click="restart">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
            </svg>
            重新练习
          </button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'

const practiceState = ref('setup')
const loading = ref(false)
const selectedType = ref('资料分析基期量计算')

const questions = ref([])
const currentIndex = ref(0)
const userAnswer = ref(null)

const currentQuestionTime = ref(0)
const totalSeconds = ref(0)
let questionTimer = null
let totalTimer = null

const currentQuestion = computed(() => questions.value[currentIndex.value])
const correctCount = computed(() => questions.value.filter(q => q.isCorrect).length)
const accuracy = computed(() => {
  if (questions.value.length === 0) return 0
  return Math.round((correctCount.value / questions.value.length) * 100)
})
const totalTimeFormatted = computed(() => {
  const m = Math.floor(totalSeconds.value / 60)
  const s = totalSeconds.value % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})
const avgTimePerQuestion = computed(() => {
  if (questions.value.length === 0) return 0
  return Math.round(totalSeconds.value / questions.value.length)
})
const progressPercent = computed(() => {
  if (questions.value.length === 0) return 0
  return ((currentIndex.value + 1) / questions.value.length) * 100
})

function generateDistractors(exactAnswer) {
  const answer = parseFloat(exactAnswer)
  if (isNaN(answer)) {
    return shuffleArray([exactAnswer, exactAnswer + '%', '≈' + exactAnswer, exactAnswer + '万'])
  }
  const distractors = new Set()
  const multipliers = [0.9, 0.95, 1.05, 1.1, 1.15, 0.85, 1.2, 0.92]
  let i = 0
  while (distractors.size < 3 && i < multipliers.length) {
    const val = answer * multipliers[i]
    const rounded = Math.round(val * 100) / 100
    if (rounded !== answer && rounded > 0) distractors.add(String(rounded))
    i++
  }
  let fallback = 1
  while (distractors.size < 3) {
    const val = answer + fallback * (answer > 0 ? 1 : -1) * Math.ceil(answer * 0.1)
    const rounded = Math.round(val * 100) / 100
    if (rounded !== answer) distractors.add(String(rounded))
    fallback++
  }
  return shuffleArray([exactAnswer, ...distractors])
}

function shuffleArray(arr) {
  const a = [...arr]
  for (let i = a.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [a[i], a[j]] = [a[j], a[i]]
  }
  return a
}

function startTimers() {
  currentQuestionTime.value = 0
  totalSeconds.value = 0
  questionTimer = setInterval(() => { currentQuestionTime.value++ }, 1000)
  totalTimer = setInterval(() => { totalSeconds.value++ }, 1000)
}
function resetQuestionTimer() { currentQuestionTime.value = 0 }
function stopTimers() {
  clearInterval(questionTimer)
  clearInterval(totalTimer)
  questionTimer = null
  totalTimer = null
}
onBeforeUnmount(stopTimers)

async function startPractice() {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const headers = {}
    if (token) headers['Authorization'] = `Bearer ${token}`
    const url = `http://localhost:8080/api/practice/speed-calc/generate?type=${encodeURIComponent(selectedType.value)}&count=10`
    const res = await fetch(url, { headers })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const data = await res.json()
    questions.value = data.map(q => ({
      ...q,
      options: generateDistractors(q.exactAnswer),
      userAnswer: null,
      isCorrect: false,
      timeSpent: 0,
    }))
    currentIndex.value = 0
    userAnswer.value = null
    practiceState.value = 'practicing'
    startTimers()
  } catch (error) {
    ElMessage.error('生成失败，请确认后端已启动')
    console.error(error)
  } finally {
    loading.value = false
  }
}

function selectOption(opt) { userAnswer.value = opt }

function nextQuestion() {
  if (!userAnswer.value) return
  const q = questions.value[currentIndex.value]
  q.userAnswer = userAnswer.value
  q.isCorrect = userAnswer.value === q.exactAnswer
  q.timeSpent = currentQuestionTime.value
  if (currentIndex.value >= questions.value.length - 1) {
    stopTimers()
    practiceState.value = 'result'
    return
  }
  currentIndex.value++
  userAnswer.value = null
  resetQuestionTimer()
}

function restart() {
  stopTimers()
  questions.value = []
  currentIndex.value = 0
  userAnswer.value = null
  currentQuestionTime.value = 0
  totalSeconds.value = 0
  practiceState.value = 'setup'
}
</script>

<style scoped>
/* ==================== Transitions ==================== */
.fade-up-enter-active, .fade-up-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.fade-up-enter-from {
  opacity: 0; transform: translateY(16px);
}
.fade-up-leave-to {
  opacity: 0; transform: translateY(-12px);
}

.slide-fade-enter-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.slide-fade-leave-active {
  transition: all 0.18s cubic-bezier(0.4, 0, 0.2, 1);
}
.slide-fade-enter-from {
  opacity: 0; transform: translateX(20px);
}
.slide-fade-leave-to {
  opacity: 0; transform: translateX(-20px);
}

.check-pop-enter-active {
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.check-pop-leave-active {
  transition: all 0.12s ease-in;
}
.check-pop-enter-from, .check-pop-leave-to {
  opacity: 0; transform: scale(0.5);
}

.fade-in {
  animation: fadeIn 0.6s ease-out;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

/* ==================== 阶段一：设置 ==================== */
.practice-container {
  flex: 1;
  min-height: 0;
  background: #fff;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
  -webkit-font-smoothing: antialiased;
  overflow-y: auto;
}

.setup-phase {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100%;
  padding: 24px;
  position: relative;
  overflow: hidden;
}

.bg-blob {
  position: absolute;
  width: 500px;
  height: 500px;
  background: radial-gradient(circle, rgba(170, 59, 255, 0.06) 0%, rgba(255, 255, 255, 0) 70%);
  top: -80px;
  right: -80px;
  filter: blur(60px);
  z-index: 0;
}

.setup-card {
  width: 100%;
  max-width: 420px;
  padding: 48px 40px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 0, 0, 0.05);
  border-radius: 24px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.03);
  z-index: 1;
}

.setup-header {
  text-align: center;
  margin-bottom: 40px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
}

.logo-dot {
  width: 10px;
  height: 10px;
  background: #111;
  border-radius: 50%;
}

.logo-text {
  font-weight: 700;
  font-size: 0.95rem;
  letter-spacing: -0.02em;
  color: #111;
}

.setup-title {
  font-size: 1.6rem;
  font-weight: 600;
  color: #111;
  margin: 0 0 8px;
  letter-spacing: -0.02em;
}

.setup-subtitle {
  font-size: 0.9rem;
  color: #999;
  margin: 0;
}

.setup-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-item {
  margin-bottom: 20px;
}

.form-label {
  display: block;
  font-size: 0.82rem;
  font-weight: 600;
  color: #444;
  margin-bottom: 8px;
  margin-left: 2px;
}

.select-wrapper {
  position: relative;
}

.setup-select {
  width: 100%;
  padding: 14px 42px 14px 16px;
  background: #f9f9f9;
  border: 1px solid #eee;
  border-radius: 12px;
  font-size: 0.95rem;
  color: #111;
  outline: none;
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;
  transition: border-color 0.2s, background 0.2s;
}

.setup-select:focus {
  border-color: #111;
  background: #fff;
}

.select-arrow {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  pointer-events: none;
}

.btn-start {
  width: 100%;
  padding: 16px;
  background: #111;
  color: #fff;
  border: none;
  border-radius: 14px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-start:hover:not(:disabled) {
  background: #333;
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

.btn-start:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

/* ==================== 阶段二：答题 ==================== */
.practice-phase {
  max-width: 680px;
  margin: 0 auto;
  padding: 20px 20px 40px;
}

.topbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.topbar-left {
  flex: 1;
  min-width: 0;
}

.topbar-label {
  display: block;
  font-size: 0.82rem;
  color: #999;
  margin-bottom: 8px;
  font-weight: 500;
}

.topbar-label b {
  color: #111;
  font-size: 0.95rem;
}

.progress-track {
  height: 4px;
  background: #eee;
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #111;
  border-radius: 2px;
  transition: width 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

.topbar-right {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}

.timer-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #f5f5f5;
  border-radius: 8px;
}

.timer-chip--dark {
  background: #111;
}

.timer-label {
  font-size: 0.7rem;
  color: #999;
  font-weight: 500;
}

.timer-chip--dark .timer-label {
  color: rgba(255, 255, 255, 0.5);
}

.timer-val {
  font-size: 0.82rem;
  font-weight: 700;
  color: #111;
  font-variant-numeric: tabular-nums;
  font-family: ui-monospace, "SF Mono", "Cascadia Code", monospace;
}

.timer-chip--dark .timer-val {
  color: #fff;
}

/* 题目卡片 */
.question-card {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 18px;
  padding: 32px 28px;
}

.q-badge {
  display: inline-block;
  font-size: 0.72rem;
  font-weight: 700;
  color: #999;
  background: #f5f5f5;
  padding: 3px 10px;
  border-radius: 6px;
  margin-bottom: 16px;
  letter-spacing: 0.02em;
}

.q-text {
  font-size: 1.15rem;
  font-weight: 600;
  color: #111;
  line-height: 1.7;
  margin: 0 0 28px;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.opt-btn {
  display: flex;
  align-items: center;
  gap: 14px;
  width: 100%;
  padding: 15px 16px;
  background: #fafafa;
  border: 1px solid #eee;
  border-radius: 12px;
  font-size: 0.95rem;
  color: #333;
  cursor: pointer;
  text-align: left;
  transition: all 0.18s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.opt-btn:hover {
  background: #f5f5f5;
  border-color: #ddd;
  transform: translateY(-1px);
}

.opt-btn--active {
  background: #f5f5f5;
  border-color: #111;
  border-width: 1.5px;
  padding: 14.5px 15.5px;
}

.opt-letter {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: #eee;
  font-size: 0.75rem;
  font-weight: 800;
  color: #999;
  flex-shrink: 0;
  transition: all 0.18s;
}

.opt-letter--active {
  background: #111;
  color: #fff;
}

.opt-text {
  flex: 1;
  font-weight: 500;
}

.opt-check {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

/* 底部按钮 */
.bottom-bar {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.btn-next {
  width: 100%;
  padding: 14px 32px;
  background: #eee;
  color: #bbb;
  border: none;
  border-radius: 999px;
  font-size: 0.9rem;
  font-weight: 600;
  cursor: not-allowed;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-next--active {
  background: #111;
  color: #fff;
  cursor: pointer;
}

.btn-next--active:hover {
  background: #333;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

/* ==================== 阶段三：复盘 ==================== */
.result-phase {
  max-width: 680px;
  margin: 0 auto;
  padding: 24px 20px 80px;
}

.stats-row {
  display: grid;
  grid-template-columns: 1.2fr 1fr 1fr;
  gap: 10px;
  margin-bottom: 28px;
}

.stat-card {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 16px;
  padding: 20px 16px;
  text-align: center;
}

.stat-value {
  font-size: 1.8rem;
  font-weight: 700;
  color: #111;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}

.stat-value--mono {
  font-family: ui-monospace, "SF Mono", monospace;
}

.stat-unit {
  font-size: 1rem;
  font-weight: 500;
  color: #999;
}

.stat-label {
  font-size: 0.78rem;
  color: #999;
  margin-top: 6px;
  font-weight: 500;
}

.stat-sub {
  font-size: 0.72rem;
  color: #bbb;
  margin-top: 2px;
}

/* 解析列表 */
.review-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.review-card {
  background: #fff;
  border: 1px solid #eee;
  border-radius: 16px;
  padding: 24px;
}

.review-card--ok {
  border-left: 3px solid #10b981;
}

.review-card--fail {
  border-left: 3px solid #ef4444;
}

.review-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.review-idx {
  font-size: 0.75rem;
  font-weight: 700;
  color: #bbb;
  background: #f5f5f5;
  padding: 2px 8px;
  border-radius: 4px;
  font-variant-numeric: tabular-nums;
}

.review-badge {
  font-size: 0.72rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
}

.badge-ok {
  background: #d1fae5;
  color: #059669;
}

.badge-fail {
  background: #fee2e2;
  color: #dc2626;
}

.review-time {
  margin-left: auto;
  font-size: 0.78rem;
  font-weight: 600;
  color: #ccc;
  font-variant-numeric: tabular-nums;
  font-family: ui-monospace, "SF Mono", monospace;
}

.review-question {
  font-size: 0.95rem;
  font-weight: 600;
  color: #111;
  line-height: 1.6;
  margin: 0 0 14px;
}

.review-opts {
  display: flex;
  flex-direction: column;
  gap: 5px;
  margin-bottom: 14px;
}

.review-opt {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 0.88rem;
  font-weight: 500;
  border: 1px solid transparent;
}

.review-opt--dim {
  background: #fafafa;
  color: #bbb;
}

.review-opt--correct {
  background: #d1fae5;
  border-color: #10b981;
  color: #065f46;
  font-weight: 600;
}

.review-opt--wrong {
  background: #fee2e2;
  border-color: #ef4444;
  color: #991b1b;
  font-weight: 600;
}

.review-opt-l {
  font-weight: 800;
  font-size: 0.75rem;
  width: 22px;
  text-align: center;
  flex-shrink: 0;
}

.review-opt-t {
  flex: 1;
}

.review-opt-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.review-opt-icon--ok { color: #10b981; }
.review-opt-icon--fail { color: #ef4444; }

/* 解析区 */
.review-analysis {
  background: #fafafa;
  border-radius: 10px;
  padding: 14px;
}

.skill-tag {
  display: inline-block;
  background: #111;
  color: #fff;
  font-size: 0.72rem;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 6px;
  margin-bottom: 10px;
}

.analysis-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.analysis-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: #999;
}

.analysis-text {
  font-size: 0.85rem;
  color: #555;
  line-height: 1.7;
  margin: 0;
}

/* 底部 */
.result-bottom {
  margin-top: 32px;
  text-align: center;
}

.btn-restart {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 13px 36px;
  background: #111;
  color: #fff;
  border: none;
  border-radius: 14px;
  font-size: 0.95rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s;
}

.btn-restart:hover {
  background: #333;
  transform: translateY(-1px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
}

/* ==================== 响应式 ==================== */
@media (max-width: 640px) {
  .setup-card {
    padding: 36px 24px;
  }
  .setup-title {
    font-size: 1.3rem;
  }
  .topbar {
    flex-direction: column;
    align-items: stretch;
  }
  .topbar-right {
    justify-content: flex-end;
  }
  .question-card {
    padding: 24px 20px;
  }
  .q-text {
    font-size: 1rem;
  }
  .stats-row {
    grid-template-columns: 1fr;
    gap: 8px;
  }
  .review-card {
    padding: 18px 16px;
  }
}
</style>
