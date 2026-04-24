package com.zwy.gk_backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zwy.gk_backend.entity.WrongQuestion;
import com.zwy.gk_backend.mapper.QuestionMapper;
import com.zwy.gk_backend.mapper.WrongQuestionMapper;
import com.zwy.gk_backend.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@CrossOrigin
public class StatisticsController {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private WrongQuestionMapper wrongQuestionMapper;

    @GetMapping("/summary")
    public Map<String, Object> getSummary(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        String token = authHeader.substring(7);
        Claims claims = jwtUtils.parseToken(token);
        Integer userId = claims.get("userId", Integer.class);

        // 1. 总题量
        Long totalQuestions = questionMapper.selectCount(null);

        // 2. 正确率 = 已掌握错题 / 总错题数
        long notMastered = wrongQuestionMapper.selectCount(
                new QueryWrapper<WrongQuestion>().eq("user_id", userId).eq("status", 0));
        long mastered = wrongQuestionMapper.selectCount(
                new QueryWrapper<WrongQuestion>().eq("user_id", userId).eq("status", 1));
        long totalWrong = notMastered + mastered;
        double accuracy = totalWrong > 0 ? Math.round(mastered * 1000.0 / totalWrong) / 10.0 : 100.0;

        // 3. 平均用时（暂未持久化，返回 0 占位）
        int avgTime = 0;

        // 4. 各分类统计 → 映射到 5 维度熟练度
        List<Map<String, Object>> categoryTotals = questionMapper.countGroupByCategory();
        List<Map<String, Object>> categoryWrongs = wrongQuestionMapper.countWrongByCategory(userId);

        Map<String, Integer> wrongMap = new HashMap<>();
        for (Map<String, Object> row : categoryWrongs) {
            wrongMap.put((String) row.get("category"), ((Number) row.get("wrong_cnt")).intValue());
        }

        // 5 个维度：言语理解、数量关系、判断推理、资料分析、常识判断
        int[] dimTotal = new int[5];
        int[] dimWrong = new int[5];

        for (Map<String, Object> row : categoryTotals) {
            String category = (String) row.get("category");
            int total = ((Number) row.get("total")).intValue();
            int wrong = wrongMap.getOrDefault(category, 0);
            int idx = mapCategoryToDimension(category);
            dimTotal[idx] += total;
            dimWrong[idx] += wrong;
        }

        int[] skillValues = new int[5];
        for (int i = 0; i < 5; i++) {
            skillValues[i] = dimTotal[i] > 0
                    ? (int) Math.round(Math.max(0, 100 - dimWrong[i] * 100.0 / dimTotal[i]))
                    : 100;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalQuestions", totalQuestions);
        result.put("accuracy", accuracy);
        result.put("avgTime", avgTime);
        result.put("skillValues", skillValues);
        return result;
    }

    private int mapCategoryToDimension(String category) {
        if (category.contains("言语")) return 0;
        if (category.contains("数量") || category.contains("速算") || category.contains("截位") || category.contains("基期"))
            return 1;
        if (category.contains("判断")) return 2;
        if (category.contains("资料")) return 3;
        if (category.contains("常识") || category.contains("申论") || category.contains("金句")) return 4;
        return 3;
    }
}
