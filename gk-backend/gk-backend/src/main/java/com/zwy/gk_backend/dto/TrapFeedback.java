package com.zwy.gk_backend.dto;

import lombok.Data;

@Data
public class TrapFeedback {
    private boolean isTrap;
    private String trapAnalysis;
    private int trapCount;
    private String concept;
}
