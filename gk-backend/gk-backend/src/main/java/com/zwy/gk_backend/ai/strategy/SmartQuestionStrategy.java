package com.zwy.gk_backend.ai.strategy;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zwy.gk_backend.entity.KnowledgePoint;
import com.zwy.gk_backend.entity.UserKnowledgeProfile;
import com.zwy.gk_backend.mapper.KnowledgePointMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SmartQuestionStrategy {

    @Autowired
    private KnowledgePointMapper knowledgePointMapper;

    public String buildProfilePrompt(List<UserKnowledgeProfile> profiles) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户知识点画像：\n");

        for (UserKnowledgeProfile p : profiles) {
            String level;
            String instruction;
            if (p.getProficiency() < 40) {
                level = "薄弱";
                instruction = "重点出题，生成基础难度题目巩固";
            } else if (p.getProficiency() < 70) {
                level = "中等";
                instruction = "适当出题，巩固+进阶";
            } else {
                level = "熟练";
                instruction = "少量出题，挑战高难度";
            }
            sb.append(String.format("- %s（熟练度%d%%，%s）：%s\n",
                    p.getConcept(), p.getProficiency(), level, instruction));
        }
        return sb.toString();
    }

    public String buildModulePrompt(String module, String moduleName, int count) {
        // 查询该模块的知识点清单
        QueryWrapper<KnowledgePoint> query = new QueryWrapper<>();
        query.eq("module", module).orderByAsc("sort_order");
        List<KnowledgePoint> points = knowledgePointMapper.selectList(query);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("请为「%s」模块生成%d道高质量行测选择题。\n\n", moduleName, count));
        sb.append("该模块知识点清单（请确保题目覆盖以下考点，不要遗漏）：\n");

        String grouped = points.stream()
                .collect(Collectors.groupingBy(KnowledgePoint::getCategory))
                .entrySet().stream()
                .map(e -> {
                    String concepts = e.getValue().stream()
                            .map(KnowledgePoint::getConcept)
                            .collect(Collectors.joining("、"));
                    return "- " + e.getKey() + "：" + concepts;
                })
                .collect(Collectors.joining("\n"));

        sb.append(grouped);
        sb.append("\n\n每道题必须明确标注所属的 category 和 concept。");
        sb.append("\n多样性要求：每道题表述形式不同（计算/概念/情景/比较），干扰项多样化。");
        sb.append("\n解析简洁，每题不超过50字。");

        return sb.toString();
    }
}
