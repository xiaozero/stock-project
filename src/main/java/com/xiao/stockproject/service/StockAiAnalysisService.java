package com.xiao.stockproject.service;

import com.xiao.stockproject.entity.StockAiAnalysis;
import java.util.List;

public interface StockAiAnalysisService {
    StockAiAnalysis analyzeStock(String stockCode, String stockName);
    List<StockAiAnalysis> getAnalysisHistory(String stockCode);
}