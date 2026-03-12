package com.example.websocketchatbacked.config;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    // TODO:整理下chatClint的配置，默认配置应该写在这里，而不是在WebSocketChatEndpoint中

    @Bean
    public ChatMemory chatMemory() {
        // Tips: ChatMemoryRepository可以自行设置实现类，当前根据默认实现类InMemoryChatMemoryRepository，存储在内存中，项目重启就会清除,根据项目网页端短时对话特点，应该在Redis中存储，并且设置过期时间与用户身份强关联
        return MessageWindowChatMemory.builder().chatMemoryRepository(new InMemoryChatMemoryRepository()).maxMessages(10).build();
    }

    @Bean
    public ChatClient dashScopeChatClient(DashScopeChatModel dashScopeChatModel, ChatMemory chatMemory) {
        return ChatClient.builder(dashScopeChatModel).defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(), new SimpleLoggerAdvisor()).build();
    }

}
