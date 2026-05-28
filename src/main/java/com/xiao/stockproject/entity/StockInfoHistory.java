package com.xiao.stockproject.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 自选股票信息
 * @TableName stock_info_history
 */
@TableName(value ="stock_info_history")
@Data
public class StockInfoHistory {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 股票名称
     */
    private String stockName;

    /**
     * 股票代码
     */
    private String stockCode;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 当前价格
     */
    private String currentPrice;

    /**
     * 浮动金额
     */
    private String floatPrice;

    /**
     * 浮动比例
     */
    private String floatRate;

    /**
     * 最高价
     */
    private String highestPrice;

    /**
     * 最低价
     */
    private String lowestPrice;

    /**
     * 开盘价
     */
    private String openPrice;

    /**
     * 成交量
     */
    private String volume;

    /**
     * 成交额
     */
    private String turnover;

    /**
     * 收盘价
     */
    private String closingPrice;

    /**
     * 交易时间
     */
    private String transactionTime;

    /**
     * 涨跌额 当前价 - 昨日收盘价
     */
    private String amountOfChange;

    /**
     * 涨跌幅 涨跌额 / 昨日收盘价（百分比）
     */
    private String priceLimit;
}