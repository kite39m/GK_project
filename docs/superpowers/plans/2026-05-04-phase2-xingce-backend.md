# Phase 2: 行测模块后端 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现行测模块的完整后端逻辑 — 答题记录、知识点画像（含艾宾浩斯衰减）、AI Agentic 诊断（@Tool + @AiService）、REST API。

**Architecture:** XingceService 处理答题记录和模块统计，KnowledgeProfileService 维护知识点画像和艾宾浩斯衰减，ExamDataTools 提供 @Tool 工具供 AI 按需查询数据库，ExamAiTutor 通过 @AiService 声明式接口实现 Agentic 诊断，XingceController 暴露 REST API。

**Tech Stack:** Spring Boot 3.5.13, MyBatis-Plus 3.5.5, LangChain4j 0.36.2, Lombok, Java 17

**Design Spec:** `docs/superpowers/specs/2026-05-04-ai-diagnosis-module-design.md`
**Phase 1 依赖:** Entity、Mapper、LangChain4j 配置已完成

---

## File Map

| Action | File | Responsibility |
|--------|------|----------------|
| Create | `service/XingceService.java` | 行测业务接口 |
| Create | `service/impl/XingceServiceImpl.java` | 答题记录保存、模块统计计算 |
| Create | `service/KnowledgeProfileService.java` | 知识点画像接口 |
| Create | `service/impl/KnowledgeProfileServiceImpl.java` | 艾宾浩斯衰减计算、画像更新 |
| Create | `ai/tools/ExamDataTools.java` | @Tool 工具类（AI 主动查 DB） |
| Create | `ai/tutor/ExamAiTutor.java` | @AiService 行测诊断接口 |
| Create | `dto/DiagnosisReport.java` | 诊断报告 DTO |
| Create | `dto/ModuleStatDTO.java` | 模块统计 DTO |
| Create | `controller/XingceController.java` | 行测 REST API |

所有路径相对于 `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/`

---

## Task 1: 创建 DTO 类

**Files:**
- Create: `dto/ModuleStatDTO.java`
- Create: `dto/DiagnosisReport.java`

- [ ] **Step 1: 创建 ModuleStatDTO**

```java
package com.zwy.gk_backend.dto;

import lombok.Data;

@Data
public class ModuleStatDTO {
    private String module;
    private String moduleName; // 中文名
    private Integer total;
    private Integer correct;
    private Double accuracy;
}
```

- [ ] **Step 2: 创建 DiagnosisReport**

```java
package com.zwy.gk_backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class DiagnosisReport {
    private String rawReport;       // AI 原始诊断文本
    private List<WeakModule> weakModules;

    @Data
    public static class WeakModule {
        private String module;
        private String moduleName;
        private Double accuracy;
        private String errorCategory; // 知识盲区/粗心陷阱/做题超时
        private String suggestion;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/dto/ModuleStatDTO.java \
       gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/dto/DiagnosisReport.java
git commit -m "feat(dto): 添加行测模块统计和诊断报告DTO"
```

---

## Task 2: 创建 KnowledgeProfileService（艾宾浩斯画像）

**Files:**
- Create: `service/KnowledgeProfileService.java`
- Create: `service/impl/KnowledgeProfileServiceImpl.java`

- [ ] **Step 1: 创建接口**

```java
package com.zwy.gk_backend.service;

public interface KnowledgeProfileService {

    /**
     * 答题后更新知识点画像
     * @param userId 用户ID
     * @param module 模块编码
     * @param concept 考点
     * @param isCorrect 是否正确
     */
    void updateAfterAnswer(Integer userId, String module, String concept, boolean isCorrect);
}
```

- [ ] **Step 2: 创建实现类**

```java
package com.zwy.gk_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.zwy.gk_backend.entity.UserKnowledgeProfile;
import com.zwy.gk_backend.mapper.UserKnowledgeProfileMapper;
import com.zwy.gk_backend.service.KnowledgeProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

@Service
public class KnowledgeProfileServiceImpl implements KnowledgeProfileService {

    @Autowired
    private UserKnowledgeProfileMapper profileMapper;

    @Override
    public void updateAfterAnswer(Integer userId, String module, String concept, boolean isCorrect) {
        // 1. 查询或创建画像记录
        QueryWrapper<UserKnowledgeProfile> query = new QueryWrapper<>();
        query.eq("user_id", userId).eq("module", module).eq("concept", concept);
        UserKnowledgeProfile profile = profileMapper.selectOne(query);

        if (profile == null) {
            profile = new UserKnowledgeProfile();
            profile.setUserId(userId);
            profile.setModule(module);
            profile.setConcept(concept);
            profile.setProficiency(50);
            profile.setTotalAttempts(0);
            profile.setCorrectCount(0);
        }

        // 2. 更新统计
        profile.setTotalAttempts(profile.getTotalAttempts() + 1);
        if (isCorrect) {
            profile.setCorrectCount(profile.getCorrectCount() + 1);
            // 正确：熟练度 +10（上限 100）
            profile.setProficiency(Math.min(100, profile.getProficiency() + 10));
        } else {
            // 错误：熟练度 -15（下限 0）
            profile.setProficiency(Math.max(0, profile.getProficiency() - 15));
        }

        // 3. 更新时间
        Date now = new Date();
        profile.setLastPracticeTime(now);
        profile.setNextReviewTime(calculateNextReviewTime(profile.getProficiency()));

        // 4. 保存
        if (profile.getId() == null) {
            profileMapper.insert(profile);
        } else {
            profileMapper.updateById(profile);
        }
    }

    /**
     * 根据艾宾浩斯曲线计算下次复习时间
     * proficiency < 30: 1天后
     * proficiency 30-60: 3天后
     * proficiency 60-80: 7天后
     * proficiency > 80: 15天后
     */
    private Date calculateNextReviewTime(int proficiency) {
        Calendar cal = Calendar.getInstance();
        int daysToAdd;
        if (proficiency < 30) {
            daysToAdd = 1;
        } else if (proficiency < 60) {
            daysToAdd = 3;
        } else if (proficiency < 80) {
            daysToAdd = 7;
        } else {
            daysToAdd = 15;
        }
        cal.add(Calendar.DAY_OF_MONTH, daysToAdd);
        return cal.getTime();
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/service/KnowledgeProfileService.java \
       gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/service/impl/KnowledgeProfileServiceImpl.java
git commit -m "feat(service): 实现知识点画像服务（含艾宾浩斯衰减）"
```

---

## Task 3: 创建 XingceService（行测业务）

**Files:**
- Create: `service/XingceService.java`
- Create: `service/impl/XingceServiceImpl.java`

- [ ] **Step 1: 创建接口**

```java
package com.zwy.gk_backend.service;

import com.zwy.gk_backend.dto.ModuleStatDTO;
import com.zwy.gk_backend.entity.UserAnswerRecord;
import java.util.List;

public interface XingceService {

    /** 保存答题记录并更新知识点画像 */
    void saveAnswer(UserAnswerRecord record);

    /** 获取今日各模块统计简报 */
    List<ModuleStatDTO> getTodayBriefing(Integer userId);

    /** 生成今日简报文本（供 AI 诊断使用） */
    String generateBriefingText(Integer userId);
}
```

- [ ] **Step 2: 创建实现类**

```java
package com.zwy.gk_backend.service.impl;

import com.zwy.gk_backend.dto.ModuleStatDTO;
import com.zwy.gk_backend.entity.UserAnswerRecord;
import com.zwy.gk_backend.mapper.UserAnswerRecordMapper;
import com.zwy.gk_backend.service.KnowledgeProfileService;
import com.zwy.gk_backend.service.XingceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

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

    @Override
    public void saveAnswer(UserAnswerRecord record) {
        // 1. 保存答题记录
        answerRecordMapper.insert(record);

        // 2. 更新知识点画像（如果有 concept）
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
}
```

- [ ] **Step 3: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/service/XingceService.java \
       gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/service/impl/XingceServiceImpl.java
git commit -m "feat(service): 实现行测业务服务（答题记录+模块统计）"
```

---

## Task 4: 创建 ExamDataTools（@Tool 工具类）

**Files:**
- Create: `ai/tools/ExamDataTools.java`

- [ ] **Step 1: 创建工具类**

```java
package com.zwy.gk_backend.ai.tools;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwy.gk_backend.entity.UserAnswerRecord;
import com.zwy.gk_backend.entity.UserKnowledgeProfile;
import com.zwy.gk_backend.mapper.UserAnswerRecordMapper;
import com.zwy.gk_backend.mapper.UserKnowledgeProfileMapper;
import dev.langchain4j.service.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ExamDataTools {

    @Autowired
    private UserAnswerRecordMapper answerRecordMapper;

    @Autowired
    private UserKnowledgeProfileMapper profileMapper;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Tool("当发现某模块正确率低于70%时，调用此方法获取该模块最近的错题明细。" +
          "返回错题的考点、用户选择的答案、正确答案、是否掉入陷阱选项。" +
          "moduleName 可选值: CHANGSHI, YUYU, ZILIAO, TUILI, SHULIANG")
    public String getRecentErrorDetails(String moduleName, int limit) {
        QueryWrapper<UserAnswerRecord> query = new QueryWrapper<>();
        query.eq("module", moduleName)
             .eq("is_correct", false)
             .orderByDesc("created_at")
             .last("LIMIT " + limit);
        List<UserAnswerRecord> errors = answerRecordMapper.selectList(query);

        if (errors.isEmpty()) {
            return "该模块暂无错题记录。";
        }

        List<Map<String, Object>> details = errors.stream().map(e -> {
            Map<String, Object> detail = new HashMap<>();
            detail.put("concept", e.getConcept());
            detail.put("userAnswer", e.getUserAnswer());
            detail.put("isTrapOption", e.getIsTrapOption());
            detail.put("timeCostSec", e.getTimeCostSec());
            return detail;
        }).collect(Collectors.toList());

        try {
            return MAPPER.writeValueAsString(details);
        } catch (Exception ex) {
            return "数据解析错误: " + ex.getMessage();
        }
    }

    @Tool("获取用户各模块的熟练度画像，返回每个考点的熟练度百分比和上次练习时间")
    public String getKnowledgeProfile(int userId) {
        QueryWrapper<UserKnowledgeProfile> query = new QueryWrapper<>();
        query.eq("user_id", userId).orderByAsc("proficiency");
        List<UserKnowledgeProfile> profiles = profileMapper.selectList(query);

        if (profiles.isEmpty()) {
            return "该用户暂无知识点画像数据。";
        }

        List<Map<String, Object>> data = profiles.stream().map(p -> {
            Map<String, Object> item = new HashMap<>();
            item.put("module", p.getModule());
            item.put("concept", p.getConcept());
            item.put("proficiency", p.getProficiency());
            item.put("totalAttempts", p.getTotalAttempts());
            item.put("lastPracticeTime", p.getLastPracticeTime());
            return item;
        }).collect(Collectors.toList());

        try {
            return MAPPER.writeValueAsString(data);
        } catch (Exception ex) {
            return "数据解析错误: " + ex.getMessage();
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/tools/ExamDataTools.java
git commit -m "feat(ai): 实现ExamDataTools @Tool工具类（AI主动查询数据库）"
```

---

## Task 5: 创建 ExamAiTutor（@AiService 行测诊断）

**Files:**
- Create: `ai/tutor/ExamAiTutor.java`

- [ ] **Step 1: 创建 AiService 接口**

```java
package com.zwy.gk_backend.ai.tutor;

import com.zwy.gk_backend.ai.tools.ExamDataTools;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.tool.Tool;

@AiService(tools = ExamDataTools.class)
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

        请用中文回复，语言专业但通俗易懂。
    """)
    String diagnose(@UserMessage String dailyBriefing);
}
```

- [ ] **Step 2: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/tutor/ExamAiTutor.java
git commit -m "feat(ai): 实现ExamAiTutor @AiService行测诊断接口"
```

---

## Task 6: 创建 XingceController（REST API）

**Files:**
- Create: `controller/XingceController.java`

- [ ] **Step 1: 创建 Controller**

```java
package com.zwy.gk_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zwy.gk_backend.ai.tutor.ExamAiTutor;
import com.zwy.gk_backend.dto.ModuleStatDTO;
import com.zwy.gk_backend.entity.Question;
import com.zwy.gk_backend.entity.UserAnswerRecord;
import com.zwy.gk_backend.entity.UserKnowledgeProfile;
import com.zwy.gk_backend.mapper.QuestionMapper;
import com.zwy.gk_backend.mapper.UserKnowledgeProfileMapper;
import com.zwy.gk_backend.service.XingceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/xingce")
@CrossOrigin
public class XingceController {

    @Autowired
    private XingceService xingceService;

    @Autowired
    private ExamAiTutor examAiTutor;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private UserKnowledgeProfileMapper profileMapper;

    /** 获取5个子模块列表及统计 */
    @GetMapping("/modules")
    public List<ModuleStatDTO> getModules(@RequestParam Integer userId) {
        return xingceService.getTodayBriefing(userId);
    }

    /** 获取某模块的题目列表 */
    @GetMapping("/questions/{module}")
    public List<Question> getQuestions(@PathVariable String module,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        QueryWrapper<Question> query = new QueryWrapper<>();
        query.eq("module", module).last("LIMIT " + size + " OFFSET " + (page - 1) * size);
        return questionMapper.selectList(query);
    }

    /** 提交答题记录 */
    @PostMapping("/answer")
    public ResponseEntity<Map<String, String>> submitAnswer(@RequestBody UserAnswerRecord record) {
        xingceService.saveAnswer(record);
        Map<String, String> resp = new HashMap<>();
        resp.put("status", "ok");
        return ResponseEntity.ok(resp);
    }

    /** 触发AI诊断 */
    @PostMapping("/diagnose")
    public ResponseEntity<Map<String, String>> diagnose(@RequestParam Integer userId) {
        String briefing = xingceService.generateBriefingText(userId);
        String report = examAiTutor.diagnose(briefing);
        Map<String, String> resp = new HashMap<>();
        resp.put("briefing", briefing);
        resp.put("report", report);
        return ResponseEntity.ok(resp);
    }

    /** 获取用户知识点画像 */
    @GetMapping("/profile/{userId}")
    public List<UserKnowledgeProfile> getProfile(@PathVariable Integer userId) {
        QueryWrapper<UserKnowledgeProfile> query = new QueryWrapper<>();
        query.eq("user_id", userId).orderByAsc("proficiency");
        return profileMapper.selectList(query);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/controller/XingceController.java
git commit -m "feat(controller): 实现XingceController行测REST API"
```

---

## Phase 2 完成检查清单

- [ ] ModuleStatDTO、DiagnosisReport DTO 编译通过
- [ ] KnowledgeProfileService 实现艾宾浩斯衰减逻辑
- [ ] XingceService 实现答题记录保存和简报生成
- [ ] ExamDataTools @Tool 工具类可被 AI 调用
- [ ] ExamAiTutor @AiService 接口定义正确
- [ ] XingceController 5 个 API 端点可用
- [ ] 端到端测试：提交答题 → 生成简报 → AI 诊断 → 返回报告

**下一步：** Phase 3 — 申论模块后端实现
