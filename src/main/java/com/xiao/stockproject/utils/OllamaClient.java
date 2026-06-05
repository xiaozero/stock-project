package com.xiao.stockproject.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
@Component
public class OllamaClient implements Closeable {

    public interface SearchDetector {
        void onSearchRequested(String query);
    }

    // 工具调用回调接口
    public interface ToolCallHandler {
        void onToolCall(String toolName, String arguments, List<Map<String, String>> messages, AtomicBoolean cancelled);
    }

    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    private static final int TIMEOUT_SECONDS = 120;

    // 每次请求创建新的 HttpClient，避免状态问题
    private CloseableHttpClient createHttpClient() {
        return HttpClients.createDefault();
    }

    public List<String> getModels() {
        try (CloseableHttpClient client = createHttpClient()) {
            HttpGet request = new HttpGet("http://localhost:11434/api/tags");
            request.setConfig(RequestConfig.custom()
                    .setConnectTimeout(TIMEOUT_SECONDS * 1000)
                    .setSocketTimeout(TIMEOUT_SECONDS * 1000)
                    .build());

            try (CloseableHttpResponse response = client.execute(request)) {
                String json = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                log.info("Ollama models response: {}", json);

                List<String> models = new ArrayList<>();

                // 解析 JSON，尝试多种方式确保不遗漏模型
                try {
                    com.alibaba.fastjson.JSONObject jsonObj = com.alibaba.fastjson.JSON.parseObject(json);
                    if (jsonObj != null) {
                        // 方式1: 直接从 "models" 数组获取
                        com.alibaba.fastjson.JSONArray modelsArray = jsonObj.getJSONArray("models");
                        if (modelsArray != null && modelsArray.size() > 0) {
                            log.info("Found {} models in 'models' array", modelsArray.size());
                            for (int i = 0; i < modelsArray.size(); i++) {
                                Object item = modelsArray.get(i);
                                if (item instanceof com.alibaba.fastjson.JSONObject) {
                                    String name = ((com.alibaba.fastjson.JSONObject) item).getString("name");
                                    if (name != null && !name.isEmpty()) {
                                        models.add(name);
                                        log.info("Found model[{}]: {}", i, name);
                                    }
                                } else if (item instanceof String) {
                                    models.add((String) item);
                                    log.info("Found model[{}]: {}", i, item);
                                }
                            }
                        } else {
                            log.warn("No 'models' array found or it's empty in response");
                        }
                    }
                } catch (Exception e) {
                    log.error("解析模型列表失败: {}", e.getMessage());
                }

                if (models.isEmpty()) {
                    log.warn("模型列表为空，请检查 Ollama 是否正常运行");
                }
                return models;
            }
        } catch (Exception e) {
            log.error("获取Ollama模型列表失败", e);
        }
        return new ArrayList<>();
    }

    public void chatStream(String model, List<java.util.Map<String, String>> messages, boolean think,
                           List<Object> lastMessageContent, // 支持多模态内容
                           Consumer<String> onToken, Consumer<String> onThink, Runnable onDone,
                           AtomicBoolean cancelled, SearchDetector searchDetector, ToolCallHandler toolCallHandler) {
        log.info("OllamaClient.chatStream called, model={}, think={}, cancelled={}, hasToolHandler={}",
            model, think, cancelled.get(), toolCallHandler != null);

        // 构建请求体 - 根据模型类型设置不同的思考参数
        StringBuilder body = new StringBuilder();
        body.append("{\"model\":\"").append(model).append("\",\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            java.util.Map<String, String> msg = messages.get(i);
            if (i > 0) body.append(",");
            String content = escapeJson(msg.get("content"));
            body.append("{\"role\":\"").append(msg.get("role")).append("\",\"content\":\"").append(content).append("\"}");
        }
        // 添加最后一条消息（可能包含图片）
        if (lastMessageContent != null && !lastMessageContent.isEmpty()) {
            body.append(",{\"role\":\"user\",\"content\":[");
            boolean firstItem = true;
            for (Object item : lastMessageContent) {
                if (!firstItem) body.append(",");
                firstItem = false;
                if (item instanceof String) {
                    body.append("{\"type\":\"text\",\"text\":\"").append(escapeJson((String) item)).append("\"}");
                } else if (item instanceof java.util.Map) {
                    java.util.Map<String, Object> mapItem = (java.util.Map<String, Object>) item;
                    Object type = mapItem.get("type");
                    Object data = mapItem.get("data");
                    if ("image".equals(type) && data != null) {
                        body.append("{\"type\":\"image\",\"data\":\"").append(String.valueOf(data)).append("\"}");
                    } else {
                        body.append("{\"type\":\"text\",\"text\":\"").append(escapeJson(String.valueOf(data))).append("\"}");
                    }
                }
            }
            body.append("]}");
        }
        body.append("],\"stream\":true");

        // 根据模型类型设置思考参数
        if (model.toLowerCase().contains("deepseek")) {
            // deepseek-r1 使用 thinking 对象
            if (think) {
                body.append(",\"thinking\":{\"enabled\":true,\"depth\":5}");
            } else {
                body.append(",\"thinking\":{\"enabled\":false,\"depth\":0}");
            }
        } else {
            // qwen3.5 等其他模型使用 think 布尔值
            body.append(",\"think\":").append(think ? "true" : "false");
        }

        if (searchDetector != null) {
            body.append(",\"search\":true");
        }

        // 如果提供了工具调用处理器，添加多种工具
        if (toolCallHandler != null) {
            // 定义多个工具，让模型根据查询内容选择
            String tools = "["
                + "{\"type\":\"function\",\"function\":{\"name\":\"get_weather\",\"description\":\"获取指定城市的天气信息\",\"parameters\":{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\",\"description\":\"城市名称，例如：北京、上海、东京\"}},\"required\":[\"city\"]}}}"
                + ",{\"type\":\"function\",\"function\":{\"name\":\"search_stock\",\"description\":\"搜索股票信息和实时行情\",\"parameters\":{\"type\":\"object\",\"properties\":{\"keyword\":{\"type\":\"string\",\"description\":\"股票代码或名称，例如：贵州茅台、000001、腾讯\"}},\"required\":[\"keyword\"]}}}"
                + ",{\"type\":\"function\",\"function\":{\"name\":\"web_search\",\"description\":\"进行网络搜索，获取最新信息\",\"parameters\":{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\",\"description\":\"搜索关键词或问题\"}},\"required\":[\"query\"]}}}"
                + "]";
            body.append(",\"tools\":").append(tools);
        }
        body.append("}");

        log.info("Ollama request body: {}", body.toString());

        try {
            HttpPost request = new HttpPost("http://localhost:11434/api/chat");
            request.setConfig(RequestConfig.custom()
                    .setConnectTimeout(TIMEOUT_SECONDS * 1000)
                    .setSocketTimeout(TIMEOUT_SECONDS * 1000)
                    .build());
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(body.toString(), StandardCharsets.UTF_8));

            try (CloseableHttpClient client = createHttpClient();
                 CloseableHttpResponse response = client.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                log.info("Ollama response status: {}", statusCode);

                if (statusCode != 200) {
                    log.error("Ollama returned error status: {}", statusCode);
                    onDone.run();
                    return;
                }

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8));

                String line;
                String fullContent = "";
                String fullThink = "";
                int totalPromptTokens = 0;
                int totalEvalTokens = 0;
                StringBuffer searchBuffer = new StringBuffer();

                while ((line = reader.readLine()) != null) {
                    // 检查是否取消
                    if (cancelled != null && cancelled.get()) {
                        log.info("Ollama stream cancelled");
                        break;
                    }

                    if (line.isEmpty()) continue;

                    try {
                        com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(line);

                        // 和 code.html 一致: j.message?.thinking
                        com.alibaba.fastjson.JSONObject messageObj = json.getJSONObject("message");
                        if (messageObj != null) {
                            // 根据模型类型处理思考内容
                            String thinking = messageObj.getString("thinking");
                            if (thinking != null && !thinking.isEmpty()) {
                                if (model.toLowerCase().contains("deepseek")) {
                                    // deepseek-r1: thinking 字段直接包含思考内容
                                    if (think) {
                                        fullThink += thinking;
                                        onThink.accept(thinking);
                                    }
                                } else {
                                    // qwen3.5: thinking 是对象，需要处理
                                    if (think) {
                                        fullThink += thinking;
                                        onThink.accept(thinking);
                                    }
                                }
                            }

                            String content = messageObj.getString("content");
                            if (content != null && !content.isEmpty()) {
                                log.info("Received content from model: {}", content.substring(0, Math.min(100, content.length())));
                                fullContent += content;
                                checkForSearchTag(content, searchDetector, searchBuffer, onToken);
                            } else {
                                log.info("Content is empty, checking for tool_calls");
                            }

                            // 处理工具调用
                            if (toolCallHandler != null) {
                                com.alibaba.fastjson.JSONArray toolCalls = messageObj.getJSONArray("tool_calls");
                                if (toolCalls != null && !toolCalls.isEmpty()) {
                                    log.info("Received tool_calls: {}", toolCalls.toJSONString());
                                    for (int i = 0; i < toolCalls.size(); i++) {
                                        com.alibaba.fastjson.JSONObject toolCall = toolCalls.getJSONObject(i);
                                        com.alibaba.fastjson.JSONObject function = toolCall.getJSONObject("function");
                                        if (function != null) {
                                            String toolName = function.getString("name");
                                            String arguments = function.getString("arguments");
                                            log.info("Tool call: name={}, arguments={}", toolName, arguments);

                                            // 调用工具处理 handler，handler 会重新调用模型
                                            toolCallHandler.onToolCall(toolName, arguments, messages, cancelled);
                                            return; // 工具调用后返回，由 handler 重新调用模型
                                        }
                                    }
                                }
                            }
                        } else {
                            log.warn("messageObj is null in response line: {}", line);
                        }

                        // 如果 done 为 true 且已处理工具调用，则不调用 onDone
                        Boolean done = json.getBoolean("done");
                        if (done != null && done) {
                            // 和 code.html 一致: j.prompt_eval_count, j.eval_count
                            Integer promptEvalCount = json.getInteger("prompt_eval_count");
                            Integer evalCount = json.getInteger("eval_count");
                            if (promptEvalCount != null) totalPromptTokens = promptEvalCount;
                            if (evalCount != null) totalEvalTokens = evalCount;

                            log.info("Ollama stream done, promptTokens={}, evalTokens={}", totalPromptTokens, totalEvalTokens);
                            // 处理剩余的搜索标记
                            if (searchBuffer.length() > 0) {
                                String query = searchBuffer.toString().trim();
                                if (!query.isEmpty() && searchDetector != null) {
                                    searchDetector.onSearchRequested(query);
                                }
                                searchBuffer.setLength(0);
                            }
                            onDone.run();
                            return;
                        }
                    } catch (Exception e) {
                        log.debug("解析行失败: {}", line);
                    }
                }
                log.info("Ollama stream finished normally, content length={}", fullContent.length());
                onDone.run();
            }
        } catch (Exception e) {
            log.error("Ollama chat流式失败: {}", e.getMessage(), e);
            onDone.run();
        }
    }

    /**
     * 调用大模型进行股票分析（同步方式）
     * @param prompt 分析提示词
     * @return 分析结果
     */
    public String analyzeStock(String prompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);

        StringBuilder result = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);

        chatStream(
            "qwen3.5:0.8b",
            messages,
            false,
            null,
            token -> result.append(token),
            think -> {},
            () -> latch.countDown(),
            new AtomicBoolean(false),
            null,
            null
        );

        try {
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return result.toString();
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    @Override
    public void close() throws IOException {
        // httpClient 现在每次请求创建，不再需要关闭
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

            // 在删除标记之前提取查询内容
            String remainingAfterEnd = text.substring(endIdx + 9);
            String query = remainingAfterEnd.trim();

            buffer.delete(0, endIdx + 9); // 删除 </SEARCH> 及其之前的内容

            // 触发搜索
            if (!query.isEmpty() && detector != null) {
                detector.onSearchRequested(query);
            }
            buffer.setLength(0);
            return;
        }

        // 如果没有开始/结束标记，输出内容
        if (!text.contains("<SEARCH>") && !text.contains("</SEARCH>")) {
            tokenConsumer.accept(text);
            buffer.setLength(0);
        }
    }
}