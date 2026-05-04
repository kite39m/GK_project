# AI 智能出题架构升级设计文档

## 概述

将现有的"盲目生成"AI 出题逻辑升级为"画像驱动精准推题 + 异步演进"架构。核心改进：用户画像关联、缓存优先+真题兜底、多样性系数约束、陷阱解析预存模板（0 延迟反馈）。

### 核心目标

- AI 生成题目与用户知识点画像关联（薄弱点强化、强项巩固）
- 题目生成异步化，用户请求秒回
- 真题兜底：AI 题库不足时从正式题库补充
- 陷阱解析预存模板，答题反馈 0 延迟
- 题目去重（questionHash）
- 多样性约束（表述形式、陷阱类型差异化）

### 技术栈

| 组件 | 技术 |
|------|------|
| AI 框架 | LangChain4j 0.36.2 (@AiService) |
| 异步机制 | Spring Event (@EventListener + @Async) |
| 后端 | Spring Boot 3.5.13 + MyBatis-Plus 3.5.5 |
| 数据库 | MySQL (utf8mb4) |

---

## 第 1 节：数据库设计

### ai_question_pool 表

```sql
CREATE TABLE ai_question_pool (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    module VARCHAR(20) NOT NULL COMMENT '模块: CHANGSHI/YUYU/ZILIAO/TUILI/SHULIANG',
    category VARCHAR(50) COMMENT '细分考点',
    title TEXT NOT NULL COMMENT '题目正文',
    optionsJson VARCHAR(1000) NOT NULL COMMENT '选项JSON（含trapOption标记）',
    answer VARCHAR(10) NOT NULL COMMENT '正确答案',
    analysis TEXT COMMENT '常规解析',
    concept VARCHAR(50) COMMENT '知识点概念',
    trapOption VARCHAR(10) COMMENT '陷阱选项标识(如 A/B/C/D)',
    trapAnalysisTemplate TEXT COMMENT '预存陷阱解析模板（用于0延迟反馈）',
    questionHash VARCHAR(64) COMMENT '题目内容哈希(MD5)，用于去重',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/ACTIVE/REJECTED',
    sourceType VARCHAR(20) DEFAULT 'AI_GEN' COMMENT '来源: AI_GEN',
    checkLog VARCHAR(500) COMMENT '校验日志',
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE INDEX uk_hash (questionHash),
    INDEX idx_module_status (module, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI题目缓冲池';
```

### Entity 类

```java
@TableName("ai_question_pool")
@Data
public class AiQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String module;
    private String category;
    private String title;
    private String optionsJson;
    private String answer;
    private String analysis;
    private String concept;
    private String trapOption;
    private String trapAnalysisTemplate;
    private String questionHash;
    private String status;
    private String sourceType;
    private String checkLog;
    private Date createdAt;
}
```

### Mapper

```java
@Mapper
public interface AiQuestionMapper extends BaseMapper<AiQuestion> {
}
```

---

## 第 2 节：SmartQuestionStrategy（画像→Prompt 转化）

### 职责

将 `List<UserKnowledgeProfile>` 转化为 LangChain4j 能理解的自然语言 Prompt 片段。

### 策略逻辑

```java
@Component
public class SmartQuestionStrategy {

    public String buildProfilePrompt(List<UserKnowledgeProfile> profiles) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户知识点画像如下：\n");

        for (UserKnowledgeProfile p : profiles) {
            String level;
            String instruction;
            if (p.getProficiency() < 50) {
                level = "薄弱";
                instruction = "生成高难度、陷阱明显的题目，强化该考点";
            } else if (p.getProficiency() < 80) {
                level = "中等";
                instruction = "生成巩固型题目，巩固基础";
            } else {
                level = "熟练";
                instruction = "生成进阶型题目，挑战更高难度";
            }
            sb.append(String.format("- %s（熟练度%d%%，%s）：%s\n",
                    p.getConcept(), p.getProficiency(), level, instruction));
        }

        // 多样性约束
        sb.append("\n多样性要求：\n");
        sb.append("1. 每道题的表述形式必须不同（计算题/概念题/情景题/图表题）\n");
        sb.append("2. 干扰项设计要多样化（偷换概念/单位陷阱/以偏概全/因果倒置）\n");
        sb.append("3. 同一考点的连续题目不能使用相同的陷阱类型\n");

        return sb.toString();
    }
}
```

---

## 第 3 节：缓存优先 + 真题兜底 + 异步生成

### 流程设计

```
用户请求出题(module=CHANGSHI)
    ↓
① 读取用户画像 (user_knowledge_profiles)
    ↓
② SmartQuestionStrategy 生成关键词
    ↓
③ 检索 ai_question_pool (status=ACTIVE, 匹配 concept)
    ↓
④ 找到 ≥5 题？ → 立即返回
    ↓
⑤ 不足 5 题 → 从 question 正式题库按 concept 补充至 5 题
    ↓
⑥ 合并返回给前端（秒回）
    ↓
⑦ 发布 QuestionGenerateEvent (Spring Event)
    ↓
⑧ @EventListener 异步处理：
   - 调用 QuestionGeneratorTutor 生成 5 题
   - 规则校验（选项数=4、答案有效、内容非空）
   - 计算 questionHash 去重
   - 通过 → status=ACTIVE 存入 ai_question_pool
   - 不通过 → status=REJECTED + checkLog
```

### Spring Event 定义

```java
public class QuestionGenerateEvent extends ApplicationEvent {
    private final Integer userId;
    private final String module;
    // getter...
}
```

### 事件监听器

```java
@Component
public class QuestionGenerateListener {

    @Autowired
    private QuestionGeneratorTutor tutor;

    @Autowired
    private SmartQuestionStrategy strategy;

    @Autowired
    private AiQuestionMapper aiQuestionMapper;

    @Autowired
    private UserKnowledgeProfileMapper profileMapper;

    @Async
    @EventListener
    public void handleGenerate(QuestionGenerateEvent event) {
        // 1. 读取画像
        // 2. 调用 tutor 生成题目
        // 3. 校验 + 去重
        // 4. 存入 ai_question_pool
    }
}
```

---

## 第 4 节：QuestionGeneratorTutor（@AiService）

```java
@AiService
public interface QuestionGeneratorTutor {

    @SystemMessage("""
        你是一位国考行测命题专家。
        根据用户的知识点画像，为指定模块生成5道高质量选择题。

        输出要求：
        - 必须输出一个合法的JSON数组
        - 每道题包含：title, optionsJson, answer, analysis, concept, trapOption, trapAnalysisTemplate
        - optionsJson 是一个JSON对象，key为A/B/C/D，value为选项文本
        - trapOption 标记最易选错的干扰项
        - trapAnalysisTemplate 是针对该陷阱的预存解析文本（用于实时反馈）

        陷阱解析模板格式示例：
        "你选择了{userAnswer}，这是常见的{陷阱类型}陷阱。正确答案是{answer}。{针对性解析}"

        多样性要求：
        1. 5道题的表述形式必须不同（计算题/概念题/情景题/图表题/比较题）
        2. 干扰项设计要多样化（偷换概念/单位陷阱/以偏概全/因果倒置/数据混淆）
        3. 同一考点不能使用相同的陷阱类型
    """)
    String generateQuestions(@UserMessage String promptWithProfile);
}
```

---

## 第 5 节：陷阱反馈闭环

### 答题后的陷阱检测逻辑（在 XingceService 中）

```java
// XingceService.saveAnswer() 中增加：
if (Boolean.FALSE.equals(record.getIsCorrect()) && Boolean.TRUE.equals(record.getIsTrapOption())) {
    // 从 question 表或 ai_question_pool 读取 trapAnalysisTemplate
    String trapTemplate = findTrapAnalysisTemplate(record.getQuestionId());

    // 查询该 concept 的历史踩坑次数
    int trapCount = countTrapHistory(record.getUserId(), record.getConcept());

    // 构造个性化反馈
    String feedback = trapTemplate
            .replace("{userAnswer}", record.getUserAnswer())
            .replace("{次数}", String.valueOf(trapCount));

    // 返回给前端（0延迟，不调用AI）
}
```

### 反馈数据结构

```java
@Data
public class TrapFeedback {
    private boolean isTrap;
    private String trapAnalysis;      // 预存模板渲染后的文本
    private int trapCount;            // 该考点历史踩坑次数
    private String concept;           // 考点名称
}
```

---

## 第 6 节：API 接口变更

### XingceController 变更

| 方法 | 路径 | 变更类型 | 说明 |
|------|------|----------|------|
| POST | `/api/xingce/generate` | 新增 | AI 出题（缓存优先+真题兜底+异步补充） |
| POST | `/api/xingce/answer` | 修改 | 返回值增加 TrapFeedback |

### 生成题目请求参数

```json
{
    "userId": 1,
    "module": "CHANGSHI",
    "count": 5
}
```

### 生成题目响应

```json
{
    "questions": [...],
    "fromCache": true,
    "totalInPool": 12
}
```

---

## 第 7 节：前端变更

### 行测主页（XingceView）
- 每个模块卡片增加"AI 出题"按钮
- 点击后调用 `POST /api/xingce/generate`，跳转到答题页

### 答题页（XingceModuleView）
- 增加"再出 5 题"按钮
- 提交答案后，如果触发陷阱，显示警示卡片（红色高亮 + 陷阱解析 + 历史踩坑次数）

---

## 建议实施阶段

### Phase A：数据库 + Entity + Mapper
- DDL 建表
- AiQuestion Entity + AiQuestionMapper

### Phase B：核心服务
- SmartQuestionStrategy
- QuestionGeneratorTutor (@AiService)
- Spring Event + 异步监听器

### Phase C：Service 层重构
- XingceService 出题逻辑（缓存优先+真题兜底）
- XingceService 答题逻辑（陷阱检测+预存模板反馈）
- XingceController 新增/修改 API

### Phase D：前端
- 行测主页 AI 出题按钮
- 答题页陷阱反馈展示
