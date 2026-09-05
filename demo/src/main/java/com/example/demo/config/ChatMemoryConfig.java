package com.example.demo.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatMemoryConfig {

    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository){
        return MessageWindowChatMemory.builder()
                .maxMessages(10)
                .chatMemoryRepository(jdbcChatMemoryRepository).build();
    }
}
