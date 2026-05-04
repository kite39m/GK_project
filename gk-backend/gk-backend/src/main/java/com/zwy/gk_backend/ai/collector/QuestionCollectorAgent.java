package com.zwy.gk_backend.ai.collector;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

@AiService(tools = {"webSearchTool", "pageFetchTool"})
public interface QuestionCollectorAgent {

    @SystemMessage("""
        你是公考真题采集专家。你的任务是从公开网页中提取公务员考试行测真题。

        工作流程：
        1. 用 searchExamQuestions 搜索指定模块的真题
        2. 用 fetchPageContent 抓取搜索结果页面
        3. 从页面内容中提取题目，每道题包含：title, optionsJson, answer, analysis, category, concept
        4. 提取完成后，将题目以JSON数组格式输出，每道题包含以下字段：
           - module: 模块代码
           - category: 考点分类
           - concept: 细分考点
           - title: 题目内容
           - optionsJson: 选项对象 {"A":"...","B":"...","C":"...","D":"..."}
           - answer: 正确答案（A/B/C/D）
           - analysis: 解析

        提取要求：
        - 只提取有明确答案的题目
        - 选项必须是 A/B/C/D 四个
        - 如果页面没有答案或解析，用你的知识补充
        - 每次最多提取10道题
        - 输出纯JSON数组，不要Markdown标记
    """)
    String collectQuestions(@UserMessage String prompt);
}
