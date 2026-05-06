<template>
  <div class="panduan-buttons">
    <el-button
      v-for="btn in buttons"
      :key="btn.action"
      :type="btn.type"
      :icon="btn.icon"
      @click="handleClick(btn)"
      :disabled="disabled"
    >
      {{ btn.label }}
    </el-button>
  </div>
</template>

<script setup>
import { Search, Document, Reading, TrendCharts } from '@element-plus/icons-vue'

const props = defineProps({
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['action'])

const buttons = [
  {
    action: 'diagnose',
    label: '诊断错题',
    type: 'primary',
    icon: Search,
    message: '帮我诊断在判断推理模块的错题，分析我的薄弱点和错因'
  },
  {
    action: 'generate',
    label: '生成题目',
    type: 'success',
    icon: Document,
    message: '根据我的水平，生成一些判断推理练习题'
  },
  {
    action: 'guide',
    label: '解题技巧',
    type: 'warning',
    icon: Reading,
    message: '讲解判断推理的解题技巧和策略'
  },
  {
    action: 'progress',
    label: '查看进步',
    type: 'info',
    icon: TrendCharts,
    message: '评估我在判断推理模块的进步情况'
  }
]

const handleClick = (btn) => {
  emit('action', btn)
}
</script>

<style scoped>
.panduan-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  padding: 16px;
  background: white;
  border-top: 1px solid #e4e7ed;
}

.panduan-buttons .el-button {
  flex: 1;
  min-width: 120px;
}
</style>
