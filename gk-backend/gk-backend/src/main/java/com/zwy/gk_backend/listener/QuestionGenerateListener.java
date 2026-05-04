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

                // 校验
                if (title == null || title.isEmpty() || answer == null || answer.isEmpty()) continue;
                Map<String, Object> opts = (Map<String, Object>) item.get("optionsJson");
                if (opts == null || opts.size() != 4) continue;

                // 去重
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
