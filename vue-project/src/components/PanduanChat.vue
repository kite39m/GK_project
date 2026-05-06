<template>
  <div class="panduan-chat">
    <div class="messages" ref="messagesContainer">
      <div
        v-for="(msg, index) in messages"
        :key="index"
        :class="['message', msg.role]"
      >
        <div class="avatar" v-if="msg.role === 'assistant'">
          <el-icon><Monitor /></el-icon>
        </div>
        <div class="content">
          <div v-if="msg.role === 'user'" class="user-bubble">
            {{ msg.content }}
          </div>
          <div v-else class="ai-bubble" v-html="renderMarkdown(msg.content)"></div>
        </div>
      </div>
      <div v-if="loading" class="message assistant">
        <div class="avatar">
          <el-icon><Monitor /></el-icon>
        </div>
        <div class="content">
          <div class="ai-bubble loading">
            <span class="dot"></span>
            <span class="dot"></span>
            <span class="dot"></span>
          </div>
        </div>
      </div>
    </div>

    <div class="input-area">
      <el-input
        v-model="inputMessage"
        type="textarea"
        :rows="2"
        placeholder="输入你的问题，例如：帮我诊断错题、生成练习题、讲解解题技巧..."
        @keydown.enter.exact.prevent="sendMessage"
        :disabled="loading"
      />
      <el-button
        type="primary"
        @click="sendMessage"
        :loading="loading"
        :disabled="!inputMessage.trim()"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { Monitor } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'

const props = defineProps({
  userId: {
    type: Number,
    required: true
  }
})

const emit = defineEmits(['message-sent'])

const md = new MarkdownIt()
const messages = ref([])
const inputMessage = ref('')
const loading = ref(false)
const messagesContainer = ref(null)

const renderMarkdown = (text) => {
  return md.render(text || '')
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

const sendMessage = async () => {
  const message = inputMessage.value.trim()
  if (!message || loading.value) return

  // 添加用户消息
  messages.value.push({ role: 'user', content: message })
  inputMessage.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const response = await fetch(`/api/panduan/chat?userId=${props.userId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message })
    })

    if (!response.ok) {
      throw new Error('请求失败')
    }

    const data = await response.json()
    messages.value.push({ role: 'assistant', content: data.response })
    emit('message-sent', { message, response: data.response })
  } catch (error) {
    console.error('发送消息失败:', error)
    messages.value.push({
      role: 'assistant',
      content: '抱歉，发生了错误，请稍后重试。'
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

// 暴露方法给父组件
defineExpose({
  addMessage: (role, content) => {
    messages.value.push({ role, content })
    scrollToBottom()
  },
  sendMessage
})
</script>

<style scoped>
.panduan-chat {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #f5f7fa;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message {
  display: flex;
  margin-bottom: 16px;
  gap: 12px;
}

.message.user {
  flex-direction: row-reverse;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  flex-shrink: 0;
}

.content {
  max-width: 70%;
}

.user-bubble {
  background: #409eff;
  color: white;
  padding: 12px 16px;
  border-radius: 12px 12px 0 12px;
  word-break: break-word;
}

.ai-bubble {
  background: white;
  padding: 12px 16px;
  border-radius: 12px 12px 12px 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  word-break: break-word;
  line-height: 1.6;
}

.ai-bubble :deep(p) {
  margin: 0 0 8px 0;
}

.ai-bubble :deep(p:last-child) {
  margin-bottom: 0;
}

.loading {
  display: flex;
  gap: 4px;
  padding: 16px;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #909399;
  animation: bounce 1.4s infinite ease-in-out both;
}

.dot:nth-child(1) {
  animation-delay: -0.32s;
}

.dot:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes bounce {
  0%, 80%, 100% {
    transform: scale(0);
  }
  40% {
    transform: scale(1);
  }
}

.input-area {
  padding: 16px;
  background: white;
  border-top: 1px solid #e4e7ed;
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input-area .el-textarea {
  flex: 1;
}

.input-area .el-button {
  height: 40px;
}
</style>
