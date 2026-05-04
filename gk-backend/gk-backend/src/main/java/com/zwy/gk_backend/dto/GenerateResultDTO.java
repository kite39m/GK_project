package com.zwy.gk_backend.dto;

import com.zwy.gk_backend.entity.Question;
import lombok.Data;
import java.util.List;

@Data
public class GenerateResultDTO {
    private List<Question> questions;
    private boolean fromCache;
    private int totalInPool;
}
