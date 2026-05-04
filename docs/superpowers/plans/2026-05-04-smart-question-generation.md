# AI 智能出题架构升级 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 AI 出题从"盲目生成"升级为"画像驱动精准推题 + 异步演进"架构，实现缓存优先、真题兜底、陷阱 0 延迟反馈。

**Architecture:** 新增 ai_question_pool 缓冲池表，SmartQuestionStrategy 将用户画像转化为 Prompt，QuestionGeneratorTutor 通过 @AiService 生成题目，Spring Event 驱动异步补充，答题时读取预存陷阱模板实现 0 延迟反馈。

**Tech Stack:** Spring Boot 3.5.13, MyBatis-Plus 3.5.5, LangChain4j 0.36.2, Spring Event, Lombok, Java 17

**Design Spec:** `docs/superpowers/specs/2026-05-04-smart-question-generation-design.md`

---

## File Map

| Action | File | Responsibility |
|--------|------|----------------|
| Create | `db/migration/V4__add_ai_question_pool.sql` | DDL 建表 |
| Create | `entity/AiQuestion.java` | AI 题目实体 |
| Create | `mapper/AiQuestionMapper.java` | AI 题目 Mapper |
| Create | `dto/TrapFeedback.java` | 陷阱反馈 DTO |
| Create | `dto/GenerateResultDTO.java` | 出题结果 DTO |
| Create | `ai/strategy/SmartQuestionStrategy.java` | 画像→Prompt 转化 |
| Create | `ai/tutor/QuestionGeneratorTutor.java` | @AiService 题目生成 |
| Create | `event/QuestionGenerateEvent.java` | Spring Event 定义 |
| Create | `listener/QuestionGenerateListener.java` | 异步事件监听器 |
| Modify | `service/XingceService.java` | 接口增加方法 |
| Modify | `service/impl/XingceServiceImpl.java` | 缓存优先+兜底+陷阱反馈 |
| Modify | `controller/XingceController.java` | 新增出题 API，修改答题返回 |

所有路径相对于 `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/`（除 DDL 外）

---

## Task 1: DDL + Entity + Mapper

**Files:**
- Create: `gk-backend/gk-backend/src/main/resources/db/migration/V4__add_ai_question_pool.sql`
- Create: `entity/AiQuestion.java`
- Create: `mapper/AiQuestionMapper.java`

- [ ] **Step 1: 创建 DDL 脚本**

```sql
-- V4: AI题目缓冲池
CREATE TABLE IF NOT EXISTS ai_question_pool (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    module VARCHAR(20) NOT NULL COMMENT '模块: CHANGSHI/YUYU/ZILIAO/TUILI/SHULIANG',
    category VARCHAR(50) COMMENT '细分考点',
    title TEXT NOT NULL COMMENT '题目正文',
    optionsJson VARCHAR(1000) NOT NULL COMMENT '选项JSON',
    answer VARCHAR(10) NOT NULL COMMENT '正确答案',
    analysis TEXT COMMENT '常规解析',
    concept VARCHAR(50) COMMENT '知识点概念',
    trapOption VARCHAR(10) COMMENT '陷阱选项标识',
    trapAnalysisTemplate TEXT COMMENT '预存陷阱解析模板',
    questionHash VARCHAR(64) COMMENT '题目内容哈希(MD5)，用于去重',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/ACTIVE/REJECTED',
    sourceType VARCHAR(20) DEFAULT 'AI_GEN' COMMENT '来源: AI_GEN',
    checkLog VARCHAR(500) COMMENT '校验日志',
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE INDEX uk_hash (questionHash),
    INDEX idx_module_status (module, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI题目缓冲池';
```

- [ ] **Step 2: 创建 Entity**

```java
package com.zwy.gk_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

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

- [ ] **Step 3: 创建 Mapper**

```java
package com.zwy.gk_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zwy.gk_backend.entity.AiQuestion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiQuestionMapper extends BaseMapper<AiQuestion> {
}
```

- [ ] **Step 4: Commit**

```bash
git add gk-backend/gk-backend/src/main/resources/db/migration/V4__add_ai_question_pool.sql \
       gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/entity/AiQuestion.java \
       gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/mapper/AiQuestionMapper.java
git commit -m "feat: 添加ai_question_pool表DDL、Entity和Mapper"
```

---

## Task 2: DTO 类

**Files:**
- Create: `dto/TrapFeedback.java`
- Create: `dto/GenerateResultDTO.java`

- [ ] **Step 1: 创建 TrapFeedback**

```java
package com.zwy.gk_backend.dto;

import lombok.Data;

@Data
public class TrapFeedback {
    private boolean isTrap;
    private String trapAnalysis;
    private int trapCount;
    private String concept;
}
```

- [ ] **Step 2: 创建 GenerateResultDTO**

```java
package com.zwy.gk_backend.dto;

import com.zwy.gk_backend.entity.Question;
import lombok.Data;
import java.util.List;

@Data
public class GenerateResultDTO {
    private List<Question> questions;
    private boolean fromCache;
    private int totalInPool;
}
```

- [ ] **Step 3: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/dto/TrapFeedback.java \
       gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/dto/GenerateResultDTO.java
git commit -m "feat: 添加TrapFeedback和GenerateResultDTO"
```

---

## Task 3: SmartQuestionStrategy

**Files:**
- Create: `ai/strategy/SmartQuestionStrategy.java`

- [ ] **Step 1: 创建策略类**

```java
package com.zwy.gk_backend.ai.strategy;

import com.zwy.gk_backend.entity.UserKnowledgeProfile;
import org.springframework.stereotype.Component;
import java.util.List;

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

        sb.append("\n多样性要求：\n");
        sb.append("1. 每道题的表述形式必须不同（计算题/概念题/情景题/图表题/比较题）\n");
        sb.append("2. 干扰项设计要多样化（偷换概念/单位陷阱/以偏概全/因果倒置/数据混淆）\n");
        sb.append("3. 同一考点的连续题目不能使用相同的陷阱类型\n");

        return sb.toString();
    }

    public String buildModulePrompt(String module, String moduleName, int count) {
        return String.format("请为「%s」模块生成%d道高质量行测选择题。", moduleName, count);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/strategy/SmartQuestionStrategy.java
git commit -m "feat: 实现SmartQuestionStrategy画像→Prompt转化器"
```

---

## Task 4: QuestionGeneratorTutor (@AiService)

**Files:**
- Create: `ai/tutor/QuestionGeneratorTutor.java`

- [ ] **Step 1: 创建 AiService 接口**

```java
package com.zwy.gk_backend.ai.tutor;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface QuestionGeneratorTutor {

    @SystemMessage("""
        你是一位国考行测命题专家。
        根据用户的知识点画像，为指定模块生成高质量选择题。

        你必须且只输出一个合法的JSON数组，不要包含任何Markdown标记或解释性文字。
        每道题的JSON格式：
        {
            "title": "题目正文",
            "optionsJson": {"A": "选项A", "B": "选项B", "C": "选项C", "D": "选项D"},
            "answer": "正确答案字母",
            "analysis": "常规解析",
            "concept": "考点名称",
            "trapOption": "最易选错的干扰项字母",
            "trapAnalysisTemplate": "陷阱解析模板，用{userAnswer}占位用户选择，{answer}占位正确答案"
        }

        要求：
        1. 每道题的表述形式必须不同（计算题/概念题/情景题/图表题/比较题）
        2. 干扰项设计要多样化（偷换概念/单位陷阱/以偏概全/因果倒置/数据混淆）
        3. trapAnalysisTemplate 必须具有针对性，解释为什么该陷阱容易被选中
    """)
    String generateQuestions(@UserMessage String prompt);
}
```

- [ ] **Step 2: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/tutor/QuestionGeneratorTutor.java
git commit -m "feat: 实现QuestionGeneratorTutor @AiService题目生成接口"
```

---

## Task 5: Spring Event + 异步监听器

**Files:**
- Create: `event/QuestionGenerateEvent.java`
- Create: `listener/QuestionGenerateListener.java`

- [ ] **Step 1: 创建 Event**

```java
package com.zwy.gk_backend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class QuestionGenerateEvent extends ApplicationEvent {
    private final Integer userId;
    private final String module;
    private final String moduleName;
    private final int count;

    public QuestionGenerateEvent(Object source, Integer userId, String module, String moduleName, int count) {
        super(source);
        this.userId = userId;
        this.module = module;
        this.moduleName = moduleName;
        this.count = count;
    }
}
```

- [ ] **Step 2: 创建 Listener**

```java
package com.zwy.gk_backend.listener;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwy.gk_backend.ai.strategy.SmartQuestionStrategy;
import com.zwy.gk_backend.ai.tutor.QuestionGeneratorTutor;
import com.zwy.gk_backend.entity.AiQuestion;
import com.zwy.gk_backend.entity.UserKnowledgeProfile;
import com.zwy.gk_backend.event.QuestionGenerateEvent;
import com.zwy.gk_backend.mapper.AiQuestionMapper;
import com.zwy.gk_backend.mapper.UserKnowledgeProfileMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

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

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Async
    @EventListener
    public void handleGenerate(QuestionGenerateEvent event) {
        // 1. 读取用户画像
        QueryWrapper<UserKnowledgeProfile> query = new QueryWrapper<>();
        query.eq("user_id", event.getUserId()).eq("module", event.getModule());
        List<UserKnowledgeProfile> profiles = profileMapper.selectList(query);

        // 2. 构建 Prompt
        String profilePrompt = profiles.isEmpty()
                ? "该用户暂无画像数据，请生成基础难度题目。"
                : strategy.buildProfilePrompt(profiles);
        String modulePrompt = strategy.buildModulePrompt(event.getModule(), event.getModuleName(), event.getCount());
        String fullPrompt = modulePrompt + "\n\n" + profilePrompt;

        // 3. 调用 AI 生成
        String aiResponse = tutor.generateQuestions(fullPrompt);

        // 4. 解析 + 校验 + 去重 + 存储
        try {
            List<Map<String, Object>> items = MAPPER.readValue(aiResponse, new TypeReference<>() {});
            for (Map<String, Object> item : items) {
                String title = (String) item.get("title");
                String optionsJson = MAPPER.writeValueAsString(item.get("optionsJson"));
                String answer = (String) item.get("answer");

                // 校验：标题非空、选项4个、答案有效
                if (title == null || title.isEmpty() || answer == null || answer.isEmpty()) continue;
                Map<String, Object> opts = (Map<String, Object>) item.get("optionsJson");
                if (opts == null || opts.size() != 4) continue;

                // 去重：计算 MD5
                String hash = md5(title + optionsJson);

                QueryWrapper<AiQuestion> existQuery = new QueryWrapper<>();
                existQuery.eq("question_hash", hash);
                if (aiQuestionMapper.selectCount(existQuery) > 0) continue;

                // 存储
                AiQuestion aiq = new AiQuestion();
                aiq.setModule(event.getModule());
                aiq.setCategory((String) item.get("concept"));
                aiq.setTitle(title);
                aiq.setOptionsJson(optionsJson);
                aiq.setAnswer(answer);
                aiq.setAnalysis((String) item.get("analysis"));
                aiq.setConcept((String) item.get("concept"));
                aiq.setTrapOption((String) item.get("trapOption"));
                aiq.setTrapAnalysisTemplate((String) item.get("trapAnalysisTemplate"));
                aiq.setQuestionHash(hash);
                aiq.setStatus("ACTIVE");
                aiq.setSourceType("AI_GEN");
                aiq.setCheckLog("OK");
                aiQuestionMapper.insert(aiq);
            }
        } catch (Exception e) {
            // 解析失败，记录日志
            System.err.println("AI题目生成解析失败: " + e.getMessage());
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

- [ ] **Step 3: 在主应用类启用异步**

在 `GkBackendApplication.java` 上添加 `@EnableAsync` 注解：

```java
@SpringBootApplication
@EnableAsync
public class GkBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(GkBackendApplication.class, args);
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/event/QuestionGenerateEvent.java \
       gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/listener/QuestionGenerateListener.java \
       gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/GkBackendApplication.java
git commit -m "feat: 实现Spring Event异步题目生成（Event+Listener+@EnableAsync）"
```

---

## Task 6: 重构 XingceService（缓存优先+真题兜底+陷阱反馈）

**Files:**
- Modify: `service/XingceService.java`
- Modify: `service/impl/XingceServiceImpl.java`

- [ ] **Step 1: 更新接口**

在 `XingceService.java` 中新增方法：

```java
package com.zwy.gk_backend.service;

import com.zwy.gk_backend.dto.GenerateResultDTO;
import com.zwy.gk_backend.dto.ModuleStatDTO;
import com.zwy.gk_backend.dto.TrapFeedback;
import com.zwy.gk_backend.entity.UserAnswerRecord;
import java.util.List;

public interface XingceService {

    void saveAnswer(UserAnswerRecord record);

    List<ModuleStatDTO> getTodayBriefing(Integer userId);

    String generateBriefingText(Integer userId);

    /** AI 出题（缓存优先+真题兜底+异步补充） */
    GenerateResultDTO generateQuestions(Integer userId, String module, int count);

    /** 陷阱检测（读取预存模板，0延迟） */
    TrapFeedback checkTrapFeedback(UserAnswerRecord record);
}
```

- [ ] **Step 2: 重构实现类**

完整替换 `XingceServiceImpl.java`：

```java
package com.zwy.gk_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zwy.gk_backend.dto.GenerateResultDTO;
import com.zwy.gk_backend.dto.ModuleStatDTO;
import com.zwy.gk_backend.dto.TrapFeedback;
import com.zwy.gk_backend.entity.AiQuestion;
import com.zwy.gk_backend.entity.Question;
import com.zwy.gk_backend.entity.UserAnswerRecord;
import com.zwy.gk_backend.entity.UserKnowledgeProfile;
import com.zwy.gk_backend.event.QuestionGenerateEvent;
import com.zwy.gk_backend.mapper.AiQuestionMapper;
import com.zwy.gk_backend.mapper.QuestionMapper;
import com.zwy.gk_backend.mapper.UserAnswerRecordMapper;
import com.zwy.gk_backend.mapper.UserKnowledgeProfileMapper;
import com.zwy.gk_backend.service.KnowledgeProfileService;
import com.zwy.gk_backend.service.XingceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class XingceServiceImpl implements XingceService {

    private static final Map<String, String> MODULE_NAMES = Map.of(
            "CHANGSHI", "常识",
            "YUYU", "言语理解",
            "ZILIAO", "资料分析",
            "TUILI", "推理判断",
            "SHULIANG", "数量关系"
    );

    @Autowired
    private UserAnswerRecordMapper answerRecordMapper;

    @Autowired
    private KnowledgeProfileService profileService;

    @Autowired
    private AiQuestionMapper aiQuestionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private UserKnowledgeProfileMapper profileMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    public void saveAnswer(UserAnswerRecord record) {
        answerRecordMapper.insert(record);

        if (record.getConcept() != null && !record.getConcept().isEmpty()) {
            profileService.updateAfterAnswer(
                    record.getUserId(),
                    record.getModule(),
                    record.getConcept(),
                    Boolean.TRUE.equals(record.getIsCorrect())
            );
        }
    }

    @Override
    public List<ModuleStatDTO> getTodayBriefing(Integer userId) {
        List<Map<String, Object>> raw = answerRecordMapper.getTodayBriefing(userId);
        List<ModuleStatDTO> result = new ArrayList<>();

        for (Map<String, Object> row : raw) {
            ModuleStatDTO dto = new ModuleStatDTO();
            dto.setModule((String) row.get("module"));
            dto.setModuleName(MODULE_NAMES.getOrDefault(dto.getModule(), dto.getModule()));
            dto.setTotal(((Number) row.get("total")).intValue());
            dto.setCorrect(((Number) row.get("correct")).intValue());
            dto.setAccuracy(((Number) row.get("accuracy")).doubleValue());
            result.add(dto);
        }
        return result;
    }

    @Override
    public String generateBriefingText(Integer userId) {
        List<ModuleStatDTO> stats = getTodayBriefing(userId);
        if (stats.isEmpty()) {
            return "今日暂无答题记录。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("今日答题简报：\n");
        for (ModuleStatDTO stat : stats) {
            sb.append(String.format("- %s: 共%d题, 对%d题, 正确率%.1f%%\n",
                    stat.getModuleName(), stat.getTotal(), stat.getCorrect(), stat.getAccuracy()));
        }
        return sb.toString();
    }

    @Override
    public GenerateResultDTO generateQuestions(Integer userId, String module, int count) {
        GenerateResultDTO result = new GenerateResultDTO();
        List<Question> merged = new ArrayList<>();
        boolean fromCache = true;

        // 1. 从 ai_question_pool 检索 ACTIVE 题目
        QueryWrapper<AiQuestion> aiQuery = new QueryWrapper<>();
        aiQuery.eq("module", module).eq("status", "ACTIVE").last("LIMIT " + count);
        List<AiQuestion> aiQuestions = aiQuestionMapper.selectList(aiQuery);

        // 转换为 Question 对象
        for (AiQuestion aiq : aiQuestions) {
            Question q = new Question();
            q.setId(aiq.getId().intValue());
            q.setModule(aiq.getModule());
            q.setCategory(aiq.getCategory());
            q.setTitle(aiq.getTitle());
            q.setOptionsJson(aiq.getOptionsJson());
            q.setAnswer(aiq.getAnswer());
            q.setAnalysis(aiq.getAnalysis());
            merged.add(q);
        }

        // 2. 不足则从 question 正式题库补充
        if (merged.size() < count) {
            int remain = count - merged.size();
            QueryWrapper<Question> formalQuery = new QueryWrapper<>();
            formalQuery.eq("module", module).last("LIMIT " + remain);
            List<Question> formal = questionMapper.selectList(formalQuery);
            merged.addAll(formal);
        }

        // 3. 如果正式题库也没有，标记非缓存
        if (aiQuestions.isEmpty() && merged.isEmpty()) {
            fromCache = false;
        }

        // 4. 统计池中总量
        QueryWrapper<AiQuestion> countQuery = new QueryWrapper<>();
        countQuery.eq("module", module).eq("status", "ACTIVE");
        int totalInPool = aiQuestionMapper.selectCount(countQuery).intValue();

        result.setQuestions(merged);
        result.setFromCache(fromCache);
        result.setTotalInPool(totalInPool);

        // 5. 发布异步事件，补充题库
        String moduleName = MODULE_NAMES.getOrDefault(module, module);
        eventPublisher.publishEvent(new QuestionGenerateEvent(this, userId, module, moduleName, count));

        return result;
    }

    @Override
    public TrapFeedback checkTrapFeedback(UserAnswerRecord record) {
        TrapFeedback feedback = new TrapFeedback();
        feedback.setTrap(false);

        if (Boolean.TRUE.equals(record.getIsCorrect()) || !Boolean.TRUE.equals(record.getIsTrapOption())) {
            return feedback;
        }

        // 从 ai_question_pool 读取 trapAnalysisTemplate
        String trapTemplate = null;
        if (record.getQuestionId() != null) {
            AiQuestion aiq = aiQuestionMapper.selectById(record.getQuestionId());
            if (aiq != null && aiq.getTrapAnalysisTemplate() != null) {
                trapTemplate = aiq.getTrapAnalysisTemplate();
            }
        }

        if (trapTemplate == null) {
            return feedback;
        }

        // 查询该 concept 的历史踩坑次数
        QueryWrapper<UserAnswerRecord> trapQuery = new QueryWrapper<>();
        trapQuery.eq("user_id", record.getUserId())
                 .eq("concept", record.getConcept())
                 .eq("is_correct", false)
                 .eq("is_trap_option", true);
        int trapCount = answerRecordMapper.selectCount(trapQuery).intValue();

        // 渲染模板
        String rendered = trapTemplate
                .replace("{userAnswer}", record.getUserAnswer() != null ? record.getUserAnswer() : "")
                .replace("{answer}", "");

        feedback.setTrap(true);
        feedback.setTrapAnalysis(rendered);
        feedback.setTrapCount(trapCount);
        feedback.setConcept(record.getConcept());

        return feedback;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/service/XingceService.java \
       gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/service/impl/XingceServiceImpl.java
git commit -m "feat: 重构XingceService（缓存优先+真题兜底+陷阱反馈+异步补充）"
```

---

## Task 7: 更新 XingceController

**Files:**
- Modify: `controller/XingceController.java`

- [ ] **Step 1: 添加出题 API，修改答题返回**

在 `XingceController.java` 中添加：

```java
@PostMapping("/generate")
public ResponseEntity<GenerateResultDTO> generateQuestions(@RequestBody Map<String, Object> body) {
    Integer userId = (Integer) body.get("userId");
    String module = (String) body.get("module");
    int count = body.containsKey("count") ? (Integer) body.get("count") : 5;
    GenerateResultDTO result = xingceService.generateQuestions(userId, module, count);
    return ResponseEntity.ok(result);
}
```

修改 `submitAnswer` 方法，返回陷阱反馈：

```java
@PostMapping("/answer")
public ResponseEntity<Map<String, Object>> submitAnswer(@RequestBody UserAnswerRecord record) {
    xingceService.saveAnswer(record);
    TrapFeedback trapFeedback = xingceService.checkTrapFeedback(record);

    Map<String, Object> resp = new HashMap<>();
    resp.put("status", "ok");
    resp.put("trapFeedback", trapFeedback);
    return ResponseEntity.ok(resp);
}
```

需要在文件顶部添加 import：

```java
import com.zwy.gk_backend.dto.GenerateResultDTO;
import com.zwy.gk_backend.dto.TrapFeedback;
```

- [ ] **Step 2: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/controller/XingceController.java
git commit -m "feat: XingceController新增出题API，答题返回陷阱反馈"
```

---

## Phase 1 完成检查清单

- [ ] ai_question_pool 表已在 MySQL 中创建
- [ ] AiQuestion Entity + AiQuestionMapper 编译通过
- [ ] TrapFeedback + GenerateResultDTO 编译通过
- [ ] SmartQuestionStrategy 画像→Prompt 转化逻辑正确
- [ ] QuestionGeneratorTutor @AiService 接口定义正确
- [ ] Spring Event + Listener 异步生成逻辑完整
- [ ] @EnableAsync 已添加到主应用类
- [ ] XingceService 新增 generateQuestions 和 checkTrapFeedback
- [ ] XingceController 出题 API 和陷阱反馈返回正确
- [ ] 端到端测试：调用出题API → 秒回 → 异步生成 → 题目入库

**下一步：** Phase D — 前端改动（AI出题按钮+陷阱反馈展示）
