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

        // 3. 从 ai_question_pool 检索（排除已答对 + 错题优先 + 随机）
        QueryWrapper<AiQuestion> aiQuery = new QueryWrapper<>();
        aiQuery.eq("module", module).eq("status", "ACTIVE");
        if (!correctIds.isEmpty()) {
            aiQuery.notIn("id", correctIds);
        }
        String wrongIdsStr = wrongIdSet.isEmpty() ? "0" : wrongIdSet.stream().map(String::valueOf).collect(Collectors.joining(","));
        aiQuery.last("ORDER BY CASE WHEN id IN (" + wrongIdsStr + ") THEN 0 ELSE 1 END, RAND() LIMIT " + count);
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
