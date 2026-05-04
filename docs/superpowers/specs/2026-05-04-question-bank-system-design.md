# 完整题库体系设计

> **目标：** 模仿粉笔/华图模式，构建带知识点标签、难度系数、AI 联网采集真题、智能推题的完整题库系统。

**架构：** 在现有 LangChain4j + MyBatis-Plus 架构上扩展，新增知识点表、采集 Agent、智能出题策略。

**Tech Stack:** Spring Boot 3.5 + MyBatis-Plus 3.5 + LangChain4j 0.36 + Vue 3

---

## 一、知识点标签体系

### 1.1 数据模型

新增 `knowledge_point` 表，存储三级知识点树：

```
模块（module）→ 大考点（category）→ 细分考点（concept）
```

示例：
```
ZILIAO → 增长率 → 同比增长率
ZILIAO → 增长率 → 环比增长率
ZILIAO → 比重 → 现期比重
YUYU → 主旨概括 → 因果结构
YUYU → 主旨概括 → 转折结构
```

### 1.2 DDL

```sql
CREATE TABLE knowledge_point (
    id INT AUTO_INCREMENT PRIMARY KEY,
    module VARCHAR(20) NOT NULL COMMENT '模块编码',
    category VARCHAR(50) NOT NULL COMMENT '大考点',
    concept VARCHAR(50) NOT NULL COMMENT '细分考点',
    description VARCHAR(200) COMMENT '考点说明',
    sort_order INT DEFAULT 0 COMMENT '排序',
    UNIQUE INDEX uk_concept (module, category, concept)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 1.3 题目关联

`question` 和 `ai_question_pool` 表已有 `category` 和 `concept` 字段，直接复用，无需新增字段。

---

## 二、难度系数

### 2.1 字段

`question` 和 `ai_question_pool` 新增 `difficulty` 字段（INT，1-5，默认 3）。

### 2.2 计算规则

| 正确率 | 难度值 |
|--------|--------|
| >80% | 1（送分） |
| 60-80% | 2（简单） |
| 40-60% | 3（中等） |
| 20-40% | 4（较难） |
| <20% | 5（高难） |

- 新题默认 difficulty = 3
- 被作答 ≥10 次后，根据正确率自动调整
- 通过 Spring Event 异步更新，不阻塞答题

### 2.3 难度匹配

用户能力由 `user_knowledge_profiles.proficiency` 表示：
- proficiency < 40 → 优先出 difficulty 1-2 的题
- proficiency 40-70 → 出 difficulty 2-4 的题
- proficiency > 70 → 优先出 difficulty 3-5 的题

---

## 三、题目来源与扩充

### 3.1 三种来源

| 来源 | 标记 | 初始状态 | 信任度 |
|------|------|----------|--------|
| REAL_EXAM（真题采集） | source='REAL_EXAM' | ACTIVE | 高 |
| AI_EXPAND（AI 扩写） | source='AI_EXPAND' | ACTIVE | 中 |
| AI_GEN（AI 原创） | source='AI_GEN' | PENDING | 低 |

### 3.2 AI 联网采集真题（核心）

新增 `QuestionCollectorAgent`（LangChain4j @AiService）：

```
用户触发 → QuestionCollectorAgent
  → @Tool webSearch(keyword)    联网搜索
  → @Tool fetchPage(url)        抓取页面
  → @Tool saveQuestion(json)    入库
  → AI 解析 → 结构化 → 去重 → 入 ai_question_pool
```

**采集流程：**
1. 搜索关键词如 `2024国考资料分析真题`
2. 抓取公开页面，提取题目/选项/答案/解析
3. 自动打知识点标签
4. questionHash 去重后入库，status = 'PENDING'

**新增文件：**
- `ai/tools/WebSearchTool.java` — @Component，联网搜索
- `ai/tools/PageFetchTool.java` — @Component，页面抓取+解析
- `ai/collector/QuestionCollectorAgent.java` — @AiService，采集 Agent
- `controller/CollectorController.java` — 触发采集的接口

### 3.3 AI 扩写

从已有真题作为"母题"，让 AI 改编生成变体：
- 换数字、换场景、换问法
- 保留考点和难度
- 标记 `source = 'AI_EXPAND'`，`parent_id` 关联母题

### 3.4 种子题扩充

`V3__seed_questions.sql` 从每模块 5 题扩充到 25 题，共 125 题。

---

## 四、智能推题

### 4.1 替代当前随机查询

重写 `XingceServiceImpl.generateQuestions()`，出题优先级：

| 优先级 | 策略 | 说明 |
|--------|------|------|
| 1 | 薄弱考点优先 | proficiency < 40 的考点 |
| 2 | 新题优先 | 用户未答过的题 |
| 3 | 难度匹配 | 根据用户能力选难度 |
| 4 | 错题重练 | 答错过的题间隔重出 |
| 5 | 考点覆盖 | 每次覆盖 2-3 个不同考点 |

### 4.2 SQL 逻辑

```sql
-- 核心查询（简化版）
SELECT q.* FROM question q
LEFT JOIN user_answer_records uar 
  ON q.id = uar.question_id AND uar.user_id = ?
WHERE q.module = ?
  AND (uar.id IS NULL OR uar.is_correct = 0)  -- 未答过 或 答错过
ORDER BY
  CASE WHEN uar.is_correct = 0 THEN 0 ELSE 1 END,  -- 错题优先
  RAND()
LIMIT 5
```

ai_question_pool 同理，两张表合并后按优先级排序取前 5。

---

## 五、去重与质量保障

### 5.1 去重机制

- **精确去重**：questionHash（MD5，已有）
- **考点去重**：同一考点同一天内最多出 2 题
- **已答去重**：排除用户已答对的题

### 5.2 质量校验

AI 生成/采集的题目入库前校验：
- 选项数 = 4
- 答案在选项中
- 标题非空且长度 > 10
- 不与已有题目哈希重复

### 5.3 数据统计

新增接口返回：
- 题库总量、各模块题数
- 各考点覆盖情况（有题/无题）
- 用户已答数、剩余未答数

---

## 六、涉及文件清单

### 新增
| 文件 | 说明 |
|------|------|
| `db/migration/V5__add_question_bank_fields.sql` | difficulty 字段 + knowledge_point 表 |
| `entity/KnowledgePoint.java` | 知识点实体 |
| `mapper/KnowledgePointMapper.java` | 知识点 Mapper |
| `ai/tools/WebSearchTool.java` | 联网搜索 Tool |
| `ai/tools/PageFetchTool.java` | 页面抓取 Tool |
| `ai/collector/QuestionCollectorAgent.java` | 采集 Agent |
| `controller/CollectorController.java` | 采集触发接口 |
| `event/DifficultyUpdateEvent.java` | 难度更新事件 |
| `listener/DifficultyUpdateListener.java` | 难度更新监听器 |

### 修改
| 文件 | 说明 |
|------|------|
| `V3__seed_questions.sql` | 扩充种子题到每模块 25 题 |
| `entity/Question.java` | 加 difficulty 字段 |
| `entity/AiQuestion.java` | 加 difficulty + parentId 字段 |
| `service/impl/XingceServiceImpl.java` | 重写 generateQuestions() 智能推题 |
| `listener/QuestionGenerateListener.java` | 按知识点定向生成 + 难度校验 |
| `ai/strategy/SmartQuestionStrategy.java` | 加入知识点和难度策略 |
| `pom.xml` | 加 Jsoup 依赖（页面解析） |
| `vue-project/src/views/XingceModuleView.vue` | 显示题目来源标签 |
