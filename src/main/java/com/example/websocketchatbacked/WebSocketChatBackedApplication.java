package com.example.websocketchatbacked;

import com.example.websocketchatbacked.controller.WebSocketChatEndpoint;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication(exclude = {
    org.springframework.cloud.function.context.config.ContextFunctionCatalogAutoConfiguration.class
})
public class WebSocketChatBackedApplication {

    private final ChatClient.Builder chatClientBuilder;

    public WebSocketChatBackedApplication(ChatClient.Builder chatClientBuilder) {
        this.chatClientBuilder = chatClientBuilder;
    }

    public static void main(String[] args) {
        SpringApplication.run(WebSocketChatBackedApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initWebSocketEndpoint() {
        WebSocketChatEndpoint.setChatClient(chatClientBuilder.build());
    }

}
