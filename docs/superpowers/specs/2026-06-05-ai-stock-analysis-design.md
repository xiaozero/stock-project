# AI 股票分析功能设计

**日期:** 2026-06-05
**状态:** 已批准

---

## 1. 需求概述

在自选股票列表中新增"AI分析"按钮，点击后调用大模型对股票进行技术面分析，并将分析结果保存到数据库。

---

## 2. 技术架构

### 2.1 整体流程

```
前端(StocksView)
    ↓ 点击"AI分析"按钮
后端(StockInfoController)
    ↓ 调用
Python 脚本 (stock_analysis.py)
    ↓ 调用 a-stock-data skill 获取数据
返回股票数据(实时行情、技术指标等)
    ↓
Java 后端调用 Ollama 大模型分析
    ↓
保存分析结果到 stock_ai_analysis 表
    ↓
返回前端展示
```

### 2.2 技术选型

- **数据获取:** Python + a-stock-data skill (A股数据分析工具包)
- **AI 分析:** Ollama 本地大模型 (qwen3.5 或 deepseek-r1)
- **存储:** MySQL 数据库

---

## 3. 数据表设计

### 3.1 stock_ai_analysis 表

```sql
CREATE TABLE stock_ai_analysis (
    id INT PRIMARY KEY AUTO_INCREMENT,
    stock_code VARCHAR(20) NOT NULL COMMENT '股票代码',
    stock_name VARCHAR(50) COMMENT '股票名称',
    analysis_content TEXT COMMENT 'AI分析内容',
    analysis_date DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '分析时间',
    model_name VARCHAR(64) COMMENT '使用的模型',
    INDEX idx_stock_code (stock_code)
);
```

---

## 4. 实施步骤

### Step 1: 前端 - 新增"AI分析"按钮

**文件:** `src/views/StocksView.vue`

- 在表格最后新增一列操作列
- 新增"AI分析"按钮，点击调用分析接口
- 分析完成后展示结果（可使用 el-dialog 弹窗）

### Step 2: 后端 - 创建实体类和 Mapper

**文件:**
- `src/main/java/com/xiao/stockproject/entity/StockAiAnalysis.java`
- `src/main/java/com/xiao/stockproject/mapper/StockAiAnalysisMapper.java`

**实体类字段:**
| 字段 | 类型 | 说明 |
|------|------|------|
| id | Integer | 主键，自增 |
| stockCode | String | 股票代码 |
| stockName | String | 股票名称 |
| analysisContent | String | AI分析内容 |
| analysisDate | LocalDateTime | 分析时间 |
| modelName | String | 使用的模型 |

### Step 3: 后端 - 新增 Controller 接口

**文件:** `src/main/java/com/xiao/stockproject/controller/StockInfoController.java`

**接口:**
```
POST /api/stock/ai-analyze
请求体: { "stockCode": "600519", "stockName": "贵州茅台" }
响应: { "code": 200, "result": { "id": 1, "analysisContent": "..." } }
```

### Step 4: Python 脚本 - 获取股票数据

**文件:** `scripts/stock_analysis.py`

调用 a-stock-data skill 获取:
- 腾讯财经实时行情 (PE/PB/市值/换手率)
- 百度股市通 K线带MA5/MA10/MA20
- 技术指标计算 (RSI/MACD 等)

### Step 5: 后端 - 调用 Ollama 大模型分析

**文件:** `src/main/java/com/xiao/stockproject/utils/OllamaClient.java`

- 复用现有的 chatStream 方法
- 将股票数据格式化为 prompt
- 调用大模型生成分析报告

### Step 6: 保存分析结果

- 插入 `stock_ai_analysis` 表
- 返回分析结果给前端

---

## 5. API 设计

### 5.1 AI 分析接口

```
POST /api/stock/ai-analyze
Content-Type: application/json

请求体:
{
    "stockCode": "600519",
    "stockName": "贵州茅台"
}

响应:
{
    "code": 200,
    "msg": "success",
    "result": {
        "id": 1,
        "stockCode": "600519",
        "stockName": "贵州茅台",
        "analysisContent": "根据技术分析，该股票当前价格为xxx元，涨幅xx%...",
        "analysisDate": "2026-06-05T10:30:00",
        "modelName": "qwen3.5:0.8b"
    }
}
```

### 5.2 查询历史分析记录

```
GET /api/stock/ai-analyze/{stockCode}

响应:
{
    "code": 200,
    "result": [
        {
            "id": 1,
            "stockCode": "600519",
            "stockName": "贵州茅台",
            "analysisContent": "...",
            "analysisDate": "2026-06-05T10:30:00",
            "modelName": "qwen3.5:0.8b"
        }
    ]
}
```

---

## 6. 前端组件设计

### 6.1 StocksView.vue 修改

- 表格新增"操作"列
- "AI分析"按钮点击后显示加载状态
- 分析完成后弹窗展示结果
- 可增加"查看历史"按钮

### 6.2 结果展示弹窗

使用 el-dialog 组件展示 AI 分析结果，包含:
- 股票名称和代码
- 分析内容（支持 Markdown 渲染）
- 分析时间
- 使用的模型名称

---

## 7. 异常处理

| 场景 | 处理方式 |
|------|---------|
| Python 脚本执行失败 | 返回错误信息，提示检查 a-stock-data skill |
| Ollama 服务不可用 | 返回错误信息，提示启动 Ollama |
| 分析超时 | 设置 60 秒超时，超时后返回提示 |
| 股票代码无效 | 返回"未找到该股票信息" |

---

## 8. 待创建文件清单

1. `src/main/java/com/xiao/stockproject/entity/StockAiAnalysis.java`
2. `src/main/java/com/xiao/stockproject/mapper/StockAiAnalysisMapper.java`
3. `src/main/java/com/xiao/stockproject/service/StockAiAnalysisService.java`
4. `src/main/java/com/xiao/stockproject/service/impl/StockAiAnalysisServiceImpl.java`
5. `scripts/stock_analysis.py`
6. 修改 `src/views/StocksView.vue`
7. 修改 `src/main/java/com/xiao/stockproject/controller/StockInfoController.java`
8. 新增 `sql/stock_ai_analysis.sql`