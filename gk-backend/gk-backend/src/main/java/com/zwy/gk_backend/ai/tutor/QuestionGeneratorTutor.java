package com.zwy.gk_backend.ai.tutor;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService
public interface QuestionGeneratorTutor {

    @SystemMessage("""
        你是国考行测命题专家。根据用户画像为指定模块生成选择题。

        【关键】直接输出JSON数组，禁止任何Markdown标记（不要```）、不要解释文字、不要"好的"等废话。
        输出格式示例：[{"title":"...","optionsJson":{"A":"...","B":"...","C":"...","D":"..."},"answer":"A","analysis":"...","concept":"...","trapOption":"B","trapAnalysisTemplate":"..."}]

        要求：
        1. 每道题表述形式不同（计算/概念/情景/比较）
        2. 干扰项多样化（偷换概念/单位陷阱/以偏概全/因果倒置）
        3. trapAnalysisTemplate用{userAnswer}和{answer}占位
        4. 解析简洁，每题不超过50字
    """)
    String generateQuestions(@UserMessage String prompt);
}
