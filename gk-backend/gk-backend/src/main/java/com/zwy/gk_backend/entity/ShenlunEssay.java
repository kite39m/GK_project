package com.zwy.gk_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@TableName("shenlun_essays")
@Data
public class ShenlunEssay {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Integer userId;
    private String topic;
    private String content;
    private Integer aiScore;
    private Integer aiStructureScore;
    private Integer aiArgumentScore;
    private Integer aiLanguageScore;
    private String aiFeedback;
    private Date createdAt;
}
