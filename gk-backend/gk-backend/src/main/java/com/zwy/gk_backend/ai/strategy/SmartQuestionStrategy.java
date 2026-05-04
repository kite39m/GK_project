package com.zwy.gk_backend.ai.strategy;

import com.zwy.gk_backend.entity.UserKnowledgeProfile;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class SmartQuestionStrategy {

    public String buildProfilePrompt(List<UserKnowledgeProfile> profiles) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户知识点画像如下：\n");

        for (UserKnowledgeProfile p : profiles) {
            String level;
            String instruction;
            if (p.getProficiency() < 50) {
                level = "薄弱";
                instruction = "生成高难度、陷阱明显的题目，强化该考点";
            } else if (p.getProficiency() < 80) {
                level = "中等";
                instruction = "生成巩固型题目，巩固基础";
            } else {
                level = "熟练";
                instruction = "生成进阶型题目，挑战更高难度";
            }
            sb.append(String.format("- %s（熟练度%d%%，%s）：%s\n",
                    p.getConcept(), p.getProficiency(), level, instruction));
        }

        sb.append("\n多样性要求：\n");
        sb.append("1. 每道题的表述形式必须不同（计算题/概念题/情景题/图表题/比较题）\n");
        sb.append("2. 干扰项设计要多样化（偷换概念/单位陷阱/以偏概全/因果倒置/数据混淆）\n");
        sb.append("3. 同一考点的连续题目不能使用相同的陷阱类型\n");

        return sb.toString();
    }

    public String buildModulePrompt(String module, String moduleName, int count) {
        return String.format("请为「%s」模块生成%d道高质量行测选择题。", moduleName, count);
    }
}
