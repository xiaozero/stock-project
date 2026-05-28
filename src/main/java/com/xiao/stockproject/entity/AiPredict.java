package com.xiao.stockproject.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * ai预测
 * @TableName ai_predict
 */
@TableName(value ="ai_predict")
@Data
public class AiPredict {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * ai来源
     */
    private String aiSource;

    /**
     * 当前价格
     */
    private Integer currentPrice;

    /**
     * 是否建议买入
     */
    private String buySuggestion;

    /**
     * 买入价格
     */
    private String buyPrice;

    /**
     * 止盈价格
     */
    private String takeProfit;

    /**
     * 止损价格
     */
    private String stopLoss;

    /**
     * 相对理由
     */
    private String relativeReason;

    /**
     * 对应风险
     */
    private String correspondingRisk;

    /**
     * 
     */
    private Date createTime;
}