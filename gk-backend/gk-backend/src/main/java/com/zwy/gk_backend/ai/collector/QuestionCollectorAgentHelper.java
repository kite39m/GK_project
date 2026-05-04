package com.zwy.gk_backend.ai.collector;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
                QueryWrapper<AiQuestion> existQuery = new QueryWrapper<>();
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
