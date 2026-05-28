package com.xiao.stockproject.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import lombok.Data;

/**
 * 自选股票信息
 * @TableName self_selected_info
 */
@TableName(value ="self_selected_info")
@Data
public class SelfSelectedInfo {
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
     * 是否删除 0未删除 1已删除
     */
    private Integer deleted;

    /**
     * 买入价格
     */
    private Double buyPrice;

    /**
     * 当前价格
     */
    private Double currentPrice;

    /**
     * 浮动金额
     */
    private Double floatPrice;

    /**
     * 浮动比例
     */
    private Double floatRate;

    /**
     * 目标金额
     */
    private Double targetPrice;

    /**
     * 是否提醒
     */
    private Integer isReminder;
}