<template>
  <div class="module-page">
    <!-- Topbar -->
    <div class="topbar">
      <div class="topbar-left">
        <button class="back-btn" @click="$router.push('/xingce')">← 返回</button>
        <span class="topbar-label">{{ moduleName }}</span>
        <div class="progress-track" v-if="questions.length > 0">
          <div class="progress-fill" :style="{ width: ((currentIndex + 1) / questions.length * 100) + '%' }"></div>
        </div>
      </div>
      <div class="topbar-right">
        <div class="timer-chip">
          <span class="timer-label">计时</span>
          <span class="timer-val">{{ formatTime(elapsed) }}</span>
        </div>
      </div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <!-- Finish state -->
    <div v-else-if="quizFinished" class="finish-area">
      <div class="finish-icon">✓</div>
      <h3>本轮练习完成</h3>
      <p class="finish-stats">共 {{ questions.length }} 题，答对 {{ correctCount }} 题，正确率 {{ accuracy }}%</p>
      <button class="btn-capsule btn-capsule--active" @click="restartQuiz" :disabled="generating">
        {{ generating ? 'AI 出题中...' : '再来一组' }}
      </button>
    </div>

    <!-- Question area -->
    <div v-else-if="currentQuestion" class="question-area">
      <!-- Question card -->
      <div class="question-card">
        <div class="question-head">
          <span class="q-badge">Q{{ currentIndex + 1 }}</span>
          <span v-if="currentQuestion.difficulty" class="difficulty">
            {{ '★'.repeat(currentQuestion.difficulty || 3) }}{{ '☆'.repeat(5 - (currentQuestion.difficulty || 3)) }}
          </span>
          <span v-if="currentQuestion.source" class="source-tag" :class="currentQuestion.source">
            {{ sourceLabel(currentQuestion.source) }}
          </span>
        </div>
        <p class="q-text">{{ currentQuestion.title }}</p>
      </div>

      <!-- Options -->
      <div class="options">
        <div
          v-for="(opt, idx) in parsedOptions"
          :key="idx"
          class="option"
          :class="{
            'option--selected': selectedAnswer === opt.label,
            'option--correct': showResult && opt.label === currentQuestion.answer,
            'option--wrong': showResult && selectedAnswer === opt.label && opt.label !== currentQuestion.answer
          }"
          @click="selectOption(opt.label)"
        >
          <span class="opt-letter">{{ opt.label }}</span>
          <span class="opt-text">{{ opt.text }}</span>
          <Transition name="check-pop">
            <span v-if="selectedAnswer === opt.label && !showResult" class="opt-check">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><polyline points="20 6 9 17 4 12"/></svg>
            </span>
          </Transition>
        </div>
      </div>

      <!-- Result area -->
      <div v-if="showResult" class="result-area">
        <div class="result-tag" :class="isCorrect ? 'result-tag--correct' : 'result-tag--wrong'">
          {{ isCorrect ? '✓ 回答正确' : '✗ 回答错误' }}
        </div>
        <p class="analysis-text">{{ currentQuestion.analysis }}</p>

        <div v-if="trapFeedback && trapFeedback.trap" class="trap-card">
          <div class="trap-title">⚠ 陷阱警示</div>
          <p class="trap-text">{{ trapFeedback.trapAnalysis }}</p>
          <div class="trap-count">该考点你已踩坑 {{ trapFeedback.trapCount }} 次</div>
        </div>
      </div>

      <!-- Bottom button -->
      <div class="bottom-bar">
        <button
          v-if="!showResult"
          class="btn-capsule"
          :class="{ 'btn-capsule--active': selectedAnswer }"
          :disabled="!selectedAnswer"
          @click="submitAnswer"
        >
          提交答案
        </button>
        <button v-else class="btn-capsule btn-capsule--active" @click="nextQuestion">
          {{ currentIndex === questions.length - 1 ? '查看结果' : '下一题' }}
        </button>
      </div>
    </div>

    <!-- Empty state -->
    <div v-else class="empty">
      <p>该模块暂无题目</p>
      <p v-if="generateError" class="error-msg">{{ generateError }}</p>
      <button class="btn-capsule btn-capsule--active" @click="generateQuestions" :disabled="generating">
        {{ generating ? 'AI 出题中...' : 'AI 出题' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const moduleCode = computed(() => route.params.module)

const MODULE_NAMES = {
  CHANGSHI: '常识', YUYU: '言语理解', ZILIAO: '资料分析',
  TUILI: '推理判断', SHULIANG: '数量关系'
}
const moduleName = computed(() => MODULE_NAMES[moduleCode.value] || moduleCode.value)

const questions = ref([])
const currentIndex = ref(0)
const loading = ref(true)
const generating = ref(false)
const selectedAnswer = ref(null)
const showResult = ref(false)
const isCorrect = ref(false)
const trapFeedback = ref(null)
const generateError = ref('')
const quizFinished = ref(false)

const correctCount = computed(() => questions.value.filter(q => q._isCorrect).length)
const accuracy = computed(() => {
  if (questions.value.length === 0) return 0
  return Math.round((correctCount.value / questions.value.length) * 100)
})
const elapsed = ref(0)
let timer = null

const currentQuestion = computed(() => questions.value[currentIndex.value] || null)

const parsedOptions = computed(() => {
  if (!currentQuestion.value?.optionsJson) return []
  try {
    const opts = JSON.parse(currentQuestion.value.optionsJson)
    return Object.entries(opts).map(([label, text]) => ({ label, text }))
  } catch { return [] }
})

const formatTime = (s) => {
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
}

const sourceLabel = (source) => {
  const labels = { 'SEED': '真题', 'REAL_EXAM': '真题采集', 'AI_EXPAND': 'AI改编', 'AI_GEN': 'AI原创' }
  return labels[source] || ''
}

const selectOption = (label) => {
  if (!showResult.value) selectedAnswer.value = label
}

const submitAnswer = async () => {
  if (!selectedAnswer.value) return
  showResult.value = true
  isCorrect.value = selectedAnswer.value === currentQuestion.value.answer
  currentQuestion.value._isCorrect = isCorrect.value
  trapFeedback.value = null

  try {
    const res = await axios.post('http://localhost:8080/api/xingce/answer', {
      userId: 1,
      questionId: currentQuestion.value.id,
      module: moduleCode.value,
      isCorrect: isCorrect.value,
      userAnswer: selectedAnswer.value,
      timeCostSec: elapsed.value,
      isTrapOption: false
    })
    if (res.data.trapFeedback) {
      trapFeedback.value = res.data.trapFeedback
    }
  } catch (e) { console.error('保存答题记录失败', e) }
}

const nextQuestion = () => {
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value++
    selectedAnswer.value = null
    showResult.value = false
    trapFeedback.value = null
    elapsed.value = 0
  } else {
    quizFinished.value = true
  }
}

const restartQuiz = async () => {
  quizFinished.value = false
  questions.value = []
  currentIndex.value = 0
  selectedAnswer.value = null
  showResult.value = false
  trapFeedback.value = null
  elapsed.value = 0
  await generateQuestions()
}

const generateQuestions = async () => {
  generating.value = true
  generateError.value = ''
  try {
    const res = await axios.post('http://localhost:8080/api/xingce/generate', {
      userId: 1,
      module: moduleCode.value,
      count: 5
    })
    if (res.data.questions && res.data.questions.length > 0) {
      questions.value = res.data.questions
      currentIndex.value = 0
      selectedAnswer.value = null
      showResult.value = false
      trapFeedback.value = null
      elapsed.value = 0
    } else {
      generateError.value = '暂无可用题目，请稍后再试'
    }
  } catch (e) {
    console.error('AI出题失败', e)
    generateError.value = e.code === 'ECONNABORTED' ? 'AI 出题超时，请稍后重试' : '出题失败，请检查网络连接'
  }
  generating.value = false
}

onMounted(async () => {
  timer = setInterval(() => { if (!showResult.value) elapsed.value++ }, 1000)
  try {
    const res = await axios.post('http://localhost:8080/api/xingce/generate', {
      userId: 1,
      module: moduleCode.value,
      count: 5
    })
    questions.value = res.data.questions || []
  } catch (e) { console.error(e) }
  loading.value = false
})

onUnmounted(() => { clearInterval(timer) })
</script>

<style scoped>
/* ===== Page layout ===== */
.module-page {
  max-width: 720px;
  margin: 0 auto;
  padding: var(--space-2) var(--space-3) var(--space-5);
  background: var(--color-bg-page);
  min-height: 100%;
  font-family: var(--font-sans);
  -webkit-font-smoothing: antialiased;
}

/* ===== Topbar ===== */
.topbar {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-3);
}

.topbar-left {
  flex: 1;
  display: flex;
  align-items: center;
  gap: var(--space-2);
  min-width: 0;
}

.back-btn {
  background: none;
  border: 1px solid #eee;
  border-radius: var(--radius-btn);
  padding: 6px 14px;
  cursor: pointer;
  font-size: 0.85rem;
  color: var(--color-text-body);
  transition: border-color 150ms;
}

.back-btn:hover {
  border-color: #ddd;
}

.topbar-label {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--color-text-title);
  white-space: nowrap;
}

.progress-track {
  flex: 1;
  height: 4px;
  background: #eee;
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #111;
  border-radius: 2px;
  transition: width 350ms cubic-bezier(0.4, 0, 0.2, 1);
}

.topbar-right {
  display: flex;
  gap: var(--space-1);
  flex-shrink: 0;
}

.timer-chip {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #111;
  border-radius: var(--radius-btn);
}

.timer-label {
  font-size: 0.7rem;
  color: rgba(255, 255, 255, 0.5);
  font-weight: 500;
}

.timer-val {
  font-size: 0.82rem;
  font-weight: 700;
  color: #fff;
  font-variant-numeric: tabular-nums;
  font-family: var(--font-mono);
}

/* ===== Question card ===== */
.question-card {
  background: var(--color-bg-card);
  border: none;
  border-radius: var(--radius-card);
  box-shadow: var(--shadow-card);
  padding: var(--space-4) var(--space-3);
  margin-bottom: var(--space-2);
}

.question-head {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  margin-bottom: var(--space-2);
}

.q-badge {
  display: inline-block;
  font-size: 0.72rem;
  font-weight: 700;
  color: var(--color-text-caption);
  background: #f5f5f5;
  padding: 3px 10px;
  border-radius: 6px;
  letter-spacing: 0.02em;
}

.difficulty {
  color: #faad14;
  font-size: 0.85rem;
}

.source-tag {
  font-size: 0.72rem;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f0f0f0;
  color: var(--color-text-caption);
}

.source-tag.REAL_EXAM { background: #e6f7ff; color: #1890ff; }
.source-tag.AI_EXPAND { background: #fff7e6; color: #fa8c16; }
.source-tag.AI_GEN { background: #f6ffed; color: #52c41a; }

.q-text {
  font-size: 1.05rem;
  font-weight: 400;
  color: var(--color-text-title);
  line-height: 1.7;
  margin: 0;
}

/* Long text modules: reduce Y padding */
.question-card--compact {
  padding: var(--space-3);
}

/* ===== Options ===== */
.options {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  margin-bottom: var(--space-2);
}

.option {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  width: 100%;
  padding: 14px 16px;
  background: #fafafa;
  border: 1px solid #eee;
  border-radius: var(--radius-btn);
  font-size: 0.95rem;
  color: var(--color-text-body);
  cursor: pointer;
  text-align: left;
  transition: all 150ms cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
}

.option:hover {
  background: #f5f5f5;
  border-color: #ddd;
  transform: translateY(-1px);
}

.option--selected {
  background: #f5f5f5;
  border-color: #111;
  border-width: 1.5px;
  padding: 13.5px 15.5px;
  animation: selectPulse 150ms cubic-bezier(0.34, 1.56, 0.64, 1);
}

@keyframes selectPulse {
  0% { transform: scale(0.98); }
  100% { transform: scale(1); }
}

.option--correct {
  background: #d1fae5;
  border-color: #10b981;
  color: #065f46;
}

.option--wrong {
  background: #fee2e2;
  border-color: #ef4444;
  color: #991b1b;
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
  color: var(--color-text-caption);
  flex-shrink: 0;
  transition: all 150ms;
}

.option--selected .opt-letter {
  background: #111;
  color: #fff;
}

.option--correct .opt-letter {
  background: #10b981;
  color: #fff;
}

.option--wrong .opt-letter {
  background: #ef4444;
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
  color: #111;
}

/* Check pop animation */
.check-pop-enter-active {
  transition: all 200ms cubic-bezier(0.34, 1.56, 0.64, 1);
}
.check-pop-leave-active {
  transition: all 120ms ease-in;
}
.check-pop-enter-from, .check-pop-leave-to {
  opacity: 0;
  transform: scale(0.5);
}

/* ===== Result area ===== */
.result-area {
  background: #fafafa;
  border-radius: var(--radius-card);
  padding: var(--space-2) var(--space-3);
  margin-bottom: var(--space-2);
}

.result-tag {
  font-weight: 600;
  margin-bottom: var(--space-1);
  font-size: 0.95rem;
}

.result-tag--correct { color: #10b981; }
.result-tag--wrong { color: #ef4444; }

.analysis-text {
  color: var(--color-text-body);
  font-size: 0.9rem;
  line-height: 1.7;
  margin: 0;
}

/* Trap card */
.trap-card {
  margin-top: var(--space-2);
  padding: var(--space-2);
  background: #fff2f0;
  border: none;
  border-radius: var(--radius-card);
}

.trap-title {
  font-weight: 700;
  color: #ef4444;
  margin-bottom: 6px;
  font-size: 0.9rem;
}

.trap-text {
  color: var(--color-text-body);
  font-size: 0.88rem;
  line-height: 1.6;
  margin: 0;
}

.trap-count {
  color: var(--color-text-caption);
  font-size: 0.78rem;
  margin-top: var(--space-1);
}

/* ===== Bottom button (capsule) ===== */
.bottom-bar {
  margin-top: var(--space-2);
  display: flex;
  justify-content: center;
}

.btn-capsule {
  width: 100%;
  padding: 14px var(--space-4);
  background: #eee;
  color: #bbb;
  border: none;
  border-radius: var(--radius-full);
  font-size: 0.9rem;
  font-weight: 600;
  cursor: not-allowed;
  transition: all 250ms cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-capsule--active {
  background: #111;
  color: #fff;
  cursor: pointer;
}

.btn-capsule--active:hover {
  background: #333;
  transform: translateY(-1px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

/* ===== Loading & Empty ===== */
.loading, .empty {
  text-align: center;
  padding: 80px 0;
  color: var(--color-text-caption);
}

.error-msg {
  color: #ef4444;
  font-size: 0.9rem;
  margin-bottom: var(--space-2);
}

/* ===== Finish state ===== */
.finish-area {
  text-align: center;
  padding: 80px var(--space-3);
}

.finish-icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #f6ffed;
  color: #10b981;
  font-size: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto var(--space-3);
}

.finish-area h3 {
  font-size: 1.4rem;
  margin: 0 0 var(--space-1);
  color: var(--color-text-title);
}

.finish-stats {
  color: var(--color-text-caption);
  font-size: 0.95rem;
  margin: 0 0 var(--space-3);
}

/* ===== Responsive ===== */
@media (max-width: 767px) {
  .module-page {
    padding: var(--space-2) var(--space-2) var(--space-4);
  }
}

@media (max-width: 640px) {
  .topbar {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-1);
  }
  .topbar-right {
    justify-content: flex-end;
  }
  .question-card {
    padding: var(--space-3) var(--space-2);
  }
  .q-text {
    font-size: 0.95rem;
  }
}
</style>
