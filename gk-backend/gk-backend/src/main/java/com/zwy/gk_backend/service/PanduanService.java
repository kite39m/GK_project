package com.zwy.gk_backend.service;

public interface PanduanService {

    /**
     * 与判断推理辅导 Agent 对话
     * @param userId 用户ID
     * @param message 用户消息
     * @return Agent 回复
     */
    String chat(int userId, String message);
}
