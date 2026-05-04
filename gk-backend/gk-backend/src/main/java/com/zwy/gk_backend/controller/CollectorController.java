package com.zwy.gk_backend.controller;

import com.zwy.gk_backend.ai.collector.QuestionCollectorAgent;
import com.zwy.gk_backend.ai.collector.QuestionCollectorAgentHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/collector")
@CrossOrigin
public class CollectorController {

    @Autowired
    private QuestionCollectorAgent collectorAgent;

    @Autowired
    private QuestionCollectorAgentHelper collectorHelper;

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
        try {
            String saveResult = collectorHelper.saveCollectedQuestions(result);
            return Map.of("result", result, "saveResult", saveResult);
        } catch (Exception e) {
            return Map.of("result", result, "saveResult", "AI返回内容非有效JSON，未保存: " + e.getMessage());
        }
    }
}
