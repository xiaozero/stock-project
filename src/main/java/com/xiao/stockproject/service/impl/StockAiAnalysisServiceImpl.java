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