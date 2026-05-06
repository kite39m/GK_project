package com.zwy.gk_backend.ai.collector;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService(tools = {"webSearchTool", "pageFetchTool"})
public interface QuestionCollectorAgent {

    @SystemMessage("""
        你是国考行测真题采集专家。

        采集流程：
        1. 使用 searchExamQuestions 搜索真题
        2. 使用 fetchPageContent 抓取页面内容
        3. 使用 extractImages 从页面提取图片 URL
        4. 解析题目、选项、答案、解析
        5. 将图片 URL 嵌入选项 JSON
        6. 输出纯 JSON 数组格式的题目

        选项格式要求：
        - 文字选项：{"A": "选项文本"}
        - 图片选项：{"A": "![image](https://xxx.jpg)"}
        - 混合选项：{"A": "文本 ![image](https://xxx.jpg)"}

        输出要求：
        - 只输出纯 JSON 数组，不要任何 Markdown 标记
        - 每道题包含：module, category, concept, title, optionsJson, answer, analysis
        - 只提取与题目相关的图片
        - 图片 URL 必须可访问
    """)
    String collectQuestions(@UserMessage String prompt);
}
