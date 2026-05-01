package com.zwy.gk_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zwy.gk_backend.entity.Question;
import com.zwy.gk_backend.entity.WrongQuestion;
import com.zwy.gk_backend.mapper.QuestionMapper;
import com.zwy.gk_backend.mapper.WrongQuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
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

    /**
     * @author 赵文阳
     * TODO: 阶段 3 核心功能 - 触发多 Agent 错题推演诊断
     * 接收用户的错题ID和草稿步骤，调用大模型 API 进行 Chain of Thought (CoT) 分析
     */
    @PostMapping("/diagnose/{questionId}")
    public ResponseEntity<String> diagnoseWrongQuestion(@PathVariable Integer questionId, @RequestBody Map<String, String> draftData) {
        // 1. 获取题目详情 (QuestionMapper)
        // Question question = questionMapper.selectById(questionId);
        
        // 2. 组装 Prompt Payload
        String userDraft = draftData.get("draftSteps");
        
        // --- Agent 工作流 (Stub 存根，待接入 MiMo API) ---
        // DiagnosticAgent agent = new DiagnosticAgent(mimoApiClient);
        // AgentResponse response = agent.analyzeCoT(question.getContent(), userDraft);
        
        // 3. 记录分析日志至 task_log 表
        // taskLogMapper.insertLog("DIAGNOSE", questionId, response.getTraceId());
        
        return ResponseEntity.ok("Agent diagnostic workflow initiated. Pending API integration.");
    }
}