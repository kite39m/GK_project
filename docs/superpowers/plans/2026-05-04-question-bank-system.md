# 完整题库体系 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建带知识点标签、难度系数、AI 联网采集真题、智能推题的完整题库系统。

**Architecture:** 在现有 LangChain4j + MyBatis-Plus 架构上扩展，新增知识点表、采集 Agent、智能出题策略。

**Tech Stack:** Spring Boot 3.5 + MyBatis-Plus 3.5 + LangChain4j 0.36 + Jsoup + Vue 3

---

## File Structure

### New Files
| File | Responsibility |
|------|----------------|
| `db/migration/V5__add_question_bank_fields.sql` | difficulty 字段 + knowledge_point 表 + 种子数据 |
| `entity/KnowledgePoint.java` | 知识点实体 |
| `mapper/KnowledgePointMapper.java` | 知识点 Mapper |
| `ai/tools/WebSearchTool.java` | 联网搜索 @Tool |
| `ai/tools/PageFetchTool.java` | 页面抓取 @Tool |
| `ai/collector/QuestionCollectorAgent.java` | 采集 Agent |
| `controller/CollectorController.java` | 采集触发接口 |
| `event/DifficultyUpdateEvent.java` | 难度更新事件 |
| `listener/DifficultyUpdateListener.java` | 难度更新监听器 |

### Modified Files
| File | Changes |
|------|---------|
| `entity/Question.java` | 加 difficulty 字段 |
| `entity/AiQuestion.java` | 加 difficulty + parentId 字段 |
| `service/impl/XingceServiceImpl.java` | 重写 generateQuestions() 智能推题 |
| `listener/QuestionGenerateListener.java` | 按知识点定向生成 + 难度校验 |
| `ai/strategy/SmartQuestionStrategy.java` | 加入知识点和难度策略 |
| `pom.xml` | 加 Jsoup 依赖 |
| `vue-project/src/views/XingceModuleView.vue` | 显示题目来源和难度标签 |

---

### Task 1: DDL - 数据库扩展

**Files:**
- Create: `gk-backend/gk-backend/src/main/resources/db/migration/V5__add_question_bank_fields.sql`

- [ ] **Step 1: 创建 V5 迁移脚本**

创建 `gk-backend/gk-backend/src/main/resources/db/migration/V5__add_question_bank_fields.sql`：

```sql
-- ============================================================
-- V5: 题库体系扩展 - 难度系数 + 知识点标签
-- ============================================================

-- 1. question 表添加难度字段
ALTER TABLE question ADD COLUMN difficulty INT DEFAULT 3 COMMENT '难度1-5';
ALTER TABLE question ADD COLUMN source VARCHAR(20) DEFAULT 'SEED' COMMENT '来源: SEED/REAL_EXAM/AI_EXPAND/AI_GEN';

-- 2. ai_question_pool 表添加难度和关联字段
ALTER TABLE ai_question_pool ADD COLUMN difficulty INT DEFAULT 3 COMMENT '难度1-5';
ALTER TABLE ai_question_pool ADD COLUMN parent_id BIGINT DEFAULT NULL COMMENT '母题ID(AI扩写用)';

-- 3. 知识点标签表
CREATE TABLE IF NOT EXISTS knowledge_point (
    id INT AUTO_INCREMENT PRIMARY KEY,
    module VARCHAR(20) NOT NULL COMMENT '模块编码',
    category VARCHAR(50) NOT NULL COMMENT '大考点',
    concept VARCHAR(50) NOT NULL COMMENT '细分考点',
    description VARCHAR(200) COMMENT '考点说明',
    sort_order INT DEFAULT 0 COMMENT '排序',
    UNIQUE INDEX uk_concept (module, category, concept)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识点标签表';

-- 4. 种子知识点数据
INSERT IGNORE INTO knowledge_point (module, category, concept, description, sort_order) VALUES
-- 常识
('CHANGSHI', '政治', '政治制度', '根本政治制度、基本政治制度', 1),
('CHANGSHI', '经济', '宏观经济', 'GDP、CPI、通货膨胀等', 2),
('CHANGSHI', '法律', '宪法', '宪法基本条款', 3),
('CHANGSHI', '科技', '科技常识', '科技成就、信息技术', 4),
('CHANGSHI', '历史', '中国近现代史', '重要历史事件', 5),
-- 言语理解
('YUYU', '主旨概括', '因果结构', '因...因...因此...结构', 1),
('YUYU', '主旨概括', '转折结构', '虽然...但是...结构', 2),
('YUYU', '主旨概括', '并列结构', '一方面...另一方面...结构', 3),
('YUYU', '细节判断', '细节查找', '根据材料查找对应信息', 4),
('YUYU', '语句排序', '逻辑排序', '按逻辑顺序排列语句', 5),
('YUYU', '逻辑填空', '词语辨析', '近义词、成语辨析', 6),
-- 资料分析
('ZILIAO', '增长率', '同比增长率', '与去年同期相比的增长率', 1),
('ZILIAO', '增长率', '环比增长率', '与上一期相比的增长率', 2),
('ZILIAO', '比重', '现期比重', '当前时期部分占整体的比例', 3),
('ZILIAO', '比重', '基期比重', '上一时期部分占整体的比例', 4),
('ZILIAO', '基期量', '基期量计算', '已知现期量和增长率求基期', 5),
('ZILIAO', '倍数', '倍数关系', 'A是B的几倍', 6),
('ZILIAO', '增长量', '增长量计算', '已知现期量和增长率求增长量', 7),
-- 推理判断
('TUILI', '逻辑判断', '充分必要条件', '如果...那么...逆否命题', 1),
('TUILI', '逻辑判断', '三段论', '大前提+小前提→结论', 2),
('TUILI', '定义判断', '概念匹配', '根据定义判断选项是否符合', 3),
('TUILI', '类比推理', '语义关系', '词语之间的逻辑关系', 4),
('TUILI', '图形推理', '数列规律', '数字序列的规律', 5),
-- 数量关系
('SHULIANG', '工程问题', '合作效率', '多人合作完成工程', 1),
('SHULIANG', '行程问题', '相遇问题', '相向而行相遇时间', 2),
('SHULIANG', '排列组合', '排列', '有序选取', 3),
('SHULIANG', '利润问题', '利润率', '进价、售价、利润率计算', 4),
('SHULIANG', '容斥原理', '两集合容斥', 'A∪B = A + B - A∩B', 5);
```

- [ ] **Step 2: 执行迁移脚本**

```bash
"/c/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe" -u root -p123456 --default-character-set=utf8mb4 gk_db < "D:/GK/App/gk-backend/gk-backend/src/main/resources/db/migration/V5__add_question_bank_fields.sql"
```

Expected: 无报错，knowledge_point 表有 25 条记录。

- [ ] **Step 3: 验证**

```bash
"/c/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe" -u root -p123456 --default-character-set=utf8mb4 gk_db -e "DESCRIBE question; DESCRIBE ai_question_pool; SELECT COUNT(*) FROM knowledge_point;"
```

Expected: question 表有 difficulty 和 source 字段，ai_question_pool 有 difficulty 和 parent_id 字段，knowledge_point 有 25 条。

- [ ] **Step 4: Commit**

```bash
git add gk-backend/gk-backend/src/main/resources/db/migration/V5__add_question_bank_fields.sql
git commit -m "feat(db): 添加难度系数字段和知识点标签表"
```

---

### Task 2: Entity 更新

**Files:**
- Modify: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/entity/Question.java`
- Modify: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/entity/AiQuestion.java`
- Create: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/entity/KnowledgePoint.java`
- Create: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/mapper/KnowledgePointMapper.java`

- [ ] **Step 1: 修改 Question.java 添加 difficulty 和 source 字段**

在 `Question.java` 的 `private String module;` 后添加：

```java
private Integer difficulty;
private String source;
```

在文件末尾 `}` 前添加 getter/setter：

```java
public Integer getDifficulty() { return difficulty; }
public void setDifficulty(Integer difficulty) { this.difficulty = difficulty; }

public String getSource() { return source; }
public void setSource(String source) { this.source = source; }
```

- [ ] **Step 2: 修改 AiQuestion.java 添加 difficulty 和 parentId 字段**

在 `AiQuestion.java` 的 `private String checkLog;` 后添加：

```java
private Integer difficulty;
private Long parentId;
```

（@Data 注解会自动生成 getter/setter）

- [ ] **Step 3: 创建 KnowledgePoint.java 实体**

创建 `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/entity/KnowledgePoint.java`：

```java
package com.zwy.gk_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("knowledge_point")
@Data
public class KnowledgePoint {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String module;
    private String category;
    private String concept;
    private String description;
    private Integer sortOrder;
}
```

- [ ] **Step 4: 创建 KnowledgePointMapper.java**

创建 `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/mapper/KnowledgePointMapper.java`：

```java
package com.zwy.gk_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zwy.gk_backend.entity.KnowledgePoint;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgePointMapper extends BaseMapper<KnowledgePoint> {
}
```

- [ ] **Step 5: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/entity/Question.java \
       gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/entity/AiQuestion.java \
       gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/entity/KnowledgePoint.java \
       gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/mapper/KnowledgePointMapper.java
git commit -m "feat: 更新实体类添加难度和知识点字段"
```

---

### Task 3: 智能推题 - 随机不重复 + 薄弱优先

**Files:**
- Modify: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/service/impl/XingceServiceImpl.java`

- [ ] **Step 1: 重写 generateQuestions() 方法**

将 `XingceServiceImpl.java` 中的 `generateQuestions` 方法替换为以下实现：

```java
@Override
public GenerateResultDTO generateQuestions(Integer userId, String module, int count) {
    GenerateResultDTO result = new GenerateResultDTO();
    List<Question> merged = new ArrayList<>();
    boolean fromCache = true;

    // 1. 查询用户已答对的题目ID（排除用）
    QueryWrapper<UserAnswerRecord> answeredQuery = new QueryWrapper<>();
    answeredQuery.eq("user_id", userId)
                 .eq("module", module)
                 .eq("is_correct", true)
                 .select("question_id");
    List<Object> answeredIds = answerRecordMapper.selectObjs(answeredQuery);
    Set<Integer> correctIds = new HashSet<>();
    for (Object id : answeredIds) {
        correctIds.add(((Number) id).intValue());
    }

    // 2. 查询用户答错的题目ID（错题优先用）
    QueryWrapper<UserAnswerRecord> wrongQuery = new QueryWrapper<>();
    wrongQuery.eq("user_id", userId)
              .eq("module", module)
              .eq("is_correct", false)
              .select("question_id");
    List<Object> wrongIds = answerRecordMapper.selectObjs(wrongQuery);
    Set<Integer> wrongIdSet = new HashSet<>();
    for (Object id : wrongIds) {
        wrongIdSet.add(((Number) id).intValue());
    }

    // 3. 从 ai_question_pool 检索（排除已答对 + 随机）
    QueryWrapper<AiQuestion> aiQuery = new QueryWrapper<>();
    aiQuery.eq("module", module).eq("status", "ACTIVE");
    if (!correctIds.isEmpty()) {
        aiQuery.notIn("id", correctIds);
    }
    aiQuery.orderByAsc(
        "CASE WHEN id IN (" + (wrongIdSet.isEmpty() ? "0" : wrongIdSet.stream().map(String::valueOf).collect(Collectors.joining(","))) + ") THEN 0 ELSE 1 END"
    );
    aiQuery.last("ORDER BY RAND() LIMIT " + count);
    List<AiQuestion> aiQuestions = aiQuestionMapper.selectList(aiQuery);

    for (AiQuestion aiq : aiQuestions) {
        Question q = new Question();
        q.setId(aiq.getId().intValue());
        q.setModule(aiq.getModule());
        q.setCategory(aiq.getCategory());
        q.setTitle(aiq.getTitle());
        q.setOptionsJson(aiq.getOptionsJson());
        q.setAnswer(aiq.getAnswer());
        q.setAnalysis(aiq.getAnalysis());
        q.setDifficulty(aiq.getDifficulty());
        q.setSource("AI_GEN");
        merged.add(q);
    }

    // 4. 不足则从 question 正式题库补充（排除已答对 + 随机）
    if (merged.size() < count) {
        int remain = count - merged.size();
        Set<Integer> existingIds = merged.stream().map(Question::getId).collect(Collectors.toSet());
        existingIds.addAll(correctIds);

        QueryWrapper<Question> formalQuery = new QueryWrapper<>();
        formalQuery.eq("module", module);
        if (!existingIds.isEmpty()) {
            formalQuery.notIn("id", existingIds);
        }
        formalQuery.last("ORDER BY RAND() LIMIT " + remain);
        List<Question> formal = questionMapper.selectList(formalQuery);
        merged.addAll(formal);
    }

    // 5. 仍然不足 → 错题重练（允许已答对的错题重新出现）
    if (merged.size() < count && !wrongIdSet.isEmpty()) {
        int remain = count - merged.size();
        Set<Integer> existingIds = merged.stream().map(Question::getId).collect(Collectors.toSet());

        QueryWrapper<Question> retryQuery = new QueryWrapper<>();
        retryQuery.eq("module", module).in("id", wrongIdSet);
        if (!existingIds.isEmpty()) {
            retryQuery.notIn("id", existingIds);
        }
        retryQuery.last("ORDER BY RAND() LIMIT " + remain);
        List<Question> retry = questionMapper.selectList(retryQuery);
        merged.addAll(retry);
    }

    if (aiQuestions.isEmpty() && merged.isEmpty()) {
        fromCache = false;
    }

    // 6. 统计池中总量
    QueryWrapper<AiQuestion> countQuery = new QueryWrapper<>();
    countQuery.eq("module", module).eq("status", "ACTIVE");
    int totalInPool = aiQuestionMapper.selectCount(countQuery).intValue();

    result.setQuestions(merged);
    result.setFromCache(fromCache);
    result.setTotalInPool(totalInPool);

    // 7. 发布异步事件（补充题库）
    String moduleName = MODULE_NAMES.getOrDefault(module, module);
    eventPublisher.publishEvent(new QuestionGenerateEvent(this, userId, module, moduleName, count));

    return result;
}
```

注意：需要在文件顶部添加 `import java.util.stream.Collectors;`。

- [ ] **Step 2: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/service/impl/XingceServiceImpl.java
git commit -m "feat: 实现智能推题（随机不重复+薄弱优先+错题重练）"
```

---

### Task 4: 难度系数自动更新

**Files:**
- Create: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/event/DifficultyUpdateEvent.java`
- Create: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/listener/DifficultyUpdateListener.java`
- Modify: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/service/impl/XingceServiceImpl.java`

- [ ] **Step 1: 创建 DifficultyUpdateEvent**

创建 `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/event/DifficultyUpdateEvent.java`：

```java
package com.zwy.gk_backend.event;

import org.springframework.context.ApplicationEvent;

public class DifficultyUpdateEvent extends ApplicationEvent {
    private final Integer questionId;
    private final boolean isCorrect;
    private final String poolType; // "question" or "ai_question_pool"

    public DifficultyUpdateEvent(Object source, Integer questionId, boolean isCorrect, String poolType) {
        super(source);
        this.questionId = questionId;
        this.isCorrect = isCorrect;
        this.poolType = poolType;
    }

    public Integer getQuestionId() { return questionId; }
    public boolean isCorrect() { return isCorrect; }
    public String getPoolType() { return poolType; }
}
```

- [ ] **Step 2: 创建 DifficultyUpdateListener**

创建 `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/listener/DifficultyUpdateListener.java`：

```java
package com.zwy.gk_backend.listener;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zwy.gk_backend.entity.AiQuestion;
import com.zwy.gk_backend.entity.Question;
import com.zwy.gk_backend.entity.UserAnswerRecord;
import com.zwy.gk_backend.event.DifficultyUpdateEvent;
import com.zwy.gk_backend.mapper.AiQuestionMapper;
import com.zwy.gk_backend.mapper.QuestionMapper;
import com.zwy.gk_backend.mapper.UserAnswerRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class DifficultyUpdateListener {

    @Autowired
    private UserAnswerRecordMapper answerRecordMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private AiQuestionMapper aiQuestionMapper;

    @Async
    @EventListener
    public void onDifficultyUpdate(DifficultyUpdateEvent event) {
        Integer questionId = event.getQuestionId();

        // 统计该题的答题数据
        QueryWrapper<UserAnswerRecord> query = new QueryWrapper<>();
        query.eq("question_id", questionId);
        long total = answerRecordMapper.selectCount(query);
        if (total < 10) return; // 不足10次不更新

        query.eq("is_correct", true);
        long correct = answerRecordMapper.selectCount(query);
        double correctRate = (double) correct / total;

        int difficulty;
        if (correctRate > 0.8) difficulty = 1;
        else if (correctRate > 0.6) difficulty = 2;
        else if (correctRate > 0.4) difficulty = 3;
        else if (correctRate > 0.2) difficulty = 4;
        else difficulty = 5;

        if ("ai_question_pool".equals(event.getPoolType())) {
            AiQuestion aiq = aiQuestionMapper.selectById(questionId);
            if (aiq != null) {
                aiq.setDifficulty(difficulty);
                aiQuestionMapper.updateById(aiq);
            }
        } else {
            Question q = questionMapper.selectById(questionId);
            if (q != null) {
                q.setDifficulty(difficulty);
                questionMapper.updateById(q);
            }
        }
    }
}
```

- [ ] **Step 3: 在 saveAnswer() 中发布难度更新事件**

在 `XingceServiceImpl.java` 的 `saveAnswer` 方法中，`answerRecordMapper.insert(record);` 之后添加：

```java
eventPublisher.publishEvent(new DifficultyUpdateEvent(this, record.getQuestionId(), Boolean.TRUE.equals(record.getIsCorrect()), "question"));
```

需要在文件顶部添加 import：
```java
import com.zwy.gk_backend.event.DifficultyUpdateEvent;
```

- [ ] **Step 4: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/event/DifficultyUpdateEvent.java \
       gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/listener/DifficultyUpdateListener.java \
       gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/service/impl/XingceServiceImpl.java
git commit -m "feat: 实现难度系数自动更新机制"
```

---

### Task 5: 知识点定向出题策略

**Files:**
- Modify: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/strategy/SmartQuestionStrategy.java`

- [ ] **Step 1: 重写 SmartQuestionStrategy**

替换 `SmartQuestionStrategy.java` 全部内容：

```java
package com.zwy.gk_backend.ai.strategy;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zwy.gk_backend.entity.KnowledgePoint;
import com.zwy.gk_backend.entity.UserKnowledgeProfile;
import com.zwy.gk_backend.mapper.KnowledgePointMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SmartQuestionStrategy {

    @Autowired
    private KnowledgePointMapper knowledgePointMapper;

    public String buildProfilePrompt(List<UserKnowledgeProfile> profiles) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户知识点画像：\n");

        for (UserKnowledgeProfile p : profiles) {
            String level;
            String instruction;
            if (p.getProficiency() < 40) {
                level = "薄弱";
                instruction = "重点出题，生成基础难度题目巩固";
            } else if (p.getProficiency() < 70) {
                level = "中等";
                instruction = "适当出题，巩固+进阶";
            } else {
                level = "熟练";
                instruction = "少量出题，挑战高难度";
            }
            sb.append(String.format("- %s（熟练度%d%%，%s）：%s\n",
                    p.getConcept(), p.getProficiency(), level, instruction));
        }
        return sb.toString();
    }

    public String buildModulePrompt(String module, String moduleName, int count) {
        // 查询该模块的知识点清单
        QueryWrapper<KnowledgePoint> query = new QueryWrapper<>();
        query.eq("module", module).orderByAsc("sort_order");
        List<KnowledgePoint> points = knowledgePointMapper.selectList(query);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("请为「%s」模块生成%d道高质量行测选择题。\n\n", moduleName, count));
        sb.append("该模块知识点清单（请确保题目覆盖以下考点，不要遗漏）：\n");

        String grouped = points.stream()
                .collect(Collectors.groupingBy(KnowledgePoint::getCategory))
                .entrySet().stream()
                .map(e -> {
                    String concepts = e.getValue().stream()
                            .map(KnowledgePoint::getConcept)
                            .collect(Collectors.joining("、"));
                    return "- " + e.getKey() + "：" + concepts;
                })
                .collect(Collectors.joining("\n"));

        sb.append(grouped);
        sb.append("\n\n每道题必须明确标注所属的 category 和 concept。");
        sb.append("\n多样性要求：每道题表述形式不同（计算/概念/情景/比较），干扰项多样化。");
        sb.append("\n解析简洁，每题不超过50字。");

        return sb.toString();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/strategy/SmartQuestionStrategy.java
git commit -m "feat: 知识点定向出题策略，按考点清单生成题目"
```

---

### Task 6: QuestionGenerateListener 知识点校验

**Files:**
- Modify: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/listener/QuestionGenerateListener.java`

- [ ] **Step 1: 更新 Listener 的存储逻辑**

在 `QuestionGenerateListener.java` 的 `aiq.setSourceType("AI_GEN");` 之后，`aiQuestionMapper.insert(aiq);` 之前添加难度默认值：

```java
aiq.setDifficulty(3);
```

在 `aiq.setConcept(...)` 那行之后，添加知识点校验逻辑（确保 category 和 concept 非空）：

```java
// 如果 AI 没返回 concept，用 category 代替
if (aiq.getConcept() == null || aiq.getConcept().isEmpty()) {
    aiq.setConcept(aiq.getCategory());
}
```

- [ ] **Step 2: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/listener/QuestionGenerateListener.java
git commit -m "feat: AI生成题目添加难度默认值和知识点校验"
```

---

### Task 7: AI 联网采集真题 Agent

**Files:**
- Create: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/tools/WebSearchTool.java`
- Create: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/tools/PageFetchTool.java`
- Create: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/collector/QuestionCollectorAgent.java`
- Create: `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/controller/CollectorController.java`
- Modify: `gk-backend/gk-backend/pom.xml`

- [ ] **Step 1: 添加 Jsoup 依赖**

在 `pom.xml` 的 `<dependencies>` 中添加：

```xml
<!-- Jsoup HTML 解析 -->
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.17.2</version>
</dependency>
```

- [ ] **Step 2: 创建 WebSearchTool**

创建 `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/tools/WebSearchTool.java`：

```java
package com.zwy.gk_backend.ai.tools;

import dev.langchain4j.agent.tool.Tool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class WebSearchTool {

    @Tool("搜索公开的公务员考试真题，返回搜索结果链接列表。输入关键词如 '2024国考资料分析真题'")
    public List<String> searchExamQuestions(String keyword) {
        List<String> results = new ArrayList<>();
        try {
            String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url = "https://www.bing.com/search?q=" + encoded;
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();
            Elements links = doc.select("a[href]");
            links.stream()
                    .map(el -> el.attr("href"))
                    .filter(href -> href.startsWith("http") && !href.contains("bing.com"))
                    .limit(5)
                    .forEach(results::add);
        } catch (Exception e) {
            results.add("搜索失败: " + e.getMessage());
        }
        return results;
    }
}
```

- [ ] **Step 3: 创建 PageFetchTool**

创建 `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/tools/PageFetchTool.java`：

```java
package com.zwy.gk_backend.ai.tools;

import dev.langchain4j.agent.tool.Tool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
public class PageFetchTool {

    @Tool("抓取指定URL的页面内容，返回纯文本。用于提取网页中的考试题目。")
    public String fetchPageContent(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(15000)
                    .get();
            // 移除 script 和 style 标签
            doc.select("script, style, nav, footer, header").remove();
            String text = doc.body().text();
            // 限制长度避免超出 token 限制
            return text.length() > 5000 ? text.substring(0, 5000) : text;
        } catch (Exception e) {
            return "抓取失败: " + e.getMessage();
        }
    }
}
```

- [ ] **Step 4: 创建 QuestionCollectorAgent**

创建 `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/collector/QuestionCollectorAgent.java`：

```java
package com.zwy.gk_backend.ai.collector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwy.gk_backend.ai.tools.PageFetchTool;
import com.zwy.gk_backend.ai.tools.WebSearchTool;
import com.zwy.gk_backend.entity.AiQuestion;
import com.zwy.gk_backend.mapper.AiQuestionMapper;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

@AiService(tools = {"webSearchTool", "pageFetchTool"})
public interface QuestionCollectorAgent {

    @SystemMessage("""
        你是公考真题采集专家。你的任务是从公开网页中提取公务员考试行测真题。

        工作流程：
        1. 用 searchExamQuestions 搜索指定模块的真题
        2. 用 fetchPageContent 抓取搜索结果页面
        3. 从页面内容中提取题目，每道题包含：title, optionsJson, answer, analysis, category, concept
        4. 用 saveCollectedQuestions 批量保存提取到的题目

        提取要求：
        - 只提取有明确答案的题目
        - 选项必须是 A/B/C/D 四个
        - 如果页面没有答案或解析，用你的知识补充
        - 每次最多提取10道题
        - 提取完成后调用 saveCollectedQuestions 保存
    """)
    String collectQuestions(@UserMessage String prompt);

    @Tool("保存采集到的题目到数据库。输入JSON数组格式的题目列表。")
    default String saveCollectedQuestions(String questionsJson) {
        // 这个方法需要注入 mapper，通过 Spring 代理实现
        return "请在实现类中处理";
    }
}
```

注意：`@AiService(tools = {"webSearchTool", "pageFetchTool"})` 使用 Bean 名称字符串。

由于 `@AiService` 接口的 `default` 方法无法直接注入 Spring Bean，需要创建一个实现类。创建 `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/collector/QuestionCollectorAgentImpl.java`：

```java
package com.zwy.gk_backend.ai.collector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwy.gk_backend.entity.AiQuestion;
import com.zwy.gk_backend.mapper.AiQuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

@Component
public class QuestionCollectorAgentHelper {

    @Autowired
    private AiQuestionMapper aiQuestionMapper;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public String saveCollectedQuestions(String questionsJson) {
        try {
            List<Map<String, Object>> items = MAPPER.readValue(questionsJson, new TypeReference<>() {});
            int saved = 0;
            for (Map<String, Object> item : items) {
                String title = (String) item.get("title");
                String optionsJson = MAPPER.writeValueAsString(item.get("optionsJson"));
                String answer = (String) item.get("answer");

                if (title == null || title.isEmpty() || answer == null) continue;

                // 去重
                String hash = md5(title + optionsJson);
                var existQuery = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<AiQuestion>();
                existQuery.eq("question_hash", hash);
                if (aiQuestionMapper.selectCount(existQuery) > 0) continue;

                AiQuestion aiq = new AiQuestion();
                aiq.setModule((String) item.get("module"));
                aiq.setCategory((String) item.get("category"));
                aiq.setTitle(title);
                aiq.setOptionsJson(optionsJson);
                aiq.setAnswer(answer);
                aiq.setAnalysis((String) item.get("analysis"));
                aiq.setConcept((String) item.get("concept"));
                aiq.setQuestionHash(hash);
                aiq.setStatus("PENDING");
                aiq.setSourceType("REAL_EXAM");
                aiq.setDifficulty(3);
                aiq.setCheckLog("采集入库");
                aiQuestionMapper.insert(aiq);
                saved++;
            }
            return "成功保存 " + saved + " 道题目";
        } catch (Exception e) {
            return "保存失败: " + e.getMessage();
        }
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
```

- [ ] **Step 5: 创建 CollectorController**

创建 `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/controller/CollectorController.java`：

```java
package com.zwy.gk_backend.controller;

import com.zwy.gk_backend.ai.collector.QuestionCollectorAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/collector")
@CrossOrigin
public class CollectorController {

    @Autowired
    private QuestionCollectorAgent collectorAgent;

    @PostMapping("/collect")
    public Map<String, String> collect(@RequestBody Map<String, String> body) {
        String module = body.get("module");
        String moduleName = body.getOrDefault("moduleName", module);
        String prompt = String.format("请采集「%s」模块的公务员考试行测真题，至少5道。模块代码：%s", moduleName, module);
        String result = collectorAgent.collectQuestions(prompt);
        return Map.of("result", result);
    }
}
```

需要在 `WebConfig.java` 中放行 `/api/collector/**`。

- [ ] **Step 6: 放行采集接口**

在 `WebConfig.java` 的 `excludePathPatterns` 中添加 `"/api/collector/**"`。

- [ ] **Step 7: Commit**

```bash
git add gk-backend/gk-backend/pom.xml \
       gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/tools/WebSearchTool.java \
       gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/tools/PageFetchTool.java \
       gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/collector/QuestionCollectorAgent.java \
       gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/collector/QuestionCollectorAgentHelper.java \
       gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/controller/CollectorController.java \
       gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/config/WebConfig.java
git commit -m "feat: 实现AI联网采集真题Agent"
```

---

### Task 8: 前端展示题目来源和难度

**Files:**
- Modify: `vue-project/src/views/XingceModuleView.vue`

- [ ] **Step 1: 在题目标题旁显示难度和来源标签**

在 `XingceModuleView.vue` 的 `<div class="question-title">` 中添加难度星星和来源标签：

```html
<div class="question-title">
  <span>{{ currentQuestion.title }}</span>
  <div class="question-meta">
    <span v-if="currentQuestion.difficulty" class="difficulty">
      {{ '★'.repeat(currentQuestion.difficulty || 3) }}{{ '☆'.repeat(5 - (currentQuestion.difficulty || 3)) }}
    </span>
    <span v-if="currentQuestion.source" class="source-tag" :class="currentQuestion.source">
      {{ sourceLabel(currentQuestion.source) }}
    </span>
  </div>
</div>
```

- [ ] **Step 2: 添加 sourceLabel 方法**

在 `<script setup>` 中添加：

```javascript
const sourceLabel = (source) => {
  const labels = { 'SEED': '真题', 'REAL_EXAM': '真题采集', 'AI_EXPAND': 'AI改编', 'AI_GEN': 'AI原创' }
  return labels[source] || ''
}
```

- [ ] **Step 3: 添加 CSS 样式**

在 `<style scoped>` 中添加：

```css
.question-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 8px;
}
.difficulty { color: #faad14; font-size: 0.85rem; }
.source-tag {
  font-size: 0.75rem;
  padding: 2px 8px;
  border-radius: 4px;
  background: #f0f0f0;
  color: #666;
}
.source-tag.REAL_EXAM { background: #e6f7ff; color: #1890ff; }
.source-tag.AI_EXPAND { background: #fff7e6; color: #fa8c16; }
.source-tag.AI_GEN { background: #f6ffed; color: #52c41a; }
```

- [ ] **Step 4: Commit**

```bash
git add vue-project/src/views/XingceModuleView.vue
git commit -m "feat: 前端展示题目难度星星和来源标签"
```

---

### Task 9: 种子题扩充（可选）

**Files:**
- Modify: `gk-backend/gk-backend/src/main/resources/db/migration/V3__seed_questions.sql`

- [ ] **Step 1: 扩充种子题**

在 `V3__seed_questions.sql` 末尾追加每模块 20 道额外题目（此处省略具体题目内容，实际执行时需补充）。每道题格式：

```sql
INSERT IGNORE INTO question (category, title, options_json, answer, analysis, module, difficulty, source) VALUES
('考点', '题目内容', '{"A":"...","B":"...","C":"...","D":"..."}', 'A', '解析', '模块', 3, 'SEED');
```

- [ ] **Step 2: 执行扩充脚本**

```bash
"/c/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe" -u root -p123456 --default-character-set=utf8mb4 gk_db < "D:/GK/App/gk-backend/gk-backend/src/main/resources/db/migration/V3__seed_questions.sql"
```

- [ ] **Step 3: Commit**

```bash
git add gk-backend/gk-backend/src/main/resources/db/migration/V3__seed_questions.sql
git commit -m "feat(db): 扩充种子题库到每模块25题"
```
