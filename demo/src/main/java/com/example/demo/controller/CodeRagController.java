package com.example.demo.controller;

import com.example.demo.service.GitHubIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class CodeRagController {

    private final GitHubIngestionService ingestionService;
    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    @PostMapping("/load")
    public String loadRepo(@RequestParam String repoUrl) {
        return ingestionService.ingestRepo(repoUrl);
    }

    @GetMapping("/chat")
    public String chatWithCodebase(@RequestParam String query, @RequestParam String conversationId) {
        List<Document> similarCode = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(5)
                        .build()
        );

        String contextString = similarCode.stream()
                .map(doc->"File: " + doc.getMetadata().get("file_path") + "\n" + doc.getText())
                .collect(Collectors.joining("\n\n---\n\n"));

        System.out.println("RETRIEVED CODE:\n" + contextString);

        return chatClient.prompt()
                .user(query)
                .system(s->s.param("code_context",contextString))
                .advisors(a->{
                    a.param(ChatMemory.CONVERSATION_ID,conversationId);
                })
                .call()
                .content();
    }
}