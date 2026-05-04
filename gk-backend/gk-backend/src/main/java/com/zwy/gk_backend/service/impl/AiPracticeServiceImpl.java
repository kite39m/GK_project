package com.zwy.gk_backend.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwy.gk_backend.dto.AiSpeedCalcResponseDTO;
import com.zwy.gk_backend.service.AiPracticeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class AiPracticeServiceImpl implements AiPracticeService {

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.base-url}")
    private String baseUrl;

    @Value("${ai.model}")
    private String model;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
            "你是一位国考行测辅导专家与速算教练。你必须且只能输出一个合法的 JSON 数组结构。" +
            "不要包含任何 Markdown 标记（如 ```json），不要输出任何解释性文字。" +
            "JSON字段包括：question, exactAnswer, fastCalcSkill, analysis。";

    @Override
    public List<AiSpeedCalcResponseDTO> generateSpeedCalcQuestions(String calcType, int count) {
        String userPrompt = "请为我生成 " + count + " 道行测纯数字速算题，题型为：" + calcType + "。直接输出JSON数组。";

        // 1. 构造请求体
        Map<String, Object> body = Map.of(
                "model", model,
                "stream", false,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        // 2. 调用 AI API
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/chat/completions",
                HttpMethod.POST,
                request,
                String.class
        );

        String rawText = response.getBody();

        // 3. 从 API 响应中提取 content 字段
        try {
            Map<String, Object> respMap = MAPPER.readValue(rawText, new TypeReference<>() {});
            List<Map<String, Object>> choices = (List<Map<String, Object>>) respMap.get("choices");
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null) {
                    rawText = (String) message.get("content");
                }
            }
        } catch (Exception e) {
            // 解析失败则尝试直接使用 rawText
        }

        // 4. 清理 Markdown 代码块标记
        String json = cleanMarkdownFence(rawText);

        // 5. 反序列化为 DTO 列表
        try {
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("AI 返回内容解析失败，原始文本: " + json, e);
        }
    }

    /**
     * 清理大模型可能附带的 ```json ... ``` 标记
     */
    private String cleanMarkdownFence(String text) {
        if (text == null) return "[]";
        String cleaned = text.trim();
        // 去掉 ```json 或 ``` 开头和结尾
        cleaned = cleaned.replaceAll("^```(?:json)?\\s*", "");
        cleaned = cleaned.replaceAll("\\s*```$", "");
        return cleaned.trim();
    }
}
