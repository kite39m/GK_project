package com.zwy.gk_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zwy.gk_backend.entity.WrongQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface WrongQuestionMapper extends BaseMapper<WrongQuestion> {

    @Select("SELECT q.category, COUNT(DISTINCT wq.question_id) AS wrong_cnt " +
            "FROM wrong_question wq " +
            "JOIN question q ON wq.question_id = q.id " +
            "WHERE wq.user_id = #{userId} AND wq.status = 0 " +
            "GROUP BY q.category")
    List<Map<String, Object>> countWrongByCategory(@Param("userId") Integer userId);
}