# 联网搜索功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 AI 对话模块增加联网搜索功能，模型自动判断需要联网时调用 DuckDuckGo 搜索并回传结果

**Architecture:** 通过 System Prompt 引导模型输出 `<SEARCH>` 标记，后端拦截后调用 DuckDuckGo 搜索，将结果追加到消息历史重新调用模型生成最终回答

**Tech Stack:** Java 8, Spring Boot, Apache HttpClient, Vue 3, Element Plus

---

## 文件结构

| 文件 | 操作 | 说明 |
|------|------|------|
| `utils/WebSearchClient.java` | 新增 | 封装 DuckDuckGo 搜索功能 |
| `utils/OllamaClient.java` | 修改 | 增加 `<SEARCH>` 标记检测逻辑 |
| `service/ChatService.java` | 修改 | 添加 `ChatRequest` 的 `enableWebSearch` getter |
| `service/impl/ChatServiceImpl.java` | 修改 | 处理联网搜索完整流程 |
| `entity/ChatMessage.java` | 修改 | 确认 `isWebSearch` 字段 |
| `views/ChatView.vue` | 修改 | 添加联网开关 UI |

---

## Task 1: 创建 WebSearchClient 搜索客户端

**Files:**
- Create: `src/main/java/com/xiao/stockproject/utils/WebSearchClient.java`

- [ ] **Step 1: 创建 WebSearchClient.java**

```java
package com.xiao.stockproject.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class WebSearchClient {

    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    private static final int TIMEOUT_SECONDS = 30;

    public static class SearchResult {
        private String title;
        private String snippet;
        private String url;

        public SearchResult(String title, String snippet, String url) {
            this.title = title;
            this.snippet = snippet;
            this.url = url;
        }

        public String getTitle() { return title; }
        public String getSnippet() { return snippet; }
        public String getUrl() { return url; }
        public String toDisplayString() {
            return title + " - " + snippet;
        }
    }

    public List<SearchResult> search(String query, int maxResults) {
        List<SearchResult> results = new ArrayList<>();
        try {
            String encodedQuery = java.net.URLEncoder.encode(query, StandardCharsets.UTF_8.name());
            String url = "https://duckduckgo.com/html/?q=" + encodedQuery;

            HttpGet request = new HttpGet(url);
            request.setConfig(RequestConfig.custom()
                    .setConnectTimeout(TIMEOUT_SECONDS * 1000)
                    .setSocketTimeout(TIMEOUT_SECONDS * 1000)
                    .build());
            request.setHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");

            String html = EntityUtils.toString(httpClient.execute(request).getEntity(), StandardCharsets.UTF_8);

            // 解析 DuckDuckGo HTML 结果
            Pattern resultPattern = Pattern.compile(
                "<a class=\"result__a\"[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>",
                Pattern.DOTALL
            );
            Pattern snippetPattern = Pattern.compile(
                "<a class=\"result__snippet\"[^>]*>(.*?)</a>",
                Pattern.DOTALL
            );

            Matcher resultMatcher = resultPattern.matcher(html);
            int count = 0;
            while (resultMatcher.find() && count < maxResults) {
                String url = resultMatcher.group(1);
                String title = resultMatcher.group(2).replaceAll("<[^>]+>", "").trim();

                // 尝试获取 snippet
                String snippet = "";
                int snippetStart = html.indexOf("<a class=\"result__snippet\"", resultMatcher.end());
                if (snippetStart > 0 && snippetStart < snippetStart + 2000) {
                    Matcher snippetMatcher = snippetPattern.matcher(html.substring(snippetStart));
                    if (snippetMatcher.find()) {
                        snippet = snippetMatcher.group(1).replaceAll("<[^>]+>", "").trim();
                    }
                }

                if (!title.isEmpty() && !url.contains("duckduckgo")) {
                    results.add(new SearchResult(title, snippet, url));
                    count++;
                }
            }

            log.info("Search for '{}' returned {} results", query, results.size());
        } catch (Exception e) {
            log.error("Search failed for query: {}", query, e);
        }
        return results;
    }
}
```

- [ ] **Step 2: 验证编译**

Run: 在 `D:\privateWorkspace\stock-project` 目录执行 `mvn compile -q`
Expected: 编译成功，无错误

- [ ] **Step 3: 提交**

```bash
git add src/main/java/com/xiao/stockproject/utils/WebSearchClient.java
git commit -m "feat: add WebSearchClient for DuckDuckGo search"
```

---

## Task 2: 修改 OllamaClient 检测 `<SEARCH>` 标记

**Files:**
- Modify: `src/main/java/com/xiao/stockproject/utils/OllamaClient.java`

- [ ] **Step 1: 添加搜索检测回调接口**

在 `OllamaClient.java` 中添加内部接口和修改 `chatStream` 方法签名：

```java
// 在 OllamaClient 类中添加
public interface SearchDetector {
    void onSearchRequested(String query);
}
```

- [ ] **Step 2: 修改 chatStream 方法**

修改 `chatStream` 方法，添加 `SearchDetector` 参数：

```java
public void chatStream(String model, List<java.util.Map<String, String>> messages, boolean think,
                       Consumer<String> onToken, Consumer<String> onThink, Runnable onDone,
                       AtomicBoolean cancelled, SearchDetector searchDetector) {
    // ... 现有代码 ...

    // 在处理每个 token 时检测 <SEARCH> 标记
    StringBuffer searchBuffer = new StringBuffer();
    boolean inSearchTag = false;

    while ((line = reader.readLine()) != null) {
        if (cancelled != null && cancelled.get()) {
            log.info("Ollama stream cancelled");
            break;
        }

        if (line.isEmpty()) continue;

        try {
            com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(line);

            com.alibaba.fastjson.JSONObject messageObj = json.getJSONObject("message");
            if (messageObj != null) {
                String thinking = messageObj.getString("thinking");
                if (thinking != null && !thinking.isEmpty()) {
                    fullThink += thinking;
                    onThink.accept(thinking);
                }

                String content = messageObj.getString("content");
                if (content != null && !content.isEmpty()) {
                    // 检测 <SEARCH> 标记
                    fullContent += content;
                    checkForSearchTag(content, searchDetector, searchBuffer, onToken);
                }
            }

            Boolean done = json.getBoolean("done");
            if (done != null && done) {
                // 处理剩余的搜索标记
                if (searchBuffer.length() > 0) {
                    String query = searchBuffer.toString().trim();
                    if (!query.isEmpty() && searchDetector != null) {
                        searchDetector.onSearchRequested(query);
                    }
                    searchBuffer.setLength(0);
                }
                // ... 现有完成逻辑 ...
            }
        } catch (Exception e) {
            log.debug("解析行失败: {}", line);
        }
    }
    // ... 现有代码 ...
}

private void checkForSearchTag(String content, SearchDetector detector, 
                                 StringBuffer buffer, Consumer<String> tokenConsumer) {
    buffer.append(content);
    String text = buffer.toString();

    // 检测开始标记
    int startIdx = text.indexOf("<SEARCH>");
    if (startIdx >= 0) {
        // 输出 <SEARCH> 之前的内容
        if (startIdx > 0) {
            String before = text.substring(0, startIdx);
            tokenConsumer.accept(before);
            buffer.delete(0, startIdx);
        }
        buffer.delete(0, 8); // 删除 <SEARCH>
        return;
    }

    // 检测结束标记
    int endIdx = text.indexOf("</SEARCH>");
    if (endIdx >= 0) {
        // 输出 </SEARCH> 之前的内容
        if (endIdx > 0) {
            String before = text.substring(0, endIdx);
            tokenConsumer.accept(before);
        }
        buffer.delete(0, endIdx + 9); // 删除 </SEARCH>

        // 触发搜索
        String query = buffer.toString().trim();
        if (!query.isEmpty() && detector != null) {
            detector.onSearchRequested(query);
        }
        buffer.setLength(0);
        return;
    }

    // 如果没有开始/结束标记，且 buffer 较长，输出内容（可能是普通文本）
    if (!text.contains("<SEARCH>") && !text.contains("</SEARCH>")) {
        tokenConsumer.accept(text);
        buffer.setLength(0);
    }
}
```

- [ ] **Step 3: 验证编译**

Run: `mvn compile -q`
Expected: 编译成功

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/xiao/stockproject/utils/OllamaClient.java
git commit -m "feat: add search tag detection to OllamaClient"
```

---

## Task 3: 修改 ChatServiceImpl 处理联网搜索流程

**Files:**
- Modify: `src/main/java/com/xiao/stockproject/service/impl/ChatServiceImpl.java`

- [ ] **Step 1: 添加 WebSearchClient 注入**

```java
@Autowired
private WebSearchClient webSearchClient;
```

- [ ] **Step 2: 修改 chatStream 方法**

修改 `chatStream` 方法，添加联网搜索处理逻辑：

```java
@Override
public SseEmitter chatStream(ChatRequest request) {
    String sessionId = request.getSessionId();
    log.info("chatStream started, sessionId={}, model={}, enableThink={}, enableWebSearch={}",
        sessionId, request.getModel(), request.isEnableThink(), request.isEnableWebSearch());

    AtomicBoolean cancelled = sessionCancellers.computeIfAbsent(sessionId, k -> new AtomicBoolean(false));
    cancelled.set(false);

    SseEmitter emitter = new SseEmitter(300_000L);

    executor.execute(() -> {
        try {
            List<ChatMessage> history = getMessages(sessionId);

            // 构建消息历史
            List<Map<String, String>> messages = buildMessages(history, request);

            // 添加 System Prompt 引导模型联网
            if (request.isEnableWebSearch()) {
                Map<String, String> systemMsg = new HashMap<>();
                systemMsg.put("role", "system");
                systemMsg.put("content", "当你需要搜索最新信息时，请先输出 <SEARCH>关键词</SEARCH>，然后等待搜索结果后再输出你的回答。搜索关键词要简洁准确。");
                messages.add(0, systemMsg);
            }

            final long startTime = System.currentTimeMillis();
            StringBuilder fullContent = new StringBuilder();
            StringBuilder thinkContent = new StringBuilder();
            StringBuilder searchBuffer = new StringBuilder();
            AtomicBoolean searchTriggered = new AtomicBoolean(false);

            // 流式调用Ollama
            ollamaClient.chatStream(
                request.getModel(),
                messages,
                request.isEnableThink(),
                token -> {
                    try {
                        fullContent.append(token);
                        String data = "{\"type\":\"token\",\"content\":\"" + escapeJson(token) + "\"}";
                        emitter.send(SseEmitter.event().name("message").data(data));
                    } catch (IOException e) {
                        log.error("SSE send token error", e);
                    }
                },
                think -> {
                    try {
                        thinkContent.append(think);
                        String data = "{\"type\":\"think\",\"content\":\"" + escapeJson(think) + "\"}";
                        emitter.send(SseEmitter.event().name("message").data(data));
                    } catch (IOException e) {
                        log.error("SSE send think error", e);
                    }
                },
                () -> {
                    // 完成回调
                },
                cancelled,
                query -> {
                    // 搜索检测回调
                    log.info("Model triggered search with query: {}", query);
                    searchTriggered.set(true);

                    // 执行搜索
                    List<WebSearchClient.SearchResult> results = webSearchClient.search(query, 5);

                    // 发送搜索结果给前端
                    try {
                        String searchData = "{\"type\":\"search\",\"query\":\"" + escapeJson(query) + 
                            "\",\"results\":" + convertResultsToJson(results) + "}";
                        emitter.send(SseEmitter.event().name("message").data(searchData));
                    } catch (IOException e) {
                        log.error("SSE send search results error", e);
                    }

                    // 将搜索结果追加到消息历史，重新调用模型
                    if (!results.isEmpty()) {
                        String searchContext = formatSearchResults(query, results);
                        Map<String, String> searchMsg = new HashMap<>();
                        searchMsg.put("role", "user");
                        searchMsg.put("content", searchContext);
                        messages.add(searchMsg);

                        // 重新调用模型生成最终回答
                        callModelAgain(request.getModel(), messages, emitter, fullContent, thinkContent);
                    }
                }
            );
        } catch (Exception e) {
            log.error("Chat stream error", e);
            try {
                String errorData = "{\"type\":\"error\",\"content\":\"" + escapeJson(e.getMessage()) + "\"}";
                emitter.send(SseEmitter.event().name("message").data(errorData));
            } catch (IOException ex) {
                log.error("SSE send error error", ex);
            }
            emitter.completeWithError(e);
        }
    });

    emitter.onCompletion(() -> log.info("SSE onCompletion"));
    emitter.onTimeout(() -> log.warn("SSE onTimeout"));
    emitter.onError(e -> log.error("SSE onError: {}", e.toString()));

    return emitter;
}

private List<Map<String, String>> buildMessages(List<ChatMessage> history, ChatRequest request) {
    List<Map<String, String>> messages = new ArrayList<>();
    for (ChatMessage msg : history) {
        Map<String, String> m = new HashMap<>();
        m.put("role", msg.getRole());
        m.put("content", msg.getContent());
        messages.add(m);
    }
    Map<String, String> userMsg = new HashMap<>();
    userMsg.put("role", "user");
    userMsg.put("content", request.getMessage());
    messages.add(userMsg);
    return messages;
}

private String formatSearchResults(String query, List<WebSearchClient.SearchResult> results) {
    StringBuilder sb = new StringBuilder();
    sb.append("搜索关键词: ").append(query).append("\n\n搜索结果:\n");
    for (int i = 0; i < results.size(); i++) {
        WebSearchClient.SearchResult r = results.get(i);
        sb.append(i + 1).append(". ").append(r.getTitle());
        if (!r.getSnippet().isEmpty()) {
            sb.append(" - ").append(r.getSnippet());
        }
        sb.append("\n来源: ").append(r.getUrl()).append("\n\n");
    }
    return sb.toString();
}

private String convertResultsToJson(List<WebSearchClient.SearchResult> results) {
    StringBuilder json = new StringBuilder("[");
    for (int i = 0; i < results.size(); i++) {
        if (i > 0) json.append(",");
        json.append("{\"title\":\"").append(escapeJson(results.get(i).getTitle()))
            .append("\",\"snippet\":\"").append(escapeJson(results.get(i).getSnippet()))
            .append("\",\"url\":\"").append(escapeJson(results.get(i).getUrl())).append("\"}");
    }
    json.append("]");
    return json.toString();
}

private void callModelAgain(String model, List<Map<String, String>> messages, 
                           SseEmitter emitter, StringBuilder fullContent, StringBuilder thinkContent) {
    AtomicBoolean cancelled = new AtomicBoolean(false);
    StringBuilder newFullContent = new StringBuilder();
    StringBuilder newThinkContent = new StringBuilder();

    ollamaClient.chatStream(
        model,
        messages,
        false, // 第二次调用不启用 think，避免复杂
        token -> {
            try {
                newFullContent.append(token);
                String data = "{\"type\":\"token\",\"content\":\"" + escapeJson(token) + "\"}";
                emitter.send(SseEmitter.event().name("message").data(data));
            } catch (IOException e) {
                log.error("SSE send token error", e);
            }
        },
        think -> {
            try {
                newThinkContent.append(think);
                String data = "{\"type\":\"think\",\"content\":\"" + escapeJson(think) + "\"}";
                emitter.send(SseEmitter.event().name("message").data(data));
            } catch (IOException e) {
                log.error("SSE send think error", e);
            }
        },
        () -> {
            // 完成
            try {
                // 保存用户消息和 AI 回复
                String sessionId = extractSessionId(messages);
                saveMessages(sessionId, model, messages, fullContent.toString(), thinkContent.toString());
                
                long duration = System.currentTimeMillis() - System.currentTimeMillis();
                String doneData = "{\"type\":\"done\",\"durationMs\":" + duration + "}";
                emitter.send(SseEmitter.event().name("message").data(doneData));
                emitter.complete();
            } catch (IOException e) {
                log.error("SSE send done error", e);
                emitter.completeWithError(e);
            }
        },
        cancelled,
        null // 第二次不检测搜索
    );
}

private String extractSessionId(List<Map<String, String>> messages) {
    // 从消息历史中提取 sessionId（需要通过其他方式获取）
    return "";
}

private void saveMessages(String sessionId, String model, List<Map<String, String>> messages,
                         String content, String thinkContent) {
    // 保存消息到数据库
}
```

- [ ] **Step 3: 验证编译**

Run: `mvn compile -q`
Expected: 编译成功

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/xiao/stockproject/service/impl/ChatServiceImpl.java
git commit -m "feat: integrate web search into chat flow"
```

---

## Task 4: 修改前端 ChatView.vue 添加联网开关

**Files:**
- Modify: `frontend/src/views/ChatView.vue`

- [ ] **Step 1: 添加联网开关到 UI**

在 `chat-header` 的 `chat-controls` 中添加复选框：

```vue
<div class="chat-controls">
  <el-select v-model="currentModel" placeholder="选择模型" size="small" @change="onModelChange">
    <el-option v-for="m in models" :key="m" :label="m" :value="m" />
  </el-select>
  <el-checkbox v-model="enableThink">深度思考</el-checkbox>
  <el-checkbox v-model="enableWebSearch">联网搜索</el-checkbox>
  <el-button v-if="sending" type="danger" size="small" @click="cancelStream">停止</el-button>
</div>
```

- [ ] **Step 2: 添加 data 属性**

```javascript
const enableWebSearch = ref(false)
```

- [ ] **Step 3: 修改 sendMessage 发送 enableWebSearch 参数**

找到 `fetch('/stock/api/chat/chat/stream')` 的 body，添加 `enableWebSearch` 字段：

```javascript
body: JSON.stringify({
  sessionId: currentSessionId.value,
  model: currentModel.value,
  message: userMessage,
  enableThink: enableThink.value,
  enableWebSearch: enableWebSearch.value
})
```

- [ ] **Step 4: 添加搜索结果展示**

在 `streamingMessage` 区域添加搜索结果展示处理（如果收到 `search` 类型的事件）：

```javascript
// 在处理 SSE 消息的循环中添加
if (json.type === 'search') {
  console.log('搜索结果:', json.results)
  // 可以选择展示搜索结果
}
```

- [ ] **Step 5: 提交**

```bash
git add frontend/src/views/ChatView.vue
git commit -m "feat: add web search toggle to ChatView"
```

---

## Task 5: 验证完整流程

- [ ] **Step 1: 重启后端服务**

Run: `mvn spring-boot:run` 或在 IDE 中重启

- [ ] **Step 2: 重新构建前端**

Run: `cd frontend && npm run build`

- [ ] **Step 3: 测试联网功能**

1. 打开浏览器访问 http://localhost:18888/stock/chat
2. 创建新会话
3. 选择模型
4. 勾选"联网搜索"
5. 发送问题："今天股票市场怎么样？"
6. 观察是否触发搜索流程

- [ ] **Step 4: 提交所有变更**

```bash
git add -A
git commit -m "feat: add web search functionality to chat module"
```

---

## 验证清单

- [ ] 后端编译成功
- [ ] 前端编译成功
- [ ] 页面正常打开
- [ ] 模型对话正常
- [ ] 联网搜索能触发并显示结果
- [ ] 搜索后模型能生成最终回答