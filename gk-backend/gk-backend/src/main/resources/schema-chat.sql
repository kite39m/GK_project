-- 会话表
CREATE TABLE IF NOT EXISTS chat_session (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    user_id    INT          NOT NULL,
    title      VARCHAR(200) NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 消息表
CREATE TABLE IF NOT EXISTS chat_message (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    session_id INT          NOT NULL,
    role       VARCHAR(20)  NOT NULL COMMENT 'user 或 assistant',
    content    TEXT         NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id)
);
