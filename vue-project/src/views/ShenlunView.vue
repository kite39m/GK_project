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
