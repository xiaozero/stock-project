package com.xiao.stockproject.service;

import com.xiao.stockproject.entity.ChatMessage;
import com.xiao.stockproject.entity.ChatSession;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface ChatService {
    List<ChatSession> getSessions();

    List<ChatMessage> getMessages(String sessionId);

    String createSession();

    void deleteSession(String sessionId);

    SseEmitter chatStream(ChatRequest request);

    class ChatRequest {
        private String sessionId;
        private String model;
        private String message;
        private boolean enableThink;
        private boolean enableWebSearch;
        private String image; // base64 encoded image

        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public boolean isEnableThink() { return enableThink; }
        public void setEnableThink(boolean enableThink) { this.enableThink = enableThink; }
        public boolean isEnableWebSearch() { return enableWebSearch; }
        public void setEnableWebSearch(boolean enableWebSearch) { this.enableWebSearch = enableWebSearch; }
        public String getImage() { return image; }
        public void setImage(String image) { this.image = image; }
    }
}