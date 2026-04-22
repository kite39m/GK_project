package com.zwy.gk_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zwy.gk_backend.entity.Question;
import org.apache.ibatis.annotations.Mapper;

// @Mapper 注解把这个接口交给 Spring 管理
// 继承了 BaseMapper，MyBatis-Plus 就自动帮你写好了增删改查的所有基础代码！
@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}