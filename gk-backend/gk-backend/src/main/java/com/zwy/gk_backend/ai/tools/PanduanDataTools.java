package com.zwy.gk_backend.ai.tools;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zwy.gk_backend.entity.UserAnswerRecord;
import com.zwy.gk_backend.entity.UserKnowledgeProfile;
import com.zwy.gk_backend.mapper.UserAnswerRecordMapper;
import com.zwy.gk_backend.mapper.UserKnowledgeProfileMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component("panduanDataTools")
@RequiredArgsConstructor
public class PanduanDataTools {

    private final UserAnswerRecordMapper userAnswerRecordMapper;
    private final UserKnowledgeProfileMapper userKnowledgeProfileMapper;

    @Tool("获取用户在判断推理模块的最近错题明细，返回错题的考点、用户答案、是否陷阱选项、答题耗时等信息")
    public String getRecentErrorDetails(
            @P("用户ID") int userId,
            @P("获取数量，默认10条") int limit) {
        log.info("获取用户{}在判断推理模块的最近{}条错题", userId, limit);

        QueryWrapper<UserAnswerRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("module", "TUILI")
               .eq("is_correct", 0)
               .orderByDesc("created_at")
               .last("LIMIT " + limit);

        List<UserAnswerRecord> records = userAnswerRecordMapper.selectList(wrapper);

        if (records.isEmpty()) {
            return "该用户在判断推理模块暂无错题记录";
        }

        List<String> errorDetails = records.stream()
                .map(r -> String.format(
                        "考点: %s, 用户答案: %s, 是否陷阱选项: %s, 答题耗时: %d秒",
                        r.getConcept(),
                        r.getUserAnswer(),
                        Boolean.TRUE.equals(r.getIsTrapOption()) ? "是" : "否",
                        r.getTimeCostSec()))
                .collect(Collectors.toList());

        return String.join("\n", errorDetails);
    }

    @Tool("获取用户在判断推理模块的熟练度画像，返回各考点的熟练度、练习次数等信息")
    public String getKnowledgeProfile(@P("用户ID") int userId) {
        log.info("获取用户{}在判断推理模块的熟练度画像", userId);

        QueryWrapper<UserKnowledgeProfile> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("module", "TUILI")
               .orderByDesc("proficiency");

        List<UserKnowledgeProfile> profiles = userKnowledgeProfileMapper.selectList(wrapper);

        if (profiles.isEmpty()) {
            return "该用户在判断推理模块暂无练习记录";
        }

        List<String> profileDetails = profiles.stream()
                .map(p -> String.format(
                        "考点: %s, 熟练度: %d%%, 练习次数: %d, 正确次数: %d",
                        p.getConcept(),
                        p.getProficiency(),
                        p.getTotalAttempts(),
                        p.getCorrectCount()))
                .collect(Collectors.toList());

        return String.join("\n", profileDetails);
    }
}
