package com.zwy.gk_backend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zwy.gk_backend.entity.UserKnowledgeProfile;
import com.zwy.gk_backend.mapper.UserKnowledgeProfileMapper;
import com.zwy.gk_backend.service.KnowledgeProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

@Service
public class KnowledgeProfileServiceImpl implements KnowledgeProfileService {

    @Autowired
    private UserKnowledgeProfileMapper profileMapper;

    @Override
    public void updateAfterAnswer(Integer userId, String module, String concept, boolean isCorrect) {
        QueryWrapper<UserKnowledgeProfile> query = new QueryWrapper<>();
        query.eq("user_id", userId).eq("module", module).eq("concept", concept);
        UserKnowledgeProfile profile = profileMapper.selectOne(query);

        if (profile == null) {
            profile = new UserKnowledgeProfile();
            profile.setUserId(userId);
            profile.setModule(module);
            profile.setConcept(concept);
            profile.setProficiency(50);
            profile.setTotalAttempts(0);
            profile.setCorrectCount(0);
        }

        profile.setTotalAttempts(profile.getTotalAttempts() + 1);
        if (isCorrect) {
            profile.setCorrectCount(profile.getCorrectCount() + 1);
            profile.setProficiency(Math.min(100, profile.getProficiency() + 10));
        } else {
            profile.setProficiency(Math.max(0, profile.getProficiency() - 15));
        }

        Date now = new Date();
        profile.setLastPracticeTime(now);
        profile.setNextReviewTime(calculateNextReviewTime(profile.getProficiency()));

        if (profile.getId() == null) {
            profileMapper.insert(profile);
        } else {
            profileMapper.updateById(profile);
        }
    }

    private Date calculateNextReviewTime(int proficiency) {
        Calendar cal = Calendar.getInstance();
        int daysToAdd;
        if (proficiency < 30) {
            daysToAdd = 1;
        } else if (proficiency < 60) {
            daysToAdd = 3;
        } else if (proficiency < 80) {
            daysToAdd = 7;
        } else {
            daysToAdd = 15;
        }
        cal.add(Calendar.DAY_OF_MONTH, daysToAdd);
        return cal.getTime();
    }
}
