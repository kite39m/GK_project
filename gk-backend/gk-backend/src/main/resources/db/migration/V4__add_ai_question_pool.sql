-- V4: AI题目缓冲池
CREATE TABLE IF NOT EXISTS ai_question_pool (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    module VARCHAR(20) NOT NULL COMMENT '模块: CHANGSHI/YUYU/ZILIAO/TUILI/SHULIANG',
    category VARCHAR(50) COMMENT '细分考点',
    title TEXT NOT NULL COMMENT '题目正文',
    optionsJson VARCHAR(1000) NOT NULL COMMENT '选项JSON',
    answer VARCHAR(10) NOT NULL COMMENT '正确答案',
    analysis TEXT COMMENT '常规解析',
    concept VARCHAR(50) COMMENT '知识点概念',
    trapOption VARCHAR(10) COMMENT '陷阱选项标识',
    trapAnalysisTemplate TEXT COMMENT '预存陷阱解析模板',
    questionHash VARCHAR(64) COMMENT '题目内容哈希(MD5)，用于去重',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/ACTIVE/REJECTED',
    sourceType VARCHAR(20) DEFAULT 'AI_GEN' COMMENT '来源: AI_GEN',
    checkLog VARCHAR(500) COMMENT '校验日志',
    createdAt DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE INDEX uk_hash (questionHash),
    INDEX idx_module_status (module, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI题目缓冲池';
