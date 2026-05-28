package com.xiao.stockproject.utils;

import com.xiao.stockproject.entity.StockInfoHistory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * 极简版股票实时查询工具（main方法直接运行）
 * 无需第三方依赖，仅用JDK原生API
 */
public class SimpleStockQuery {
    // 新浪财经免费股票接口
    private static final String SINA_STOCK_API = "https://hq.sinajs.cn/list=";
    private static final Scanner SCANNER = new Scanner(System.in);

    /**
     * 核心方法：根据股票代码查询实时数据
     * @param stockCodes 股票代码（如600000、000001）
     * @return 股票信息字符串（名称|当前价|昨收价|涨跌幅），查询失败返回null
     */
    public static List<StockInfoHistory> queryStock(String stockCodes) {
        List<StockInfoHistory> stockInfoHistoryList = new ArrayList<>();
        // 1. 拆分并转换为新浪格式的代码（加sh/sz/bj前缀）
        String[] codes = stockCodes.split(",");
        StringBuilder sinaCodes = new StringBuilder();
        for (String code : codes) {
            code = code.trim();
            String sinaCode = getSinaStockCode(code);
            if (sinaCode != null) {
                if (sinaCodes.length() > 0) {
                    sinaCodes.append(","); // 多个代码用逗号分隔
                }
                sinaCodes.append(sinaCode);
            }
        }
        // 2. 调用新浪接口获取数据发送批量查询请求
        try {
            URL url = new URL(SINA_STOCK_API + sinaCodes);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Referer", "https://finance.sina.com.cn/");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000); // 连接超时5秒
            conn.setReadTimeout(5000);    // 读取超时5秒

            // 3. 读取并解析多只股票数据
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "GBK"));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("hq_str_") && line.contains("=")) {
                    // 拆分单只股票的数据
                    String[] parts = line.split("=");
                    String sinaCode = parts[0].replace("var hq_str_", ""); // 新浪代码（如sh600000）
                    String rawData = parts[1].replace("\"", "").replace(";", "");
                    // 解析股票信息
                    StockInfoHistory stockInfoHistory = parseStockData(rawData, sinaCode.substring(2)); // 去掉sh/sz/bj前缀
                    stockInfoHistoryList.add(stockInfoHistory);
                }
            }
            reader.close();
            conn.disconnect();
        } catch (Exception e) {
           e.printStackTrace();
        }
        return stockInfoHistoryList;
    }

    /**
     * 拼接新浪接口的股票代码（sh600000/sz000001/bj830881）
     */
    private static String getSinaStockCode(String stockCode) {
        if (stockCode == null || stockCode.trim().isEmpty()) {
            return null;
        }
        stockCode = stockCode.trim();
        if (stockCode.startsWith("6")) return "sh" + stockCode;    // 沪市
        if (stockCode.startsWith("0") || stockCode.startsWith("3")) return "sz" + stockCode; // 深市
        if (stockCode.startsWith("8")) return "bj" + stockCode;    // 北交所
        if (stockCode.startsWith("sh") || stockCode.startsWith("sz") || stockCode.startsWith("bj")) return stockCode;
        return null;
    }

    /**
     * 解析新浪接口返回的原始数据
     */
    private static StockInfoHistory parseStockData(String rawData, String stockCode) {
        StockInfoHistory stockInfoHistory = null;
        try {
            String[] fields = rawData.split(",");
            if (fields.length >= 3) {
                stockInfoHistory =  new StockInfoHistory();
                String name = fields[0];          // 股票名称
                String openPrice = fields[1];     // 今日开盘价
                String lastClose = fields[2];     // 昨日收盘价
                String currentPrice = fields[3];  // 当前价格
                String lowPrice = fields[4];  // 今日最低价
                String hightPrice = fields[5];  //今日最高价
                String volume = fields[8]; //成交总量
                String turnover = fields[9]; //成交总金额
                String time = fields[31];         // 实时时间
                //根据昨日收盘价获取涨跌幅
                BigDecimal changeRate = new BigDecimal(currentPrice).subtract(new BigDecimal(lastClose)).divide(new BigDecimal(lastClose), 4, RoundingMode.HALF_UP);
                //涨跌额
                stockInfoHistory.setAmountOfChange(new BigDecimal(currentPrice).subtract(new BigDecimal(lastClose)).setScale(2, RoundingMode.HALF_UP).toString());
                stockInfoHistory.setPriceLimit(new BigDecimal(currentPrice).subtract(changeRate.multiply(new BigDecimal(100))).setScale(2, RoundingMode.HALF_UP).toString());
                stockInfoHistory.setFloatRate(changeRate.multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP).toString());
                stockInfoHistory.setFloatPrice(new BigDecimal(currentPrice).subtract(new BigDecimal(lastClose)).setScale(2, RoundingMode.HALF_UP).toString());
                stockInfoHistory.setStockName(name);
                stockInfoHistory.setStockCode(stockCode);
                stockInfoHistory.setCurrentPrice(currentPrice);
                stockInfoHistory.setHighestPrice(hightPrice);
                stockInfoHistory.setLowestPrice(lowPrice);
                stockInfoHistory.setOpenPrice(openPrice);
                stockInfoHistory.setTransactionTime(time);
                stockInfoHistory.setVolume(volume);
                stockInfoHistory.setTurnover(turnover);
                stockInfoHistory.setClosingPrice(lastClose);
                // 拼接返回结果
                return stockInfoHistory;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return stockInfoHistory;
    }

    // main方法：程序入口
    public static void main(String[] args) {
        System.out.println("=== 极简版股票实时查询工具 ===");
        System.out.println("支持股票代码：沪市(6开头)、深市(0/3开头)、北交所(8开头)");
        System.out.println("输入 'exit' 退出程序\n");

        while (true) {
            // 控制台输入股票代码
            System.out.print("请输入股票代码：");
            String input = SCANNER.nextLine().trim();

            // 退出条件
            if ("exit".equalsIgnoreCase(input)) {
                System.out.println("程序已退出！");
                SCANNER.close();
                return;
            }

            // 调用查询方法并输出结果
            List<StockInfoHistory> result = queryStock(input);
            System.out.println("------------------------");
            System.out.println(result.toString());
            System.out.println("------------------------\n");
        }
    }
}
