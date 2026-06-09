# AI 对话模块实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于 Ollama 本地大模型的对话模块，支持模型选择、深度思考、联网搜索、流式输出

**Architecture:** Spring Boot 后端提供 REST API + SSE 流式接口，前端 Vue 3 + Element Plus 实现对话界面和会话管理

**Tech Stack:** Spring Boot, MyBatis-Plus, WebClient (Ollama), SSE, Vue 3, Element Plus

---

## 文件结构

### 后端新增
- `entity/ChatSession.java` - 会话实体
- `entity/ChatMessage.java` - 消息实体
- `mapper/ChatSessionMapper.java` - 会话Mapper
- `mapper/ChatMessageMapper.java` - 消息Mapper
- `service/ChatService.java` - 对话服务接口
- `service/impl/ChatServiceImpl.java` - 对话服务实现
- `controller/ChatController.java` - 对话控制器
- `utils/OllamaClient.java` - Ollama API客户端

### 前端新增/修改
- `api/chat.js` - 对话API
- `views/ChatView.vue` - 对话页面（重写）
- `router/index.js` - 路由（可能需要调整）

---

## 任务列表

### Task 1: 数据库和实体类

**Files:**
- Create: `src/main/java/com/xiao/stockproject/entity/ChatSession.java`
- Create: `src/main/java/com/xiao/stockproject/entity/ChatMessage.java`
- Create: `src/main/java/com/xiao/stockproject/mapper/ChatSessionMapper.java`
- Create: `src/main/java/com/xiao/stockproject/mapper/ChatMessageMapper.java`

- [ ] **Step 1: 创建 ChatSession 实体类**

```java
package com.xiao.stockproject.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@TableName(value = "chat_session")
@Data
public class ChatSession {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String sessionId;

    private String title;

    private Date createdTime;

    private Date updatedTime;
}
```

- [ ] **Step 2: 创建 ChatMessage 实体类**

```java
package com.xiao.stockproject.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.util.Date;

@TableName(value = "chat_message")
@Data
public class ChatMessage {
    @TableId(type = IdType.AUTO)
    private Integer id;

    private String sessionId;

    private String role;

    private String content;

    private String modelName;

    private String thinkContent;

    private Integer tokenUsed;

    private Integer durationMs;

    private Integer isWebSearch;

    private Date createdTime;
}
```

- [ ] **Step 3: 创建 ChatSessionMapper**

```java
package com.xiao.stockproject.mapper;

import com.xiao.stockproject.entity.ChatSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
```

- [ ] **Step 4: 创建 ChatMessageMapper**

```java
package com.xiao.stockproject.mapper;

import com.xiao.stockproject.entity.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
```

- [ ] **Step 5: 执行 SQL 创建表**

```sql
CREATE TABLE chat_session (
    id INT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(255),
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE chat_message (
    id INT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT,
    model_name VARCHAR(64),
    think_content TEXT,
    token_used INT,
    duration_ms INT,
    is_web_search TINYINT DEFAULT 0,
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id)
);
```

---

### Task 2: Ollama 客户端封装

**Files:**
- Create: `src/main/java/com/xiao/stockproject/utils/OllamaClient.java`

- [ ] **Step 1: 创建 OllamaClient 工具类**

```java
package com.xiao.stockproject.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OllamaClient {

    private final WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:11434")
            .build();

    public Flux<String> chatStream(String model, List<Map<String, String>> messages, boolean think) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("stream", true);
        if (think) {
            body.put("think", true);
        }

        return webClient.post()
                .uri("/api/chat")
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class);
    }

    public List<String> getModels() {
        try {
            Map<?, ?> response = webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response != null && response.containsKey("models")) {
                List<?> models = (List<?>) response.get("models");
                return models.stream()
                        .map(m -> {
                            Map<?, ?> model = (Map<?, ?>) m;
                            return (String) model.get("name");
                        })
                        .toList();
            }
        } catch (Exception e) {
            log.error("获取Ollama模型列表失败", e);
        }
        return List.of();
    }
}
```

---

### Task 3: 对话服务层

**Files:**
- Create: `src/main/java/com/xiao/stockproject/service/ChatService.java`
- Create: `src/main/java/com/xiao/stockproject/service/impl/ChatServiceImpl.java`

- [ ] **Step 1: 创建 ChatService 接口**

```java
package com.xiao.stockproject.service;

import com.xiao.stockproject.entity.ChatMessage;
import com.xiao.stockproject.entity.ChatSession;

import java.util.List;

public interface ChatService {
    List<ChatSession> getSessions();

    List<ChatMessage> getMessages(String sessionId);

    String createSession();

    void deleteSession(String sessionId);

    Flux<ChatStreamData> chat(ChatRequest request);
}

@Data
class ChatRequest {
    private String sessionId;
    private String model;
    private String message;
    private boolean enableThink;
    private boolean enableWebSearch;
}

@Data
class ChatStreamData {
    private String type; // think, token, done
    private String content;
    private Integer tokenUsed;
    private Integer durationMs;
}
```

- [ ] **Step 2: 创建 ChatServiceImpl 实现类**

```java
package com.xiao.stockproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiao.stockproject.entity.ChatMessage;
import com.xiao.stockproject.entity.ChatSession;
import com.xiao.stockproject.mapper.ChatMessageMapper;
import com.xiao.stockproject.mapper.ChatSessionMapper;
import com.xiao.stockproject.service.ChatService;
import com.xiao.stockproject.utils.OllamaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatSessionMapper sessionMapper;

    @Autowired
    private ChatMessageMapper messageMapper;

    @Autowired
    private OllamaClient ollamaClient;

    @Override
    public List<ChatSession> getSessions() {
        return sessionMapper.selectList(
            new QueryWrapper<ChatSession>().orderByDesc("updated_time")
        );
    }

    @Override
    public List<ChatMessage> getMessages(String sessionId) {
        return messageMapper.selectList(
            new QueryWrapper<ChatMessage>()
                .eq("session_id", sessionId)
                .orderByAsc("created_time")
        );
    }

    @Override
    public String createSession() {
        String sessionId = UUID.randomUUID().toString();
        ChatSession session = new ChatSession();
        session.setSessionId(sessionId);
        session.setTitle("新会话");
        session.setCreatedTime(LocalDateTime.now());
        session.setUpdatedTime(LocalDateTime.now());
        sessionMapper.insert(session);
        return sessionId;
    }

    @Override
    public void deleteSession(String sessionId) {
        sessionMapper.delete(new QueryWrapper<ChatSession>().eq("session_id", sessionId));
        messageMapper.delete(new QueryWrapper<ChatMessage>().eq("session_id", sessionId));
    }

    @Override
    public Flux<ChatStreamData> chat(ChatRequest request) {
        String sessionId = request.getSessionId();
        List<ChatMessage> history = getMessages(sessionId);

        List<Map<String, String>> messages = new ArrayList<>();
        for (ChatMessage msg : history) {
            messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
        }
        messages.add(Map.of("role", "user", "content", request.getMessage()));

        final long startTime = System.currentTimeMillis();

        return ollamaClient.chatStream(
                request.getModel(),
                messages,
                request.isEnableThink()
            ).map(line -> {
                // 解析SSE数据
                // 返回 ChatStreamData
                // type: think / token / done
            });
    }
}
```

---

### Task 4: 对话控制器

**Files:**
- Create: `src/main/java/com/xiao/stockproject/controller/ChatController.java`

- [ ] **Step 1: 创建 ChatController**

```java
package com.xiao.stockproject.controller;

import com.xiao.stockproject.entity.ChatMessage;
import com.xiao.stockproject.entity.ChatSession;
import com.xiao.stockproject.service.ChatService;
import com.xiao.stockproject.service.ChatService.ChatRequest;
import com.xiao.stockproject.service.ChatService.ChatStreamData;
import com.xiao.stockproject.utils.OllamaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private OllamaClient ollamaClient;

    @GetMapping("/models")
    public List<String> getModels() {
        return ollamaClient.getModels();
    }

    @GetMapping("/sessions")
    public List<ChatSession> getSessions() {
        return chatService.getSessions();
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public List<ChatMessage> getMessages(@PathVariable String sessionId) {
        return chatService.getMessages(sessionId);
    }

    @PostMapping("/session")
    public String createSession() {
        return chatService.createSession();
    }

    @DeleteMapping("/session/{sessionId}")
    public void deleteSession(@PathVariable String sessionId) {
        chatService.deleteSession(sessionId);
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request) {
        return chatService.chat(request).map(data -> {
            return "data: " + data.toString() + "\n\n";
        });
    }
}
```

---

### Task 5: 前端 API

**Files:**
- Create: `src/main/java/com/xiao/stockproject/frontend/api/chat.js`

- [ ] **Step 1: 创建 chat API 模块**

```javascript
import axios from 'axios'

const request = axios.create({
  baseURL: '/stock',
  timeout: 30000
})

export const chatApi = {
  getModels: () => request.get('/api/chat/models'),

  getSessions: () => request.get('/api/chat/sessions'),

  getMessages: (sessionId) => request.get(`/api/chat/sessions/${sessionId}/messages`),

  createSession: () => request.post('/api/chat/session'),

  deleteSession: (sessionId) => request.delete(`/api/chat/session/${sessionId}`),

  chat: (data) => {
    return new EventSource(`/stock/api/chat/chat`, {
      method: 'POST',
      body: JSON.stringify(data),
      headers: { 'Content-Type': 'application/json' }
    })
  }
}
```

---

### Task 6: 前端对话页面

**Files:**
- Create: `src/main/java/com/xiao/stockproject/frontend/src/views/ChatView.vue`

- [ ] **Step 1: 创建 ChatView.vue 组件**

```vue
<template>
  <div class="chat-container">
    <!-- 左侧会话列表 -->
    <div class="session-panel">
      <div class="panel-header">
        <span>会话列表</span>
        <el-button type="primary" size="small" @click="createSession">新建</el-button>
      </div>
      <div class="session-list">
        <div
          v-for="session in sessions"
          :key="session.sessionId"
          :class="['session-item', { active: currentSessionId === session.sessionId }]"
          @click="selectSession(session.sessionId)"
        >
          <span class="session-title">{{ session.title }}</span>
          <el-icon class="delete-btn" @click.stop="deleteSession(session.sessionId)"><Delete /></el-icon>
        </div>
      </div>
    </div>

    <!-- 右侧对话区域 -->
    <div class="chat-main">
      <div class="chat-header">
        <h2>AI 对话</h2>
        <div class="chat-controls">
          <el-select v-model="currentModel" placeholder="选择模型" size="small">
            <el-option v-for="m in models" :key="m" :label="m" :value="m" />
          </el-select>
          <el-checkbox v-model="enableThink">深度思考</el-checkbox>
          <el-checkbox v-model="enableWebSearch">联网搜索</el-checkbox>
        </div>
      </div>

      <!-- 消息列表 -->
      <div class="message-list" ref="messageListRef">
        <div v-for="msg in messages" :key="msg.id" :class="['message', msg.role]">
          <div class="message-content">
            <div v-if="msg.role === 'user'" class="message-text">{{ msg.content }}</div>
            <template v-else>
              <div v-if="msg.thinkContent" class="think-content">
                <span class="think-label">思考中...</span>
                <span class="think-text">{{ msg.thinkContent }}</span>
              </div>
              <div class="message-text">{{ msg.content }}</div>
              <div class="message-meta">
                <span v-if="msg.tokenUsed">Token: {{ msg.tokenUsed }}</span>
                <span v-if="msg.durationMs">耗时: {{ (msg.durationMs / 1000).toFixed(1) }}s</span>
                <el-tag v-if="msg.isWebSearch" size="small" type="info">联网</el-tag>
              </div>
            </template>
          </div>
        </div>
        <!-- 流式输出中的消息 -->
        <div v-if="streamingMessage" class="message assistant streaming">
          <div class="message-content">
            <div v-if="streamingThink" class="think-content">
              <span class="think-label">思考中...</span>
              <span class="think-text">{{ streamingThink }}</span>
            </div>
            <div class="message-text">{{ streamingMessage }}</div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="2"
          placeholder="输入消息..."
          @keydown.enter.ctrl="sendMessage"
        />
        <el-button type="primary" @click="sendMessage" :loading="sending">发送</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { chatApi } from '@/api/chat'
import { ElMessage } from 'element-plus'

const models = ref([])
const sessions = ref([])
const currentSessionId = ref(null)
const currentModel = ref('')
const enableThink = ref(false)
const enableWebSearch = ref(false)

const messages = ref([])
const inputText = ref('')
const sending = ref(false)
const streamingMessage = ref('')
const streamingThink = ref('')
const messageListRef = ref(null)

// 加载模型列表
const loadModels = async () => {
  try {
    const res = await chatApi.getModels()
    models.value = res.result || []
    if (models.value.length > 0) {
      currentModel.value = models.value[0]
    }
  } catch (e) {
    console.error(e)
  }
}

// 加载会话列表
const loadSessions = async () => {
  try {
    const res = await chatApi.getSessions()
    sessions.value = res.result || []
  } catch (e) {
    console.error(e)
  }
}

// 创建会话
const createSession = async () => {
  try {
    const res = await chatApi.createSession()
    currentSessionId.value = res.result
    messages.value = []
    await loadSessions()
  } catch (e) {
    console.error(e)
  }
}

// 选择会话
const selectSession = async (sessionId) => {
  currentSessionId.value = sessionId
  try {
    const res = await chatApi.getMessages(sessionId)
    messages.value = res.result || []
    scrollToBottom()
  } catch (e) {
    console.error(e)
  }
}

// 删除会话
const deleteSession = async (sessionId) => {
  try {
    await chatApi.deleteSession(sessionId)
    if (currentSessionId.value === sessionId) {
      currentSessionId.value = null
      messages.value = []
    }
    await loadSessions()
  } catch (e) {
    console.error(e)
  }
}

// 发送消息
const sendMessage = async () => {
  if (!inputText.value.trim() || !currentSessionId.value) return
  if (!currentModel.value) {
    ElMessage.warning('请先选择一个模型')
    return
  }

  sending.value = true
  const userMessage = inputText.value
  inputText.value = ''

  // 添加用户消息
  messages.value.push({
    id: Date.now(),
    role: 'user',
    content: userMessage
  })
  scrollToBottom()

  // 发起SSE请求
  const eventSource = new EventSource('/stock/api/chat/chat', {
    method: 'POST',
    body: JSON.stringify({
      sessionId: currentSessionId.value,
      model: currentModel.value,
      message: userMessage,
      enableThink: enableThink.value,
      enableWebSearch: enableWebSearch.value
    }),
    headers: { 'Content-Type': 'application/json' }
  })

  streamingMessage.value = ''
  streamingThink.value = ''
  let assistantContent = ''

  eventSource.onmessage = (e) => {
    const data = JSON.parse(e.data)
    if (data.type === 'think') {
      streamingThink.value += data.content
    } else if (data.type === 'token') {
      streamingMessage.value += data.content
      assistantContent += data.content
      scrollToBottom()
    } else if (data.type === 'done') {
      // 保存消息到数据库
      saveMessage(assistantContent, data.tokenUsed, data.durationMs)
      eventSource.close()
      sending.value = false
      streamingMessage.value = ''
      streamingThink.value = ''
    }
  }

  eventSource.onerror = () => {
    eventSource.close()
    sending.value = false
    streamingMessage.value = ''
    streamingThink.value = ''
  }
}

// 保存AI回复到数据库
const saveMessage = async (content, tokenUsed, durationMs) => {
  try {
    const res = await chatApi.getMessages(currentSessionId.value)
    messages.value = res.result || []
  } catch (e) {
    console.error(e)
  }
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

onMounted(() => {
  loadModels()
  loadSessions()
})
</script>

<style scoped>
.chat-container {
  display: flex;
  height: calc(100vh - 120px);
  background: #1a1a2e;
  border-radius: 12px;
  overflow: hidden;
}

.session-panel {
  width: 240px;
  background: #16213e;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #0f3460;
  color: #e8e8e8;
}

.session-list {
  flex: 1;
  overflow-y: auto;
}

.session-item {
  padding: 12px 16px;
  cursor: pointer;
  color: #a0a0a0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #0f3460;
}

.session-item:hover {
  background: #0f3460;
}

.session-item.active {
  background: #0f3460;
  color: #e8e8e8;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 16px 20px;
  background: #16213e;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #0f3460;
}

.chat-header h2 {
  margin: 0;
  color: #e8e8e8;
  font-size: 18px;
}

.chat-controls {
  display: flex;
  gap: 16px;
  align-items: center;
}

.chat-controls .el-select {
  width: 150px;
}

.chat-controls .el-checkbox {
  color: #e8e8e8;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.message {
  margin-bottom: 16px;
  display: flex;
}

.message.user {
  justify-content: flex-end;
}

.message.assistant {
  justify-content: flex-start;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 16px;
  line-height: 1.5;
}

.message.user .message-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
}

.message.assistant .message-content {
  background: #16213e;
  color: #e8e8e8;
}

.message-text {
  word-break: break-word;
}

.think-content {
  font-style: italic;
  color: #a78bfa;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid #0f3460;
}

.think-label {
  font-size: 12px;
  margin-right: 8px;
}

.message-meta {
  margin-top: 8px;
  font-size: 12px;
  color: #666;
  display: flex;
  gap: 12px;
  align-items: center;
}

.input-area {
  padding: 16px 20px;
  background: #16213e;
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.input-area .el-textarea {
  flex: 1;
}
</style>
```

---

### Task 7: 集成测试

**Files:**
- Modify: `src/main/java/com/xiao/stockproject/controller/ChatController.java` (CORS配置)

- [ ] **Step 1: 添加 CORS 配置**

在已有的 WebMvcConfig 或新建配置类中添加 CORS 配置

- [ ] **Step 2: 启动测试**

1. 启动 Spring Boot 应用
2. 访问 `http://localhost:18888/stock`
3. 打开 Chrome DevTools -> Network 查看 SSE 流式响应
4. 验证模型列表加载、会话创建、消息发送功能

---

## Spec 覆盖检查

- [x] 数据库表设计 (chat_session, chat_message)
- [x] 后端 API (models, sessions, messages, chat)
- [x] 模型选择功能
- [x] 深度思考开关
- [x] 联网搜索开关
- [x] 流式输出
- [x] Token 统计
- [x] 响应时间显示
- [x] 会话管理 (创建、删除、列表)
- [x] 历史消息记录
- [x] 页面美化 (深色主题)

## 执行方式

**Plan complete and saved to `docs/superpowers/plans/2026-06-01-ai-chat-plan.md`. Two execution options:**

**1. Subagent-Driven (recommended)** - I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** - Execute tasks in this session using executing-plans, batch execution with checkpoints

**Which approach?**