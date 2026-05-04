package com.zwy.gk_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@TableName("ai_question_pool")
@Data
public class AiQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String module;
    private String category;
    private String title;
    private String optionsJson;
    private String answer;
    private String analysis;
    private String concept;
    private String trapOption;
    private String trapAnalysisTemplate;
    private String questionHash;
    private String status;
    private String sourceType;
    private String checkLog;
    private Date createdAt;
}
