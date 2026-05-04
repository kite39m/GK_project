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

    @PostMapping("/essay/submit")
    public ShenlunEssay submitEssay(@RequestBody Map<String, Object> body) {
        Integer userId = (Integer) body.get("userId");
        String topic = (String) body.get("topic");
        String content = (String) body.get("content");
        return shenlunService.submitEssay(userId, topic, content);
    }

    @GetMapping("/essay/history")
    public IPage<ShenlunEssay> getEssayHistory(
            @RequestParam Integer userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return shenlunService.getEssayHistory(userId, page, size);
    }

    @PostMapping("/material/recommend")
    public ResponseEntity<String> recommendMaterials(@RequestBody Map<String, String> body) {
        String topic = body.get("topic");
        String result = shenlunAiTutor.recommendMaterials(topic);
        return ResponseEntity.ok(result);
    }
}
