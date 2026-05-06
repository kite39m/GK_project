package com.zwy.gk_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@TableName("question_image")
@Data
public class QuestionImage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String url;
    private String contentHash;
    private String sourcePage;
    private String imageType;
    private String format;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private String status;
    private Date createdAt;
}
