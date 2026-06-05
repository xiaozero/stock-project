package com.xiao.stockproject.controller;

import com.xiao.stockproject.entity.SelfSelectedInfo;
import com.xiao.stockproject.entity.StockAiAnalysis;
import com.xiao.stockproject.entity.vo.Result;
import com.xiao.stockproject.service.SelfSelectedInfoService;
import com.xiao.stockproject.service.StockAiAnalysisService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author xiaohuijia
 * @date 2026/1/12 8:51
 */
@Api(value = "股票信息管理", tags = "股票信息管理")
@RestController
@RequestMapping("/api/stock")
public class StockInfoController {
    @Autowired
    private SelfSelectedInfoService selfSelectedInfoService;

    @Autowired
    private StockAiAnalysisService stockAiAnalysisService;

    /**
     *  获取股票价格
     * @return
     * @throws Exception
     */
    @ApiOperation(value="获取股票价格", notes="获取股票价格")
    @PostMapping("/getStockRealPrice")
    public Result<Object> getStockRealPrice() throws Exception {
        selfSelectedInfoService.updateStackInfoReal();
        return Result.ok("查询成功");
    }

    @ApiOperation(value="获取自选股票列表", notes="获取自选股票列表")
    @GetMapping("/list")
    public Result<List<SelfSelectedInfo>> listSelfSelected() {
        List<SelfSelectedInfo> list = selfSelectedInfoService.list(
            new QueryWrapper<SelfSelectedInfo>().eq("deleted", 0)
        );
        return Result.OK(list);
    }

    @ApiOperation(value="AI股票分析", notes="AI股票分析")
    @PostMapping("/ai-analyze")
    public Result<StockAiAnalysis> aiAnalyze(@RequestBody AiAnalyzeRequest request) {
        StockAiAnalysis result = stockAiAnalysisService.analyzeStock(
            request.getStockCode(), request.getStockName());
        return Result.success(result);
    }

    @ApiOperation(value="获取AI分析历史", notes="获取AI分析历史")
    @GetMapping("/ai-analyze/{stockCode}")
    public Result<List<StockAiAnalysis>> getAiAnalysisHistory(@PathVariable String stockCode) {
        return Result.success(stockAiAnalysisService.getAnalysisHistory(stockCode));
    }

    @Data
    public static class AiAnalyzeRequest {
        private String stockCode;
        private String stockName;
    }
}


