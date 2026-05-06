<!-- QuestionOption.vue -->
<template>
  <div
    class="option-item"
    :class="{ 'option-selected': isSelected, 'option-correct': isCorrect, 'option-wrong': isWrong }"
    @click="handleClick"
  >
    <span class="option-label">{{ label }}.</span>
    <div class="option-content">
      <template v-for="(part, index) in parsedParts" :key="index">
        <span v-if="part.type === 'text'">{{ part.content }}</span>
        <img
          v-else-if="part.type === 'image'"
          :src="part.url"
          :alt="'选项图片'"
          class="option-image"
          @load="onImageLoad"
          @error="onImageError"
        />
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { parseOptionText } from '@/utils/optionParser';

const props = defineProps({
  label: {
    type: String,
    required: true
  },
  text: {
    type: String,
    required: true
  },
  isSelected: {
    type: Boolean,
    default: false
  },
  isCorrect: {
    type: Boolean,
    default: false
  },
  isWrong: {
    type: Boolean,
    default: false
  },
  disabled: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['select']);

const parsedParts = computed(() => {
  return parseOptionText(props.text);
});

function handleClick() {
  if (!props.disabled) {
    emit('select', props.label);
  }
}

function onImageLoad(e) {
  // 图片加载成功
}

function onImageError(e) {
  e.target.style.display = 'none';
  const placeholder = document.createElement('span');
  placeholder.textContent = '[图片加载失败]';
  placeholder.className = 'image-error';
  e.target.parentNode.appendChild(placeholder);
}
</script>

<style scoped>
.option-item {
  display: flex;
  align-items: flex-start;
  padding: 12px 16px;
  margin: 8px 0;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.option-item:hover {
  border-color: #1976d2;
  background-color: #f5f9ff;
}

.option-selected {
  border-color: #1976d2;
  background-color: #e3f2fd;
}

.option-correct {
  border-color: #4caf50;
  background-color: #e8f5e9;
}

.option-wrong {
  border-color: #f44336;
  background-color: #ffebee;
}

.option-label {
  font-weight: bold;
  margin-right: 12px;
  min-width: 24px;
}

.option-content {
  flex: 1;
  line-height: 1.6;
}

.option-image {
  max-width: 100%;
  max-height: 200px;
  margin: 8px 0;
  border-radius: 4px;
  border: 1px solid #eee;
}

.image-error {
  color: #999;
  font-style: italic;
  font-size: 12px;
}
</style>
