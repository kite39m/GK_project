package com.zwy.gk_backend.dto;

import lombok.Data;

/**
 * AI 速算题生成响应 DTO
 * @author 赵文阳
 */
@Data
public class AiSpeedCalcResponseDTO {

    /** 速算题干 (例如: 12345 / (1 + 5.2%)) */
    private String question;

    /** 精确答案 */
    private String exactAnswer;

    /** 速算技巧名称 */
    private String fastCalcSkill;

    /** 解析步骤 */
    private String analysis;
}
