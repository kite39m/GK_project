-- ============================================================
-- V6: 题目图片缓存表
-- ============================================================

CREATE TABLE IF NOT EXISTS question_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    url VARCHAR(500) NOT NULL COMMENT '图片URL',
    content_hash VARCHAR(64) NOT NULL COMMENT '图片内容哈希（用于去重）',
    source_page VARCHAR(500) COMMENT '来源页面URL',
    image_type VARCHAR(20) DEFAULT 'OPTION' COMMENT '图片类型: OPTION/ANALYSIS/TITLE',
    format VARCHAR(10) COMMENT '图片格式: jpg/png/gif',
    file_size BIGINT COMMENT '文件大小（字节）',
    width INT COMMENT '图片宽度',
    height INT COMMENT '图片高度',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INVALID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE INDEX uk_hash (content_hash),
    INDEX idx_source (source_page)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目图片缓存表';
