package com.zwy.gk_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@TableName("user_answer_records")
@Data
public class UserAnswerRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    private Long questionId;
    private String module;
    private String concept;
    private Boolean isCorrect;
    private String userAnswer;
    private Integer timeCostSec;
    private Boolean isTrapOption;
    private Date createdAt;
}
