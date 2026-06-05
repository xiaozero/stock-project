package com.xiao.stockproject.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.InputStreamReader;

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