package com.zwy.gk_backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

@TableName("wrong_question")
public class WrongQuestion {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer userId;
    private Integer questionId;
    private Integer wrongCount;
    private Date lastWrongTime;
    private Integer status; // 0:未掌握, 1:已斩杀

    // 省略 Getter 和 Setter (建议后续项目引入 Lombok，使用 @Data 注解干掉这些样板代码)
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Integer getQuestionId() { return questionId; }
    public void setQuestionId(Integer questionId) { this.questionId = questionId; }
    public Integer getWrongCount() { return wrongCount; }
    public void setWrongCount(Integer wrongCount) { this.wrongCount = wrongCount; }
    public Date getLastWrongTime() { return lastWrongTime; }
    public void setLastWrongTime(Date lastWrongTime) { this.lastWrongTime = lastWrongTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}