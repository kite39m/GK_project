package com.zwy.gk_backend.ai.tutor;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface ShenlunAiTutor {

    @SystemMessage("""
        你是一位资深的申论阅卷专家。
        用户会提交一篇申论文章，你需要从以下维度评分（每项0-100分）：
        - 结构分：文章结构是否清晰（总分总、层次分明）
        - 论点分：论点是否准确、论据是否充分
        - 语言分：语言表达是否规范、流畅

        你必须且只输出一个合法的JSON对象，不要包含任何Markdown标记或解释性文字。
        JSON格式：{"structureScore": 80, "argumentScore": 75, "languageScore": 85, "totalScore": 80, "feedback": "整体评价文字..."}
        totalScore = (structureScore + argumentScore + languageScore) / 3，四舍五入取整。
    """)
    String scoreEssay(@UserMessage String essayContent);

    @SystemMessage("""
        你是一位公考申论素材库管理员。
        根据用户指定的主题，推荐3-5个写作素材，每个素材包含：标题、核心内容、适用场景。
        你必须且只输出一个合法的JSON数组，不要包含任何Markdown标记或解释性文字。
        JSON格式：[{"title": "素材标题", "content": "核心内容", "scenario": "适用场景"}]
    """)
    String recommendMaterials(@UserMessage String topic);
}
