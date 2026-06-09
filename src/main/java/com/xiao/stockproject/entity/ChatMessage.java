package com.xiao.stockproject.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@TableName(value = "chat_message")
@Data
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String sessionId;

    private String role;

    private String content;

    private String imageData;

    private String modelName;

    private String thinkContent;

    private Integer tokenUsed;

    private Integer durationMs;

    private Integer isWebSearch;

    private LocalDateTime createdTime;
}