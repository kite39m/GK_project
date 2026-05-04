package com.zwy.gk_backend.listener;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zwy.gk_backend.entity.AiQuestion;
import com.zwy.gk_backend.entity.Question;
import com.zwy.gk_backend.entity.UserAnswerRecord;
import com.zwy.gk_backend.event.DifficultyUpdateEvent;
import com.zwy.gk_backend.mapper.AiQuestionMapper;
import com.zwy.gk_backend.mapper.QuestionMapper;
import com.zwy.gk_backend.mapper.UserAnswerRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class DifficultyUpdateListener {

    @Autowired
    private UserAnswerRecordMapper answerRecordMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private AiQuestionMapper aiQuestionMapper;

    @Async
    @EventListener
    public void onDifficultyUpdate(DifficultyUpdateEvent event) {
        Integer questionId = event.getQuestionId();

        // 统计该题的答题数据
        QueryWrapper<UserAnswerRecord> query = new QueryWrapper<>();
        query.eq("question_id", questionId);
        long total = answerRecordMapper.selectCount(query);
        if (total < 10) return; // 不足10次不更新

        query.eq("is_correct", true);
        long correct = answerRecordMapper.selectCount(query);
        double correctRate = (double) correct / total;

        int difficulty;
        if (correctRate > 0.8) difficulty = 1;
        else if (correctRate > 0.6) difficulty = 2;
        else if (correctRate > 0.4) difficulty = 3;
        else if (correctRate > 0.2) difficulty = 4;
        else difficulty = 5;

        if ("ai_question_pool".equals(event.getPoolType())) {
            AiQuestion aiq = aiQuestionMapper.selectById(questionId);
            if (aiq != null) {
                aiq.setDifficulty(difficulty);
                aiQuestionMapper.updateById(aiq);
            }
        } else {
            Question q = questionMapper.selectById(questionId);
            if (q != null) {
                q.setDifficulty(difficulty);
                questionMapper.updateById(q);
            }
        }
    }
}
