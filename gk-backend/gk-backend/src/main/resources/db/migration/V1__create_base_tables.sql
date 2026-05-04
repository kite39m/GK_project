-- ============================================================
-- V1: 基础表 - 题库、错题本、会话
-- ============================================================

-- 1. 题库表
CREATE TABLE IF NOT EXISTS question (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    category VARCHAR(50) COMMENT '分类',
    title TEXT NOT NULL COMMENT '题目内容',
    options_json TEXT COMMENT '选项JSON',
    answer VARCHAR(10) COMMENT '正确答案',
    analysis TEXT COMMENT '解析',
    module VARCHAR(20) COMMENT '模块编码: CHANGSHI/YUYU/ZILIAO/TUILI/SHULIANG',
    INDEX idx_module (module)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库表';

-- 2. 错题本表
CREATE TABLE IF NOT EXISTS wrong_question (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id INT NOT NULL COMMENT '用户ID',
    question_id INT NOT NULL COMMENT '题目ID',
    wrong_count INT DEFAULT 1 COMMENT '错误次数',
    last_wrong_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最近错误时间',
    status INT DEFAULT 0 COMMENT '状态 0:未掌握 1:已斩杀',
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本表';

-- 3. 会话表
CREATE TABLE IF NOT EXISTS chat_session (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    title VARCHAR(200) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表';

-- 4. 消息表
CREATE TABLE IF NOT EXISTS chat_message (
    id INT AUTO_INCREMENT PRIMARY KEY,
    session_id INT NOT NULL,
    role VARCHAR(20) NOT NULL COMMENT 'user 或 assistant',
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表';
