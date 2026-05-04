# 行测训练 UI 重设计 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign XingceView and XingceModuleView with a unified design system, 8pt grid, borderless cards with diffuse shadows, and polished micro-interactions.

**Architecture:** Create a shared `tokens.css` design system file, then rewrite the two view components' template and style sections while preserving all existing business logic (API calls, state management, quiz flow).

**Tech Stack:** Vue 3 Composition API, CSS Custom Properties, CSS Grid

---

## File Structure

| Action | File | Responsibility |
|---|---|---|
| Create | `vue-project/src/styles/tokens.css` | Design system variables (colors, spacing, shadows, radius, typography, scrollbar, focus, dark mode) |
| Modify | `vue-project/src/main.js:1-5` | Import tokens.css globally |
| Rewrite | `vue-project/src/views/XingceView.vue` | Module selection page: 2x2 grid, SVG icons, borderless cards |
| Rewrite | `vue-project/src/views/XingceModuleView.vue` | Quiz page: topbar, question card, options, analysis, finish state |

---

### Task 1: Create Design Tokens (`tokens.css`)

**Files:**
- Create: `vue-project/src/styles/tokens.css`

- [ ] **Step 1: Create styles directory and tokens file**

```bash
mkdir -p vue-project/src/styles
```

- [ ] **Step 2: Write tokens.css with all design system variables**

Create `vue-project/src/styles/tokens.css` with the following content:

```css
/* ============================================
   Design Tokens — 8pt Grid System
   ============================================ */

:root {
  /* --- Spacing (8pt grid) --- */
  --space-1: 8px;
  --space-2: 16px;
  --space-3: 24px;
  --space-4: 32px;
  --space-5: 40px;
  --space-6: 48px;

  /* --- Typography colors --- */
  --color-text-title: #1D2129;
  --color-text-body: #4E5969;
  --color-text-caption: #86909C;
  --color-text-disabled: #C9CDD4;

  /* --- Backgrounds & shadows (borderless cards) --- */
  --color-bg-page: #F7F8FA;
  --color-bg-card: #FFFFFF;
  --shadow-card: 0 4px 20px rgba(0, 0, 0, 0.04);
  --shadow-card-hover: 0 8px 30px rgba(0, 0, 0, 0.08);

  /* --- Border radius --- */
  --radius-card: 16px;
  --radius-btn: 12px;
  --radius-full: 999px;

  /* --- Font stacks --- */
  --font-sans: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif;
  --font-mono: ui-monospace, "SF Mono", "Cascadia Code", monospace;
}

/* --- Focus state (keyboard navigation) --- */
:focus-visible {
  outline: 2px solid #1D2129;
  outline-offset: 2px;
}

/* --- Scrollbar styling --- */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.12);
  border-radius: 3px;
}
::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.2);
}

/* --- Dark mode placeholder --- */
[data-theme="dark"] {
  --color-text-title: #E5E6EB;
  --color-text-body: #A3A6AB;
  --color-text-caption: #6B7280;
  --color-text-disabled: #4B5563;
  --color-bg-page: #121212;
  --color-bg-card: #1E1E1E;
  --shadow-card: 0 4px 20px rgba(0, 0, 0, 0.2);
  --shadow-card-hover: 0 8px 30px rgba(0, 0, 0, 0.3);
}
```

- [ ] **Step 3: Import tokens.css in main.js**

In `vue-project/src/main.js`, add after line 1 (`import { createApp } from 'vue'`):

```js
import './styles/tokens.css'
```

- [ ] **Step 4: Commit**

```bash
git add vue-project/src/styles/tokens.css vue-project/src/main.js
git commit -m "feat: add design tokens system with 8pt grid, scrollbar, focus state, dark mode placeholder"
```

---

### Task 2: Rewrite XingceView — Module Selection Page

**Files:**
- Rewrite: `vue-project/src/views/XingceView.vue` (template + style, script unchanged)

- [ ] **Step 1: Rewrite XingceView.vue**

Replace the entire file with the following. The `script` section is unchanged; only `template` and `style` are rewritten.

```vue
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
```

- [ ] **Step 2: Verify in browser**

Start dev server and check `http://localhost:5173/xingce`:

```bash
cd vue-project && npm run dev
```

Expected: 2x2 grid of borderless cards with SVG icons, diffuse shadows, hover lift effect.

- [ ] **Step 3: Commit**

```bash
git add vue-project/src/views/XingceView.vue
git commit -m "feat: redesign XingceView with 2x2 grid, SVG icons, borderless cards"
```

---

### Task 3: Rewrite XingceModuleView — Topbar & Question Card

**Files:**
- Rewrite: `vue-project/src/views/XingceModuleView.vue` (template + style, script preserved)

- [ ] **Step 1: Rewrite XingceModuleView.vue template and style**

Replace the entire file. The `<script setup>` section is preserved exactly as-is (lines 84-228 from the current file). Only `<template>` and `<style>` are rewritten.

```vue
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
```

- [ ] **Step 2: Verify in browser**

Start dev server and navigate to `http://localhost:5173/xingce/ZILIAO`:

```bash
cd vue-project && npm run dev
```

Expected:
- Topbar with back button, module name, progress bar, dark timer chip
- Borderless question card with diffuse shadow
- Options with letter circles, hover lift, select pulse animation
- Capsule buttons at bottom
- "下一题" button on last question shows "查看结果" and navigates to finish screen

- [ ] **Step 3: Commit**

```bash
git add vue-project/src/views/XingceModuleView.vue
git commit -m "feat: redesign XingceModuleView with topbar, borderless cards, capsule buttons, micro-interactions"
```

---

### Task 4: Final Verification & Cleanup

**Files:**
- None (verification only)

- [ ] **Step 1: Test all module pages**

Visit each module route and verify:
- `http://localhost:5173/xingce` — 2x2 grid, 5 modules, hover effects
- `http://localhost:5173/xingce/CHANGSHI` — 常识答题页
- `http://localhost:5173/xingce/ZILIAO` — 资料分析答题页
- `http://localhost:5173/xingce/SHULIANG` — 数量关系答题页

- [ ] **Step 2: Test complete quiz flow**

On any module page:
1. Answer all 5 questions
2. Verify last question shows "查看结果" button
3. Click "查看结果" → finish screen with accuracy stats
4. Click "再来一组" → new questions load

- [ ] **Step 3: Test responsive layout**

Open browser DevTools, toggle device toolbar:
- Desktop (>1100px): 2x2 grid with side margins
- Tablet (768-1099px): 2x2 grid, tighter margins
- Mobile (<768px): single column

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "feat: complete xingce UI redesign with design tokens, borderless cards, micro-interactions"
```
