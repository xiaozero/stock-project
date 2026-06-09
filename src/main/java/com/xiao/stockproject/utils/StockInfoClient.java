package com.xiao.stockproject.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class StockInfoClient {

    private static final int TIMEOUT_SECONDS = 15;

    public static class StockInfo {
        private String code;
        private String name;
        private String price;
        private String change;
        private String changePercent;
        private String open;
        private String close;
        private String high;
        private String low;
        private String volume;
        private String amount;
        private String market;
        private String industry;
        private String sector;

        public String getCode() { return code; }
        public String getName() { return name; }
        public String getPrice() { return price; }
        public String getChange() { return change; }
        public String getChangePercent() { return changePercent; }
        public String getOpen() { return open; }
        public String getClose() { return close; }
        public String getHigh() { return high; }
        public String getLow() { return low; }
        public String getVolume() { return volume; }
        public String getAmount() { return amount; }
        public String getMarket() { return market; }
        public String getIndustry() { return industry; }
        public String getSector() { return sector; }

        public String toFormattedString() {
            StringBuilder sb = new StringBuilder();
            sb.append("【").append(name).append("】").append(code).append("\n");
            sb.append("当前价格: ").append(price).append("\n");
            sb.append("涨跌: ").append(change).append(" (").append(changePercent).append(")\n");
            sb.append("开盘: ").append(open).append(" | 最高: ").append(high).append(" | 最低: ").append(low).append("\n");
            sb.append("成交量: ").append(volume).append(" | 成交额: ").append(amount).append("\n");
            return sb.toString();
        }
    }

    private CloseableHttpClient createHttpClient() {
        return HttpClients.custom()
            .setSSLContext(createInsecureSSLContext())
            .build();
    }

    private javax.net.ssl.SSLContext createInsecureSSLContext() {
        try {
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                new javax.net.ssl.X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                }
            };
            javax.net.ssl.SSLContext ssl = javax.net.ssl.SSLContext.getInstance("TLS");
            ssl.init(null, trustAllCerts, new java.security.SecureRandom());
            return ssl;
        } catch (Exception e) {
            log.error("Failed to create SSL context", e);
            throw new RuntimeException(e);
        }
    }

    public StockInfo getStockInfo(String stockCode) {
        try {
            String sinajsCode = convertToSinaCode(stockCode);
            String url = "https://hq.sinajs.cn/list=" + sinajsCode;

            try (CloseableHttpClient client = createHttpClient()) {
                HttpGet request = new HttpGet(url);
                request.setConfig(RequestConfig.custom()
                        .setConnectTimeout(TIMEOUT_SECONDS * 1000)
                        .setSocketTimeout(TIMEOUT_SECONDS * 1000)
                        .build());
                request.setHeader("User-Agent", "Mozilla/5.0");
                request.setHeader("Referer", "https://finance.sina.com.cn");

                String response = EntityUtils.toString(client.execute(request).getEntity(), StandardCharsets.UTF_8);
                return parseSinaResponse(stockCode, response);
            }
        } catch (Exception e) {
            log.error("获取股票信息失败: stockCode={}, error={}", stockCode, e.getMessage());
            return createEmptyStockInfo(stockCode);
        }
    }

    public Map<String, StockInfo> getBatchStockInfo(String... stockCodes) {
        Map<String, StockInfo> result = new HashMap<>();
        if (stockCodes == null || stockCodes.length == 0) {
            return result;
        }

        try {
            StringBuilder codes = new StringBuilder();
            for (int i = 0; i < stockCodes.length; i++) {
                if (i > 0) codes.append(",");
                codes.append(convertToSinaCode(stockCodes[i]));
            }

            String url = "https://hq.sinajs.cn/list=" + codes.toString();

            try (CloseableHttpClient client = createHttpClient()) {
                HttpGet request = new HttpGet(url);
                request.setConfig(RequestConfig.custom()
                        .setConnectTimeout(TIMEOUT_SECONDS * 1000)
                        .setSocketTimeout(TIMEOUT_SECONDS * 1000)
                        .build());
                request.setHeader("User-Agent", "Mozilla/5.0");
                request.setHeader("Referer", "https://finance.sina.com.cn");

                String response = EntityUtils.toString(client.execute(request).getEntity(), StandardCharsets.UTF_8);

                String[] parts = response.split(";");
                for (int i = 0; i < parts.length && i < stockCodes.length; i++) {
                    StockInfo info = parseSinaResponse(stockCodes[i], parts[i] + ";");
                    result.put(stockCodes[i], info);
                }
            }
        } catch (Exception e) {
            log.error("批量获取股票信息失败: error={}", e.getMessage());
        }
        return result;
    }

    public String searchStocks(String keyword, int maxResults) {
        try {
            String url = "https://searchapi.eastmoney.com/api/suggest/get?input=" +
                    java.net.URLEncoder.encode(keyword, StandardCharsets.UTF_8.name()) +
                    "&type=14&token=D43BF722C8E33BDC906FB84D85E326E8&markettype=&mktnum=&jys=&classify=&securitytype=&status=&count=" + maxResults;

            try (CloseableHttpClient client = createHttpClient()) {
                HttpGet request = new HttpGet(url);
                request.setConfig(RequestConfig.custom()
                        .setConnectTimeout(TIMEOUT_SECONDS * 1000)
                        .setSocketTimeout(TIMEOUT_SECONDS * 1000)
                        .build());
                request.setHeader("User-Agent", "Mozilla/5.0");
                request.setHeader("Referer", "https://www.eastmoney.com");

                String response = EntityUtils.toString(client.execute(request).getEntity(), StandardCharsets.UTF_8);
                return parseStockSuggestions(keyword, response, maxResults);
            }
        } catch (Exception e) {
            log.error("搜索股票失败: keyword={}, error={}", keyword, e.getMessage());
            return "搜索股票失败: " + e.getMessage();
        }
    }

    public String getRealTimeQuote(String stockCode) {
        StockInfo info = getStockInfo(stockCode);
        if (info.getName() == null || info.getName().isEmpty()) {
            return "未找到股票: " + stockCode;
        }
        return info.toFormattedString();
    }

    private String convertToSinaCode(String stockCode) {
        stockCode = stockCode.trim().toLowerCase();

        if (stockCode.startsWith("sh") || stockCode.startsWith("sz")) {
            return stockCode;
        }

        try {
            int code = Integer.parseInt(stockCode.replaceAll("[^0-9]", ""));
            if (code >= 600000 && code < 700000) {
                return "sh" + code;
            } else if (code >= 000000 && code < 100000) {
                return "sz" + code;
            } else if (code >= 730000 && code < 740000) {
                return "sh" + code;
            } else if (code >= 800000 && code < 900000) {
                return "sh" + code;
            }
        } catch (NumberFormatException ignored) {}

        return "sz" + stockCode;
    }

    private StockInfo parseSinaResponse(String stockCode, String response) {
        StockInfo info = new StockInfo();
        info.code = stockCode;

        try {
            Pattern pattern = Pattern.compile("\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(response);

            if (matcher.find()) {
                String data = matcher.group(1);
                String[] fields = data.split(",");

                if (fields.length >= 32) {
                    info.name = fields[0];
                    info.open = fields[1];
                    info.close = fields[2];
                    info.price = fields[3];
                    info.high = fields[4];
                    info.low = fields[5];

                    double now = 0, close = 0;
                    try { now = Double.parseDouble(info.price); } catch (Exception ignored) {}
                    try { close = Double.parseDouble(info.close); } catch (Exception ignored) {}
                    double changeVal = now - close;
                    info.change = String.format("%.2f", changeVal);
                    if (close > 0) {
                        info.changePercent = String.format("%.2f%%", (changeVal / close) * 100);
                    } else {
                        info.changePercent = "0.00%";
                    }

                    info.volume = fields[8];
                    info.amount = fields[9];

                    if (stockCode.startsWith("sh") || stockCode.startsWith("6")) {
                        info.market = "上海";
                    } else {
                        info.market = "深圳";
                    }
                }
            }
        } catch (Exception e) {
            log.debug("解析股票响应失败: response={}, error={}", response, e.getMessage());
        }

        return info;
    }

    private String parseStockSuggestions(String keyword, String response, int maxResults) {
        StringBuilder result = new StringBuilder();
        result.append("搜索关键词: ").append(keyword).append("\n\n");

        try {
            com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(response);
            com.alibaba.fastjson.JSONArray data = json.getJSONArray("Data");
            if (data == null || data.isEmpty()) {
                result.append("未找到相关股票\n");
                return result.toString();
            }

            int count = 0;
            for (int i = 0; i < data.size() && count < maxResults; i++) {
                com.alibaba.fastjson.JSONObject item = data.getJSONObject(i);
                String name = item.getString("Word");
                String code = item.getString("Code");
                String type = item.getString("SecurityTypeName");

                if (type != null && (type.contains("指数") || type.contains("板块"))) {
                    continue;
                }

                result.append(count + 1).append(". ").append(name)
                        .append(" (").append(code).append(")\n");
                count++;
            }

            if (count == 0) {
                result.append("未找到相关股票\n");
            }
        } catch (Exception e) {
            log.error("解析股票搜索结果失败: response={}, error={}", response, e.getMessage());
            result.append("搜索解析失败: ").append(e.getMessage()).append("\n");
        }

        return result.toString();
    }

    private StockInfo createEmptyStockInfo(String stockCode) {
        StockInfo info = new StockInfo();
        info.code = stockCode;
        info.name = "";
        info.price = "--";
        info.change = "--";
        info.changePercent = "--";
        info.open = "--";
        info.high = "--";
        info.low = "--";
        info.volume = "--";
        info.amount = "--";
        return info;
    }
}