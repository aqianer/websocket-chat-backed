package com.example.websocketchatbacked;

import com.example.websocketchatbacked.controller.ws.WebSocketChatEndpoint;
import com.example.websocketchatbacked.service.UserQueryService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class WebSocketChatBackedApplication {

    private final ChatClient dashScopeChatClient;
    private final UserQueryService userQueryService;
    private final ChatMemory chatMemory;

    public WebSocketChatBackedApplication(ChatClient chatClient, UserQueryService userQueryService, ChatMemory chatMemory) {
        this.dashScopeChatClient = chatClient;
        this.userQueryService = userQueryService;
        this.chatMemory = chatMemory;
    }

    public static void main(String[] args) {
        SpringApplication.run(WebSocketChatBackedApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initWebSocketEndpoint() {
        WebSocketChatEndpoint.setChatClient(dashScopeChatClient);

        WebSocketChatEndpoint.setUserQueryService(userQueryService);
        WebSocketChatEndpoint.setChatMemory(chatMemory);
    }

}
