package com.xiao.stockproject.controller;

import com.xiao.stockproject.entity.ChatMessage;
import com.xiao.stockproject.entity.ChatSession;
import com.xiao.stockproject.service.ChatService;
import com.xiao.stockproject.service.ChatService.ChatRequest;
import com.xiao.stockproject.service.impl.ChatServiceImpl;
import com.xiao.stockproject.utils.OllamaClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatServiceImpl chatServiceImpl;

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

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        log.info("chatStream called, sessionId={}, model={}, enableThink={}, enableWebSearch={}",
            request.getSessionId(), request.getModel(), request.isEnableThink(), request.isEnableWebSearch());
        return chatService.chatStream(request);
    }

    @PostMapping("/chat/cancel/{sessionId}")
    public void cancelStream(@PathVariable String sessionId) {
        log.info("cancelStream called for sessionId={}", sessionId);
        chatServiceImpl.cancelStream(sessionId);
    }
}