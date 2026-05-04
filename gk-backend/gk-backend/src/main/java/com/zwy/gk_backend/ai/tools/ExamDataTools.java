package com.zwy.gk_backend.ai.tools;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwy.gk_backend.entity.UserAnswerRecord;
import com.zwy.gk_backend.entity.UserKnowledgeProfile;
import com.zwy.gk_backend.mapper.UserAnswerRecordMapper;
import com.zwy.gk_backend.mapper.UserKnowledgeProfileMapper;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ExamDataTools {

    @Autowired
    private UserAnswerRecordMapper answerRecordMapper;

    @Autowired
    private UserKnowledgeProfileMapper profileMapper;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Tool("当发现某模块正确率低于70%时，调用此方法获取该模块最近的错题明细。" +
          "返回错题的考点、用户选择的答案、正确答案、是否掉入陷阱选项。" +
          "moduleName 可选值: CHANGSHI, YUYU, ZILIAO, TUILI, SHULIANG")
    public String getRecentErrorDetails(String moduleName, int limit) {
        QueryWrapper<UserAnswerRecord> query = new QueryWrapper<>();
        query.eq("module", moduleName)
             .eq("is_correct", false)
             .orderByDesc("created_at")
             .last("LIMIT " + limit);
        List<UserAnswerRecord> errors = answerRecordMapper.selectList(query);

        if (errors.isEmpty()) {
            return "该模块暂无错题记录。";
        }

        List<Map<String, Object>> details = errors.stream().map(e -> {
            Map<String, Object> detail = new HashMap<>();
            detail.put("concept", e.getConcept());
            detail.put("userAnswer", e.getUserAnswer());
            detail.put("isTrapOption", e.getIsTrapOption());
            detail.put("timeCostSec", e.getTimeCostSec());
            return detail;
        }).collect(Collectors.toList());

        try {
            return MAPPER.writeValueAsString(details);
        } catch (Exception ex) {
            return "数据解析错误: " + ex.getMessage();
        }
    }

    @Tool("获取用户各模块的熟练度画像，返回每个考点的熟练度百分比和上次练习时间")
    public String getKnowledgeProfile(int userId) {
        QueryWrapper<UserKnowledgeProfile> query = new QueryWrapper<>();
        query.eq("user_id", userId).orderByAsc("proficiency");
        List<UserKnowledgeProfile> profiles = profileMapper.selectList(query);

        if (profiles.isEmpty()) {
            return "该用户暂无知识点画像数据。";
        }

        List<Map<String, Object>> data = profiles.stream().map(p -> {
            Map<String, Object> item = new HashMap<>();
            item.put("module", p.getModule());
            item.put("concept", p.getConcept());
            item.put("proficiency", p.getProficiency());
            item.put("totalAttempts", p.getTotalAttempts());
            item.put("lastPracticeTime", p.getLastPracticeTime());
            return item;
        }).collect(Collectors.toList());

        try {
            return MAPPER.writeValueAsString(data);
        } catch (Exception ex) {
            return "数据解析错误: " + ex.getMessage();
        }
    }
}
