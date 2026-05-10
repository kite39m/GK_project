package com.zwy.gk_backend.controller;

import com.zwy.gk_backend.service.PanduanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/panduan")
@CrossOrigin
@RequiredArgsConstructor
public class PanduanController {

    private final PanduanService panduanService;

    /**
     * 与判断推理辅导 Agent 对话
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(
            @RequestParam int userId,
            @RequestBody Map<String, String> request) {

        String message = request.get("message");
        if (message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "消息不能为空"));
        }

        log.info("用户{}发起判断推理辅导对话: {}", userId, message);

        String response = panduanService.chat(userId, message);

        return ResponseEntity.ok(Map.of("response", response));
    }
}
