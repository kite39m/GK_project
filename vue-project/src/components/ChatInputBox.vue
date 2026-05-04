<template>
  <div class="chat-input-wrapper">
    <div class="chat-input-box" :class="{ 'chat-input-box--disabled': disabled }">
      <!-- 输入区 -->
      <textarea
        ref="textareaRef"
        class="chat-textarea"
        :placeholder="placeholder"
        v-model="inputText"
        @input="autoResize"
        @keydown="handleKeydown"
        :disabled="disabled"
        rows="1"
      ></textarea>

      <!-- 底部工具栏 -->
      <div class="toolbar">
        <!-- 左侧 -->
        <div class="toolbar-left">
          <button class="tool-btn" @click="$emit('attach')" :disabled="disabled">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
            </svg>
          </button>
          <button class="tool-btn tool-btn--label" @click="$emit('tools')" :disabled="disabled">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/>
            </svg>
            <span class="tool-label">工具</span>
          </button>
        </div>

        <!-- 右侧 -->
        <div class="toolbar-right">
          <button class="tool-btn tool-btn--model" @click="$emit('model-change')" :disabled="disabled">
            <span class="model-name">{{ model }}</span>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
              <polyline points="6 9 12 15 18 9"/>
            </svg>
          </button>

          <!-- 麦克风 / 发送 动态切换 -->
          <Transition name="icon-swap" mode="out-in">
            <button
              v-if="hasContent"
              key="send"
              class="tool-btn tool-btn--send"
              @click="handleSend"
              title="发送"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
              </svg>
            </button>
            <button
              v-else
              key="mic"
              class="tool-btn"
              @click="$emit('mic')"
              :disabled="disabled"
              title="语音输入"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
                <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
                <line x1="12" y1="19" x2="12" y2="23"/><line x1="8" y1="23" x2="16" y2="23"/>
              </svg>
            </button>
          </Transition>
        </div>
      </div>
    </div>

    <!-- 免责声明 -->
    <p class="disclaimer">内容由 AI 生成，仅供参考</p>
  </div>
</template>

<script setup>
import { ref, computed, nextTick } from 'vue'

const props = defineProps({
  placeholder: { type: String, default: '输入你的备考问题...' },
  model: { type: String, default: 'Pro' },
  disabled: { type: Boolean, default: false },
})

const emit = defineEmits(['submit', 'attach', 'tools', 'model-change', 'mic'])

const inputText = ref('')
const textareaRef = ref(null)

const hasContent = computed(() => inputText.value.trim().length > 0)

function autoResize() {
  const el = textareaRef.value
  if (!el) return
  el.style.height = 'auto'
  const max = 200
  el.style.height = Math.min(el.scrollHeight, max) + 'px'
  el.style.overflowY = el.scrollHeight > max ? 'auto' : 'hidden'
}

function handleSend() {
  const text = inputText.value.trim()
  if (!text || props.disabled) return
  emit('submit', text)
  inputText.value = ''
  nextTick(() => {
    autoResize()
    textareaRef.value?.focus()
  })
}

function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    handleSend()
  }
}
</script>

<style scoped>
.chat-input-wrapper {
  width: 100%;
  max-width: 56rem;
  margin: 0 auto;
}

.chat-input-box {
  background: #fff;
  border: 1px solid #e5e5e5;
  border-radius: 20px;
  padding: 12px 16px 10px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.chat-input-box:focus-within {
  border-color: #111;
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.06);
}

.chat-input-box--disabled {
  opacity: 0.5;
  pointer-events: none;
}

.chat-textarea {
  width: 100%;
  background: transparent;
  border: none;
  outline: none;
  resize: none;
  color: #111;
  font-size: 0.95rem;
  line-height: 1.5;
  font-family: inherit;
  padding: 4px 4px 0;
  min-height: 24px;
  max-height: 200px;
}

.chat-textarea::placeholder {
  color: #bbb;
}

/* 工具栏 */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
  padding: 0 2px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 2px;
}

.tool-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 34px;
  height: 34px;
  border: none;
  background: transparent;
  color: #999;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.15s;
  flex-shrink: 0;
}

.tool-btn:hover {
  color: #111;
  background: rgba(0, 0, 0, 0.05);
}

.tool-btn--send {
  color: #fff;
  background: #111;
}

.tool-btn--send:hover {
  color: #fff;
  background: #333;
}

.tool-btn--label {
  width: auto;
  padding: 0 10px 0 8px;
  border-radius: 16px;
  gap: 5px;
}

.tool-label {
  font-size: 0.78rem;
  font-weight: 500;
  white-space: nowrap;
}

.tool-btn--model {
  width: auto;
  padding: 0 8px 0 10px;
  border-radius: 16px;
  gap: 3px;
}

.model-name {
  font-size: 0.78rem;
  font-weight: 600;
  color: #999;
  transition: color 0.15s;
}

.tool-btn--model:hover .model-name {
  color: #111;
}

/* 图标切换动画 */
.icon-swap-enter-active {
  transition: all 0.15s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.icon-swap-leave-active {
  transition: all 0.1s ease-in;
}
.icon-swap-enter-from {
  opacity: 0;
  transform: scale(0.7);
}
.icon-swap-leave-to {
  opacity: 0;
  transform: scale(0.7);
}

/* 免责声明 */
.disclaimer {
  text-align: center;
  font-size: 0.7rem;
  color: #ccc;
  margin: 10px 0 0;
  padding: 0 16px;
}
</style>
