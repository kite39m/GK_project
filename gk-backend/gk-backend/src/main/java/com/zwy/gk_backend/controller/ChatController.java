package com.zwy.gk_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwy.gk_backend.entity.ChatMessage;
import com.zwy.gk_backend.entity.ChatSession;
import com.zwy.gk_backend.mapper.ChatMessageMapper;
import com.zwy.gk_backend.mapper.ChatSessionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin
public class ChatController {

    @Autowired
    private ChatSessionMapper sessionMapper;

    @Autowired
    private ChatMessageMapper messageMapper;

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.base-url}")
    private String baseUrl;

    @Value("${ai.model}")
    private String model;

    private static final String SYSTEM_PROMPT =
            "你现在是一位拥有十年教龄的国家公务员考试金牌辅导专家。\n" +
            "你的专长涵盖行测五大模块（言语理解、数量关系、判断推理、资料分析、常识判断）以及申论写作。\n" +
            "请用专业但通俗的语言回答学生问题，必要时使用表格、列表、代码块等排版让知识点更清晰。\n" +
            "如果用户的问题偏离了公考备考话题，请礼貌地引导他们回到备考相关内容上来。";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ===== 会话管理 =====

    @PostMapping("/session")
    public ChatSession createSession(@RequestParam Integer userId, @RequestParam String title) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(title);
        session.setCreatedAt(new Date());
        sessionMapper.insert(session);
        return session;
    }

    @GetMapping("/sessions")
    public List<ChatSession> getSessions(@RequestParam Integer userId) {
        QueryWrapper<ChatSession> query = new QueryWrapper<>();
        query.eq("user_id", userId).orderByDesc("created_at");
        return sessionMapper.selectList(query);
    }

    @GetMapping("/messages")
    public List<ChatMessage> getMessages(@RequestParam Integer sessionId) {
        QueryWrapper<ChatMessage> query = new QueryWrapper<>();
        query.eq("session_id", sessionId).orderByAsc("created_at");
        return messageMapper.selectList(query);
    }

    // ===== 流式问答 =====

    @GetMapping(value = "/stream", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> chatStream(
            @RequestParam String userPrompt,
            @RequestParam(required = false) Integer sessionId) {

        // 如果没有传 sessionId，自动创建一个新会话
        if (sessionId == null) {
            ChatSession session = new ChatSession();
            session.setUserId(1);
            session.setTitle(userPrompt.length() > 20 ? userPrompt.substring(0, 20) + "..." : userPrompt);
            session.setCreatedAt(new Date());
            sessionMapper.insert(session);
            sessionId = session.getId();
        }

        // 保存用户消息
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(userPrompt);
        userMsg.setCreatedAt(new Date());
        messageMapper.insert(userMsg);

        final Integer finalSessionId = sessionId;

        WebClient client = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "stream", true,
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        return client.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .mapNotNull(this::extractContent)
                .doOnNext(chunk -> {})  // 流式输出中不做额外操作
                .doOnComplete(() -> {
                    // 流结束后：这里拿不到完整内容，改为前端主动调保存接口
                });
    }

    @PostMapping("/save")
    public void saveAiMessage(@RequestParam Integer sessionId, @RequestBody Map<String, String> body) {
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSessionId(sessionId);
        aiMsg.setRole("assistant");
        aiMsg.setContent(body.get("content"));
        aiMsg.setCreatedAt(new Date());
        messageMapper.insert(aiMsg);
    }

    private String extractContent(String raw) {
        if (raw == null) return null;
        String data = raw.trim();

        if (data.startsWith("data:")) {
            data = data.substring(5).trim();
        }

        if (data.isEmpty() || data.equals("[DONE]") || data.equals("null")) {
            return null;
        }

        try {
            JsonNode root = MAPPER.readTree(data);
            JsonNode choices = root.path("choices");
            if (choices.isMissingNode() || choices.isEmpty()) return null;
            JsonNode delta = choices.path(0).path("delta").path("content");
            if (delta.isMissingNode() || delta.isNull()) return null;
            String text = delta.asText();
            return text.isEmpty() ? null : text;
        } catch (Exception e) {
            return null;
        }
    }
}
