# Phase 3: 申论模块后端 Implementation Plan

**Goal:** 实现申论模块的完整后端 — 作文提交与 AI 评分、素材推荐、历史记录查询。

**Architecture:** ShenlunService 处理作文保存和历史查询，ShenlunAiTutor 通过 @AiService 提供作文评分和素材推荐，ShenlunController 暴露 REST API。

---

## File Map

| Action | File | Responsibility |
|--------|------|----------------|
| Create | `dto/ShenlunFeedback.java` | 申论评分反馈 DTO |
| Create | `service/ShenlunService.java` | 申论业务接口 |
| Create | `service/impl/ShenlunServiceImpl.java` | 作文保存、历史查询 |
| Create | `ai/tutor/ShenlunAiTutor.java` | @AiService 评分+素材推荐 |
| Create | `controller/ShenlunController.java` | 申论 REST API |

所有路径相对于 `gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/`

---

## Task 1: 创建 ShenlunFeedback DTO

**Files:**
- Create: `dto/ShenlunFeedback.java`

- [ ] **Step 1: 创建 DTO**

```java
package com.zwy.gk_backend.dto;

import lombok.Data;

@Data
public class ShenlunFeedback {
    private Integer structureScore;
    private Integer argumentScore;
    private Integer languageScore;
    private Integer totalScore;
    private String feedback;
}
```

- [ ] **Step 2: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/dto/ShenlunFeedback.java
git commit -m "feat(dto): 添加ShenlunFeedback申论评分反馈DTO"
```

---

## Task 2: 创建 ShenlunService

**Files:**
- Create: `service/ShenlunService.java`
- Create: `service/impl/ShenlunServiceImpl.java`

- [ ] **Step 1: 创建接口**

```java
package com.zwy.gk_backend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zwy.gk_backend.entity.ShenlunEssay;

public interface ShenlunService {

    /** 保存作文并触发AI评分 */
    ShenlunEssay submitEssay(Integer userId, String topic, String content);

    /** 获取用户作文历史（分页） */
    IPage<ShenlunEssay> getEssayHistory(Integer userId, int page, int size);
}
```

- [ ] **Step 2: 创建实现类**

```java
package com.zwy.gk_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwy.gk_backend.ai.tutor.ShenlunAiTutor;
import com.zwy.gk_backend.dto.ShenlunFeedback;
import com.zwy.gk_backend.entity.ShenlunEssay;
import com.zwy.gk_backend.mapper.ShenlunEssayMapper;
import com.zwy.gk_backend.service.ShenlunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ShenlunServiceImpl implements ShenlunService {

    @Autowired
    private ShenlunEssayMapper essayMapper;

    @Autowired
    private ShenlunAiTutor shenlunAiTutor;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public ShenlunEssay submitEssay(Integer userId, String topic, String content) {
        // 1. 调用 AI 评分
        String aiResponse = shenlunAiTutor.scoreEssay(content);

        // 2. 解析 AI 返回的 JSON
        ShenlunEssay essay = new ShenlunEssay();
        essay.setUserId(userId);
        essay.setTopic(topic);
        essay.setContent(content);

        try {
            ShenlunFeedback feedback = MAPPER.readValue(aiResponse, ShenlunFeedback.class);
            essay.setAiScore(feedback.getTotalScore());
            essay.setAiStructureScore(feedback.getStructureScore());
            essay.setAiArgumentScore(feedback.getArgumentScore());
            essay.setAiLanguageScore(feedback.getLanguageScore());
            essay.setAiFeedback(aiResponse);
        } catch (Exception e) {
            // JSON 解析失败，保存原始文本
            essay.setAiFeedback(aiResponse);
        }

        // 3. 保存到数据库
        essayMapper.insert(essay);
        return essay;
    }

    @Override
    public IPage<ShenlunEssay> getEssayHistory(Integer userId, int page, int size) {
        QueryWrapper<ShenlunEssay> query = new QueryWrapper<>();
        query.eq("user_id", userId).orderByDesc("created_at");
        return essayMapper.selectPage(new Page<>(page, size), query);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/service/ShenlunService.java \
       gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/service/impl/ShenlunServiceImpl.java
git commit -m "feat(service): 实现申论业务服务（作文提交+AI评分+历史查询）"
```

---

## Task 3: 创建 ShenlunAiTutor（@AiService）

**Files:**
- Create: `ai/tutor/ShenlunAiTutor.java`

- [ ] **Step 1: 创建 AiService 接口**

```java
package com.zwy.gk_backend.ai.tutor;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface ShenlunAiTutor {

    @SystemMessage("""
        你是一位资深的申论阅卷专家。
        用户会提交一篇申论文章，你需要从以下维度评分（每项0-100分）：
        - 结构分：文章结构是否清晰（总分总、层次分明）
        - 论点分：论点是否准确、论据是否充分
        - 语言分：语言表达是否规范、流畅

        你必须且只输出一个合法的JSON对象，不要包含任何Markdown标记或解释性文字。
        JSON格式：{"structureScore": 80, "argumentScore": 75, "languageScore": 85, "totalScore": 80, "feedback": "整体评价文字..."}
        totalScore = (structureScore + argumentScore + languageScore) / 3，四舍五入取整。
    """)
    String scoreEssay(@UserMessage String essayContent);

    @SystemMessage("""
        你是一位公考申论素材库管理员。
        根据用户指定的主题，推荐3-5个写作素材，每个素材包含：标题、核心内容、适用场景。
        你必须且只输出一个合法的JSON数组，不要包含任何Markdown标记或解释性文字。
        JSON格式：[{"title": "素材标题", "content": "核心内容", "scenario": "适用场景"}]
    """)
    String recommendMaterials(@UserMessage String topic);
}
```

- [ ] **Step 2: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/ai/tutor/ShenlunAiTutor.java
git commit -m "feat(ai): 实现ShenlunAiTutor @AiService申论评分+素材推荐接口"
```

---

## Task 4: 创建 ShenlunController（REST API）

**Files:**
- Create: `controller/ShenlunController.java`

- [ ] **Step 1: 创建 Controller**

```java
package com.zwy.gk_backend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zwy.gk_backend.ai.tutor.ShenlunAiTutor;
import com.zwy.gk_backend.entity.ShenlunEssay;
import com.zwy.gk_backend.service.ShenlunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/shenlun")
@CrossOrigin
public class ShenlunController {

    @Autowired
    private ShenlunService shenlunService;

    @Autowired
    private ShenlunAiTutor shenlunAiTutor;

    /** 提交作文，返回AI评分 */
    @PostMapping("/essay/submit")
    public ShenlunEssay submitEssay(@RequestBody Map<String, Object> body) {
        Integer userId = (Integer) body.get("userId");
        String topic = (String) body.get("topic");
        String content = (String) body.get("content");
        return shenlunService.submitEssay(userId, topic, content);
    }

    /** 获取用户作文历史 */
    @GetMapping("/essay/history")
    public IPage<ShenlunEssay> getEssayHistory(
            @RequestParam Integer userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return shenlunService.getEssayHistory(userId, page, size);
    }

    /** 获取素材推荐 */
    @PostMapping("/material/recommend")
    public ResponseEntity<String> recommendMaterials(@RequestBody Map<String, String> body) {
        String topic = body.get("topic");
        String result = shenlunAiTutor.recommendMaterials(topic);
        return ResponseEntity.ok(result);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add gk-backend/gk-backend/src/main/java/com/zwy/gk_backend/controller/ShenlunController.java
git commit -m "feat(controller): 实现ShenlunController申论REST API"
```

---

## Phase 3 完成检查清单

- [ ] ShenlunFeedback DTO 编译通过
- [ ] ShenlunService 实现作文提交+AI评分+历史查询
- [ ] ShenlunAiTutor @AiService 评分和素材推荐接口定义正确
- [ ] ShenlunController 3 个 API 端点可用
- [ ] 端到端测试：提交作文 → AI评分 → 保存 → 查询历史

**下一步：** Phase 4 — 前端实现
