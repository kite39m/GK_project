<template>
  <div class="panduan-view">
    <div class="header">
      <el-page-header @back="goBack">
        <template #content>
          <span class="title">判断推理辅导</span>
        </template>
      </el-page-header>
    </div>

    <div class="content">
      <PanduanChat
        ref="chatRef"
        :userId="userId"
        @message-sent="onMessageSent"
      />
    </div>

    <PanduanButtons
      :disabled="loading"
      @action="handleAction"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import PanduanChat from '../components/PanduanChat.vue'
import PanduanButtons from '../components/PanduanButtons.vue'

const router = useRouter()

const userId = 1
const chatRef = ref(null)
const loading = ref(false)

const goBack = () => {
  router.push('/xingce')
}

const handleAction = (btn) => {
  if (chatRef.value) {
    chatRef.value.addMessage('user', btn.message)
    // 触发发送，传入消息内容
    chatRef.value.sendMessage(btn.message)
  }
}

const onMessageSent = (data) => {
  // 可以在这里处理消息发送后的逻辑
  console.log('消息已发送:', data)
}
</script>

<style scoped>
.panduan-view {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f7fa;
}

.header {
  padding: 16px 20px;
  background: white;
  border-bottom: 1px solid #e4e7ed;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.content {
  flex: 1;
  overflow: hidden;
}
</style>
