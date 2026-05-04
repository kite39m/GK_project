package com.zwy.gk_backend.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class QuestionGenerateEvent extends ApplicationEvent {
    private final Integer userId;
    private final String module;
    private final String moduleName;
    private final int count;

    public QuestionGenerateEvent(Object source, Integer userId, String module, String moduleName, int count) {
        super(source);
        this.userId = userId;
        this.module = module;
        this.moduleName = moduleName;
        this.count = count;
    }
}
