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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
@Component
public class OllamaClient implements Closeable {

    public interface SearchDetector {
        void onSearchRequested(String query);
    }

    private final CloseableHttpClient httpClient = HttpClients.createDefault();

    private static final int TIMEOUT_SECONDS = 120;

    public List<String> getModels() {
        try {
            HttpGet request = new HttpGet("http://localhost:11434/api/tags");
            request.setConfig(RequestConfig.custom()
                    .setConnectTimeout(TIMEOUT_SECONDS * 1000)
                    .setSocketTimeout(TIMEOUT_SECONDS * 1000)
                    .build());

            try (CloseableHttpResponse response = httpClient.execute(request)) {
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
                           Consumer<String> onToken, Consumer<String> onThink, Runnable onDone,
                           AtomicBoolean cancelled, SearchDetector searchDetector) {
        log.info("OllamaClient.chatStream called, model={}, think={}, cancelled={}", model, think, cancelled.get());

        try {
            HttpPost request = new HttpPost("http://localhost:11434/api/chat");
            request.setConfig(RequestConfig.custom()
                    .setConnectTimeout(TIMEOUT_SECONDS * 1000)
                    .setSocketTimeout(TIMEOUT_SECONDS * 1000)
                    .build());
            request.setHeader("Content-Type", "application/json");

            // 构建请求体 - 和 code.html 完全一致
            StringBuilder body = new StringBuilder();
            body.append("{\"model\":\"").append(model).append("\",\"messages\":[");
            for (int i = 0; i < messages.size(); i++) {
                java.util.Map<String, String> msg = messages.get(i);
                if (i > 0) body.append(",");
                String content = escapeJson(msg.get("content"));
                body.append("{\"role\":\"").append(msg.get("role")).append("\",\"content\":\"").append(content).append("\"}");
            }
            body.append("],\"stream\":true,\"think\":").append(think ? "true" : "false").append("}");

            log.info("Ollama request: {}", body.toString());

            request.setEntity(new StringEntity(body.toString(), StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
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
                            String thinking = messageObj.getString("thinking");
                            if (thinking != null && !thinking.isEmpty()) {
                                fullThink += thinking;
                                onThink.accept(thinking);
                            }

                            String content = messageObj.getString("content");
                            if (content != null && !content.isEmpty()) {
                                fullContent += content;
                                checkForSearchTag(content, searchDetector, searchBuffer, onToken);
                            }
                        }

                        // 和 code.html 一致: j.done 检测
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

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    @Override
    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
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

        // 如果没有开始/结束标记，输出内容
        if (!text.contains("<SEARCH>") && !text.contains("</SEARCH>")) {
            tokenConsumer.accept(text);
            buffer.setLength(0);
        }
    }
}