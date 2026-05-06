package com.zwy.gk_backend.ai.tools;

import dev.langchain4j.agent.tool.Tool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PageFetchTool {

    @Autowired
    private ImageExtractor imageExtractor;

    @Tool("抓取指定URL的页面内容，返回纯文本。用于提取网页中的考试题目。")
    public String fetchPageContent(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(15000)
                    .get();
            // 移除 script 和 style 标签
            doc.select("script, style, nav, footer, header").remove();
            String text = doc.body().text();
            // 限制长度避免超出 token 限制
            return text.length() > 5000 ? text.substring(0, 5000) : text;
        } catch (Exception e) {
            return "抓取失败: " + e.getMessage();
        }
    }

    @Tool("从指定URL的页面中提取图片URL。返回找到的图片URL列表。")
    public List<String> extractImages(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(15000)
                    .get();
            String html = doc.html();
            return imageExtractor.extractFromPage(html, url);
        } catch (Exception e) {
            return List.of();
        }
    }
}
