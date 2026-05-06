package com.zwy.gk_backend.ai.tutor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PanduanTutorTest {

    @Autowired
    private PanduanTutor panduanTutor;

    @Test
    void testDiagnose() {
        String response = panduanTutor.tutor("用户ID: 1\n用户消息: 帮我诊断在判断推理模块的错题");
        assertNotNull(response);
        assertFalse(response.isEmpty());
        System.out.println("诊断结果: " + response);
    }

    @Test
    void testGenerateQuestions() {
        String response = panduanTutor.tutor("用户ID: 1\n用户消息: 根据我的水平，生成一些判断推理练习题");
        assertNotNull(response);
        assertFalse(response.isEmpty());
        System.out.println("生成题目: " + response);
    }

    @Test
    void testGuide() {
        String response = panduanTutor.tutor("用户ID: 1\n用户消息: 讲解判断推理的解题技巧和策略");
        assertNotNull(response);
        assertFalse(response.isEmpty());
        System.out.println("解题指导: " + response);
    }

    @Test
    void testProgress() {
        String response = panduanTutor.tutor("用户ID: 1\n用户消息: 评估我在判断推理模块的进步情况");
        assertNotNull(response);
        assertFalse(response.isEmpty());
        System.out.println("进步评估: " + response);
    }
}
