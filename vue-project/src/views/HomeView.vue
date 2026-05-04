<template>
  <div class="chat-layout">
    <!-- 左侧边栏 -->
    <aside class="sidebar">
      <button class="new-chat-btn" @click="handleNewChat">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
        </svg>
        发起新对话
      </button>
      <div class="history-title">历史对话</div>
      <div class="history-list">
        <div
          v-for="item in chatHistory"
          :key="item.id"
          class="history-item"
          :class="{ active: item.id === activeChatId }"
          @click="switchChat(item.id)"
        >
          {{ item.title }}
        </div>
      </div>
    </aside>

    <!-- 右侧主区域 -->
    <main class="main-area">
      <!-- 消息列表 -->
      <div ref="chatContainerRef" class="chat-scroll">
        <div class="chat-content">
          <!-- 空状态 -->
          <div v-if="chatMessages.length === 0" class="chat-empty">
            <div class="empty-icon">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#ddd" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              </svg>
            </div>
            <h2>有什么备考问题，尽管问我</h2>
            <p>十年国考专家在线答疑，支持行测、申论</p>
          </div>

          <!-- 消息列表 -->
          <template v-for="(msg, idx) in chatMessages" :key="idx">
            <!-- 用户消息 -->
            <div v-if="msg.role === 'user'" class="msg-row msg-row--user">
              <div class="bubble-user">{{ msg.content }}</div>
            </div>

            <!-- AI 消息 -->
            <div v-else class="msg-row msg-row--ai">
              <div class="ai-avatar">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                  <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                </svg>
              </div>
              <div class="ai-body markdown-body" v-html="renderMarkdown(msg.content)"></div>
            </div>
          </template>

          <!-- 思考中 -->
          <div v-if="isChatLoading" class="msg-row msg-row--ai">
            <div class="ai-avatar">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
              </svg>
            </div>
            <div class="thinking-indicator">
              <span class="dot-pulse"></span>
              <span>正在思考...</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部输入区 -->
      <div class="input-section">
        <div class="input-fade"></div>
        <div class="input-inner">
          <ChatInputBox
            placeholder="输入你的备考问题..."
            model="Pro"
            :disabled="isChatLoading"
            @submit="handleChatSend"
          />
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import axios from 'axios'
import MarkdownIt from 'markdown-it'
import 'github-markdown-css/github-markdown-light.css'
import ChatInputBox from '../components/ChatInputBox.vue'

const md = new MarkdownIt({ html: true, breaks: true })
const BASE = 'http://localhost:8080/api/chat'
const USER_ID = 1

const chatMessages = ref([])
const isChatLoading = ref(false)
const chatContainerRef = ref(null)
let abortController = null

const chatHistory = ref([])
const activeChatId = ref(null)

const scrollChatToBottom = () => {
  nextTick(() => {
    if (chatContainerRef.value) {
      chatContainerRef.value.scrollTop = chatContainerRef.value.scrollHeight
    }
  })
}

const fetchSessions = async () => {
  try {
    const res = await axios.get(`${BASE}/sessions`, { params: { userId: USER_ID } })
    chatHistory.value = res.data
  } catch { chatHistory.value = [] }
}

const handleNewChat = () => {
  chatMessages.value = []
  activeChatId.value = null
  abortController?.abort()
  isChatLoading.value = false
}

const switchChat = async (id) => {
  activeChatId.value = id
  abortController?.abort()
  isChatLoading.value = false
  try {
    const res = await axios.get(`${BASE}/messages`, { params: { sessionId: id } })
    chatMessages.value = res.data.map(m => ({ role: m.role, content: m.content }))
    scrollChatToBottom()
  } catch { chatMessages.value = [] }
}

const handleChatSend = async (prompt) => {
  if (!prompt || isChatLoading.value) return

  chatMessages.value.push({ role: 'user', content: prompt })
  isChatLoading.value = true

  chatMessages.value.push({ role: 'assistant', content: '' })
  const aiMsg = chatMessages.value[chatMessages.value.length - 1]
  scrollChatToBottom()

  try {
    abortController = new AbortController()
    const params = new URLSearchParams({ userPrompt: prompt })
    if (activeChatId.value) params.set('sessionId', activeChatId.value)

    const response = await fetch(`${BASE}/stream?${params}`, { signal: abortController.signal })
    if (!response.ok) throw new Error(`HTTP ${response.status}`)

    const reader = response.body.getReader()
    const decoder = new TextDecoder()

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      const chunk = decoder.decode(value, { stream: true })
      if (chunk) aiMsg.content += chunk
      scrollChatToBottom()
    }

    if (aiMsg.content) {
      if (!activeChatId.value) {
        await fetchSessions()
        if (chatHistory.value.length > 0) {
          activeChatId.value = chatHistory.value[0].id
        }
      }
      if (activeChatId.value) {
        await axios.post(`${BASE}/save`, { content: aiMsg.content }, {
          params: { sessionId: activeChatId.value }
        })
      }
    }
  } catch (error) {
    if (error.name !== 'AbortError') {
      aiMsg.content = '抱歉，网络连接出现问题，请稍后再试。'
    }
  } finally {
    isChatLoading.value = false
    abortController = null
  }
}

const renderMarkdown = (text) => md.render(text || '')

onMounted(() => { fetchSessions() })
onUnmounted(() => { abortController?.abort() })
</script>

<style scoped>
/* ===== 整体布局 ===== */
.chat-layout {
  display: flex;
  flex: 1;
  min-height: 0;
  background: #fff;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Microsoft YaHei", sans-serif;
  -webkit-font-smoothing: antialiased;
  overflow: hidden;
}

/* ===== 左侧边栏 ===== */
.sidebar {
  width: 260px;
  background: #f9f9f9;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
  flex: none;
  padding: 16px 12px;
  min-height: 0;
}

.new-chat-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 11px 0;
  background: #111;
  color: #fff;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background 0.2s;
}

.new-chat-btn:hover { background: #333; }

.history-title {
  font-size: 0.7rem;
  font-weight: 600;
  color: #999;
  margin: 20px 0 8px 4px;
  letter-spacing: 0.05em;
  text-transform: uppercase;
}

.history-list {
  flex: 1;
  overflow-y: auto;
}

.history-item {
  padding: 10px 12px;
  border-radius: 10px;
  font-size: 14px;
  color: #444;
  cursor: pointer;
  transition: background 0.15s;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.history-item:hover { background: #eee; }
.history-item.active { background: #e8e8e8; font-weight: 500; color: #111; }

/* ===== 右侧主区域 ===== */
.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  position: relative;
}

/* ===== 消息滚动区 ===== */
.chat-scroll {
  flex: 1;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.chat-content {
  max-width: 56rem;
  margin: 0 auto;
  padding: 32px 24px 0;
}

/* ===== 空状态 ===== */
.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 400px;
  text-align: center;
}

.empty-icon {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.chat-empty h2 {
  font-size: 1.4rem;
  font-weight: 600;
  color: #333;
  margin: 0 0 6px;
}

.chat-empty p {
  font-size: 0.9rem;
  color: #aaa;
  margin: 0;
}

/* ===== 消息行 ===== */
.msg-row {
  display: flex;
  margin-bottom: 24px;
  gap: 12px;
}

.msg-row--user {
  justify-content: flex-end;
}

.msg-row--ai {
  justify-content: flex-start;
  align-items: flex-start;
}

/* 用户气泡 */
.bubble-user {
  background: #f0f0f0;
  color: #111;
  padding: 12px 18px;
  border-radius: 20px;
  border-bottom-right-radius: 6px;
  font-size: 0.95rem;
  line-height: 1.6;
  max-width: 70%;
  word-break: break-word;
}

/* AI 头像 */
.ai-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #111;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 2px;
}

/* AI 正文（无气泡） */
.ai-body {
  flex: 1;
  min-width: 0;
  font-size: 0.95rem;
  line-height: 1.75;
  color: #222;
  padding-top: 4px;
}

/* ===== Markdown 样式覆盖（AI 消息） ===== */
.ai-body :deep(h1),
.ai-body :deep(h2),
.ai-body :deep(h3) {
  margin: 16px 0 8px;
  font-weight: 700;
  line-height: 1.4;
}

.ai-body :deep(h1) { font-size: 1.3rem; }
.ai-body :deep(h2) { font-size: 1.15rem; }
.ai-body :deep(h3) { font-size: 1.05rem; }

.ai-body :deep(p) {
  margin: 0 0 12px;
}

.ai-body :deep(p:last-child) {
  margin-bottom: 0;
}

.ai-body :deep(ul),
.ai-body :deep(ol) {
  padding-left: 24px;
  margin: 8px 0 12px;
}

.ai-body :deep(li) {
  margin-bottom: 4px;
}

.ai-body :deep(table) {
  border-collapse: collapse;
  margin: 12px 0;
  width: 100%;
  font-size: 0.88rem;
}

.ai-body :deep(th),
.ai-body :deep(td) {
  border: 1px solid #e5e5e5;
  padding: 8px 12px;
  text-align: left;
}

.ai-body :deep(th) {
  background: #f9f9f9;
  font-weight: 600;
}

.ai-body :deep(pre) {
  background: #1e1e1e;
  color: #d4d4d4;
  padding: 16px;
  border-radius: 10px;
  overflow-x: auto;
  font-size: 0.85rem;
  margin: 12px 0;
}

.ai-body :deep(code) {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 0.85rem;
}

.ai-body :deep(p code) {
  background: #f0f0f0;
  padding: 2px 6px;
  border-radius: 4px;
  color: #d63384;
}

.ai-body :deep(blockquote) {
  border-left: 3px solid #ddd;
  padding-left: 16px;
  color: #666;
  margin: 12px 0;
}

.ai-body :deep(hr) {
  border: none;
  border-top: 1px solid #eee;
  margin: 16px 0;
}

/* ===== 思考中指示器 ===== */
.thinking-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #999;
  font-size: 0.88rem;
  padding-top: 6px;
}

.dot-pulse {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #999;
  animation: dotPulse 1.4s infinite ease-in-out;
}

@keyframes dotPulse {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}

/* ===== 底部输入区 ===== */
.input-section {
  position: relative;
  flex-shrink: 0;
}

.input-fade {
  height: 80px;
  background: linear-gradient(to bottom, rgba(255, 255, 255, 0), #fff);
  pointer-events: none;
}

.input-inner {
  padding: 0 24px 28px;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .sidebar { display: none; }
  .chat-content { padding: 20px 16px 0; }
  .bubble-user { max-width: 85%; }
  .input-inner { padding: 0 12px 20px; }
}
</style>
