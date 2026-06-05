package com.xiao.stockproject.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@TableName(value = "stock_ai_analysis")
@Data
public class StockAiAnalysis {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String stockCode;
    private String stockName;
    private String analysisContent;
    private LocalDateTime analysisDate;
    private String modelName;
}