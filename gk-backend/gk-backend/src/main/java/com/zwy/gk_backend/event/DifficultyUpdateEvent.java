package com.zwy.gk_backend.event;

import org.springframework.context.ApplicationEvent;

public class DifficultyUpdateEvent extends ApplicationEvent {
    private final Long questionId;
    private final boolean isCorrect;
    private final String poolType; // "question" or "ai_question_pool"

    public DifficultyUpdateEvent(Object source, Long questionId, boolean isCorrect, String poolType) {
        super(source);
        this.questionId = questionId;
        this.isCorrect = isCorrect;
        this.poolType = poolType;
    }

    public Long getQuestionId() { return questionId; }
    public boolean isCorrect() { return isCorrect; }
    public String getPoolType() { return poolType; }
}
