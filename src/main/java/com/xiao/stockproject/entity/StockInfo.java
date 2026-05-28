package com.xiao.stockproject.entity;

import lombok.Data;

/**
 * @author xiaohuijia
 * @date 2026/1/13 15:11
 */
@Data
public class StockInfo {
    private String stockCode;      // 股票代码
    private String stockName;      // 股票名称
    private Double currentPrice;   // 当前价格
    private Double openPrice;      // 开盘价
    private Double closePriceYesterday; // 昨日收盘价
    private Double highestPrice;   // 最高价
    private Double lowestPrice;    // 最低价
    private Double buyPrice;       // 买入价
    private Double sellPrice;      // 卖出价
    private Long volume;           // 成交量
    private Long turnover;         // 成交额
    private String date;           // 日期
    private String time;           // 时间
    private Double changePercent;  // 涨跌幅百分比
    @Override
    public String toString() {
        return String.format(
                "股票名称：%s (%s)\n当前价格：%.2f\n涨跌幅：%.2f%%\n今日开盘价：%.2f\n昨日收盘价：%.2f\n最高价：%.2f\n最低价：%.2f\n成交量：%d\n成交额：%d\n更新时间：%s %s",
                stockName, stockCode, currentPrice, changePercent, openPrice, closePriceYesterday,
                highestPrice, lowestPrice, volume, turnover, date, time
        );
    }
}
