# AI 股票分析功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在自选股票列表新增"AI分析"按钮，点击后调用大模型对股票进行技术面分析，并将分析结果保存到数据库。

**Architecture:** 混合架构 - Python 脚本调用 a-stock-data skill 获取股票数据，Java 后端调用 Ollama 大模型分析，MySQL 存储结果。

**Tech Stack:** Java (Spring Boot) + Python (a-stock-data skill) + Ollama + MySQL

---

## 文件结构

| 文件 | 职责 |
|------|------|
| `src/main/java/.../entity/StockAiAnalysis.java` | AI分析结果实体类 |
| `src/main/java/.../mapper/StockAiAnalysisMapper.java` | Mapper接口 |
| `src/main/java/.../service/StockAiAnalysisService.java` | Service接口 |
| `src/main/java/.../service/impl/StockAiAnalysisServiceImpl.java` | Service实现 |
| `src/main/java/.../controller/StockInfoController.java` | 新增AI分析接口 |
| `scripts/stock_analysis.py` | Python脚本获取股票数据 |
| `src/main/java/.../utils/StockAnalysisClient.java` | 调用Python脚本的工具类 |
| `src/views/StocksView.vue` | 前端新增AI分析按钮 |
| `sql/stock_ai_analysis.sql` | 建表SQL |

---

## Task 1: 创建数据库表

**Files:**
- Create: `sql/stock_ai_analysis.sql`

- [ ] **Step 1: 创建建表SQL文件**

```sql
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
```

- [ ] **Step 2: 提交**

```bash
git add sql/stock_ai_analysis.sql
git commit -m "feat: add stock_ai_analysis table SQL"
```

---

## Task 2: 创建实体类和 Mapper

**Files:**
- Create: `src/main/java/com/xiao/stockproject/entity/StockAiAnalysis.java`
- Create: `src/main/java/com/xiao/stockproject/mapper/StockAiAnalysisMapper.java`

- [ ] **Step 1: 创建 StockAiAnalysis 实体类**

```java
package com.xiao.stockproject.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@TableName(value = "stock_ai_analysis")
@Data
public class StockAiAnalysis {
    @TableId(type = IdType.AUTO)
    private Integer id;
    
    private String stockCode;
    private String stockName;
    private String analysisContent;
    private LocalDateTime analysisDate;
    private String modelName;
}
```

- [ ] **Step 2: 创建 StockAiAnalysisMapper**

```java
package com.xiao.stockproject.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiao.stockproject.entity.StockAiAnalysis;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockAiAnalysisMapper extends BaseMapper<StockAiAnalysis> {
}
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/xiao/stockproject/entity/StockAiAnalysis.java
git add src/main/java/com/xiao/stockproject/mapper/StockAiAnalysisMapper.java
git commit -m "feat: add StockAiAnalysis entity and mapper"
```

---

## Task 3: 创建 Service 层

**Files:**
- Create: `src/main/java/com/xiao/stockproject/service/StockAiAnalysisService.java`
- Create: `src/main/java/com/xiao/stockproject/service/impl/StockAiAnalysisServiceImpl.java`

- [ ] **Step 1: 创建 Service 接口**

```java
package com.xiao.stockproject.service;

import com.xiao.stockproject.entity.StockAiAnalysis;
import java.util.List;

public interface StockAiAnalysisService {
    StockAiAnalysis analyzeStock(String stockCode, String stockName);
    List<StockAiAnalysis> getAnalysisHistory(String stockCode);
}
```

- [ ] **Step 2: 创建 Service 实现类**

```java
package com.xiao.stockproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xiao.stockproject.entity.StockAiAnalysis;
import com.xiao.stockproject.mapper.StockAiAnalysisMapper;
import com.xiao.stockproject.service.StockAiAnalysisService;
import com.xiao.stockproject.utils.OllamaClient;
import com.xiao.stockproject.utils.StockAnalysisClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class StockAiAnalysisServiceImpl extends ServiceImpl<StockAiAnalysisMapper, StockAiAnalysis>
    implements StockAiAnalysisService {

    @Autowired
    private StockAnalysisClient stockAnalysisClient;

    @Autowired
    private OllamaClient ollamaClient;

    @Override
    public StockAiAnalysis analyzeStock(String stockCode, String stockName) {
        // 1. 调用Python脚本获取股票数据
        String stockData = stockAnalysisClient.getStockData(stockCode);
        log.info("获取股票数据: {}", stockData);

        // 2. 构建分析prompt
        String prompt = buildAnalysisPrompt(stockCode, stockName, stockData);

        // 3. 调用Ollama进行分析
        String analysisResult = ollamaClient.analyzeStock(prompt);

        // 4. 保存结果
        StockAiAnalysis analysis = new StockAiAnalysis();
        analysis.setStockCode(stockCode);
        analysis.setStockName(stockName);
        analysis.setAnalysisContent(analysisResult);
        analysis.setAnalysisDate(LocalDateTime.now());
        analysis.setModelName("qwen3.5:0.8b");
        baseMapper.insert(analysis);

        return analysis;
    }

    @Override
    public List<StockAiAnalysis> getAnalysisHistory(String stockCode) {
        QueryWrapper<StockAiAnalysis> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("stock_code", stockCode).orderByDesc("analysis_date");
        return baseMapper.selectList(queryWrapper);
    }

    private String buildAnalysisPrompt(String stockCode, String stockName, String stockData) {
        return String.format("请分析以下股票的技术面情况：\n\n股票代码: %s\n股票名称: %s\n\n数据:\n%s\n\n请给出技术分析，包括：1.当前价格位置 2.K线形态 3.技术指标信号 4.综合建议",
            stockCode, stockName, stockData);
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/xiao/stockproject/service/StockAiAnalysisService.java
git add src/main/java/com/xiao/stockproject/service/impl/StockAiAnalysisServiceImpl.java
git commit -m "feat: add StockAiAnalysisService"
```

---

## Task 4: 创建 Python 脚本和工具类

**Files:**
- Create: `scripts/stock_analysis.py`
- Create: `src/main/java/com/xiao/stockproject/utils/StockAnalysisClient.java`

- [ ] **Step 1: 创建 Python 脚本**

```python
#!/usr/bin/env python3
"""
股票数据获取脚本
调用 a-stock-data skill 获取股票实时行情和技术指标
"""
import sys
import json
import urllib.request

def tencent_quote(codes):
    """腾讯财经实时行情"""
    prefixed = []
    for c in codes:
        if c.startswith(("6", "9")):
            prefixed.append(f"sh{c}")
        elif c.startswith("8"):
            prefixed.append(f"bj{c}")
        else:
            prefixed.append(f"sz{c}")

    url = "https://qt.gtimg.cn/q=" + ",".join(prefixed)
    req = urllib.request.Request(url)
    req.add_header("User-Agent", "Mozilla/5.0")
    resp = urllib.request.urlopen(req, timeout=10)
    data = resp.read().decode("gbk")

    result = {}
    for line in data.strip().split(";"):
        if not line.strip() or "=" not in line or '"' not in line:
            continue
        key = line.split("=")[0].split("_")[-1]
        vals = line.split('"')[1].split("~")
        if len(vals) < 53:
            continue
        code = key[2:]
        result[code] = {
            "name": vals[1],
            "price": float(vals[3]) if vals[3] else 0,
            "change_pct": float(vals[32]) if vals[32] else 0,
            "pe_ttm": float(vals[39]) if vals[39] else 0,
            "pb": float(vals[46]) if vals[46] else 0,
            "mcap_yi": float(vals[44]) if vals[44] else 0,
        }
    return result

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python stock_analysis.py <stock_code>")
        sys.exit(1)

    stock_code = sys.argv[1]
    quotes = tencent_quote([stock_code])

    if stock_code in quotes:
        result = {
            "code": stock_code,
            "name": quotes[stock_code]["name"],
            "price": quotes[stock_code]["price"],
            "change_pct": quotes[stock_code]["change_pct"],
            "pe_ttm": quotes[stock_code]["pe_ttm"],
            "pb": quotes[stock_code]["pb"],
            "mcap_yi": quotes[stock_code]["mcap_yi"],
        }
        print(json.dumps(result, ensure_ascii=False))
    else:
        print(json.dumps({"error": f"未找到股票 {stock_code} 的数据"}))
```

- [ ] **Step 2: 创建 StockAnalysisClient 工具类**

```java
package com.xiao.stockproject.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
@Component
public class StockAnalysisClient {

    @Value("${stock.analysis.python-path:python}")
    private String pythonPath;

    @Value("${stock.analysis.script-path:scripts/stock_analysis.py}")
    private String scriptPath;

    public String getStockData(String stockCode) {
        try {
            String pythonScript = System.getProperty("user.dir") + "/" + scriptPath;
            ProcessBuilder pb = new ProcessBuilder(pythonPath, pythonScript, stockCode);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                return output.toString();
            } else {
                log.error("Python script failed with exit code: {}", exitCode);
                return "{\"error\": \"获取股票数据失败\"}";
            }
        } catch (Exception e) {
            log.error("调用Python脚本失败", e);
            return "{\"error\": \"调用Python脚本失败: " + e.getMessage() + "\"}";
        }
    }
}
```

- [ ] **Step 3: 在 application.yml 中添加配置**

```yaml
stock:
  analysis:
    python-path: python
    script-path: scripts/stock_analysis.py
```

- [ ] **Step 4: 提交**

```bash
git add scripts/stock_analysis.py
git add src/main/java/com/xiao/stockproject/utils/StockAnalysisClient.java
git add src/main/resources/application.yml
git commit -m "feat: add Python stock analysis script and client"
```

---

## Task 5: 在 OllamaClient 中新增分析接口

**Files:**
- Modify: `src/main/java/com/xiao/stockproject/utils/OllamaClient.java`

- [ ] **Step 1: 新增 analyzeStock 方法**

在 OllamaClient 类中添加：

```java
/**
 * 调用大模型进行股票分析
 * @param prompt 分析提示词
 * @return 分析结果
 */
public String analyzeStock(String prompt) {
    List<Map<String, String>> messages = new ArrayList<>();
    Map<String, String> userMsg = new HashMap<>();
    userMsg.put("role", "user");
    userMsg.put("content", prompt);
    messages.add(userMsg);

    StringBuilder result = new StringBuilder();
    CountDownLatch latch = new CountDownLatch(1);

    chatStream(
        "qwen3.5:0.8b",
        messages,
        false,
        null,
        token -> result.append(token),
        think -> {},
        () -> latch.countDown(),
        new AtomicBoolean(false),
        null,
        null
    );

    try {
        latch.await(60, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }

    return result.toString();
}
```

- [ ] **Step 2: 添加必要的 import**

```java
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/xiao/stockproject/utils/OllamaClient.java
git commit -m "feat: add analyzeStock method to OllamaClient"
```

---

## Task 6: 新增 Controller 接口

**Files:**
- Modify: `src/main/java/com/xiao/stockproject/controller/StockInfoController.java`

- [ ] **Step 1: 新增 AI 分析接口**

在 StockInfoController 中添加：

```java
@Autowired
private StockAiAnalysisService stockAiAnalysisService;

@PostMapping("/ai-analyze")
public Result<StockAiAnalysis> aiAnalyze(@RequestBody AiAnalyzeRequest request) {
    log.info("AI分析请求: stockCode={}, stockName={}", request.getStockCode(), request.getStockName());
    StockAiAnalysis result = stockAiAnalysisService.analyzeStock(
        request.getStockCode(), request.getStockName());
    return Result.success(result);
}

@GetMapping("/ai-analyze/{stockCode}")
public Result<List<StockAiAnalysis>> getAiAnalysisHistory(@PathVariable String stockCode) {
    return Result.success(stockAiAnalysisService.getAnalysisHistory(stockCode));
}
```

- [ ] **Step 2: 添加内部类**

```java
@Data
public static class AiAnalyzeRequest {
    private String stockCode;
    private String stockName;
}
```

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/xiao/stockproject/controller/StockInfoController.java
git commit -m "feat: add AI analyze endpoints"
```

---

## Task 7: 前端新增 AI 分析按钮

**Files:**
- Modify: `src/views/StocksView.vue`

- [ ] **Step 1: 新增 AI 分析按钮和弹窗**

```vue
<template>
  <div class="stocks-view">
    <!-- 原有表格代码保持不变 -->
    <el-table :data="stocks" ...>
      <!-- 原有列保持不变 -->
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="primary" size="small" @click="handleAiAnalyze(row)">
            AI分析
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- AI分析结果弹窗 -->
    <el-dialog v-model="aiDialogVisible" title="AI分析结果" width="600px">
      <div v-if="aiAnalysisResult">
        <p><strong>股票：</strong>{{ aiAnalysisResult.stockName }} ({{ aiAnalysisResult.stockCode }})</p>
        <p><strong>分析时间：</strong>{{ aiAnalysisResult.analysisDate }}</p>
        <p><strong>使用模型：</strong>{{ aiAnalysisResult.modelName }}</p>
        <el-divider />
        <div class="analysis-content">{{ aiAnalysisResult.analysisContent }}</div>
      </div>
      <div v-else-if="aiAnalyzing" class="loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>AI 正在分析中，请稍候...</span>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { stockApi } from '@/api/stock'
import { ElMessage } from 'element-plus'

const aiDialogVisible = ref(false)
const aiAnalyzing = ref(false)
const aiAnalysisResult = ref(null)

const handleAiAnalyze = async (row) => {
  aiDialogVisible.value = true
  aiAnalyzing.value = true
  aiAnalysisResult.value = null
  
  try {
    const res = await stockApi.aiAnalyze(row.stockCode, row.stockName)
    if (res.code === 200) {
      aiAnalysisResult.value = res.result
    } else {
      ElMessage.error(res.msg || '分析失败')
    }
  } catch (e) {
    ElMessage.error('分析失败: ' + e.message)
  } finally {
    aiAnalyzing.value = false
  }
}
</script>
```

- [ ] **Step 2: 在 stock.js 中新增 API 方法**

```javascript
export const stockApi = {
  // ... existing methods ...
  aiAnalyze: (stockCode, stockName) => {
    return request.post('/stock/ai/analyze', { stockCode, stockName })
  }
}
```

- [ ] **Step 3: 提交**

```bash
git add src/views/StocksView.vue
git add src/api/stock.js
git commit -m "feat: add AI analyze button to stocks view"
```

---

## Task 8: 集成测试

- [ ] **Step 1: 启动后端服务**

```bash
cd /d/privateWorkspace/stock-project
mvn spring-boot:run
```

- [ ] **Step 2: 测试 AI 分析接口**

```bash
curl -X POST http://localhost:18888/stock/api/stock/ai-analyze \
  -H "Content-Type: application/json" \
  -d '{"stockCode":"600519","stockName":"贵州茅台"}'
```

- [ ] **Step 3: 验证结果并提交**

```bash
git add -A
git commit -m "feat: complete AI stock analysis feature"
```

---

## 执行选项

**Plan complete and saved to `docs/superpowers/plans/2026-06-05-ai-stock-analysis-design.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**