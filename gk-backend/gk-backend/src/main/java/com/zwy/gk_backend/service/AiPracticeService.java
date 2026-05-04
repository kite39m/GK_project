package com.zwy.gk_backend.service;

import com.zwy.gk_backend.dto.AiSpeedCalcResponseDTO;
import java.util.List;

public interface AiPracticeService {

    /**
     * 调用 AI 生成速算题
     * @param calcType 题型 (如 "基期量计算")
     * @param count    数量
     * @return 题目列表
     */
    List<AiSpeedCalcResponseDTO> generateSpeedCalcQuestions(String calcType, int count);
}
