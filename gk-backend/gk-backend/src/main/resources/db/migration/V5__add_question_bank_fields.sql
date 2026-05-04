-- ============================================================
-- V5: 题库体系扩展 - 难度系数 + 知识点标签
-- ============================================================

-- 1. question 表添加难度字段
ALTER TABLE question ADD COLUMN difficulty INT DEFAULT 3 COMMENT '难度1-5';
ALTER TABLE question ADD COLUMN source VARCHAR(20) DEFAULT 'SEED' COMMENT '来源: SEED/REAL_EXAM/AI_EXPAND/AI_GEN';

-- 2. ai_question_pool 表添加难度和关联字段
ALTER TABLE ai_question_pool ADD COLUMN difficulty INT DEFAULT 3 COMMENT '难度1-5';
ALTER TABLE ai_question_pool ADD COLUMN parent_id BIGINT DEFAULT NULL COMMENT '母题ID(AI扩写用)';

-- 3. 知识点标签表
CREATE TABLE IF NOT EXISTS knowledge_point (
    id INT AUTO_INCREMENT PRIMARY KEY,
    module VARCHAR(20) NOT NULL COMMENT '模块编码',
    category VARCHAR(50) NOT NULL COMMENT '大考点',
    concept VARCHAR(50) NOT NULL COMMENT '细分考点',
    description VARCHAR(200) COMMENT '考点说明',
    sort_order INT DEFAULT 0 COMMENT '排序',
    UNIQUE INDEX uk_concept (module, category, concept)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识点标签表';

-- 4. 种子知识点数据
INSERT IGNORE INTO knowledge_point (module, category, concept, description, sort_order) VALUES
-- 常识
('CHANGSHI', '政治', '政治制度', '根本政治制度、基本政治制度', 1),
('CHANGSHI', '经济', '宏观经济', 'GDP、CPI、通货膨胀等', 2),
('CHANGSHI', '法律', '宪法', '宪法基本条款', 3),
('CHANGSHI', '科技', '科技常识', '科技成就、信息技术', 4),
('CHANGSHI', '历史', '中国近现代史', '重要历史事件', 5),
-- 言语理解
('YUYU', '主旨概括', '因果结构', '因...因...因此...结构', 1),
('YUYU', '主旨概括', '转折结构', '虽然...但是...结构', 2),
('YUYU', '主旨概括', '并列结构', '一方面...另一方面...结构', 3),
('YUYU', '细节判断', '细节查找', '根据材料查找对应信息', 4),
('YUYU', '语句排序', '逻辑排序', '按逻辑顺序排列语句', 5),
('YUYU', '逻辑填空', '词语辨析', '近义词、成语辨析', 6),
-- 资料分析
('ZILIAO', '增长率', '同比增长率', '与去年同期相比的增长率', 1),
('ZILIAO', '增长率', '环比增长率', '与上一期相比的增长率', 2),
('ZILIAO', '比重', '现期比重', '当前时期部分占整体的比例', 3),
('ZILIAO', '比重', '基期比重', '上一时期部分占整体的比例', 4),
('ZILIAO', '基期量', '基期量计算', '已知现期量和增长率求基期', 5),
('ZILIAO', '倍数', '倍数关系', 'A是B的几倍', 6),
('ZILIAO', '增长量', '增长量计算', '已知现期量和增长率求增长量', 7),
-- 推理判断
('TUILI', '逻辑判断', '充分必要条件', '如果...那么...逆否命题', 1),
('TUILI', '逻辑判断', '三段论', '大前提+小前提→结论', 2),
('TUILI', '定义判断', '概念匹配', '根据定义判断选项是否符合', 3),
('TUILI', '类比推理', '语义关系', '词语之间的逻辑关系', 4),
('TUILI', '图形推理', '数列规律', '数字序列的规律', 5),
-- 数量关系
('SHULIANG', '工程问题', '合作效率', '多人合作完成工程', 1),
('SHULIANG', '行程问题', '相遇问题', '相向而行相遇时间', 2),
('SHULIANG', '排列组合', '排列', '有序选取', 3),
('SHULIANG', '利润问题', '利润率', '进价、售价、利润率计算', 4),
('SHULIANG', '容斥原理', '两集合容斥', 'A∪B = A + B - A∩B', 5);
