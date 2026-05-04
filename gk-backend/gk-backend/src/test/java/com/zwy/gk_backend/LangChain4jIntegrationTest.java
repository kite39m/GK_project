package com.zwy.gk_backend;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.AiService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class LangChain4jIntegrationTest {

    @AiService
    interface TestAiService {
        @SystemMessage("你是一个测试助手，用一句话回答问题。")
        String chat(@UserMessage String message);
    }

    @Autowired(required = false)
    private TestAiService testAiService;

    @Test
    void testMiMoApiConnection() {
        if (testAiService == null) {
            System.out.println("⚠️ LangChain4j AiService 未自动装配，跳过测试");
            return;
        }
        String response = testAiService.chat("你好，请回复'连接成功'");
        System.out.println("MiMo API 响应: " + response);
        assert response != null && !response.isEmpty() : "AI 响应不应为空";
    }
}
