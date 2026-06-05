-- AI 股票分析结果表
CREATE TABLE stock_ai_analysis (
    id INT PRIMARY KEY AUTO_INCREMENT,
    stock_code VARCHAR(20) NOT NULL COMMENT '股票代码',
    stock_name VARCHAR(50) COMMENT '股票名称',
    analysis_content TEXT COMMENT 'AI分析内容',
    analysis_date DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '分析时间',
    model_name VARCHAR(64) COMMENT '使用的模型',
    INDEX idx_stock_code (stock_code)
);