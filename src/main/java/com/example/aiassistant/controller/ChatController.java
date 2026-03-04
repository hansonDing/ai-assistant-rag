package com.example.aiassistant.controller;

import com.example.aiassistant.dto.ChatRequest;
import com.example.aiassistant.dto.ChatResponse;
import com.example.aiassistant.model.KnowledgeDocument;
import com.example.aiassistant.service.AiAssistantService;
import com.example.aiassistant.service.VectorStoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChatController {

    private final AiAssistantService aiAssistantService;
    private final VectorStoreService vectorStoreService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        log.info("Received chat request: {}", request.getQuestion());
        ChatResponse response = aiAssistantService.chat(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/chat")
    public ResponseEntity<ChatResponse> chatGet(
            @RequestParam String question,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false, defaultValue = "true") boolean useConfluenceFallback) {
        
        ChatRequest request = new ChatRequest();
        request.setQuestion(question);
        request.setSessionId(sessionId);
        request.setUseConfluenceFallback(useConfluenceFallback);
        
        ChatResponse response = aiAssistantService.chat(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/knowledge")
    public ResponseEntity<String> addKnowledge(@RequestBody KnowledgeDocument document) {
        log.info("Adding knowledge document: {}", document.getTitle());
        vectorStoreService.addDocument(document);
        return ResponseEntity.ok("Document added successfully");
    }

    @GetMapping("/knowledge/search")
    public ResponseEntity<List<KnowledgeDocument>> searchKnowledge(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = "5") int maxResults) {
        
        log.info("Searching knowledge base for: {}", query);
        List<KnowledgeDocument> results = vectorStoreService.search(query, maxResults);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("OK");
    }
}
