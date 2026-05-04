package com.zwy.gk_backend.dto;

import lombok.Data;

@Data
public class ShenlunFeedback {
    private Integer structureScore;
    private Integer argumentScore;
    private Integer languageScore;
    private Integer totalScore;
    private String feedback;
}
