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

        <!-- 陷阱反馈卡片 -->
        <div v-if="trapFeedback && trapFeedback.trap" class="trap-card">
          <div class="trap-title">⚠️ 陷阱警示</div>
          <div class="trap-text">{{ trapFeedback.trapAnalysis }}</div>
          <div class="trap-count">该考点你已踩坑 {{ trapFeedback.trapCount }} 次</div>
        </div>
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
      <button class="generate-btn" @click="generateQuestions" :disabled="generating">
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
  }
}

const generateQuestions = async () => {
  generating.value = true
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
    }
  } catch (e) { console.error('AI出题失败', e) }
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
.trap-card {
  margin-top: 12px;
  padding: 14px 18px;
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 10px;
}
.trap-title { font-weight: 700; color: #ff4d4f; margin-bottom: 6px; }
.trap-text { color: #555; font-size: 0.9rem; line-height: 1.6; }
.trap-count { color: #999; font-size: 0.8rem; margin-top: 8px; }
.generate-btn {
  padding: 14px 40px;
  background: #111;
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  margin-top: 16px;
}
.generate-btn:disabled { background: #ccc; cursor: not-allowed; }
</style>
