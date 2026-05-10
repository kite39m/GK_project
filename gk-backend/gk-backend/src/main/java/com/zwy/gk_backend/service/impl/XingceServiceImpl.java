package com.zwy.gk_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zwy.gk_backend.dto.GenerateResultDTO;
import com.zwy.gk_backend.dto.ModuleStatDTO;
import com.zwy.gk_backend.dto.TrapFeedback;
import com.zwy.gk_backend.entity.AiQuestion;
import com.zwy.gk_backend.entity.Question;
import com.zwy.gk_backend.entity.UserAnswerRecord;
import com.zwy.gk_backend.entity.UserKnowledgeProfile;
import com.zwy.gk_backend.event.DifficultyUpdateEvent;
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

        // 发布难度更新事件
        eventPublisher.publishEvent(new DifficultyUpdateEvent(this, record.getQuestionId(), Boolean.TRUE.equals(record.getIsCorrect()), "question"));

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

        // 1. 读取用户知识点画像
        QueryWrapper<UserKnowledgeProfile> profileQuery = new QueryWrapper<>();
        profileQuery.eq("user_id", userId).eq("module", module);
        List<UserKnowledgeProfile> profiles = profileMapper.selectList(profileQuery);

        // 构建考点→熟练度映射
        Map<String, Integer> conceptProficiency = new HashMap<>();
        for (UserKnowledgeProfile p : profiles) {
            conceptProficiency.put(p.getConcept(), p.getProficiency());
        }

        // 2. 查询用户已答对的题目ID（排除用）
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

        // 3. 查询用户答错的题目ID（错题优先用）
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

        // 4. 画像驱动选题：根据 proficiency 确定目标难度范围
        // proficiency < 40 → difficulty 1-2（薄弱点强化）
        // proficiency 40-70 → difficulty 2-4（巩固）
        // proficiency > 70 → difficulty 3-5（挑战高难度）
        int avgProficiency = 50; // 默认中等
        if (!profiles.isEmpty()) {
            avgProficiency = (int) profiles.stream()
                    .mapToInt(UserKnowledgeProfile::getProficiency)
                    .average()
                    .orElse(50);
        }

        int minDifficulty, maxDifficulty;
        if (avgProficiency < 40) {
            minDifficulty = 1;
            maxDifficulty = 2;
        } else if (avgProficiency < 70) {
            minDifficulty = 2;
            maxDifficulty = 4;
        } else {
            minDifficulty = 3;
            maxDifficulty = 5;
        }

        // 5. 多样性约束：确定需要覆盖的考点数量（2-3个）
        int targetConceptCount = Math.min(3, Math.max(2, count / 2));

        // 6. 从 ai_question_pool 检索（画像驱动 + 多样性 + 错题优先）
        QueryWrapper<AiQuestion> aiQuery = new QueryWrapper<>();
        aiQuery.eq("module", module)
               .eq("status", "ACTIVE")
               .ge("difficulty", minDifficulty)
               .le("difficulty", maxDifficulty);

        // 排除已答对的题目
        if (!correctIds.isEmpty()) {
            aiQuery.notIn("id", correctIds);
        }

        // 优先选择薄弱考点的题目
        List<String> weakConcepts = profiles.stream()
                .filter(p -> p.getProficiency() < 40)
                .map(UserKnowledgeProfile::getConcept)
                .collect(Collectors.toList());

        if (!weakConcepts.isEmpty()) {
            aiQuery.in("concept", weakConcepts);
        }

        // 错题优先排序 + 随机
        String wrongIdsStr = wrongIdSet.isEmpty() ? "0" : wrongIdSet.stream().map(String::valueOf).collect(Collectors.joining(","));
        aiQuery.last("ORDER BY CASE WHEN id IN (" + wrongIdsStr + ") THEN 0 ELSE 1 END, RAND() LIMIT " + (count * 2)); // 多取一些用于多样性筛选
        List<AiQuestion> aiQuestions = aiQuestionMapper.selectList(aiQuery);

        // 7. 多样性筛选：确保覆盖不同考点
        Map<String, List<AiQuestion>> byConcept = aiQuestions.stream()
                .collect(Collectors.groupingBy(q -> q.getConcept() != null ? q.getConcept() : "unknown"));

        List<AiQuestion> selectedAi = new ArrayList<>();
        Set<String> coveredConcepts = new HashSet<>();

        // 先从每个考点取1题，确保覆盖
        for (Map.Entry<String, List<AiQuestion>> entry : byConcept.entrySet()) {
            if (selectedAi.size() >= count) break;
            if (coveredConcepts.size() >= targetConceptCount && !coveredConcepts.contains(entry.getKey())) continue;

            List<AiQuestion> conceptQuestions = entry.getValue();
            // 优先选择错题
            conceptQuestions.sort((a, b) -> {
                boolean aWrong = wrongIdSet.contains(a.getId().intValue());
                boolean bWrong = wrongIdSet.contains(b.getId().intValue());
                if (aWrong != bWrong) return aWrong ? -1 : 1;
                return 0;
            });

            selectedAi.add(conceptQuestions.get(0));
            coveredConcepts.add(entry.getKey());
        }

        // 再补充剩余题目
        for (AiQuestion q : aiQuestions) {
            if (selectedAi.size() >= count) break;
            if (!selectedAi.contains(q)) {
                selectedAi.add(q);
            }
        }

        // 转换为 Question 对象
        for (AiQuestion aiq : selectedAi) {
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

        // 8. 不足则从 question 正式题库补充（画像驱动 + 多样性）
        if (merged.size() < count) {
            int remain = count - merged.size();
            Set<Integer> existingIds = merged.stream().map(Question::getId).collect(Collectors.toSet());
            existingIds.addAll(correctIds);

            QueryWrapper<Question> formalQuery = new QueryWrapper<>();
            formalQuery.eq("module", module)
                       .ge("difficulty", minDifficulty)
                       .le("difficulty", maxDifficulty);

            if (!existingIds.isEmpty()) {
                formalQuery.notIn("id", existingIds);
            }

            // 优先薄弱考点
            if (!weakConcepts.isEmpty() && coveredConcepts.size() < targetConceptCount) {
                formalQuery.in("category", weakConcepts);
            }

            formalQuery.last("ORDER BY RAND() LIMIT " + remain);
            List<Question> formal = questionMapper.selectList(formalQuery);
            merged.addAll(formal);
        }

        // 9. 仍然不足 → 错题重练（允许已答对的错题重新出现）
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

        // 10. 最终兜底：如果仍然不足，放宽难度限制
        if (merged.size() < count) {
            int remain = count - merged.size();
            Set<Integer> existingIds = merged.stream().map(Question::getId).collect(Collectors.toSet());
            existingIds.addAll(correctIds);

            QueryWrapper<Question> fallbackQuery = new QueryWrapper<>();
            fallbackQuery.eq("module", module);
            if (!existingIds.isEmpty()) {
                fallbackQuery.notIn("id", existingIds);
            }
            fallbackQuery.last("ORDER BY RAND() LIMIT " + remain);
            List<Question> fallback = questionMapper.selectList(fallbackQuery);
            merged.addAll(fallback);
        }

        if (aiQuestions.isEmpty() && merged.isEmpty()) {
            fromCache = false;
        }

        // 11. 统计池中总量
        QueryWrapper<AiQuestion> countQuery = new QueryWrapper<>();
        countQuery.eq("module", module).eq("status", "ACTIVE");
        int totalInPool = aiQuestionMapper.selectCount(countQuery).intValue();

        result.setQuestions(merged);
        result.setFromCache(fromCache);
        result.setTotalInPool(totalInPool);

        // 12. 发布异步事件（补充题库）
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
