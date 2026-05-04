package com.zwy.gk_backend.service;

import com.zwy.gk_backend.dto.GenerateResultDTO;
import com.zwy.gk_backend.dto.ModuleStatDTO;
import com.zwy.gk_backend.dto.TrapFeedback;
import com.zwy.gk_backend.entity.UserAnswerRecord;
import java.util.List;

public interface XingceService {

    void saveAnswer(UserAnswerRecord record);

    List<ModuleStatDTO> getTodayBriefing(Integer userId);

    String generateBriefingText(Integer userId);

    GenerateResultDTO generateQuestions(Integer userId, String module, int count);

    TrapFeedback checkTrapFeedback(UserAnswerRecord record);
}
