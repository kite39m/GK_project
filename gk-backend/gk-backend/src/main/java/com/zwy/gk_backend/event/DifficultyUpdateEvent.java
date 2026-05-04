package com.zwy.gk_backend.event;

import org.springframework.context.ApplicationEvent;

public class DifficultyUpdateEvent extends ApplicationEvent {
    private final Integer questionId;
    private final boolean isCorrect;
    private final String poolType; // "question" or "ai_question_pool"

    public DifficultyUpdateEvent(Object source, Integer questionId, boolean isCorrect, String poolType) {
        super(source);
        this.questionId = questionId;
        this.isCorrect = isCorrect;
        this.poolType = poolType;
    }

    public Integer getQuestionId() { return questionId; }
    public boolean isCorrect() { return isCorrect; }
    public String getPoolType() { return poolType; }
}
