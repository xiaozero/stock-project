package com.xiao.stockproject.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@TableName(value = "chat_session")
@Data
public class ChatSession {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String sessionId;

    private String title;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}