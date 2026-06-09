# AI 对话模块设计方案

## 概述

基于 Ollama 本地大模型的对话模块，支持模型选择、深度思考、联网搜索、会话管理和历史记录。

## 数据库设计

### 表 1: chat_session（会话表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT | 主键，自增 |
| session_id | VARCHAR(64) | 会话唯一标识(UUID) |
| title | VARCHAR(255) | 会话标题(首条消息前20字) |
| created_time | DATETIME | 创建时间 |
| updated_time | DATETIME | 最后更新时间 |

### 表 2: chat_message（消息表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT | 主键，自增 |
| session_id | VARCHAR(64) | 关联会话UUID |
| role | VARCHAR(20) | user / assistant |
| content | TEXT | 消息内容 |
| model_name | VARCHAR(64) | 使用的模型名 |
| think_content | TEXT | 思考过程(可空) |
| token_used | INT | 消耗token数 |
| duration_ms | INT | 响应耗时(毫秒) |
| is_web_search | TINYINT | 是否联网搜索(0否/1是) |
| created_time | DATETIME | 创建时间 |

## 后端 API

### 1. 获取模型列表
```
GET /api/chat/models
Response: { code: 200, result: ["qwen2.5:7b", "llama3:8b", ...] }
```

### 2. 获取会话列表
```
GET /api/chat/sessions
Response: { code: 200, result: [{ sessionId, title, updatedTime }, ...] }
```

### 3. 获取会话消息
```
GET /api/chat/sessions/{sessionId}/messages
Response: { code: 200, result: [{ id, role, content, thinkContent, tokenUsed, durationMs, createdTime }, ...] }
```

### 4. 新建会话
```
POST /api/chat/session
Response: { code: 200, result: { sessionId: "uuid" } }
```

### 5. 删除会话
```
DELETE /api/chat/session/{sessionId}
Response: { code: 200 }
```

### 6. 发送消息（流式SSE）
```
POST /api/chat/chat
Body: { sessionId, model, message, enableThink, enableWebSearch }
Response: SSE stream
  - event: think - data: { content: "思考中..." }
  - event: token - data: { content: "逐字输出" }
  - event: done  - data: { tokenUsed: 1234, durationMs: 2300 }
```

## 前端页面设计

### 布局
```
┌────────────────────────────────────────────────────────────┐
│  AI 对话                     [新建会话]  [删除会话]       │
├─────────────┬────────────────────────────────────────────┤
│             │  ┌──────────────────────────────────────┐ │
│  会话列表    │  │  模型: [qwen2.5:7b    ▼]              │ │
│             │  │  □ 深度思考   □ 联网搜索              │ │
│  ○ 会话1    │  └──────────────────────────────────────┘ │
│  ● 会话2    │                                            │
│  ○ 会话3    │  ┌─ 用户消息 ────────────────────────────┐│
│             │  │ 今天大盘怎么样？           10:30:15   ││
│             │  └──────────────────────────────────────┘│
│             │                                            │
│             │  ┌─ AI 思考 ────────────────────────────┐│
│             │  │ 正在分析市场数据... (打字效果)        ││
│             │  └──────────────────────────────────────┘│
│             │                                            │
│             │  ┌─ AI 回答 ────────────────────────────┐│
│             │  │ 根据今日行情分析...                   ││
│             │  │                          10:30:18    ││
│             │  │ Token: 1234 | 耗时: 2.3s | 🔍联网   ││
│             │  └──────────────────────────────────────┘│
│             │                                            │
├─────────────┴────────────────────────────────────────────┤
│  [输入消息...]                                 [发送]    │
└────────────────────────────────────────────────────────────┘
```

### 样式美化
- 整体：深色主题(#1a1a2e背景)，渐变卡片
- 聊天区域：左侧用户消息(蓝色渐变)、右侧AI消息(灰色渐变)
- 思考过程：斜体紫色文字，带加载动画
- 消息气泡：圆角16px，带阴影和微光效果
- 输入框：底部固定，毛玻璃效果
- Token/耗时：小型标签显示
- 会话列表：悬浮高亮，选中状态高亮

## 技术实现

### 后端技术
- Spring Boot + MyBatis-Plus
- WebClient 调用 Ollama API
- SSE 流式响应
- DuckDuckGo API 联网搜索（免费，无需Key）
- ChatGPT-API-Free 或 直接HTTP调用搜索

### 前端技术
- Vue 3 + Element Plus
- ECharts 暂无需求
- 原生 EventSource 处理 SSE
- 流式文字渲染组件

### Ollama API 调用

**聊天接口**：
```json
POST http://localhost:11434/api/chat
{
  "model": "qwen2.5:7b",
  "messages": [{"role": "user", "content": "..."}],
  "stream": true,
  "think": true
}
```

**获取模型**：
```json
GET http://localhost:11434/api/tags
```

### 联网搜索流程

1. 用户勾选"联网搜索"，后端先调 DuckDuckGo API
2. 将搜索结果格式化为 context
3. 将 context 加入 system prompt 或首条消息
4. 调用 Ollama 获取回复

### Token 统计

Ollama 响应头包含 `Ollama-Metrics`:
```
Ollama-Metrics: prompt_eval_count=100, prompt_eval_duration=1.2s, eval_count=200, eval_duration=2.3s
```
token_used = prompt_eval_count + eval_count

## 实施步骤

1. 数据库新建表 chat_session, chat_message
2. 后端：实体类、Mapper、Service
3. 后端：Ollama 客户端封装
4. 后端：ChatController 实现所有API
5. 后端：联网搜索功能（选做）
6. 前端：页面布局和样式
7. 前端：SSE 流式接收和渲染
8. 前端：会话管理功能
9. 测试联调

## 文件清单

### 后端新增
- `entity/ChatSession.java`
- `entity/ChatMessage.java`
- `mapper/ChatSessionMapper.java`
- `mapper/ChatMessageMapper.java`
- `service/ChatService.java`
- `service/impl/ChatServiceImpl.java`
- `controller/ChatController.java`
- `utils/OllamaClient.java`
- `utils/WebSearchUtils.java`

### 前端修改
- `views/ChatView.vue` - 重写
- `api/chat.js` - 新增

### 数据库
- `chat_session` 表
- `chat_message` 表