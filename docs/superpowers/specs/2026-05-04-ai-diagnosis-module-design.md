# 公考AI智能诊断模块设计文档

## 概述

为 GK-Diagnosis-Agent 系统新增两大模块：**行测**（含 5 个子模块）和**申论**，并引入 LangChain4j 框架实现基于 Agentic 架构的 AI 诊断功能。

### 核心目标

- 行测 5 个子模块：常识、言语理解、资料分析、推理判断、数量关系
- 申论模块：作文评分 + 素材推荐
- LangChain4j @Tool/@AiService 实现 AI 主动查询数据库的 Agentic 诊断
- 艾宾浩斯记忆衰减机制支撑知识点画像

### 技术选型

| 组件 | 技术 |
|------|------|
| AI 框架 | LangChain4j 0.36.2 (spring-boot-starter + open-ai-spring-boot-starter) |
| AI 模型 | 小米 MiMo-V2.5-Pro（通过 OpenAI 兼容协议对接） |
| 后端 | Spring Boot 3.5.13 + MyBatis-Plus 3.5.5 |
| 前端 | Vue 3 + Element Plus + Vue Router |
| 数据库 | MySQL (utf8mb4) |

### 与旧模块的关系

现有的速算诊断模块（SpeedCalcController + AiPracticeService）保持不动，与新模块并行共存。现有的 ChatController（SSE 流式聊天）暂时保留，后续可渐进迁移到 LangChain4j。

---

## 第 1 节：数据库设计

### 表1：user_answer_records（答题记录明细表）

```sql
CREATE TABLE user_answer_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id INT NOT NULL COMMENT '用户ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    module VARCHAR(20) NOT NULL COMMENT '模块: CHANGSHI/YUYU/ZILIAO/TUILI/SHULIANG/SHENLUN',
    concept VARCHAR(50) COMMENT '细分考点(如: 基期量计算/主旨概括)',
    is_correct TINYINT(1) NOT NULL COMMENT '是否正确 0:错 1:对',
    user_answer VARCHAR(500) COMMENT '用户提交的答案',
    time_cost_sec INT COMMENT '答题耗时(秒)',
    is_trap_option TINYINT(1) DEFAULT 0 COMMENT '是否选中高频陷阱项 0:否 1:是',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '答题时间',
    INDEX idx_user_module (user_id, module),
    INDEX idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户答题记录明细表';
```

**字段说明**：
- `module`：枚举字符串，区分行测 5 个子模块和申论
- `concept`：细分考点，用于知识点画像的粒度
- `is_trap_option`：标记用户是否选中了高频干扰项，用于区分"粗心陷阱"类错因
- `time_cost_sec`：答题耗时，用于识别"做题超时"类问题

### 表2：user_knowledge_profiles（用户知识点画像表）

```sql
CREATE TABLE user_knowledge_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id INT NOT NULL COMMENT '用户ID',
    module VARCHAR(20) NOT NULL COMMENT '模块编码',
    concept VARCHAR(50) NOT NULL COMMENT '细分考点',
    proficiency INT DEFAULT 50 COMMENT '熟练度(0-100)',
    total_attempts INT DEFAULT 0 COMMENT '总练习次数',
    correct_count INT DEFAULT 0 COMMENT '正确次数',
    last_practice_time DATETIME COMMENT '上次练习时间',
    next_review_time DATETIME COMMENT '下次复习时间(艾宾浩斯)',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_user_concept (user_id, module, concept)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户知识点动态画像表';
```

**艾宾浩斯衰减机制**：
- `proficiency`：每次答题后根据对错更新，正确 +10（上限100），错误 -15（下限0）
- `last_practice_time`：记录上次练习时间
- `next_review_time`：根据艾宾浩斯曲线计算下次最佳复习时间（1天、2天、4天、7天、15天、30天）
- 知识点画像服务在每次答题后自动更新这两个字段

### 题库说明

现有的 `question` 表将扩展以支持新模块：
- 新增 `module` 字段（VARCHAR(20)），值与 `user_answer_records.module` 一致
- `category` 字段用于更细粒度的分类（如"主旨概括"、"细节判断"等）
- `optionsJson` 中可包含 `trapOption` 字段标记高频干扰项，用于 `is_trap_option` 的判定逻辑
- 当用户提交答案时，后端对比 `optionsJson.trapOption` 自动设置 `is_trap_option`

### 表3：shenlun_essays（申论作文提交表）

```sql
CREATE TABLE shenlun_essays (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id INT NOT NULL COMMENT '用户ID',
    topic VARCHAR(200) NOT NULL COMMENT '作文题目',
    content TEXT NOT NULL COMMENT '用户提交的作文内容',
    ai_score INT COMMENT 'AI综合评分(0-100)',
    ai_structure_score INT COMMENT '结构评分',
    ai_argument_score INT COMMENT '论点评分',
    ai_language_score INT COMMENT '语言评分',
    ai_feedback TEXT COMMENT 'AI诊断反馈(JSON)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申论作文提交与评分表';
```

### Entity 类设计

所有 Entity 使用 MyBatis-Plus 注解 + Lombok @Data（项目已引入 Lombok 依赖）。

```java
@TableName("user_answer_records")
@Data
public class UserAnswerRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    private Long questionId;
    private String module;
    private String concept;
    private Boolean isCorrect;
    private String userAnswer;
    private Integer timeCostSec;
    private Boolean isTrapOption;
    private Date createdAt;
}

@TableName("user_knowledge_profiles")
@Data
public class UserKnowledgeProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    private String module;
    private String concept;
    private Integer proficiency;
    private Integer totalAttempts;
    private Integer correctCount;
    private Date lastPracticeTime;
    private Date nextReviewTime;
    private Date updatedAt;
}

@TableName("shenlun_essays")
@Data
public class ShenlunEssay {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    private String topic;
    private String content;
    private Integer aiScore;
    private Integer aiStructureScore;
    private Integer aiArgumentScore;
    private Integer aiLanguageScore;
    private String aiFeedback;
    private Date createdAt;
}
```

### 现有 question 表扩展

```sql
ALTER TABLE question ADD COLUMN module VARCHAR(20) DEFAULT NULL COMMENT '模块编码: CHANGSHI/YUYU/ZILIAO/TUILI/SHULIANG';
ALTER TABLE question ADD INDEX idx_module (module);
```

---

## 第 2 节：LangChain4j 集成

### Maven 依赖

```xml
<!-- LangChain4j Core -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-spring-boot-starter</artifactId>
    <version>0.36.2</version>
</dependency>

<!-- OpenAI 兼容模块（对接 MiMo API） -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai-spring-boot-starter</artifactId>
    <version>0.36.2</version>
</dependency>
```

### application.yml 配置

```yaml
langchain4j:
  open-ai:
    chat-model:
      base-url: ${ai.base-url}
      api-key: ${ai.api-key}
      model-name: ${ai.model}
      temperature: 0.7
      log-requests: true
      log-responses: true
```

复用现有的 `ai.base-url`、`ai.api-key`、`ai.model` 配置项，LangChain4j 通过 OpenAI 兼容协议直接对接 MiMo API。

---

## 第 3 节：模块架构

### 后端包结构

```
com.zwy.gk_backend/
├── controller/
│   ├── XingceController.java          # 行测统一入口
│   └── ShenlunController.java         # 申论入口
├── entity/
│   ├── UserAnswerRecord.java
│   ├── UserKnowledgeProfile.java
│   └── ShenlunEssay.java
├── mapper/
│   ├── UserAnswerRecordMapper.java
│   ├── UserKnowledgeProfileMapper.java
│   └── ShenlunEssayMapper.java
├── service/
│   ├── XingceService.java             # 行测业务逻辑
│   ├── ShenlunService.java            # 申论业务逻辑
│   └── KnowledgeProfileService.java   # 知识点画像+艾宾浩斯
├── ai/
│   ├── tools/
│   │   └── ExamDataTools.java         # @Tool 工具类
│   └── tutor/
│       ├── ExamAiTutor.java           # @AiService 行测诊断
│       └── ShenlunAiTutor.java        # @AiService 申论评分+素材
└── dto/
    ├── DiagnosisReport.java           # 行测诊断报告
    └── ShenlunFeedback.java           # 申论评分反馈
```

### 前端路由

| 路径 | 页面 | 说明 |
|------|------|------|
| `/xingce` | 行测主页 | 5 个子模块卡片入口，显示正确率、练习次数 |
| `/xingce/:module` | 子模块页 | 题目列表 + 答题界面 + 计时器 |
| `/shenlun` | 申论主页 | 作文历史 + 入口 |
| `/shenlun/write` | 申论写作 | 富文本编辑器 + 提交 |
| `/shenlun/diagnosis` | 诊断报告 | AI 评分结果展示 |

### Agentic 诊断流程

```
用户完成答题
    ↓
后端计算"今日简报"（各模块正确率统计）
    ↓
调用 ExamAiTutor.chat(简报文本)
    ↓
AI 分析简报 → 发现某模块正确率 < 70%
    ↓
AI 自动调用 @Tool getRecentErrorDetails(module, limit)
    ↓
工具查询 DB 返回错题明细 JSON
    ↓
AI 结合明细输出结构化诊断报告
    ↓
前端展示诊断报告（错因分类：知识盲区/粗心陷阱/做题超时）
```

---

## 第 4 节：AI 诊断核心设计

### ExamDataTools.java（@Tool 工具类）

```java
@Component
public class ExamDataTools {

    @Autowired
    private UserAnswerRecordMapper answerRecordMapper;

    @Autowired
    private UserKnowledgeProfileMapper profileMapper;

    @Tool("当发现某模块正确率低于70%时，调用此方法获取该模块最近的错题明细。" +
          "返回错题的考点、用户选择的答案、正确答案、是否掉入陷阱选项。" +
          "moduleName 可选值: CHANGSHI, YUYU, ZILIAO, TUILI, SHULIANG")
    public String getRecentErrorDetails(String moduleName, int limit) {
        // 查询 user_answer_records 表
        // WHERE user_id = ? AND module = ? AND is_correct = 0
        // ORDER BY created_at DESC LIMIT ?
        // 返回 JSON 格式的错题明细
    }

    @Tool("获取用户各模块的熟练度画像，返回每个考点的熟练度百分比和上次练习时间")
    public String getKnowledgeProfile(int userId) {
        // 查询 user_knowledge_profiles 表
        // 返回 JSON 格式的画像数据
    }
}
```

### ExamAiTutor.java（@AiService 行测诊断）

```java
@AiService
public interface ExamAiTutor {

    @SystemMessage("""
        你是一位拥有十年教龄的国考行测金牌辅导专家。
        你将收到一份"今日答题简报"，包含用户各模块的正确率数据。

        你的诊断流程必须严格遵循以下步骤：
        1. 分析简报，找出正确率低于70%的薄弱模块。
        2. 对于每个薄弱模块，你必须调用 getRecentErrorDetails 工具获取错题明细。
        3. 结合错题明细，判断错因属于以下哪类：
           - 知识盲区：完全不会，缺乏相关知识点
           - 粗心陷阱：会做但选了高频干扰项
           - 做题超时：会做但耗时过长，影响整体节奏
        4. 输出结构化诊断报告，包含：薄弱模块、错因分类、具体建议。

        如果所有模块正确率均≥70%，直接给出鼓励和保持建议，无需调用工具。
    """)
    String diagnose(@UserMessage String dailyBriefing);
}
```

### ShenlunAiTutor.java（@AiService 申论评分）

```java
@AiService
public interface ShenlunAiTutor {

    @SystemMessage("""
        你是一位资深的申论阅卷专家。
        用户会提交一篇申论文章，你需要从以下维度评分（每项0-100分）：
        - 结构分：文章结构是否清晰（总分总、层次分明）
        - 论点分：论点是否准确、论据是否充分
        - 语言分：语言表达是否规范、流畅

        输出 JSON 格式：{"structureScore": 80, "argumentScore": 75, "languageScore": 85, "totalScore": 80, "feedback": "..."}
    """)
    String scoreEssay(@UserMessage String essayContent);

    @SystemMessage("""
        你是一位公考申论素材库管理员。
        根据用户指定的主题，推荐3-5个写作素材，每个素材包含：标题、核心内容、适用场景。
        输出 JSON 数组格式。
    """)
    String recommendMaterials(@UserMessage String topic);
}
```

---

## 第 5 节：API 接口设计

### 行测 API（XingceController）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/xingce/answer` | 提交答题记录（含 questionId, module, isCorrect, timeCostSec 等） |
| GET | `/api/xingce/modules` | 获取 5 个子模块列表及各模块统计（正确率、练习次数） |
| GET | `/api/xingce/questions/{module}` | 获取某模块的题目列表（分页） |
| POST | `/api/xingce/diagnose` | 触发 AI 诊断（传入 userId，后端自动计算简报并调用 AI） |
| GET | `/api/xingce/profile/{userId}` | 获取用户知识点画像 |

### 申论 API（ShenlunController）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/shenlun/essay/submit` | 提交作文，返回 AI 评分 |
| GET | `/api/shenlun/essay/history` | 获取用户作文历史（分页） |
| POST | `/api/shenlun/material/recommend` | 获取素材推荐（传入主题） |

### 错误处理策略

| 场景 | 处理方式 |
|------|----------|
| AI 调用失败 | 返回 503 + "AI 服务暂时不可用，请稍后重试" |
| @Tool 执行异常 | 捕获异常返回空结果，AI 在报告中说明"暂无详细数据" |
| MiMo API 超时（30s） | 降级为纯简报展示，不含深度诊断 |
| Function Calling 不支持 | 降级为手动解析方案（简化版 Agentic） |

### 知识点画像服务（KnowledgeProfileService）

每次答题后自动触发：
1. 查询或创建 `user_knowledge_profiles` 记录
2. 根据答题结果更新 `proficiency`（正确 +10，错误 -15，范围 0-100）
3. 更新 `total_attempts` 和 `correct_count`
4. 更新 `last_practice_time` 为当前时间
5. 根据艾宾浩斯曲线计算 `next_review_time`
   - proficiency < 30：1 天后复习
   - proficiency 30-60：3 天后复习
   - proficiency 60-80：7 天后复习
   - proficiency > 80：15 天后复习

---

## 前端页面概要

### 行测主页（/xingce）
- 5 个子模块卡片（常识、言语理解、资料分析、推理判断、数量关系）
- 每个卡片显示：模块图标、正确率环形图、练习次数、薄弱考点标签
- 点击卡片进入子模块答题页

### 答题页（/xingce/:module）
- 题目展示区 + 选项列表
- 底部计时器（自动开始）
- 提交后即时显示对错 + 解析
- 支持"下一题"连续作答

### 诊断报告页
- AI 生成的结构化报告
- 用 Element Plus Card 展示每个薄弱模块
- 用 Tag 展示错因分类（知识盲区/粗心陷阱/做题超时）
- 用 Progress 展示各模块正确率

### 申论写作页（/shenlun/write）
- 富文本编辑器（可用 wang-editor 或 quill）
- 题目输入框
- 提交按钮 → 调用 AI 评分 → 展示评分结果
- 评分结果：三个维度的雷达图 + 综合分数 + 文字反馈

### 素材推荐页
- 主题搜索框
- 素材卡片列表（标题 + 核心内容 + 适用场景）

---

## 建议实施阶段

本项目范围较大，建议按以下阶段实施：

### Phase 1：基础设施（数据库 + LangChain4j 集成）
- 执行 DDL 建表 + ALTER TABLE
- 创建 Entity、Mapper
- 引入 LangChain4j 依赖并配置
- 验证 LangChain4j 能正常调用 MiMo API

### Phase 2：行测模块（后端）
- 实现 XingceService（答题记录、模块统计）
- 实现 KnowledgeProfileService（艾宾浩斯画像）
- 实现 ExamDataTools（@Tool 工具类）
- 实现 ExamAiTutor（@AiService 诊断接口）
- 实现 XingceController（全部 API）

### Phase 3：申论模块（后端）
- 实现 ShenlunService（作文保存、素材管理）
- 实现 ShenlunAiTutor（@AiService 评分+素材）
- 实现 ShenlunController（全部 API）

### Phase 4：前端
- 行测主页 + 5 个子模块答题页
- 行测诊断报告页
- 申论主页 + 写作页 + 素材推荐页

### Phase 5：集成测试与优化
- 端到端测试 Agentic 诊断流程
- 验证 Function Calling 是否正常工作
- 性能优化（AI 调用超时处理、降级策略）
