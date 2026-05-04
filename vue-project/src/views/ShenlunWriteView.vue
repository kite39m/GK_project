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
