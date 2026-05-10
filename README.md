# GK-Diagnosis-Agent

**国考行测智能备考系统 -- 基于多 Agent 协作的自适应学习平台**

开发者: 赵文阳

---

## 项目简介

GK-Diagnosis-Agent 是一个面向公务员考试（国考/省考）的 AI 驱动备考平台。系统通过 **多 Agent 协作架构**，实现错题深度归因、个性化策略推荐、动态题目生成、网络真题自动采集等能力，帮助考生从"题海战术"转向"精准突破"。

## 核心痛点

传统公考备考存在三个根本性问题：

| 痛点 | 表现 | 本系统解法 |
|------|------|-----------|
| **错因分析缺失** | 刷题后只知道"错了"，不知道"为什么错"，同类错误反复出现 | ExamAiTutor 三维度归因（知识盲区 / 粗心陷阱 / 做题超时） |
| **复习策略盲目** | 所有知识点平均用力，薄弱环节得不到强化 | SmartQuestionStrategy 基于知识点画像的自适应出题 |
| **真题资源分散** | 真题散落在各个网站，手动收集效率低、格式不统一 | QuestionCollectorAgent 全流程自动采集 + 结构化入库 |

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + Vite + Vue Router 4 + Element Plus + ECharts |
| 后端 | Spring Boot 3.5 + MyBatis-Plus + Flyway |
| 数据库 | MySQL 8.0 |
| 鉴权 | JWT (jjwt) |
| AI 框架 | LangChain4j (OpenAI 兼容协议) |
| 大模型 | 小米 MiMo-V2.5-Pro |
| 工具集成 | WebSearch (Bing via Jsoup)、PageFetch、ImageExtractor |

## 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                      Vue 3 SPA 前端                         │
│  ┌──────┐ ┌────────┐ ┌────────┐ ┌──────┐ ┌───────┐        │
│  │ 行测  │ │ 申论写作 │ │ 判断推理 │ │ 题库  │ │ 统计  │        │
│  └──┬───┘ └───┬────┘ └───┬────┘ └──┬───┘ └───┬───┘        │
└─────┼─────────┼──────────┼─────────┼─────────┼─────────────┘
      │         │          │         │         │
      ▼         ▼          ▼         ▼         ▼
┌─────────────────────────────────────────────────────────────┐
│                   Spring Boot REST API                       │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              LangChain4j AI 调度层                    │   │
│  │                                                     │   │
│  │   ┌─────────────┐   ┌──────────────┐   ┌─────────┐ │   │
│  │   │ ExamAiTutor │   │ PanduanTutor │   │ Shenlun │ │   │
│  │   │  (行测诊断)  │   │  (判断推理)   │   │ AiTutor │ │   │
│  │   └──────┬──────┘   └──────┬───────┘   │(申论批改)│ │   │
│  │          │                 │            └─────────┘ │   │
│  │   ┌──────┴──────┐   ┌─────┴──────┐   ┌───────────┐ │   │
│  │   │  Question   │   │ Collector  │   │   Smart   │ │   │
│  │   │  Generator  │   │   Agent    │   │ Strategy  │ │   │
│  │   │  (题目生成)  │   │ (真题采集)  │   │ (策略引擎)│ │   │
│  │   └─────────────┘   └────────────┘   └───────────┘ │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │                Tool 层 (Agent 工具)                   │   │
│  │  ExamDataTools │ PanduanDataTools │ WebSearchTool    │   │
│  │  PageFetchTool │ ImageExtractor                      │   │
│  └─────────────────────────────────────────────────────┘   │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
                     ┌──────────┐
                     │  MySQL   │
                     └──────────┘
```

## 多 Agent 协作架构

系统包含 **6 个专职 Agent**，基于 LangChain4j 的 `@AiService` + `@Tool` 注解构建，每个 Agent 拥有独立的 System Prompt、工具集和推理链。

### Agent 总览

```
用户请求
   │
   ▼
┌──────────────────────────────────────────────────────────┐
│                    Controller 层                          │
│  XingceController │ PanduanController │ ShenlunController │
│  CollectorController                                      │
└──────────┬───────────────────────────────────────────────┘
           │
           ▼
┌──────────────────────────────────────────────────────────┐
│                    Agent 协作层                            │
│                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────┐ │
│  │ ExamAiTutor  │  │ PanduanTutor │  │ ShenlunAiTutor │ │
│  │  三维度归因   │  │  多工具编排   │  │  三维评分      │ │
│  │  长链推理     │  │  联网搜索     │  │  纯 LLM 推理   │ │
│  └──────┬───────┘  └──────┬───────┘  └────────────────┘ │
│         │                 │                               │
│         ▼                 ▼                               │
│  ┌──────────────┐  ┌──────────────┐  ┌────────────────┐ │
│  │ SmartStrategy│  │ Collector    │  │ QuestionGen    │ │
│  │  自适应出题   │  │  真题采集     │  │  题目生成      │ │
│  │  画像驱动     │  │  六步流水线   │  │  异步事件驱动  │ │
│  └──────────────┘  └──────────────┘  └────────────────┘ │
└──────────────────────────────────────────────────────────┘
```

### 1. ExamAiTutor -- 行测诊断 Agent（长链推理）

**核心能力**: 接收用户答题简报，自动执行四步推理链完成错题归因。

**推理链 (Chain-of-Thought)**:

```
Step 1: 扫描简报 → 识别正确率 < 70% 的薄弱模块
   │
   ▼  (若所有模块 ≥ 70%，直接跳到 Step 4 给出鼓励)
Step 2: 调用 ExamDataTools.getRecentErrorDetails() → 获取该模块近期错题
   │
   ▼
Step 3: 对每道错题进行三维度归因
   ├── 知识盲区 (Knowledge Gap) → 用户缺乏该知识点
   ├── 粗心陷阱 (Careless Trap) → 选了高频干扰项
   └── 做题超时 (Timeout) → 知识掌握但耗时过长
   │
   ▼
Step 4: 输出结构化诊断报告 + 针对性建议
```

**工具集**: `ExamDataTools`（答题记录查询、知识点画像查询）

**调用入口**: `POST /api/xingce/diagnose`

---

### 2. PanduanTutor -- 判断推理 Agent（多工具编排）

**核心能力**: 配备 3 个工具 Bean，支持 4 种工作模式，是系统中工具编排最复杂的 Agent。

**工具编排矩阵**:

| 工作模式 | 触发场景 | 工具调用链 | 输出 |
|---------|---------|-----------|------|
| 错题诊断 | 用户问"我哪里做错了" | `getPanduanErrorDetails` → 归因分析 | 结构化诊断报告 |
| 题目生成 | 用户要求练习 | `getPanduanKnowledgeProfile` → 按画像生成 | JSON 题目数组 |
| 解题指导 | 用户问某类题怎么做 | LLM 推理 + `webSearchTool` 搜索粉笔网技巧 | 具体方法 + 示例 |
| 进步评估 | 用户问"我进步了吗" | `getPanduanKnowledgeProfile` → 趋势分析 | 进步报告 |

**联网搜索链**:

```
用户提问 → PanduanTutor 判断需要搜索
   │
   ▼
WebSearchTool.searchExamQuestions(keyword)
   │  → Bing 搜索，提取 5 个非 Bing 链接
   ▼
PageFetchTool.fetchPageContent(url)
   │  → Jsoup 抓取，清理脚本/样式，返回 5000 字正文
   ▼
LLM 综合分析 → 输出带来源的解答
```

**调用入口**: `POST /api/panduan/chat`

---

### 3. ShenlunAiTutor -- 申论批改 Agent（纯 LLM 推理）

**核心能力**: 无工具依赖，纯靠 LLM 推理完成申论评分和素材推荐。

**评分维度**:

```
用户提交申论
   │
   ├── 结构分 (0-100) → 是否总分总、层次是否分明
   ├── 论点分 (0-100) → 论点是否准确、论据是否充分
   └── 语言分 (0-100) → 表达是否规范流畅
   │
   ▼
总分 = (结构分 + 论点分 + 语言分) / 3，四舍五入
   │
   ▼
输出严格 JSON → 持久化到数据库
```

**调用入口**: `POST /api/shenlun/submit`

---

### 4. QuestionCollectorAgent -- 真题采集 Agent（六步流水线）

**核心能力**: 全自动化真题采集，从搜索到结构化入库。

**采集流水线**:

```
Step 1: searchExamQuestions(keyword)
   │  → Bing 搜索公务员真题关键词
   ▼
Step 2: fetchPageContent(url)
   │  → 抓取页面正文
   ▼
Step 3: extractImages(url)
   │  → 提取、验证、去重、持久化图片 URL
   ▼
Step 4: LLM 解析
   │  → 识别题目、选项、答案、解析
   ▼
Step 5: 图片嵌入
   │  → 将图片 URL 以 Markdown 语法嵌入选项 JSON
   ▼
Step 6: 输出 JSON 数组
   → Controller 校验 → MD5 去重 → 入库 (status=PENDING)
```

**选项格式**:

| 类型 | 示例 |
|------|------|
| 纯文本 | `{"A": "选项文本"}` |
| 纯图片 | `{"A": "![image](https://xxx.jpg)"}` |
| 图文混合 | `{"A": "文本 ![image](https://xxx.jpg)"}` |

**调用入口**: `POST /api/collector/collect`

---

### 5. QuestionGeneratorTutor -- 题目生成 Agent

**核心能力**: 接收 SmartQuestionStrategy 构建的富 Prompt，生成带干扰项的高质量练习题。

**触发方式**: 异步事件驱动。当现有题库不足时，`XingceServiceImpl` 发布 `QuestionGenerateEvent`，`QuestionGenerateListener` 异步捕获并调用。

**调用入口**: `POST /api/xingce/generate`

---

### 6. SmartQuestionStrategy -- 策略引擎

**核心能力**: 不是 AI Agent 本身，而是基于用户画像构建自适应 Prompt 的策略引擎。

**自适应出题策略**:

```
用户请求出题
   │
   ▼
读取 UserKnowledgeProfile (知识点熟练度)
   │
   ├── 熟练度 < 40% (薄弱) → 重点出基础题巩固
   ├── 熟练度 40-70% (中等) → 巩固 + 进阶混合
   └── 熟练度 > 70% (熟练) → 少出题，挑战高难度
   │
   ▼
查询 KnowledgePoint 表 → 按分类分组
   │
   ▼
构建 Prompt:
   ├── 用户知识点画像段 (proficiency tiers)
   ├── 知识点覆盖要求 (grouped by category)
   ├── 多样性要求 (题型/干扰项/表述)
   └── 简洁分析要求 (每题 ≤ 50 字)
   │
   ▼
传给 QuestionGeneratorTutor → 生成 JSON 题目
```

## 自适应出题完整流程

这是系统最核心的链路，涉及多层协作：

```
POST /api/xingce/generate
   │
   ▼
XingceServiceImpl.generateQuestions()
   │
   ├── 1. 读取用户知识点画像 → 确定难度区间
   │      < 40% → difficulty 1-2
   │      40-70% → difficulty 2-4
   │      > 70% → difficulty 3-5
   │
   ├── 2. 从现有题池筛选
   │      ├── 排除已答对的题
   │      ├── 优先薄弱知识点 + 曾错题
   │      └── 多样性过滤 (覆盖 2-3 个知识点)
   │
   ├── 3. 四级降级策略
   │      AI 题池 → 正式题库 → 错题重练 → 无限制兜底
   │
   └── 4. 若题池不足 → 发布 QuestionGenerateEvent (异步)
              │
              ▼
   QuestionGenerateListener (@Async)
              │
              ├── SmartQuestionStrategy.buildProfilePrompt() → 画像段
              ├── SmartQuestionStrategy.buildModulePrompt() → 知识点覆盖段
              │
              ▼
   QuestionGeneratorTutor.generateQuestions(合并 Prompt)
              │
              ▼
   解析 JSON → 校验 → MD5 去重 → 入库 (sourceType=AI_GEN, status=ACTIVE)
```

## Agent 工具共享矩阵

| 工具 | ExamAiTutor | PanduanTutor | Collector | Generator | Shenlun |
|------|:-----------:|:------------:|:---------:|:---------:|:-------:|
| ExamDataTools | **独占** | | | | |
| PanduanDataTools | | **独占** | | | |
| WebSearchTool | | **共享** | **共享** | | |
| PageFetchTool | | **共享** | **共享** | | |
| ImageExtractor | | (间接) | (间接) | | |
| 无工具 (纯 LLM) | | | | **是** | **是** |

## 项目结构

```
gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/
├── ai/
│   ├── tutor/                          # AI 辅导 Agent
│   │   ├── ExamAiTutor.java            # 行测诊断 (长链推理)
│   │   ├── PanduanTutor.java           # 判断推理 (多工具编排)
│   │   ├── ShenlunAiTutor.java         # 申论批改 (纯 LLM)
│   │   └── QuestionGeneratorTutor.java # 题目生成
│   ├── collector/
│   │   └── QuestionCollectorAgent.java # 真题采集 (六步流水线)
│   ├── strategy/
│   │   └── SmartQuestionStrategy.java  # 自适应出题策略
│   └── tools/                          # Agent 可调用的工具
│       ├── ExamDataTools.java          # 答题数据查询
│       ├── PanduanDataTools.java       # 判断推理数据查询
│       ├── WebSearchTool.java          # 网络搜索 (Bing)
│       ├── PageFetchTool.java          # 页面抓取 + 图片提取
│       └── ImageExtractor.java         # 图片验证/去重/持久化
├── config/                             # 配置 (JWT, LangChain4j, CORS)
├── controller/                         # REST API
├── dto/                                # 数据传输对象
├── entity/                             # 数据库实体
└── mapper/                             # MyBatis-Plus Mapper

vue-project/src/
├── views/                              # 页面
│   ├── HomeView.vue                    # 首页
│   ├── LoginView.vue                   # 登录注册
│   ├── XingceView.vue                  # 行测模块选择
│   ├── XingceModuleView.vue            # 行测答题
│   ├── ShenlunView.vue                 # 申论写作
│   ├── ShenlunWriteView.vue            # 申论作答
│   ├── PanduanView.vue                 # 判断推理
│   ├── PracticeView.vue                # 练习模式
│   └── BankView.vue                    # 题库管理
└── components/                         # 组件
    ├── ChatInputBox.vue                # 对话输入框
    ├── NavBar.vue                      # 导航栏
    ├── PanduanChat.vue                 # 判断推理对话
    └── QuestionOption.vue              # 题目选项 (支持图片渲染)
```

## 功能模块

| 模块 | 功能 | AI 能力 |
|------|------|---------|
| 行测练习 | 分模块刷题、答题计时 | ExamAiTutor 三维度自动诊断 + SmartStrategy 自适应出题 |
| 判断推理 | 图形/定义/类比/逻辑推理 | PanduanTutor 四模式实时辅导 + 联网搜索解题技巧 |
| 申论写作 | 在线写作、素材推荐 | ShenlunAiTutor 三维评分 + 素材智能推荐 |
| 题库管理 | 题目浏览、错题本 | SmartStrategy 个性化出题 + 错题重练 |
| 真题采集 | 自动采集网络真题 | CollectorAgent 全流程自动化 (搜索→抓取→解析→入库) |
| 统计分析 | 正确率、知识点画像 | 基于数据的自适应学习路径 |

## 本地运行

### 环境要求

- Java 17+
- Node.js 18+
- MySQL 8.0+

### 后端

```bash
cd gk-backend/gk-backend
# 配置 src/main/resources/application.yml 中的 MySQL 连接和 AI API Key
mvn spring-boot:run
```

### 前端

```bash
cd vue-project
npm install
npm run dev
```

访问 `http://localhost:5173` 即可。

## License

MIT
