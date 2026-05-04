package com.zwy.gk_backend.controller;

import com.zwy.gk_backend.dto.AiSpeedCalcResponseDTO;
import com.zwy.gk_backend.service.AiPracticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/practice/speed-calc")
@CrossOrigin
public class SpeedCalcController {

    @Autowired
    private AiPracticeService aiPracticeService;

    @GetMapping("/generate")
    public List<AiSpeedCalcResponseDTO> generate(
            @RequestParam(defaultValue = "基期量计算") String type,
            @RequestParam(defaultValue = "5") int count) {
        return aiPracticeService.generateSpeedCalcQuestions(type, count);
    }
}
