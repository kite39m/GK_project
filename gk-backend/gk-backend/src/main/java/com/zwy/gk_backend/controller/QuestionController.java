package com.zwy.gk_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zwy.gk_backend.entity.Question;
import com.zwy.gk_backend.mapper.QuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/question")
@CrossOrigin
public class QuestionController {

    @Autowired
    private QuestionMapper questionMapper;

    // 保留原有的全量查询接口（用于前端管理页面查看所有题目）
    @GetMapping("/list")
    public List<Question> getQuestionList() {
        return questionMapper.selectList(null);
    }

    // 🔥 新增 1：专项训练接口 (每次固定抽某个模块的10道题)
    // 浏览器/Vue访问示例：/api/question/category?name=截位直除-强差距
    @GetMapping("/category")
    public List<Question> getByCategory(@RequestParam("name") String categoryName) {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        // 对应数据库中的 category 字段
        queryWrapper.eq("category", categoryName);
        return questionMapper.selectList(queryWrapper);
    }

    // 🔥 新增 2：综合模考接口 (每次从100道题库中随机抽取10道)
    // 浏览器/Vue访问示例：/api/question/random-ten
    @GetMapping("/random-ten")
    public List<Question> getRandomTen() {
        QueryWrapper<Question> queryWrapper = new QueryWrapper<>();
        // 利用 MySQL 的 RAND() 函数随机打乱，并限制只取 10 条
        queryWrapper.last("ORDER BY RAND() LIMIT 10");
        return questionMapper.selectList(queryWrapper);
    }
}