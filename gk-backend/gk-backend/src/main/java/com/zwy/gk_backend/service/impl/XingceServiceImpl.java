package com.zwy.gk_backend.service.impl;

import com.zwy.gk_backend.dto.ModuleStatDTO;
import com.zwy.gk_backend.entity.UserAnswerRecord;
import com.zwy.gk_backend.mapper.UserAnswerRecordMapper;
import com.zwy.gk_backend.service.KnowledgeProfileService;
import com.zwy.gk_backend.service.XingceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class XingceServiceImpl implements XingceService {

    private static final Map<String, String> MODULE_NAMES = Map.of(
            "CHANGSHI", "常识",
            "YUYU", "言语理解",
            "ZILIAO", "资料分析",
            "TUILI", "推理判断",
            "SHULIANG", "数量关系"
    );

    @Autowired
    private UserAnswerRecordMapper answerRecordMapper;

    @Autowired
    private KnowledgeProfileService profileService;

    @Override
    public void saveAnswer(UserAnswerRecord record) {
        answerRecordMapper.insert(record);

        if (record.getConcept() != null && !record.getConcept().isEmpty()) {
            profileService.updateAfterAnswer(
                    record.getUserId(),
                    record.getModule(),
                    record.getConcept(),
                    Boolean.TRUE.equals(record.getIsCorrect())
            );
        }
    }

    @Override
    public List<ModuleStatDTO> getTodayBriefing(Integer userId) {
        List<Map<String, Object>> raw = answerRecordMapper.getTodayBriefing(userId);
        List<ModuleStatDTO> result = new ArrayList<>();

        for (Map<String, Object> row : raw) {
            ModuleStatDTO dto = new ModuleStatDTO();
            dto.setModule((String) row.get("module"));
            dto.setModuleName(MODULE_NAMES.getOrDefault(dto.getModule(), dto.getModule()));
            dto.setTotal(((Number) row.get("total")).intValue());
            dto.setCorrect(((Number) row.get("correct")).intValue());
            dto.setAccuracy(((Number) row.get("accuracy")).doubleValue());
            result.add(dto);
        }
        return result;
    }

    @Override
    public String generateBriefingText(Integer userId) {
        List<ModuleStatDTO> stats = getTodayBriefing(userId);
        if (stats.isEmpty()) {
            return "今日暂无答题记录。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("今日答题简报：\n");
        for (ModuleStatDTO stat : stats) {
            sb.append(String.format("- %s: 共%d题, 对%d题, 正确率%.1f%%\n",
                    stat.getModuleName(), stat.getTotal(), stat.getCorrect(), stat.getAccuracy()));
        }
        return sb.toString();
    }
}
