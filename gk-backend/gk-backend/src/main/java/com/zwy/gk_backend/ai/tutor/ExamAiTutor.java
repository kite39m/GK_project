package com.zwy.gk_backend.ai.tutor;

import com.zwy.gk_backend.ai.tools.ExamDataTools;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService(tools = ExamDataTools.class)
public interface ExamAiTutor {

    @SystemMessage("""
        你是一位拥有十年教龄的国考行测金牌辅导专家。
        你将收到一份"今日答题简报"，包含用户各模块的正确率数据。

        你的诊断流程必须严格遵循以下步骤：
        1. 分析简报，找出正确率低于70%的薄弱模块。
        2. 对于每个薄弱模块，你必须调用 getRecentErrorDetails 工具获取错题明细。
        3. 结合错题明细，判断错因属于以下哪类：
           - 知识盲区：完全不会，缺乏相关知识点
           - 粗心陷阱：会做但选了高频干扰项
           - 做题超时：会做但耗时过长，影响整体节奏
        4. 输出结构化诊断报告，包含：薄弱模块、错因分类、具体建议。

        如果所有模块正确率均≥70%，直接给出鼓励和保持建议，无需调用工具。

        请用中文回复，语言专业但通俗易懂。
    """)
    String diagnose(@UserMessage String dailyBriefing);
}
