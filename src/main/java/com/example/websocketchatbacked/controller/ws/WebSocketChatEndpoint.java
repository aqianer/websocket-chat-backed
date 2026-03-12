package com.example.websocketchatbacked.controller.ws;

import com.example.websocketchatbacked.service.UserQueryService;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
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

    private static UserQueryService userQueryService;

    private static ChatMemory chatMemory;

    private static final ConcurrentHashMap<String, AtomicBoolean> sessionActiveMap = new ConcurrentHashMap<>();

    public WebSocketChatEndpoint() {
    }

    public static void setChatClient(ChatClient client) {
        chatClient = client;
    }

    public static void setUserQueryService(UserQueryService service) {
        userQueryService = service;
    }

    public static void setChatMemory(ChatMemory memory) {
        chatMemory = memory;
    }

    @OnOpen
    public void onOpen(Session session) {
        logger.info("新的WebSocket连接建立: {}", session.getId());
        sessionActiveMap.put(session.getId(), new AtomicBoolean(true));
        // TODO : 前后端的交互有问题，每点击一次聊天浮窗就会新建一个session，预期是只有用户退出登录才会注销session，连接过以后就不再onOpen, 待处理
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

        // TODO: 系统提示词的优化思路，目前只使用一个系统提示词
        String systemPrompt = "你是后台管理系统的AI客服小K，用户进入聊天悬浮窗时，要首先发送一句热情、亲切且简洁的欢迎语，如'您好，欢迎来到后台管理系统，我是小K，有什么可以帮您？'之后回答问题要简洁明了，以帮助用户解决在使用系统中遇到的如查询请求等问题为主，语气要专业且友好。";

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
                // Tips : CONVERSATION_ID是chatmemory的默认参数，可以自定义
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, sessionId))
                .tools(userQueryService)
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
