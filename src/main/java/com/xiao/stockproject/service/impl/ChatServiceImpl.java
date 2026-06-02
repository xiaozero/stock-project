package com.xiao.stockproject.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xiao.stockproject.entity.ChatMessage;
import com.xiao.stockproject.entity.ChatSession;
import com.xiao.stockproject.mapper.ChatMessageMapper;
import com.xiao.stockproject.mapper.ChatSessionMapper;
import com.xiao.stockproject.service.ChatService;
import com.xiao.stockproject.utils.OllamaClient;
import com.xiao.stockproject.utils.WebSearchClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatSessionMapper sessionMapper;

    @Autowired
    private ChatMessageMapper messageMapper;

    @Autowired
    private OllamaClient ollamaClient;

    @Autowired
    private WebSearchClient webSearchClient;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    // 用于跟踪每个 session 的取消状态
    private final ConcurrentHashMap<String, AtomicBoolean> sessionCancellers = new ConcurrentHashMap<>();

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
        sessionCancellers.remove(sessionId);
    }

    public void cancelStream(String sessionId) {
        AtomicBoolean canceller = sessionCancellers.get(sessionId);
        if (canceller != null) {
            canceller.set(true);
        }
    }

    @Override
    public SseEmitter chatStream(ChatRequest request) {
        String sessionId = request.getSessionId();
        log.info("chatStream started, sessionId={}, model={}, enableThink={}",
            sessionId, request.getModel(), request.isEnableThink());

        // 获取或创建该 session 的取消标志
        AtomicBoolean cancelled = sessionCancellers.computeIfAbsent(sessionId, k -> new AtomicBoolean(false));
        cancelled.set(false); // 重置取消状态

        SseEmitter emitter = new SseEmitter(300_000L);
        AtomicBoolean searchTriggered = new AtomicBoolean(false);

        executor.execute(() -> {
            List<ChatMessage> history = getMessages(sessionId);

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

            // Issue 2 fix: use a copy to avoid mutation during iteration
            final List<Map<String, String>> messagesCopy = new ArrayList<>(messages);

            final long startTime = System.currentTimeMillis();
            StringBuilder fullContent = new StringBuilder();
            StringBuilder thinkContent = new StringBuilder();

            try {
                // 保存用户消息
                ChatMessage userMsgEntity = new ChatMessage();
                userMsgEntity.setSessionId(sessionId);
                userMsgEntity.setRole("user");
                userMsgEntity.setContent(request.getMessage());
                userMsgEntity.setModelName(request.getModel());
                userMsgEntity.setCreatedTime(LocalDateTime.now());
                userMsgEntity.setIsWebSearch(request.isEnableWebSearch() ? 1 : 0);
                messageMapper.insert(userMsgEntity);
                log.info("User message saved");

                // 如果启用联网搜索，在消息前添加系统提示
                if (request.isEnableWebSearch()) {
                    Map<String, String> systemMsg = new HashMap<>();
                    systemMsg.put("role", "system");
                    systemMsg.put("content", "当你需要搜索最新信息时，请先输出 <SEARCH>关键词</SEARCH>，然后等待搜索结果后再输出你的回答。搜索关键词要简洁准确。");
                    messages.add(0, systemMsg);
                }

                // 流式调用Ollama，传递取消标志和搜索检测回调
                ollamaClient.chatStream(
                    request.getModel(),
                    messagesCopy,
                    request.isEnableThink(),
                    token -> {
                        // 普通token
                        try {
                            fullContent.append(token);
                            String data = "{\"type\":\"token\",\"content\":\"" + escapeJson(token) + "\"}";
                            emitter.send(SseEmitter.event().name("message").data(data));
                        } catch (IOException e) {
                            log.error("SSE send token error", e);
                        }
                    },
                    think -> {
                        // think内容
                        try {
                            thinkContent.append(think);
                            String data = "{\"type\":\"think\",\"content\":\"" + escapeJson(think) + "\"}";
                            emitter.send(SseEmitter.event().name("message").data(data));
                        } catch (IOException e) {
                            log.error("SSE send think error", e);
                        }
                    },
                    () -> {
                        // 完成
                        // Issue 3 fix: 如果已经触发了搜索流程，不要再处理完成
                        if (searchTriggered.get()) {
                            return;
                        }
                        try {
                            long duration = System.currentTimeMillis() - startTime;
                            log.info("Ollama stream completed, duration={}ms, content length={}", duration, fullContent.length());

                            ChatMessage aiMsg = new ChatMessage();
                            aiMsg.setSessionId(sessionId);
                            aiMsg.setRole("assistant");
                            aiMsg.setContent(fullContent.toString());
                            aiMsg.setModelName(request.getModel());
                            aiMsg.setThinkContent(thinkContent.toString());
                            aiMsg.setDurationMs((int) duration);
                            aiMsg.setCreatedTime(LocalDateTime.now());
                            aiMsg.setIsWebSearch(request.isEnableWebSearch() ? 1 : 0);
                            messageMapper.insert(aiMsg);
                            log.info("AI response saved to DB");

                            updateSessionTitle(sessionId, request.getMessage());

                            String doneData = "{\"type\":\"done\",\"durationMs\":" + duration + ",\"contentLength\":" + fullContent.length() + "}";
                            emitter.send(SseEmitter.event().name("message").data(doneData));
                            emitter.complete();
                            log.info("SSE emitter completed");
                        } catch (IOException e) {
                            log.error("SSE send done error", e);
                            emitter.completeWithError(e);
                        }
                    },
                    cancelled,
                    query -> {
                        // 搜索检测回调 - 模型触发搜索
                        log.info("Model triggered search with query: {}", query);
                        searchTriggered.set(true);

                        // 执行搜索
                        List<WebSearchClient.SearchResult> results = webSearchClient.search(query, 5);

                        // 发送搜索结果到前端
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
                            callModelAgain(request.getModel(), messages, emitter);
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

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }

    private void updateSessionTitle(String sessionId, String firstMessage) {
        ChatSession session = sessionMapper.selectOne(new QueryWrapper<ChatSession>().eq("session_id", sessionId));
        if (session != null && "新会话".equals(session.getTitle())) {
            String title = firstMessage.length() > 20 ? firstMessage.substring(0, 20) + "..." : firstMessage;
            session.setTitle(title);
            session.setUpdatedTime(LocalDateTime.now());
            sessionMapper.updateById(session);
        }
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

    private void callModelAgain(String model, List<Map<String, String>> messages, SseEmitter emitter) {
        // Issue 1 fix: check if emitter was already completed to avoid double completion
        AtomicBoolean emitterCompleted = new AtomicBoolean(false);
        if (emitterCompleted.getAndSet(true)) {
            return;
        }

        // 重新调用模型，这次不检测搜索
        AtomicBoolean cancelled = new AtomicBoolean(false);

        ollamaClient.chatStream(
            model,
            messages,
            false, // 不启用 think
            token -> {
                try {
                    String data = "{\"type\":\"token\",\"content\":\"" + escapeJson(token) + "\"}";
                    emitter.send(SseEmitter.event().name("message").data(data));
                } catch (IOException e) {
                    log.error("SSE send token error", e);
                }
            },
            think -> {
                try {
                    String data = "{\"type\":\"think\",\"content\":\"" + escapeJson(think) + "\"}";
                    emitter.send(SseEmitter.event().name("message").data(data));
                } catch (IOException e) {
                    log.error("SSE send think error", e);
                }
            },
            () -> {
                try {
                    String doneData = "{\"type\":\"done\"}";
                    emitter.send(SseEmitter.event().name("message").data(doneData));
                    emitter.complete();
                } catch (IOException e) {
                    log.error("SSE complete error", e);
                    emitter.completeWithError(e);
                }
            },
            cancelled,
            null // 不检测搜索
        );
    }
}