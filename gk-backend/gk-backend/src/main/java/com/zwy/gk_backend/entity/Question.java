package com.zwy.gk_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

// 告诉管理员，这个类对应数据库里的 "question" 表
@TableName("question")
public class Question {

    // 告诉管理员这是主键，且是自增的
    @TableId(type = IdType.AUTO)
    private Integer id;
    private String category;
    private String title;
    private String optionsJson;
    private String answer;
    private String analysis;

    // 下面是 Java 必须的 Get 和 Set 方法（方便存取数据）
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getOptionsJson() { return optionsJson; }
    public void setOptionsJson(String optionsJson) { this.optionsJson = optionsJson; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getAnalysis() { return analysis; }
    public void setAnalysis(String analysis) { this.analysis = analysis; }
}