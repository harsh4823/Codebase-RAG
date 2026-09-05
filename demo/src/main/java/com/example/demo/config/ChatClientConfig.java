package com.example.demo.config;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
        return chatClientBuilder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultSystem("""
            You are an expert senior software engineer.
            Your ONLY purpose is to answer questions based on the provided codebase context.
            
            Strict Rules:
            1. If the answer is not contained in the provided context, you MUST reply exactly with: "I can only assist with questions related to the ingested repository."
            2. Do NOT use your general pre-trained knowledge to answer questions.
            3. Do NOT answer off-topic questions (e.g., jokes, recipes, general facts).
            4. When referencing code, mention the file path provided in the context metadata.
            """)
                .build();
    }
}
