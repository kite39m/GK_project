# Phase 4: 前端实现 Implementation Plan

**Goal:** 实现行测和申论模块的前端页面 — 行测主页（5 子模块卡片）、答题页、申论主页、申论写作页。

**Architecture:** Vue 3 Composition API + Element Plus + Vue Router，遵循现有项目模式（script setup、axios 拦截器、NavBar 导航）。

---

## File Map

| Action | File | Responsibility |
|--------|------|----------------|
| Modify | `vue-project/src/router/index.js` | 添加新路由 |
| Modify | `vue-project/src/components/NavBar.vue` | 添加行测、申论导航链接 |
| Create | `vue-project/src/views/XingceView.vue` | 行测主页 |
| Create | `vue-project/src/views/XingceModuleView.vue` | 子模块答题页 |
| Create | `vue-project/src/views/ShenlunView.vue` | 申论主页 |
| Create | `vue-project/src/views/ShenlunWriteView.vue` | 申论写作页 |

所有路径相对于 `vue-project/src/`

---

## Task 1: 更新路由和导航

**Files:**
- Modify: `vue-project/src/router/index.js`
- Modify: `vue-project/src/components/NavBar.vue`

- [ ] **Step 1: 更新路由**

在 `router/index.js` 中添加新路由：

```javascript
import XingceView from '../views/XingceView.vue'
import XingceModuleView from '../views/XingceModuleView.vue'
import ShenlunView from '../views/ShenlunView.vue'
import ShenlunWriteView from '../views/ShenlunWriteView.vue'

// 在 routes 数组中添加：
{
  path: '/xingce',
  name: 'xingce',
  component: XingceView
},
{
  path: '/xingce/:module',
  name: 'xingce-module',
  component: XingceModuleView
},
{
  path: '/shenlun',
  name: 'shenlun',
  component: ShenlunView
},
{
  path: '/shenlun/write',
  name: 'shenlun-write',
  component: ShenlunWriteView
}
```

- [ ] **Step 2: 更新 NavBar**

在 NavBar 的 `nav-links` 中添加行测和申论链接：

```html
<router-link to="/xingce" class="nav-item">
  <span>行测</span>
</router-link>
<router-link to="/shenlun" class="nav-item">
  <span>申论</span>
</router-link>
```

- [ ] **Step 3: Commit**

```bash
git add vue-project/src/router/index.js vue-project/src/components/NavBar.vue
git commit -m "feat(frontend): 添加行测和申论路由及导航链接"
```

---

## Task 2: 创建行测主页（XingceView）

**Files:**
- Create: `vue-project/src/views/XingceView.vue`

- [ ] **Step 1: 创建页面**

5 个子模块卡片，每个显示模块名、图标、正确率。

```vue
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
```

- [ ] **Step 2: Commit**

```bash
git add vue-project/src/views/XingceView.vue
git commit -m "feat(frontend): 实现行测主页（5个子模块卡片）"
```

---

## Task 3: 创建答题页（XingceModuleView）

**Files:**
- Create: `vue-project/src/views/XingceModuleView.vue`

- [ ] **Step 1: 创建页面**

```vue
<template>
  <div class="module-page">
    <div class="module-header">
      <button class="back-btn" @click="$router.push('/xingce')">← 返回</button>
      <h2>{{ moduleName }}</h2>
      <div class="timer">{{ formatTime(elapsed) }}</div>
    </div>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="currentQuestion" class="question-area">
      <div class="question-title">{{ currentQuestion.title }}</div>
      <div class="options">
        <div
          v-for="(opt, idx) in parsedOptions"
          :key="idx"
          class="option"
          :class="{
            selected: selectedAnswer === opt.label,
            correct: showResult && opt.label === currentQuestion.answer,
            wrong: showResult && selectedAnswer === opt.label && opt.label !== currentQuestion.answer
          }"
          @click="selectOption(opt.label)"
        >
          <span class="opt-label">{{ opt.label }}</span>
          <span class="opt-text">{{ opt.text }}</span>
        </div>
      </div>

      <div v-if="showResult" class="result-area">
        <div class="result-tag" :class="isCorrect ? 'correct' : 'wrong'">
          {{ isCorrect ? '✓ 回答正确' : '✗ 回答错误' }}
        </div>
        <div class="analysis">{{ currentQuestion.analysis }}</div>
      </div>

      <div class="actions">
        <button v-if="!showResult" class="submit-btn" :disabled="!selectedAnswer" @click="submitAnswer">
          提交答案
        </button>
        <button v-else class="next-btn" @click="nextQuestion">
          下一题 →
        </button>
      </div>
    </div>

    <div v-else class="empty">
      <p>该模块暂无题目</p>
      <button class="back-btn" @click="$router.push('/xingce')">返回行测主页</button>
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
const selectedAnswer = ref(null)
const showResult = ref(false)
const isCorrect = ref(false)
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

const selectOption = (label) => {
  if (!showResult.value) selectedAnswer.value = label
}

const submitAnswer = async () => {
  if (!selectedAnswer.value) return
  showResult.value = true
  isCorrect.value = selectedAnswer.value === currentQuestion.value.answer

  try {
    await axios.post('http://localhost:8080/api/xingce/answer', {
      userId: 1,
      questionId: currentQuestion.value.id,
      module: moduleCode.value,
      isCorrect: isCorrect.value,
      userAnswer: selectedAnswer.value,
      timeCostSec: elapsed.value,
      isTrapOption: false
    })
  } catch (e) { console.error('保存答题记录失败', e) }
}

const nextQuestion = () => {
  if (currentIndex.value < questions.value.length - 1) {
    currentIndex.value++
    selectedAnswer.value = null
    showResult.value = false
    elapsed.value = 0
  }
}

onMounted(async () => {
  timer = setInterval(() => { if (!showResult.value) elapsed.value++ }, 1000)
  try {
    const res = await axios.get(`http://localhost:8080/api/xingce/questions/${moduleCode.value}`)
    questions.value = res.data
  } catch (e) { console.error(e) }
  loading.value = false
})

onUnmounted(() => { clearInterval(timer) })
</script>

<style scoped>
.module-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 32px 24px;
}
.module-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
}
.back-btn {
  background: none;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 8px 16px;
  cursor: pointer;
  font-size: 0.9rem;
}
.module-header h2 { flex: 1; margin: 0; font-size: 1.4rem; }
.timer {
  font-family: monospace;
  font-size: 1.2rem;
  color: #666;
}
.question-title {
  font-size: 1.1rem;
  line-height: 1.7;
  margin-bottom: 24px;
  color: #222;
}
.options { display: flex; flex-direction: column; gap: 12px; margin-bottom: 24px; }
.option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  border: 1px solid #e5e5e5;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.15s;
}
.option:hover { border-color: #999; }
.option.selected { border-color: #111; background: #f8f8f8; }
.option.correct { border-color: #52c41a; background: #f6ffed; }
.option.wrong { border-color: #ff4d4f; background: #fff2f0; }
.opt-label {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 0.85rem;
  flex-shrink: 0;
}
.option.selected .opt-label { background: #111; color: #fff; }
.option.correct .opt-label { background: #52c41a; color: #fff; }
.option.wrong .opt-label { background: #ff4d4f; color: #fff; }
.result-area {
  background: #fafafa;
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 20px;
}
.result-tag {
  font-weight: 600;
  margin-bottom: 8px;
}
.result-tag.correct { color: #52c41a; }
.result-tag.wrong { color: #ff4d4f; }
.analysis { color: #666; font-size: 0.9rem; line-height: 1.6; }
.actions { text-align: center; margin-top: 20px; }
.submit-btn, .next-btn {
  padding: 12px 40px;
  border: none;
  border-radius: 10px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
}
.submit-btn { background: #111; color: #fff; }
.submit-btn:disabled { background: #ccc; cursor: not-allowed; }
.next-btn { background: #111; color: #fff; }
.loading, .empty { text-align: center; padding: 60px 0; color: #999; }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add vue-project/src/views/XingceModuleView.vue
git commit -m "feat(frontend): 实现行测答题页（计时+选项+即时反馈）"
```

---

## Task 4: 创建申论主页（ShenlunView）

**Files:**
- Create: `vue-project/src/views/ShenlunView.vue`

- [ ] **Step 1: 创建页面**

```vue
<template>
  <div class="shenlun-page">
    <h1 class="page-title">申论训练</h1>
    <p class="page-desc">AI 智能评分，精准诊断写作短板</p>

    <div class="actions">
      <button class="write-btn" @click="$router.push('/shenlun/write')">开始写作</button>
      <button class="material-btn" @click="showMaterial = true">素材推荐</button>
    </div>

    <h2 class="section-title">写作历史</h2>
    <div v-if="essays.length === 0" class="empty">暂无写作记录</div>
    <div v-else class="essay-list">
      <div v-for="essay in essays" :key="essay.id" class="essay-card">
        <div class="essay-header">
          <h3>{{ essay.topic }}</h3>
          <span class="score" v-if="essay.aiScore !== null">{{ essay.aiScore }}分</span>
        </div>
        <div class="essay-meta">{{ formatDate(essay.createdAt) }}</div>
      </div>
    </div>

    <!-- 素材推荐弹窗 -->
    <el-dialog v-model="showMaterial" title="素材推荐" width="600px">
      <el-input v-model="materialTopic" placeholder="输入主题关键词" @keyup.enter="fetchMaterials">
        <template #append>
          <el-button @click="fetchMaterials">搜索</el-button>
        </template>
      </el-input>
      <div v-if="materials.length" class="material-list">
        <div v-for="(m, i) in materials" :key="i" class="material-card">
          <h4>{{ m.title }}</h4>
          <p>{{ m.content }}</p>
          <div class="scenario">适用场景：{{ m.scenario }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const essays = ref([])
const showMaterial = ref(false)
const materialTopic = ref('')
const materials = ref([])

const fetchHistory = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/shenlun/essay/history', {
      params: { userId: 1, page: 1, size: 20 }
    })
    essays.value = res.data.records || []
  } catch (e) { console.error(e) }
}

const fetchMaterials = async () => {
  if (!materialTopic.value) return
  try {
    const res = await axios.post('http://localhost:8080/api/shenlun/material/recommend', {
      topic: materialTopic.value
    })
    materials.value = typeof res.data === 'string' ? JSON.parse(res.data) : res.data
  } catch (e) { console.error(e) }
}

const formatDate = (d) => d ? new Date(d).toLocaleDateString('zh-CN') : ''

onMounted(() => { fetchHistory() })
</script>

<style scoped>
.shenlun-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 40px 24px;
}
.page-title { font-size: 1.8rem; font-weight: 700; color: #111; margin: 0 0 8px; }
.page-desc { color: #888; margin: 0 0 24px; }
.actions { display: flex; gap: 12px; margin-bottom: 40px; }
.write-btn {
  padding: 12px 32px;
  background: #111;
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
}
.material-btn {
  padding: 12px 32px;
  background: #fff;
  border: 1px solid #ddd;
  border-radius: 10px;
  font-size: 1rem;
  cursor: pointer;
}
.section-title { font-size: 1.2rem; margin: 0 0 16px; color: #333; }
.essay-list { display: flex; flex-direction: column; gap: 12px; }
.essay-card {
  padding: 16px 20px;
  border: 1px solid #eee;
  border-radius: 12px;
  transition: border-color 0.15s;
}
.essay-card:hover { border-color: #999; }
.essay-header { display: flex; justify-content: space-between; align-items: center; }
.essay-header h3 { margin: 0; font-size: 1rem; }
.score {
  font-size: 1.2rem;
  font-weight: 700;
  color: #111;
}
.essay-meta { font-size: 0.8rem; color: #999; margin-top: 4px; }
.empty { text-align: center; padding: 40px 0; color: #999; }
.material-list { margin-top: 16px; display: flex; flex-direction: column; gap: 12px; }
.material-card {
  padding: 16px;
  background: #fafafa;
  border-radius: 10px;
}
.material-card h4 { margin: 0 0 8px; font-size: 0.95rem; }
.material-card p { margin: 0 0 6px; font-size: 0.85rem; color: #555; line-height: 1.5; }
.scenario { font-size: 0.8rem; color: #999; }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add vue-project/src/views/ShenlunView.vue
git commit -m "feat(frontend): 实现申论主页（历史+素材推荐）"
```

---

## Task 5: 创建申论写作页（ShenlunWriteView）

**Files:**
- Create: `vue-project/src/views/ShenlunWriteView.vue`

- [ ] **Step 1: 创建页面**

```vue
<template>
  <div class="write-page">
    <div class="write-header">
      <button class="back-btn" @click="$router.push('/shenlun')">← 返回</button>
      <h2>申论写作</h2>
    </div>

    <el-input v-model="topic" placeholder="请输入作文题目" class="topic-input" />

    <textarea
      v-model="content"
      class="editor"
      placeholder="在此输入你的申论文章..."
    ></textarea>

    <div class="footer">
      <span class="word-count">字数：{{ content.length }}</span>
      <button class="submit-btn" :disabled="!topic || !content || submitting" @click="submitEssay">
        {{ submitting ? 'AI 评分中...' : '提交评分' }}
      </button>
    </div>

    <div v-if="feedback" class="feedback-area">
      <h3>AI 评分结果</h3>
      <div class="score-cards">
        <div class="score-card">
          <div class="score-value">{{ feedback.structureScore }}</div>
          <div class="score-label">结构分</div>
        </div>
        <div class="score-card">
          <div class="score-value">{{ feedback.argumentScore }}</div>
          <div class="score-label">论点分</div>
        </div>
        <div class="score-card">
          <div class="score-value">{{ feedback.languageScore }}</div>
          <div class="score-label">语言分</div>
        </div>
        <div class="score-card total">
          <div class="score-value">{{ feedback.totalScore }}</div>
          <div class="score-label">综合分</div>
        </div>
      </div>
      <div class="feedback-text">{{ feedback.feedback }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'

const topic = ref('')
const content = ref('')
const submitting = ref(false)
const feedback = ref(null)

const submitEssay = async () => {
  submitting.value = true
  feedback.value = null
  try {
    const res = await axios.post('http://localhost:8080/api/shenlun/essay/submit', {
      userId: 1,
      topic: topic.value,
      content: content.value
    })
    const essay = res.data
    feedback.value = {
      structureScore: essay.aiStructureScore,
      argumentScore: essay.aiArgumentScore,
      languageScore: essay.aiLanguageScore,
      totalScore: essay.aiScore,
      feedback: tryParseFeedback(essay.aiFeedback)
    }
  } catch (e) {
    console.error(e)
  }
  submitting.value = false
}

const tryParseFeedback = (raw) => {
  if (!raw) return '暂无反馈'
  try {
    const obj = JSON.parse(raw)
    return obj.feedback || '暂无反馈'
  } catch { return raw }
}
</script>

<style scoped>
.write-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px 24px;
}
.write-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}
.back-btn {
  background: none;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 8px 16px;
  cursor: pointer;
}
.write-header h2 { flex: 1; margin: 0; font-size: 1.4rem; }
.topic-input { margin-bottom: 16px; }
.editor {
  width: 100%;
  min-height: 400px;
  padding: 20px;
  border: 1px solid #e5e5e5;
  border-radius: 12px;
  font-size: 1rem;
  line-height: 1.8;
  resize: vertical;
  font-family: inherit;
  box-sizing: border-box;
}
.editor:focus { outline: none; border-color: #111; }
.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16px;
}
.word-count { color: #999; font-size: 0.85rem; }
.submit-btn {
  padding: 12px 40px;
  background: #111;
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
}
.submit-btn:disabled { background: #ccc; cursor: not-allowed; }
.feedback-area {
  margin-top: 32px;
  padding: 24px;
  background: #fafafa;
  border-radius: 16px;
}
.feedback-area h3 { margin: 0 0 16px; font-size: 1.1rem; }
.score-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}
.score-card {
  text-align: center;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #eee;
}
.score-card.total { border-color: #111; }
.score-value { font-size: 1.8rem; font-weight: 700; color: #111; }
.score-label { font-size: 0.8rem; color: #999; margin-top: 4px; }
.feedback-text { color: #555; line-height: 1.7; font-size: 0.95rem; }
</style>
```

- [ ] **Step 2: Commit**

```bash
git add vue-project/src/views/ShenlunWriteView.vue
git commit -m "feat(frontend): 实现申论写作页（编辑器+AI评分展示）"
```

---

## Phase 4 完成检查清单

- [ ] 路由已更新，NavBar 已添加行测、申论链接
- [ ] 行测主页显示 5 个子模块卡片
- [ ] 答题页支持题目展示、选项选择、计时、即时反馈
- [ ] 申论主页显示写作历史和素材推荐
- [ ] 申论写作页支持作文提交和 AI 评分展示
