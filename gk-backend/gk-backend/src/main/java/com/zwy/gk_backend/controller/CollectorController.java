package com.zwy.gk_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwy.gk_backend.ai.collector.QuestionCollectorAgent;
import com.zwy.gk_backend.entity.AiQuestion;
import com.zwy.gk_backend.mapper.AiQuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collector")
@CrossOrigin
public class CollectorController {

    @Autowired
    private QuestionCollectorAgent collectorAgent;

    @Autowired
    private AiQuestionMapper aiQuestionMapper;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @PostMapping("/collect")
    public Map<String, String> collect(@RequestBody Map<String, String> body) {
        String module = body.get("module");
        String moduleName = body.getOrDefault("moduleName", module);
        String prompt = String.format(
                "请采集「%s」模块的公务员考试行测真题，至少5道。模块代码：%s。" +
                "采集完成后将题目以纯JSON数组格式输出，字段包含：module, category, concept, title, optionsJson, answer, analysis。",
                moduleName, module);
        String result = collectorAgent.collectQuestions(prompt);

        // 尝试解析 AI 返回的 JSON 并保存到数据库
        String saveResult;
        try {
            String json = stripMarkdownCodeBlock(result);
            List<Map<String, Object>> items = MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
            int saved = 0;
            for (Map<String, Object> item : items) {
                String title = (String) item.get("title");
                String optionsJson = MAPPER.writeValueAsString(item.get("optionsJson"));
                String answer = (String) item.get("answer");

                if (title == null || title.isEmpty() || answer == null) continue;

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
            saveResult = "成功保存 " + saved + " 道题目";
        } catch (Exception e) {
            saveResult = "AI返回内容非有效JSON，未保存: " + e.getMessage();
        }

        return Map.of("result", result, "saveResult", saveResult);
    }

    private String stripMarkdownCodeBlock(String text) {
        if (text == null) return null;
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) trimmed = trimmed.substring(firstNewline + 1);
            if (trimmed.endsWith("```")) trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
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
