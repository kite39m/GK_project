# 判断推理辅导 Agent 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为判断推理模块添加 AI 辅导功能，包括错题诊断、题目生成、解题指导和进步评估

**Architecture:** 使用 LangChain4j 的 @AiService 注解创建 PanduanTutor Agent，绑定 PanduanDataTools（本地数据库查询）和现有的 WebSearchTool/PageFetchTool（外部网站采集）。前端创建独立的辅导页面，支持对话和快捷按钮两种交互方式。

**Tech Stack:** Spring Boot, LangChain4j, MyBatis-Plus, Vue 3, Element Plus

---

## 文件结构

### 后端文件

| 文件路径 | 职责 |
|---------|------|
| `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/tutor/PanduanTutor.java` | AI Service 接口 |
| `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/tools/PanduanDataTools.java` | 数据访问工具 |
| `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/controller/PanduanController.java` | REST 控制器 |
| `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/service/PanduanService.java` | 服务接口 |
| `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/service/impl/PanduanServiceImpl.java` | 服务实现 |

### 前端文件

| 文件路径 | 职责 |
|---------|------|
| `vue-project/src/views/PanduanView.vue` | 判断推理辅导页面 |
| `vue-project/src/components/PanduanChat.vue` | 对话组件 |
| `vue-project/src/components/PanduanButtons.vue` | 快捷按钮组件 |

---

## Task 1: 创建 PanduanDataTools

**Files:**
- Create: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/tools/PanduanDataTools.java`

- [ ] **Step 1: 创建 PanduanDataTools 类**

```java
package com.zwy.gk_backend.ai.tools;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zwy.gk_backend.entity.UserAnswerRecord;
import com.zwy.gk_backend.entity.UserKnowledgeProfile;
import com.zwy.gk_backend.mapper.UserAnswerRecordMapper;
import com.zwy.gk_backend.mapper.UserKnowledgeProfileMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component("panduanDataTools")
@RequiredArgsConstructor
public class PanduanDataTools {

    private final UserAnswerRecordMapper userAnswerRecordMapper;
    private final UserKnowledgeProfileMapper userKnowledgeProfileMapper;

    @Tool("获取用户在判断推理模块的最近错题明细，返回错题的考点、用户答案、是否陷阱选项、答题耗时等信息")
    public String getRecentErrorDetails(
            @P("用户ID") int userId,
            @P("获取数量，默认10条") int limit) {
        log.info("获取用户{}在判断推理模块的最近{}条错题", userId, limit);

        QueryWrapper<UserAnswerRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("module", "TUILI")
               .eq("is_correct", 0)
               .orderByDesc("created_at")
               .last("LIMIT " + limit);

        List<UserAnswerRecord> records = userAnswerRecordMapper.selectList(wrapper);

        if (records.isEmpty()) {
            return "该用户在判断推理模块暂无错题记录";
        }

        List<String> errorDetails = records.stream()
                .map(r -> String.format(
                        "考点: %s, 用户答案: %s, 是否陷阱选项: %s, 答题耗时: %d秒",
                        r.getConcept(),
                        r.getUserAnswer(),
                        r.getIsTrapOption() == 1 ? "是" : "否",
                        r.getTimeCostSec()))
                .collect(Collectors.toList());

        return String.join("\n", errorDetails);
    }

    @Tool("获取用户在判断推理模块的熟练度画像，返回各考点的熟练度、练习次数等信息")
    public String getKnowledgeProfile(@P("用户ID") int userId) {
        log.info("获取用户{}在判断推理模块的熟练度画像", userId);

        QueryWrapper<UserKnowledgeProfile> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("module", "TUILI")
               .orderByDesc("proficiency");

        List<UserKnowledgeProfile> profiles = userKnowledgeProfileMapper.selectList(wrapper);

        if (profiles.isEmpty()) {
            return "该用户在判断推理模块暂无练习记录";
        }

        List<String> profileDetails = profiles.stream()
                .map(p -> String.format(
                        "考点: %s, 熟练度: %d%%, 练习次数: %d, 正确次数: %d",
                        p.getConcept(),
                        p.getProficiency(),
                        p.getTotalAttempts(),
                        p.getCorrectCount()))
                .collect(Collectors.toList());

        return String.join("\n", profileDetails);
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `cd D:/GK/App/gk-backend/gk-backend && mvn compile -pl . -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/tools/PanduanDataTools.java
git commit -m "feat: add PanduanDataTools for panduan tutor agent"
```

---

## Task 2: 创建 PanduanTutor AI Service 接口

**Files:**
- Create: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/tutor/PanduanTutor.java`

- [ ] **Step 1: 创建 PanduanTutor 接口**

```java
package com.zwy.gk_backend.ai.tutor;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService(tools = {"panduanDataTools", "webSearchTool", "pageFetchTool"})
public interface PanduanTutor {

    @SystemMessage("""
        你是判断推理模块的金牌辅导专家，拥有十年公考教学经验。
        你的任务是帮助用户提升判断推理模块的解题能力。

        ## 能力范围
        1. **错题诊断**：分析用户的错题，找出薄弱点和错因
        2. **题目生成**：根据用户水平生成新的练习题
        3. **解题指导**：提供判断推理的解题技巧和策略
        4. **进步评估**：评估用户在判断推理模块的进步情况

        ## 工作流程
        ### 错题诊断
        1. 调用 getRecentErrorDetails 获取用户在判断推理模块的错题
        2. 分析错题，识别错因类型：
           - 知识盲区：缺乏相关知识点
           - 粗心陷阱：会做但选了高频干扰项
           - 做题超时：会做但耗时过长
        3. 输出结构化诊断报告

        ### 题目生成
        1. 调用 getKnowledgeProfile 获取用户在判断推理模块的熟练度
        2. 根据熟练度水平，生成适合的练习题
        3. 题目类型包括：图形推理、定义判断、类比推理、逻辑判断
        4. 输出格式：JSON 数组，每道题包含 title、optionsJson、answer、analysis、category、concept

        ### 解题指导
        1. 根据用户的问题，提供针对性的解题技巧
        2. 技巧应具体、可操作，包含示例
        3. 可以调用 webSearchTool 搜索粉笔网的解题技巧

        ### 进步评估
        1. 调用 getKnowledgeProfile 获取用户的历史数据
        2. 分析用户在判断推理模块的进步趋势
        3. 输出进步报告，包含：当前水平、进步幅度、下一步建议

        ## 输出格式
        - 使用中文回复
        - 语言专业但通俗易懂
        - 结构化输出，便于前端展示
    """)
    String tutor(@UserMessage String message);
}
```

- [ ] **Step 2: 验证编译**

Run: `cd D:/GK/App/gk-backend/gk-backend && mvn compile -pl . -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/tutor/PanduanTutor.java
git commit -m "feat: add PanduanTutor AI service interface"
```

---

## Task 3: 创建 PanduanService 服务层

**Files:**
- Create: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/service/PanduanService.java`
- Create: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/service/impl/PanduanServiceImpl.java`

- [ ] **Step 1: 创建 PanduanService 接口**

```java
package com.zwy.gk_backend.service;

public interface PanduanService {

    /**
     * 与判断推理辅导 Agent 对话
     * @param userId 用户ID
     * @param message 用户消息
     * @return Agent 回复
     */
    String chat(int userId, String message);
}
```

- [ ] **Step 2: 创建 PanduanServiceImpl 实现**

```java
package com.zwy.gk_backend.service.impl;

import com.zwy.gk_backend.ai.tutor.PanduanTutor;
import com.zwy.gk_backend.service.PanduanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PanduanServiceImpl implements PanduanService {

    private final PanduanTutor panduanTutor;

    @Override
    public String chat(int userId, String message) {
        log.info("用户{}发起判断推理辅导对话: {}", userId, message);

        // 将用户ID和消息组合，传递给 Agent
        String prompt = String.format("用户ID: %d\n用户消息: %s", userId, message);

        String response = panduanTutor.tutor(prompt);
        log.info("判断推理辅导Agent回复: {}", response);

        return response;
    }
}
```

- [ ] **Step 3: 验证编译**

Run: `cd D:/GK/App/gk-backend/gk-backend && mvn compile -pl . -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/service/PanduanService.java
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/service/impl/PanduanServiceImpl.java
git commit -m "feat: add PanduanService for panduan tutor"
```

---

## Task 4: 创建 PanduanController

**Files:**
- Create: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/controller/PanduanController.java`

- [ ] **Step 1: 创建 PanduanController**

```java
package com.zwy.gk_backend.controller;

import com.zwy.gk_backend.service.PanduanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/panduan")
@RequiredArgsConstructor
public class PanduanController {

    private final PanduanService panduanService;

    /**
     * 与判断推理辅导 Agent 对话
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(
            @RequestParam int userId,
            @RequestBody Map<String, String> request) {

        String message = request.get("message");
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "消息不能为空"));
        }

        log.info("用户{}发起判断推理辅导对话: {}", userId, message);

        String response = panduanService.chat(userId, message);

        return ResponseEntity.ok(Map.of("response", response));
    }
}
```

- [ ] **Step 2: 验证编译**

Run: `cd D:/GK/App/gk-backend/gk-backend && mvn compile -pl . -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/controller/PanduanController.java
git commit -m "feat: add PanduanController for panduan tutor"
```

---

## Task 5: 创建前端 PanduanChat 组件

**Files:**
- Create: `vue-project/src/components/PanduanChat.vue`

- [ ] **Step 1: 创建 PanduanChat 组件**

```vue
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
  }
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
```

- [ ] **Step 2: 验证前端编译**

Run: `cd D:/GK/App/vue-project && npm run build -- --mode development 2>&1 | head -20`
Expected: 编译成功，无错误

- [ ] **Step 3: Commit**

```bash
git add vue-project/src/components/PanduanChat.vue
git commit -m "feat: add PanduanChat component"
```

---

## Task 6: 创建前端 PanduanButtons 组件

**Files:**
- Create: `vue-project/src/components/PanduanButtons.vue`

- [ ] **Step 1: 创建 PanduanButtons 组件**

```vue
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
import { Diagnosis, Document, Reading, TrendCharts } from '@element-plus/icons-vue'

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
    icon: Diagnosis,
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
```

- [ ] **Step 2: 验证前端编译**

Run: `cd D:/GK/App/vue-project && npm run build -- --mode development 2>&1 | head -20`
Expected: 编译成功，无错误

- [ ] **Step 3: Commit**

```bash
git add vue-project/src/components/PanduanButtons.vue
git commit -m "feat: add PanduanButtons component"
```

---

## Task 7: 创建 PanduanView 页面

**Files:**
- Create: `vue-project/src/views/PanduanView.vue`

- [ ] **Step 1: 创建 PanduanView 页面**

```vue
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
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import PanduanChat from '@/components/PanduanChat.vue'
import PanduanButtons from '@/components/PanduanButtons.vue'

const router = useRouter()
const userStore = useUserStore()

const userId = computed(() => userStore.userId || 1)
const chatRef = ref(null)
const loading = ref(false)

const goBack = () => {
  router.push('/xingce')
}

const handleAction = (btn) => {
  if (chatRef.value) {
    chatRef.value.addMessage('user', btn.message)
    // 触发发送
    chatRef.value.sendMessage()
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
```

- [ ] **Step 2: 添加路由配置**

在 `vue-project/src/router/index.js` 中添加路由：

```javascript
{
  path: '/panduan',
  name: 'Panduan',
  component: () => import('@/views/PanduanView.vue'),
  meta: { requiresAuth: true }
}
```

- [ ] **Step 3: 验证前端编译**

Run: `cd D:/GK/App/vue-project && npm run build -- --mode development 2>&1 | head -20`
Expected: 编译成功，无错误

- [ ] **Step 4: Commit**

```bash
git add vue-project/src/views/PanduanView.vue
git add vue-project/src/router/index.js
git commit -m "feat: add PanduanView page and route"
```

---

## Task 8: 添加导航入口

**Files:**
- Modify: `vue-project/src/views/XingceView.vue`

- [ ] **Step 1: 在 XingceView 添加判断推理辅导入口**

在模块选择页面添加一个卡片或按钮，链接到判断推理辅导页面。

```vue
<!-- 在适当位置添加 -->
<el-card class="module-card panduan-card" @click="goToPanduan">
  <div class="card-icon">
    <el-icon><ChatLineRound /></el-icon>
  </div>
  <div class="card-title">判断推理辅导</div>
  <div class="card-desc">AI 辅导专家帮你提升判断推理能力</div>
</el-card>
```

添加点击方法：

```javascript
const goToPanduan = () => {
  router.push('/panduan')
}
```

- [ ] **Step 2: 验证前端编译**

Run: `cd D:/GK/App/vue-project && npm run build -- --mode development 2>&1 | head -20`
Expected: 编译成功，无错误

- [ ] **Step 3: Commit**

```bash
git add vue-project/src/views/XingceView.vue
git commit -m "feat: add panduan tutor entry in XingceView"
```

---

## Task 9: 后端集成测试

**Files:**
- Create: `gk-backend/gk-backend/src/test/java/com/zwy/gk_backend/ai/tutor/PanduanTutorTest.java`

- [ ] **Step 1: 创建集成测试**

```java
package com.zwy.gk_backend.ai.tutor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PanduanTutorTest {

    @Autowired
    private PanduanTutor panduanTutor;

    @Test
    void testDiagnose() {
        String response = panduanTutor.tutor("用户ID: 1\n用户消息: 帮我诊断在判断推理模块的错题");
        assertNotNull(response);
        assertFalse(response.isEmpty());
        System.out.println("诊断结果: " + response);
    }

    @Test
    void testGenerateQuestions() {
        String response = panduanTutor.tutor("用户ID: 1\n用户消息: 根据我的水平，生成一些判断推理练习题");
        assertNotNull(response);
        assertFalse(response.isEmpty());
        System.out.println("生成题目: " + response);
    }

    @Test
    void testGuide() {
        String response = panduanTutor.tutor("用户ID: 1\n用户消息: 讲解判断推理的解题技巧和策略");
        assertNotNull(response);
        assertFalse(response.isEmpty());
        System.out.println("解题指导: " + response);
    }

    @Test
    void testProgress() {
        String response = panduanTutor.tutor("用户ID: 1\n用户消息: 评估我在判断推理模块的进步情况");
        assertNotNull(response);
        assertFalse(response.isEmpty());
        System.out.println("进步评估: " + response);
    }
}
```

- [ ] **Step 2: 运行测试**

Run: `cd D:/GK/App/gk-backend/gk-backend && mvn test -Dtest=PanduanTutorTest -pl .`
Expected: 所有测试通过

- [ ] **Step 3: Commit**

```bash
git add gk-backend/gk-backend/src/test/java/com/zwy/gk_backend/ai/tutor/PanduanTutorTest.java
git commit -m "test: add PanduanTutor integration tests"
```

---

## Task 10: 端到端测试

- [ ] **Step 1: 启动后端服务**

Run: `cd D:/GK/App/gk-backend/gk-backend && mvn spring-boot:run`
Expected: 服务启动成功

- [ ] **Step 2: 测试 API 端点**

Run: `curl -X POST "http://localhost:8080/api/panduan/chat?userId=1" -H "Content-Type: application/json" -d '{"message": "帮我诊断错题"}'`
Expected: 返回 JSON 响应，包含 Agent 的诊断结果

- [ ] **Step 3: 启动前端服务**

Run: `cd D:/GK/App/vue-project && npm run dev`
Expected: 前端服务启动成功

- [ ] **Step 4: 测试前端页面**

1. 访问 http://localhost:5173/panduan
2. 点击"诊断错题"按钮
3. 验证 Agent 回复正常显示
4. 测试其他快捷按钮功能

- [ ] **Step 5: Commit 最终版本**

```bash
git add -A
git commit -m "feat: complete panduan tutor implementation"
```

---

## 完成

实施计划完成。判断推理辅导 Agent 已实现以下功能：

1. **错题诊断**：分析用户在判断推理模块的错题，找出薄弱点和错因
2. **题目生成**：根据用户水平生成判断推理练习题
3. **解题指导**：提供判断推理的解题技巧和策略
4. **进步评估**：评估用户在判断推理模块的进步情况

前端支持对话和快捷按钮两种交互方式，用户体验友好。
