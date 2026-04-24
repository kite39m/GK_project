<template>
  <div class="practice-layout">
    <div class="desktop-card">
      
      <div class="practice-header">
        <div class="header-left">
          <span class="category-tag">{{ currentCategory }}</span>
        </div>
        <div class="timer-display">
          <svg viewBox="0 0 1024 1024" width="18" height="18"><path d="M512 853.333333c-188.16 0-341.333333-153.173333-341.333333-341.333333s153.173333-341.333333 341.333333-341.333333 341.333333 153.173333 341.333333 341.333333-153.173333 341.333333-341.333333 341.333333z m0-640c-164.693333 0-298.666667 133.973333-298.666667 298.666667s133.973333 298.666667 298.666667 298.666667 298.666667-133.973333 298.666667-298.666667-133.973333-298.666667-298.666667-298.666667zM490.666667 533.333333h-170.666667a21.333333 21.333333 0 0 1 0-42.666666h149.333333v-170.666667a21.333333 21.333333 0 0 1 42.666667 0v192a21.333333 21.333333 0 0 1-21.333333 21.333333z" fill="currentColor"></path></svg>
          <span class="time-text">{{ isFinished ? formatTotalTime : formatTime }}</span>
        </div>
      </div>

      <div v-if="isLoading" class="loading-state">
        <div class="spinner"></div>
        <p>正在为您构建专属题库，请稍候...</p>
      </div>

      <div v-else-if="!isFinished && questions.length > 0" class="question-container fade-in">
        
        <div class="progress-indicator">
          <span>Question {{ currentIndex + 1 }}</span>
          <span class="progress-total">/ {{ questions.length }}</span>
        </div>
        
        <div class="equation-box">
          {{ currentQuestion.title }}
        </div>

        <div class="hint-text">请选择最符合逻辑的答案</div>

        <div class="options-grid">
          <button 
            v-for="opt in currentQuestion.options" 
            :key="opt.key"
            class="option-card"
            :class="{
              'is-correct': selectedOption === opt.key && isCorrect === true,
              'is-wrong': selectedOption === opt.key && isCorrect === false
            }"
            @click="handleSelect(opt.key)"
            :disabled="isWaiting"
          >
            <span class="opt-key">{{ opt.key }}</span>
            <span class="opt-value">{{ opt.value }}</span>
          </button>
        </div>
      </div>

      <div v-else-if="isFinished" class="result-container fade-in">
        
        <div class="result-hero">
          <h1 class="result-title">尊敬的赵书记您辛苦了！</h1>
          <p class="result-subtitle">本次训练已结束，以下是您的能力评估报告。</p>
          
          <div class="stats-grid">
            <div class="stat-card">
              <div class="stat-label">总耗时</div>
              <div class="stat-value">{{ formatTotalTime }}</div>
            </div>
            <div class="stat-card">
              <div class="stat-label">平均用时</div>
              <div class="stat-value">{{ formatAverageTime }}<span class="unit"> /题</span></div>
            </div>
            <div class="stat-card">
              <div class="stat-label">正确率</div>
              <div class="stat-value" :class="correctCount >= (questions.length * 0.8) ? 'text-success' : 'text-danger'">
                {{ Math.round((correctCount / questions.length) * 100) }}<span class="unit">%</span>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-label">战绩</div>
              <div class="stat-value">{{ correctCount }}<span class="unit"> / {{ questions.length }}</span></div>
            </div>
          </div>
        </div>

        <div class="review-section">
          <h2 class="section-heading">答题明细追踪</h2>
          
          <div class="review-list">
            <div 
              v-for="(res, index) in questionResults" 
              :key="index" 
              class="review-item" 
              :class="{'has-error': !res.isCorrect}"
            >
              <div class="review-header" @click="res.showAnalysis = !res.showAnalysis">
                <div class="header-main">
                  <span class="q-index">Q{{ index + 1 }}</span>
                  <span class="q-title-preview">{{ res.title }}</span>
                </div>
                <div class="header-meta">
                  <span class="meta-time">{{ res.timeSpent }}s</span>
                  <span class="meta-status" :class="res.isCorrect ? 'status-good' : 'status-bad'">
                    {{ res.isCorrect ? 'Correct' : 'Error' }}
                  </span>
                  <span class="chevron" :class="{'is-open': res.showAnalysis}">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg>
                  </span>
                </div>
              </div>

              <div v-if="res.showAnalysis" class="review-body">
                
                <div class="review-options">
                  <div 
                    v-for="opt in res.options" 
                    :key="opt.key" 
                    class="r-opt-item"
                    :class="{
                      'is-answer': opt.key === res.correctAnswer,
                      'is-mistake': opt.key === res.userAnswer && !res.isCorrect
                    }"
                  >
                    <span class="r-opt-key">{{ opt.key }}</span> {{ opt.value }}
                  </div>
                </div>

                <div class="review-insight">
                  <div class="insight-label">💡 算法解析</div>
                  <div class="insight-text">{{ res.analysis }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="action-footer">
          <button class="btn-primary" @click="fetchQuestions('random')">继续随机模考</button>
          <button class="btn-primary" style="background: #ef4444; margin-top: 12px;" @click="fetchQuestions('mistake')">
            ⚔️ 清理我的错题本
          </button>
          <div class="secondary-btn-group" style="margin-top: 12px;">
            <button class="btn-secondary" @click="fetchQuestions('category', '截位直除-强差距')">专项：强差距</button>
            <button class="btn-secondary" @click="fetchQuestions('category', '基期量-正增长率')">专项：基期量</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
// 1. 所有的 import 必须放在最顶端
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { useRoute } from 'vue-router'

const route = useRoute();

// 2. 状态声明区（先声明，后使用，保证逻辑严密）
const isLoading = ref(true);
const questions = ref([]);
const currentCategory = ref("准备中...");
const currentIndex = ref(0);
const isFinished = ref(false);
const correctCount = ref(0);

const selectedOption = ref(null);
const isCorrect = ref(null);
const isWaiting = ref(false);

const seconds = ref(0);       // 单题计时
const totalSeconds = ref(0);  // 总计时
let timerInterval = null;

const questionResults = ref([]);

const currentQuestion = computed(() => questions.value[currentIndex.value] || {});

// 3. 核心方法区
const fetchQuestions = async (mode = 'random', categoryName = '') => {
  isLoading.value = true;
  isFinished.value = false;
  currentIndex.value = 0;
  correctCount.value = 0;
  seconds.value = 0;
  totalSeconds.value = 0;
  questionResults.value = []; 
  selectedOption.value = null;
  isCorrect.value = null;
  isWaiting.value = false;
  stopTimer();

  try {
    let url = '';
    if (mode === 'random') {
      url = 'http://localhost:8080/api/question/random-ten';
      currentCategory.value = '综合随机模考 (10题)';
    } else if (mode === 'category') {
      url = `http://localhost:8080/api/question/category?name=${categoryName}`;
      currentCategory.value = `${categoryName} 专项`;
    } else if (mode === 'mistake') {
      url = `http://localhost:8080/api/wrong-question/list?userId=1`;
      currentCategory.value = '我的错题本';
    }

    const response = await axios.get(url);
    const rawData = response.data;

    // 🔥 修复死胡同逻辑：如果错题本为空，不要卡死，直接跳转去随机模考！
    if (rawData.length === 0 && mode === 'mistake') {
       ElMessage.success({ message: '太棒了！错题本已被清空！为你开启随机模考~', duration: 3000 });
       return fetchQuestions('random'); 
    }

    questions.value = rawData.map(item => {
      const parsedOptions = JSON.parse(item.optionsJson);
      const optionsArray = Object.keys(parsedOptions).map(key => ({
        key: key,
        value: parsedOptions[key]
      }));
      return { ...item, options: optionsArray }
    });

    isLoading.value = false;
    startTimer();
  } catch (error) {
    ElMessage.error('获取题库失败，请确保 Spring Boot 后端已启动！');
    isLoading.value = false;
  }
};

// 🔥 修复点：加上了 async 关键字，否则里面的 await 会报错！
const handleSelect = async (optKey) => {
  if (isWaiting.value) return;
  
  selectedOption.value = optKey;
  isWaiting.value = true;
  const currentIsCorrect = (optKey === currentQuestion.value.answer);
   
  const currentUserId = 1;

  if (currentIsCorrect) {
    isCorrect.value = true;
    correctCount.value++;
    ElMessage({ message: '正确', type: 'success', duration: 1000 });
  
    if (currentCategory.value === '我的错题本') {
        // 对于不需要立刻等待结果的斩杀操作，可以不用 await 阻塞前端
        axios.post(`http://localhost:8080/api/wrong-question/master?userId=${currentUserId}&questionId=${currentQuestion.value.id}`).catch(e => console.warn(e));
    }
  } else {
    isCorrect.value = false;
    ElMessage({ message: `失误了，正确答案: ${currentQuestion.value.answer}`, type: 'error', duration: 2000 });
    
    // 静默上报错题（这里使用了 await，必须保证外层方法是 async 的）
    try {
      await axios.post(`http://localhost:8080/api/wrong-question/record?userId=${currentUserId}&questionId=${currentQuestion.value.id}`);
    } catch (e) {
      console.warn('错题上报失败', e);
    }
  }

  questionResults.value.push({
    title: currentQuestion.value.title,
    options: currentQuestion.value.options,
    correctAnswer: currentQuestion.value.answer,
    userAnswer: optKey,
    isCorrect: currentIsCorrect,
    timeSpent: seconds.value,
    analysis: currentQuestion.value.analysis,
    showAnalysis: false 
  });

  setTimeout(() => {
    if (currentIndex.value < questions.value.length - 1) {
      currentIndex.value++;
      selectedOption.value = null;
      isCorrect.value = null;
      isWaiting.value = false;
      seconds.value = 0; 
    } else {
      stopTimer();
      isFinished.value = true;
    }
  }, 1000);
};

// 4. 计时与格式化区
const startTimer = () => {
  if (!timerInterval) {
    timerInterval = setInterval(() => { 
      if (!isWaiting.value) { seconds.value++; }
      totalSeconds.value++;
    }, 1000);
  }
};

const stopTimer = () => { 
  if (timerInterval) {
    clearInterval(timerInterval); 
    timerInterval = null; 
  }
};

const formatTime = computed(() => {
  const m = Math.floor(seconds.value / 60);
  const s = seconds.value % 60;
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
});

const formatTotalTime = computed(() => {
  const m = Math.floor(totalSeconds.value / 60);
  const s = totalSeconds.value % 60;
  return `${String(m).padStart(2, '0')}分${String(s).padStart(2, '0')}秒`;
});

const formatAverageTime = computed(() => {
  if (questions.value.length === 0) return '0秒';
  const avg = Math.floor(totalSeconds.value / questions.value.length);
  return `${avg}秒`;
});

// 5. 生命周期钩子 (Lifecycle Hooks 放在最后)
onMounted(() => { 
  const initMode = route.query.mode || 'random';
  fetchQuestions(initMode); 
});

onUnmounted(() => { stopTimer(); });
</script>

<style scoped>
/* =========================================
   全局架构与字体设定 (SaaS Desktop First)
========================================= */
.practice-layout {
  min-height: 100vh;
  background-color: #fafafa;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 40px 20px 80px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  color: #111;
}

.desktop-card {
  width: 100%;
  max-width: 900px;
  background: #ffffff;
  border-radius: 24px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.03), 0 1px 2px rgba(0, 0, 0, 0.02);
  overflow: hidden;
  position: relative;
  border: 1px solid rgba(0, 0, 0, 0.04);
}

/* =========================================
   顶部 Header 
========================================= */
.practice-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 40px;
  border-bottom: 1px solid #f2f2f2;
}

.category-tag {
  font-size: 14px;
  font-weight: 600;
  color: #555;
  background: #f4f4f5;
  padding: 6px 14px;
  border-radius: 999px;
  letter-spacing: 0.02em;
}

.timer-display {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #888;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 15px;
}

/* =========================================
   答题区 (Question Area)
========================================= */
.question-container {
  padding: 60px 80px 80px;
}

.progress-indicator {
  font-size: 14px;
  font-weight: 600;
  color: #a0a0a0;
  margin-bottom: 24px;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}
.progress-total { color: #d0d0d0; }

.equation-box {
  font-size: 3.5rem;
  font-weight: 700;
  color: #111;
  letter-spacing: -0.02em;
  line-height: 1.2;
  margin-bottom: 20px;
  word-break: break-all;
}

.hint-text {
  font-size: 15px;
  color: #999;
  margin-bottom: 60px;
}

.options-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.option-card {
  background: #ffffff;
  border: 1px solid #eaeaea;
  border-radius: 16px;
  padding: 28px 32px;
  text-align: left;
  cursor: pointer;
  display: flex;
  align-items: baseline;
  gap: 16px;
  transition: all 0.25s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.option-card:hover:not(:disabled) {
  border-color: #d0d0d0;
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04);
}

.option-card:active:not(:disabled) { transform: translateY(0); }

.opt-key {
  font-size: 18px;
  font-weight: 700;
  color: #888;
}

.opt-value {
  font-size: 22px;
  font-weight: 600;
  color: #222;
}

.option-card.is-correct {
  background: #f4fdf8;
  border-color: #a3e6cd;
}
.option-card.is-correct .opt-key, .option-card.is-correct .opt-value { color: #10b981; }

.option-card.is-wrong {
  background: #fef8f8;
  border-color: #fca5a5;
}
.option-card.is-wrong .opt-key, .option-card.is-wrong .opt-value { color: #ef4444; }

/* =========================================
   结算仪表盘 (Result Dashboard)
========================================= */
.result-container {
  padding: 60px 80px 80px;
}

.result-hero {
  text-align: center;
  margin-bottom: 60px;
}

.result-title {
  font-size: 2.25rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #111;
  margin-bottom: 12px;
}

.result-subtitle {
  font-size: 1.125rem;
  color: #666;
  margin-bottom: 48px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.stat-card {
  background: #f9f9f9;
  border-radius: 16px;
  padding: 24px;
  text-align: center;
}

.stat-label {
  font-size: 14px;
  font-weight: 500;
  color: #888;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 2rem;
  font-weight: 700;
  color: #111;
  letter-spacing: -0.02em;
}
.stat-value .unit { font-size: 1rem; color: #aaa; font-weight: 500; }
.text-success { color: #10b981; }
.text-danger { color: #ef4444; }

/* =========================================
   明细解析 (Review Section)
========================================= */
.section-heading {
  font-size: 1.25rem;
  font-weight: 600;
  color: #111;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eaeaea;
}

.review-list {
  display: flex;
  flex-direction: column;
}

.review-item {
  border-bottom: 1px solid #f2f2f2;
}
.review-item:last-child { border-bottom: none; }

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 0;
  cursor: pointer;
  transition: opacity 0.2s;
}
.review-header:hover { opacity: 0.7; }

.header-main {
  display: flex;
  align-items: center;
  gap: 16px;
}
.q-index {
  font-family: ui-monospace, monospace;
  font-size: 14px;
  color: #888;
  font-weight: 600;
}
.q-title-preview {
  font-size: 16px;
  font-weight: 500;
  color: #222;
}

.header-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}
.meta-time { font-size: 14px; color: #aaa; }
.meta-status {
  font-size: 13px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 6px;
}
.status-good { background: #e6f6f0; color: #10b981; }
.status-bad { background: #fef0f0; color: #ef4444; }

.chevron { color: #ccc; transition: transform 0.3s ease; display: flex; }
.chevron.is-open { transform: rotate(180deg); color: #111; }

.review-body {
  padding: 0 0 32px 0;
  animation: slideDown 0.3s ease;
}

.review-options {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
  margin-bottom: 24px;
}

.r-opt-item {
  padding: 10px 16px;
  background: #f7f7f8;
  border-radius: 8px;
  font-size: 15px;
  color: #555;
}
.r-opt-key { font-weight: 600; margin-right: 4px; color: #888; }

.r-opt-item.is-answer {
  background: #f4fdf8;
  color: #10b981;
  font-weight: 500;
}
.r-opt-item.is-answer .r-opt-key { color: #10b981; }

.r-opt-item.is-mistake {
  background: #fef8f8;
  color: #ef4444;
  text-decoration: line-through;
}

.review-insight {
  background: #f9f9f9;
  border-radius: 12px;
  padding: 20px;
}
.insight-label { font-weight: 600; color: #333; margin-bottom: 8px; font-size: 14px; }
.insight-text { font-size: 15px; color: #555; line-height: 1.6; }

/* =========================================
   操作按钮区 (Footer Actions)
========================================= */
.action-footer {
  margin-top: 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

button { font-family: inherit; }

.btn-primary {
  background: #111;
  color: #fff;
  font-size: 16px;
  font-weight: 500;
  padding: 16px 40px;
  border-radius: 999px;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
  width: 100%;
  max-width: 400px;
}
.btn-primary:hover { background: #333; transform: translateY(-1px); }

.secondary-btn-group {
  display: flex;
  gap: 12px;
  width: 100%;
  max-width: 400px;
}

.btn-secondary {
  flex: 1;
  background: #f4f4f5;
  color: #444;
  font-size: 14px;
  font-weight: 500;
  padding: 14px 20px;
  border-radius: 999px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-secondary:hover { background: #ebebeb; color: #111; }

/* =========================================
   动画与加载态
========================================= */
.fade-in { animation: fadeIn 0.5s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
@keyframes slideDown { from { opacity: 0; margin-top: -10px; } to { opacity: 1; margin-top: 0; } }

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120px 0;
  color: #888;
}
.spinner {
  width: 32px; height: 32px;
  border: 3px solid #f3f3f3; border-top: 3px solid #111;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}
@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }

@media (max-width: 768px) {
  .question-container, .result-container { padding: 40px 24px; }
  .equation-box { font-size: 2.5rem; }
  .options-grid { grid-template-columns: 1fr; }
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
<style scoped>
/* =========================================
   全局架构与字体设定 (SaaS Desktop First)
========================================= */
.practice-layout {
  min-height: 100vh;
  background-color: #fafafa;
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 40px 20px 80px;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  color: #111;
}

/* 打破原本 480px 的局限，加宽到 900px，形成桌面级沉浸大屏 */
.desktop-card {
  width: 100%;
  max-width: 900px;
  background: #ffffff;
  border-radius: 24px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.03), 0 1px 2px rgba(0, 0, 0, 0.02);
  overflow: hidden;
  position: relative;
  border: 1px solid rgba(0, 0, 0, 0.04);
}

/* =========================================
   顶部 Header 
========================================= */
.practice-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 40px;
  border-bottom: 1px solid #f2f2f2;
}

.category-tag {
  font-size: 14px;
  font-weight: 600;
  color: #555;
  background: #f4f4f5;
  padding: 6px 14px;
  border-radius: 999px;
  letter-spacing: 0.02em;
}

.timer-display {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #888;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 15px;
}

/* =========================================
   答题区 (Question Area)
========================================= */
.question-container {
  padding: 60px 80px 80px;
}

.progress-indicator {
  font-size: 14px;
  font-weight: 600;
  color: #a0a0a0;
  margin-bottom: 24px;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}
.progress-total { color: #d0d0d0; }

.equation-box {
  font-size: 3.5rem;
  font-weight: 700;
  color: #111;
  letter-spacing: -0.02em;
  line-height: 1.2;
  margin-bottom: 20px;
  word-break: break-all;
}

.hint-text {
  font-size: 15px;
  color: #999;
  margin-bottom: 60px;
}

/* 核心改造：使用 CSS Grid 将选项变为 2x2 网格，更适配宽屏 */
.options-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.option-card {
  background: #ffffff;
  border: 1px solid #eaeaea;
  border-radius: 16px;
  padding: 28px 32px;
  text-align: left;
  cursor: pointer;
  display: flex;
  align-items: baseline;
  gap: 16px;
  transition: all 0.25s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.option-card:hover:not(:disabled) {
  border-color: #d0d0d0;
  transform: translateY(-2px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.04);
}

.option-card:active:not(:disabled) { transform: translateY(0); }

.opt-key {
  font-size: 18px;
  font-weight: 700;
  color: #888;
}

.opt-value {
  font-size: 22px;
  font-weight: 600;
  color: #222;
}

/* 选项状态 */
.option-card.is-correct {
  background: #f4fdf8;
  border-color: #a3e6cd;
}
.option-card.is-correct .opt-key, .option-card.is-correct .opt-value { color: #10b981; }

.option-card.is-wrong {
  background: #fef8f8;
  border-color: #fca5a5;
}
.option-card.is-wrong .opt-key, .option-card.is-wrong .opt-value { color: #ef4444; }

/* =========================================
   结算仪表盘 (Result Dashboard)
========================================= */
.result-container {
  padding: 60px 80px 80px;
}

.result-hero {
  text-align: center;
  margin-bottom: 60px;
}

.result-title {
  font-size: 2.25rem;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #111;
  margin-bottom: 12px;
}

.result-subtitle {
  font-size: 1.125rem;
  color: #666;
  margin-bottom: 48px;
}

/* 数据卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.stat-card {
  background: #f9f9f9;
  border-radius: 16px;
  padding: 24px;
  text-align: center;
}

.stat-label {
  font-size: 14px;
  font-weight: 500;
  color: #888;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 2rem;
  font-weight: 700;
  color: #111;
  letter-spacing: -0.02em;
}
.stat-value .unit { font-size: 1rem; color: #aaa; font-weight: 500; }
.text-success { color: #10b981; }
.text-danger { color: #ef4444; }

/* =========================================
   明细解析 (Review Section)
========================================= */
.section-heading {
  font-size: 1.25rem;
  font-weight: 600;
  color: #111;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #eaeaea;
}

.review-list {
  display: flex;
  flex-direction: column;
}

.review-item {
  border-bottom: 1px solid #f2f2f2;
}
.review-item:last-child { border-bottom: none; }

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 0;
  cursor: pointer;
  transition: opacity 0.2s;
}
.review-header:hover { opacity: 0.7; }

.header-main {
  display: flex;
  align-items: center;
  gap: 16px;
}
.q-index {
  font-family: ui-monospace, monospace;
  font-size: 14px;
  color: #888;
  font-weight: 600;
}
.q-title-preview {
  font-size: 16px;
  font-weight: 500;
  color: #222;
}

.header-meta {
  display: flex;
  align-items: center;
  gap: 16px;
}
.meta-time { font-size: 14px; color: #aaa; }
.meta-status {
  font-size: 13px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 6px;
}
.status-good { background: #e6f6f0; color: #10b981; }
.status-bad { background: #fef0f0; color: #ef4444; }

.chevron { color: #ccc; transition: transform 0.3s ease; display: flex; }
.chevron.is-open { transform: rotate(180deg); color: #111; }

.review-body {
  padding: 0 0 32px 0;
  animation: slideDown 0.3s ease;
}

/* 解析区的选项展示，横向排列 */
.review-options {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 16px;
  margin-bottom: 24px;
}

.r-opt-item {
  padding: 10px 16px;
  background: #f7f7f8;
  border-radius: 8px;
  font-size: 15px;
  color: #555;
}
.r-opt-key { font-weight: 600; margin-right: 4px; color: #888; }

.r-opt-item.is-answer {
  background: #f4fdf8;
  color: #10b981;
  font-weight: 500;
}
.r-opt-item.is-answer .r-opt-key { color: #10b981; }

.r-opt-item.is-mistake {
  background: #fef8f8;
  color: #ef4444;
  text-decoration: line-through;
}

.review-insight {
  background: #f9f9f9;
  border-radius: 12px;
  padding: 20px;
}
.insight-label { font-weight: 600; color: #333; margin-bottom: 8px; font-size: 14px; }
.insight-text { font-size: 15px; color: #555; line-height: 1.6; }

/* =========================================
   操作按钮区 (Footer Actions)
========================================= */
.action-footer {
  margin-top: 60px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

button { font-family: inherit; }

.btn-primary {
  background: #111;
  color: #fff;
  font-size: 16px;
  font-weight: 500;
  padding: 16px 40px;
  border-radius: 999px;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
  width: 100%;
  max-width: 400px;
}
.btn-primary:hover { background: #333; transform: translateY(-1px); }

.secondary-btn-group {
  display: flex;
  gap: 12px;
  width: 100%;
  max-width: 400px;
}

.btn-secondary {
  flex: 1;
  background: #f4f4f5;
  color: #444;
  font-size: 14px;
  font-weight: 500;
  padding: 14px 20px;
  border-radius: 999px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-secondary:hover { background: #ebebeb; color: #111; }

/* =========================================
   动画与加载态
========================================= */
.fade-in { animation: fadeIn 0.5s ease; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
@keyframes slideDown { from { opacity: 0; margin-top: -10px; } to { opacity: 1; margin-top: 0; } }

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 120px 0;
  color: #888;
}
.spinner {
  width: 32px; height: 32px;
  border: 3px solid #f3f3f3; border-top: 3px solid #111;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}
@keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }

/* 响应式回退：当屏幕真的小于电脑尺寸时，自动变为单列 */
@media (max-width: 768px) {
  .question-container, .result-container { padding: 40px 24px; }
  .equation-box { font-size: 2.5rem; }
  .options-grid { grid-template-columns: 1fr; }
  .stats-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>