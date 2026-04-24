package com.zwy.gk_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zwy.gk_backend.entity.Question;
import com.zwy.gk_backend.entity.WrongQuestion;
import com.zwy.gk_backend.mapper.QuestionMapper;
import com.zwy.gk_backend.mapper.WrongQuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wrong-question")
@CrossOrigin // 依然放行跨域
public class WrongQuestionController {

    @Autowired
    private WrongQuestionMapper wrongQuestionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    // 🌟 1. 静默记录错题 (由前端答错时自动调用)
    @PostMapping("/record")
    public void recordWrongQuestion(@RequestParam Integer userId, @RequestParam Integer questionId) {
        QueryWrapper<WrongQuestion> query = new QueryWrapper<>();
        query.eq("user_id", userId).eq("question_id", questionId);
        WrongQuestion exist = wrongQuestionMapper.selectOne(query);

        if (exist != null) {
            // 题已经错过：增加错题权重，重置为“未掌握”状态
            exist.setWrongCount(exist.getWrongCount() + 1);
            exist.setLastWrongTime(new Date());
            exist.setStatus(0);
            wrongQuestionMapper.updateById(exist);
        } else {
            // 第一次错：新插入一条记录
            WrongQuestion wq = new WrongQuestion();
            wq.setUserId(userId);
            wq.setQuestionId(questionId);
            wq.setWrongCount(1);
            wq.setLastWrongTime(new Date());
            wq.setStatus(0);
            wrongQuestionMapper.insert(wq);
        }
    }

    // 🌟 2. 获取当前用户的专属错题本 (复习用)
    @GetMapping("/list")
    public List<Question> getMyWrongQuestions(@RequestParam Integer userId) {
        // 先查出当前用户所有 [未掌握] 的错题关联记录，按最近做错时间倒序
        QueryWrapper<WrongQuestion> query = new QueryWrapper<>();
        query.eq("user_id", userId).eq("status", 0).orderByDesc("last_wrong_time");
        List<WrongQuestion> wqList = wrongQuestionMapper.selectList(query);

        if(wqList.isEmpty()) return List.of();

        // 提取真正的 questionId 列表
        List<Integer> qIds = wqList.stream().map(WrongQuestion::getQuestionId).collect(Collectors.toList());

        // Mybatis-Plus 批量查询，一次查出所有题目详细内容，拒绝在 for 循环里查数据库！
        return questionMapper.selectBatchIds(qIds);
    }

    // 🌟 3. 斩杀错题 (复习时做对了，标记为已掌握)
    @PostMapping("/master")
    public void masterQuestion(@RequestParam Integer userId, @RequestParam Integer questionId) {
        QueryWrapper<WrongQuestion> query = new QueryWrapper<>();
        query.eq("user_id", userId).eq("question_id", questionId);
        WrongQuestion exist = wrongQuestionMapper.selectOne(query);
        if (exist != null) {
            exist.setStatus(1); // 标记为已斩杀
            wrongQuestionMapper.updateById(exist);
        }
    }
}