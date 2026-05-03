<template>
  <div class="chat-layout">
    <!-- 左侧边栏 -->
    <aside class="sidebar">
      <button class="new-chat-btn" @click="handleNewChat">
        <span class="plus-icon">+</span> 发起新对话
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
      <div ref="chatContainerRef" class="chat-messages">
        <div v-if="chatMessages.length === 0" class="chat-empty">
          <h2>有什么备考问题，尽管问我</h2>
          <p>十年国考专家在线答疑，支持行测、申论</p>
        </div>
        <div
          v-for="(msg, idx) in chatMessages"
          :key="idx"
          class="chat-msg"
          :class="msg.role"
        >
          <div v-if="msg.role === 'user'" class="msg-bubble user-bubble">
            {{ msg.content }}
          </div>
          <div
            v-else
            class="msg-bubble ai-bubble markdown-body"
            v-html="renderMarkdown(msg.content)"
          ></div>
        </div>
        <div v-if="isChatLoading" class="chat-thinking">
          <span class="dot-pulse"></span> 正在思考...
        </div>
      </div>

      <div class="input-area">
        <div class="input-bar">
          <input
            v-model="chatInput"
            class="chat-input"
            placeholder="输入你的备考问题..."
            @keyup.enter="handleChatSend"
            :disabled="isChatLoading"
          />
          <button
            class="chat-send-btn"
            @click="handleChatSend"
            :disabled="isChatLoading || !chatInput.trim()"
          >
            发送
          </button>
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

const md = new MarkdownIt()
const BASE = 'http://localhost:8080/api/chat'
const USER_ID = 1

const chatMessages = ref([])
const chatInput = ref('')
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
  chatInput.value = ''
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

const handleChatSend = async () => {
  const prompt = chatInput.value.trim()
  if (!prompt || isChatLoading.value) return

  chatMessages.value.push({ role: 'user', content: prompt })
  chatInput.value = ''
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

    // 流结束后保存 AI 回复到数据库
    if (aiMsg.content) {
      // 如果是新对话，后端已自动创建 session，需要从响应头或重新获取
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
  height: calc(100vh - 56px);
  background: #fff;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

/* ===== 左侧边栏 ===== */
.sidebar {
  width: 260px;
  background: #f9f9f9;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  padding: 16px 12px;
}

.new-chat-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 12px 0;
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

.plus-icon {
  font-size: 18px;
  font-weight: 300;
  line-height: 1;
}

.history-title {
  font-size: 12px;
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
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 32px 0;
  scroll-behavior: smooth;
}

.chat-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #bbb;
  text-align: center;
}

.chat-empty h2 {
  font-size: 1.5rem;
  font-weight: 600;
  color: #555;
  margin-bottom: 8px;
}

.chat-empty p {
  font-size: 14px;
  color: #aaa;
}

.chat-msg {
  margin-bottom: 20px;
  display: flex;
  padding: 0 24px;
}

.chat-msg.user { justify-content: flex-end; }
.chat-msg.assistant { justify-content: flex-start; }

.msg-bubble {
  max-width: 70%;
  padding: 14px 20px;
  border-radius: 18px;
  font-size: 15px;
  line-height: 1.6;
  word-break: break-word;
}

.user-bubble {
  background: #111;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.ai-bubble {
  background: #f5f5f5;
  color: #222;
  border-bottom-left-radius: 4px;
}

.ai-bubble :deep(table) { border-collapse: collapse; margin: 12px 0; width: 100%; }
.ai-bubble :deep(th),
.ai-bubble :deep(td) { border: 1px solid #ddd; padding: 8px 12px; text-align: left; font-size: 14px; }
.ai-bubble :deep(th) { background: #f0f0f0; font-weight: 600; }
.ai-bubble :deep(pre) { background: #1e1e1e; color: #d4d4d4; padding: 16px; border-radius: 10px; overflow-x: auto; font-size: 13px; margin: 12px 0; }
.ai-bubble :deep(code) { font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace; font-size: 13px; }
.ai-bubble :deep(p code) { background: #e8e8e8; padding: 2px 6px; border-radius: 4px; color: #d63384; }
.ai-bubble :deep(ul),
.ai-bubble :deep(ol) { padding-left: 20px; margin: 8px 0; }
.ai-bubble :deep(blockquote) { border-left: 3px solid #ddd; padding-left: 16px; color: #666; margin: 12px 0; }

.chat-thinking {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #999;
  font-size: 14px;
  padding: 0 24px;
}

.dot-pulse {
  width: 6px; height: 6px;
  border-radius: 50%;
  background: #999;
  animation: dotPulse 1.4s infinite ease-in-out;
}

@keyframes dotPulse {
  0%, 80%, 100% { opacity: 0.3; transform: scale(0.8); }
  40% { opacity: 1; transform: scale(1); }
}

/* ===== 底部输入区 ===== */
.input-area {
  padding: 16px 24px 24px;
  flex-shrink: 0;
}

.input-bar {
  display: flex;
  gap: 12px;
  max-width: 800px;
  margin: 0 auto;
}

.chat-input {
  flex: 1;
  padding: 14px 20px;
  border: 1px solid #e0e0e0;
  border-radius: 14px;
  font-size: 15px;
  outline: none;
  transition: border-color 0.2s;
  font-family: inherit;
}

.chat-input:focus { border-color: #111; }
.chat-input:disabled { background: #f9f9f9; cursor: not-allowed; }

.chat-send-btn {
  padding: 14px 28px;
  background: #111;
  color: #fff;
  border: none;
  border-radius: 14px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.chat-send-btn:hover:not(:disabled) { background: #333; }
.chat-send-btn:disabled { opacity: 0.4; cursor: not-allowed; }

@media (max-width: 768px) {
  .sidebar { display: none; }
  .chat-msg { padding: 0 16px; }
  .msg-bubble { max-width: 90%; }
  .input-area { padding: 12px 16px 20px; }
}
</style>
