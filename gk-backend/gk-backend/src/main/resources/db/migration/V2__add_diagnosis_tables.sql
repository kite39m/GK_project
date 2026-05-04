-- ============================================================
-- V2: 公考AI智能诊断模块 - 数据库表
-- ============================================================

-- 1. 扩展 question 表：新增 module 字段
ALTER TABLE question ADD COLUMN module VARCHAR(20) DEFAULT NULL COMMENT '模块编码: CHANGSHI/YUYU/ZILIAO/TUILI/SHULIANG';
ALTER TABLE question ADD INDEX idx_module (module);

-- 2. 用户答题记录明细表
CREATE TABLE IF NOT EXISTS user_answer_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id INT NOT NULL COMMENT '用户ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    module VARCHAR(20) NOT NULL COMMENT '模块: CHANGSHI/YUYU/ZILIAO/TUILI/SHULIANG/SHENLUN',
    concept VARCHAR(50) COMMENT '细分考点(如: 基期量计算/主旨概括)',
    is_correct TINYINT(1) NOT NULL COMMENT '是否正确 0:错 1:对',
    user_answer VARCHAR(500) COMMENT '用户提交的答案',
    time_cost_sec INT COMMENT '答题耗时(秒)',
    is_trap_option TINYINT(1) DEFAULT 0 COMMENT '是否选中高频陷阱项 0:否 1:是',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '答题时间',
    INDEX idx_user_module (user_id, module),
    INDEX idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户答题记录明细表';

-- 3. 用户知识点动态画像表
CREATE TABLE IF NOT EXISTS user_knowledge_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id INT NOT NULL COMMENT '用户ID',
    module VARCHAR(20) NOT NULL COMMENT '模块编码',
    concept VARCHAR(50) NOT NULL COMMENT '细分考点',
    proficiency INT DEFAULT 50 COMMENT '熟练度(0-100)',
    total_attempts INT DEFAULT 0 COMMENT '总练习次数',
    correct_count INT DEFAULT 0 COMMENT '正确次数',
    last_practice_time DATETIME COMMENT '上次练习时间',
    next_review_time DATETIME COMMENT '下次复习时间(艾宾浩斯)',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_user_concept (user_id, module, concept)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户知识点动态画像表';

-- 4. 申论作文提交与评分表
CREATE TABLE IF NOT EXISTS shenlun_essays (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id INT NOT NULL COMMENT '用户ID',
    topic VARCHAR(200) NOT NULL COMMENT '作文题目',
    content TEXT NOT NULL COMMENT '用户提交的作文内容',
    ai_score INT COMMENT 'AI综合评分(0-100)',
    ai_structure_score INT COMMENT '结构评分',
    ai_argument_score INT COMMENT '论点评分',
    ai_language_score INT COMMENT '语言评分',
    ai_feedback TEXT COMMENT 'AI诊断反馈(JSON)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申论作文提交与评分表';
