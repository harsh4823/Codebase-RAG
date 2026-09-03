package com.example.demo.controller;

import com.example.demo.service.GitHubIngestionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag")
public class CodeRagController {

    private final GitHubIngestionService ingestionService;
    private final ChatClient chatClient;

    public CodeRagController(GitHubIngestionService ingestionService, 
                             ChatClient.Builder chatClientBuilder,
                             VectorStore vectorStore,
                             ChatMemory chatMemory) { // Injects your JpaChatMemory
        this.ingestionService = ingestionService;
        
        this.chatClient = chatClientBuilder
                // Interceptor 1: Sliding Window Chat Memory
                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory, "default_chat", 10))
                // Interceptor 2: Automatic RAG Retrieval & Prompt Injection
                .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .withSearchRequest(SearchRequest.defaults().withTopK(5))
                        .build())
                .build();
    }

    @PostMapping("/load")
    public String loadRepo(@RequestParam String repoUrl) {
        return ingestionService.ingestRepository(repoUrl);
    }

    @GetMapping("/chat")
    public String chatWithCodebase(@RequestParam String query, @RequestParam String conversationId) {
        return chatClient.prompt()
                .user(query)
                // Dynamically bind the conversation ID for the memory advisor
                .advisors(a -> a.param(MessageChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
                .call()
                .content();
    }
}