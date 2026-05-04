package com.zwy.gk_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zwy.gk_backend.ai.tutor.ExamAiTutor;
import com.zwy.gk_backend.dto.GenerateResultDTO;
import com.zwy.gk_backend.dto.ModuleStatDTO;
import com.zwy.gk_backend.dto.TrapFeedback;
import com.zwy.gk_backend.entity.Question;
import com.zwy.gk_backend.entity.UserAnswerRecord;
import com.zwy.gk_backend.entity.UserKnowledgeProfile;
import com.zwy.gk_backend.mapper.QuestionMapper;
import com.zwy.gk_backend.mapper.UserKnowledgeProfileMapper;
import com.zwy.gk_backend.service.XingceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/xingce")
@CrossOrigin
public class XingceController {

    @Autowired
    private XingceService xingceService;

    @Autowired
    private ExamAiTutor examAiTutor;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private UserKnowledgeProfileMapper profileMapper;

    @GetMapping("/modules")
    public List<ModuleStatDTO> getModules(@RequestParam Integer userId) {
        return xingceService.getTodayBriefing(userId);
    }

    @GetMapping("/questions/{module}")
    public List<Question> getQuestions(@PathVariable String module,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size) {
        QueryWrapper<Question> query = new QueryWrapper<>();
        query.eq("module", module).last("LIMIT " + size + " OFFSET " + (page - 1) * size);
        return questionMapper.selectList(query);
    }

    @PostMapping("/answer")
    public ResponseEntity<Map<String, Object>> submitAnswer(@RequestBody UserAnswerRecord record) {
        xingceService.saveAnswer(record);
        TrapFeedback trapFeedback = xingceService.checkTrapFeedback(record);

        Map<String, Object> resp = new HashMap<>();
        resp.put("status", "ok");
        resp.put("trapFeedback", trapFeedback);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/generate")
    public ResponseEntity<GenerateResultDTO> generateQuestions(@RequestBody Map<String, Object> body) {
        Integer userId = (Integer) body.get("userId");
        String module = (String) body.get("module");
        int count = body.containsKey("count") ? (Integer) body.get("count") : 5;
        GenerateResultDTO result = xingceService.generateQuestions(userId, module, count);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/diagnose")
    public ResponseEntity<Map<String, String>> diagnose(@RequestParam Integer userId) {
        String briefing = xingceService.generateBriefingText(userId);
        String report = examAiTutor.diagnose(briefing);
        Map<String, String> resp = new HashMap<>();
        resp.put("briefing", briefing);
        resp.put("report", report);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/profile/{userId}")
    public List<UserKnowledgeProfile> getProfile(@PathVariable Integer userId) {
        QueryWrapper<UserKnowledgeProfile> query = new QueryWrapper<>();
        query.eq("user_id", userId).orderByAsc("proficiency");
        return profileMapper.selectList(query);
    }
}
