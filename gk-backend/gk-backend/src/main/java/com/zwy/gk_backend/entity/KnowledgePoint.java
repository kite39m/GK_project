package com.zwy.gk_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@TableName("knowledge_point")
@Data
public class KnowledgePoint {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String module;
    private String category;
    private String concept;
    private String description;
    private Integer sortOrder;
}
