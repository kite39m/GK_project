package com.zwy.gk_backend.service;

public interface KnowledgeProfileService {

    /**
     * 答题后更新知识点画像
     * @param userId 用户ID
     * @param module 模块编码
     * @param concept 考点
     * @param isCorrect 是否正确
     */
    void updateAfterAnswer(Integer userId, String module, String concept, boolean isCorrect);
}
