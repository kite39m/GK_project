package com.zwy.gk_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zwy.gk_backend.ai.tutor.ShenlunAiTutor;
import com.zwy.gk_backend.dto.ShenlunFeedback;
import com.zwy.gk_backend.entity.ShenlunEssay;
import com.zwy.gk_backend.mapper.ShenlunEssayMapper;
import com.zwy.gk_backend.service.ShenlunService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ShenlunServiceImpl implements ShenlunService {

    @Autowired
    private ShenlunEssayMapper essayMapper;

    @Autowired
    private ShenlunAiTutor shenlunAiTutor;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public ShenlunEssay submitEssay(Integer userId, String topic, String content) {
        String aiResponse = shenlunAiTutor.scoreEssay(content);

        ShenlunEssay essay = new ShenlunEssay();
        essay.setUserId(userId);
        essay.setTopic(topic);
        essay.setContent(content);

        try {
            ShenlunFeedback feedback = MAPPER.readValue(aiResponse, ShenlunFeedback.class);
            essay.setAiScore(feedback.getTotalScore());
            essay.setAiStructureScore(feedback.getStructureScore());
            essay.setAiArgumentScore(feedback.getArgumentScore());
            essay.setAiLanguageScore(feedback.getLanguageScore());
            essay.setAiFeedback(aiResponse);
        } catch (Exception e) {
            essay.setAiFeedback(aiResponse);
        }

        essayMapper.insert(essay);
        return essay;
    }

    @Override
    public IPage<ShenlunEssay> getEssayHistory(Integer userId, int page, int size) {
        QueryWrapper<ShenlunEssay> query = new QueryWrapper<>();
        query.eq("user_id", userId).orderByDesc("created_at");
        return essayMapper.selectPage(new Page<>(page, size), query);
    }
}
