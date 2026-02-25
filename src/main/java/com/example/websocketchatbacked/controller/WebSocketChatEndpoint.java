package com.example.websocketchatbacked.controller;

import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@ServerEndpoint(value = "/ws/chat")
@Component
public class WebSocketChatEndpoint {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketChatEndpoint.class);

    private static ChatClient chatClient;

    private static final ConcurrentHashMap<String, AtomicBoolean> sessionActiveMap = new ConcurrentHashMap<>();

    public WebSocketChatEndpoint() {
    }

    public static void setChatClient(ChatClient client) {
        chatClient = client;
    }

    @OnOpen
    public void onOpen(Session session) {
        logger.info("新的WebSocket连接建立: {}", session.getId());
        sessionActiveMap.put(session.getId(), new AtomicBoolean(true));
        handleWelcomeMessage(session);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        logger.info("收到消息: {} 来自会话: {}", message, session.getId());
        String sessionId = session.getId();
        
        try {
            if ("ping".equals(message)) {
                session.getBasicRemote().sendText("pong");
                logger.info("回复ping消息为pong");
            } else {
                handleChatMessage(message, session, sessionId);
            }
        } catch (IOException e) {
            logger.error("发送消息失败: {}", e.getMessage());
        }
    }

    private void handleWelcomeMessage(Session session) {
        String sessionId = session.getId();
        String systemPrompt = "你是后台管理系统的AI客服小K，用户进入聊天悬浮窗时，要首先发送一句热情、亲切且简洁的欢迎语，如'您好，欢迎来到后台管理系统，我是小K，有什么可以帮您？'之后回答问题要简洁明了，以帮助用户解决在使用系统中遇到的如权限设置、数据导入导出、功能模块操作等问题为主，语气要专业且友好。";
        
        chatClient.prompt()
            .system(systemPrompt)
            .user("请发送欢迎语")
            .stream()
            .content()
            .publishOn(Schedulers.boundedElastic())
            .doOnNext(chunk -> {
                if (session.isOpen() && sessionActiveMap.getOrDefault(sessionId, new AtomicBoolean(false)).get()) {
                    try {
                        session.getBasicRemote().sendText(chunk);
                    } catch (IOException e) {
                        logger.error("发送欢迎消息失败: {}", e.getMessage());
                        sessionActiveMap.put(sessionId, new AtomicBoolean(false));
                    }
                }
            })
            .doOnComplete(() -> {
                logger.info("欢迎消息流式传输完成: {}", sessionId);
            })
            .doOnError(error -> {
                logger.error("欢迎消息流式传输错误: {}", error.getMessage());
                try {
                    if (session.isOpen()) {
                        session.getBasicRemote().sendText("抱歉，欢迎消息发送失败，请稍后再试。");
                    }
                } catch (IOException e) {
                    logger.error("发送错误消息失败: {}", e.getMessage());
                }
            }).concatWith(Flux.just("[STREAM_END]"))
            .subscribe();
    }

    private void handleChatMessage(String message, Session session, String sessionId) {
        chatClient.prompt()
            .user(message)
            .stream()
            .content()
            .publishOn(Schedulers.boundedElastic())
            .doOnNext(chunk -> {
                if (session.isOpen() && sessionActiveMap.getOrDefault(sessionId, new AtomicBoolean(false)).get()) {
                    try {
                        session.getBasicRemote().sendText(chunk);
                    } catch (IOException e) {
                        logger.error("发送流式数据失败: {}", e.getMessage());
                        sessionActiveMap.put(sessionId, new AtomicBoolean(false));
                    }
                }
            })
            .doOnComplete(() -> {
                logger.info("流式传输完成: {}", sessionId);
            })
            .doOnError(error -> {
                logger.error("流式传输错误: {}", error.getMessage());
                try {
                    if (session.isOpen()) {
                        session.getBasicRemote().sendText("抱歉，AI服务暂时不可用，请稍后再试。");
                    }
                } catch (IOException e) {
                    logger.error("发送错误消息失败: {}", e.getMessage());
                }
            }).concatWith(Flux.just("[STREAM_END]"))
            .subscribe();
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        String sessionId = session.getId();
        logger.info("WebSocket连接关闭: {}，原因: {}", sessionId, reason.getReasonPhrase());
        sessionActiveMap.remove(sessionId);
    }

}
