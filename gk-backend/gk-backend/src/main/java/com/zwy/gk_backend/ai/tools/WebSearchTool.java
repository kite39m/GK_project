package com.zwy.gk_backend.ai.tools;

import dev.langchain4j.agent.tool.Tool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class WebSearchTool {

    @Tool("搜索公开的公务员考试真题，返回搜索结果链接列表。输入关键词如 '2024国考资料分析真题'")
    public List<String> searchExamQuestions(String keyword) {
        List<String> results = new ArrayList<>();
        try {
            String encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8);
            String url = "https://www.bing.com/search?q=" + encoded;
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(10000)
                    .get();
            Elements links = doc.select("a[href]");
            links.stream()
                    .map(el -> el.attr("href"))
                    .filter(href -> href.startsWith("http") && !href.contains("bing.com"))
                    .limit(5)
                    .forEach(results::add);
        } catch (Exception e) {
            results.add("搜索失败: " + e.getMessage());
        }
        return results;
    }
}
