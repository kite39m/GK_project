package com.zwy.gk_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@TableName("user_knowledge_profiles")
@Data
public class UserKnowledgeProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    private String module;
    private String concept;
    private Integer proficiency;
    private Integer totalAttempts;
    private Integer correctCount;
    private Date lastPracticeTime;
    private Date nextReviewTime;
    private Date updatedAt;
}
