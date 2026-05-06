package com.zwy.gk_backend.service.impl;

import com.zwy.gk_backend.ai.tutor.PanduanTutor;
import com.zwy.gk_backend.service.PanduanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PanduanServiceImpl implements PanduanService {

    private final PanduanTutor panduanTutor;

    @Override
    public String chat(int userId, String message) {
        log.info("用户{}发起判断推理辅导对话: {}", userId, message);

        // 将用户ID和消息组合，传递给 Agent
        String prompt = String.format("用户ID: %d\n用户消息: %s", userId, message);

        String response = panduanTutor.tutor(prompt);
        log.info("判断推理辅导Agent回复: {}", response);

        return response;
    }
}
