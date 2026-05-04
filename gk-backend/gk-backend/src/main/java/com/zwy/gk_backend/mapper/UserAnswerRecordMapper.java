package com.zwy.gk_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zwy.gk_backend.entity.UserAnswerRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserAnswerRecordMapper extends BaseMapper<UserAnswerRecord> {

    @Select("SELECT module, " +
            "COUNT(*) AS total, " +
            "SUM(is_correct) AS correct, " +
            "ROUND(SUM(is_correct) * 100.0 / COUNT(*), 1) AS accuracy " +
            "FROM user_answer_records " +
            "WHERE user_id = #{userId} AND DATE(created_at) = CURDATE() " +
            "GROUP BY module")
    List<Map<String, Object>> getTodayBriefing(Integer userId);
}
