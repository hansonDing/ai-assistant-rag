package com.example.aiassistant.service;

import com.example.aiassistant.dto.ChatRequest;
import com.example.aiassistant.dto.ChatResponse;
import com.example.aiassistant.model.KnowledgeDocument;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistantService {

    private final ChatLanguageModel chatLanguageModel;
    private final VectorStoreService vectorStoreService;
    private final ConfluenceService confluenceService;

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            You are a helpful AI assistant. Answer the user's question based on the provided context.
            
            Guidelines:
            1. Use the provided context to answer the question accurately
            2. If the context doesn't contain enough information, say so clearly
            3. Always cite your sources when using information from the context
            4. Be concise but thorough in your responses
            5. If multiple sources provide conflicting information, mention this
            
            Context from local knowledge base:
            %s
            
            Context from Confluence:
            %s
            """;

    public ChatResponse chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();
        String sessionId = request.getSessionId() != null ? request.getSessionId() : UUID.randomUUID().toString();
        
        log.info("Processing chat request - Session: {}, Question: {}", sessionId, request.getQuestion());

        // Step 1: Search local vector database
        List<KnowledgeDocument> localResults = vectorStoreService.search(
                request.getQuestion(), 5, 0.7);
        log.info("Found {} documents in local vector store", localResults.size());

        // Step 2: If no local results and Confluence fallback is enabled, search Confluence
        List<KnowledgeDocument> confluenceResults = new ArrayList<>();
        if (localResults.isEmpty() && request.isUseConfluenceFallback() && confluenceService.isEnabled()) {
            log.info("No local results found, searching Confluence...");
            confluenceResults = confluenceService.searchPages(request.getQuestion(), 5);
            log.info("Found {} pages in Confluence", confluenceResults.size());
        }

        // Step 3: Build context from sources
        String localContext = buildContext(localResults);
        String confluenceContext = buildContext(confluenceResults);

        // Step 4: Generate response using LLM
        String systemPrompt = String.format(SYSTEM_PROMPT_TEMPLATE, localContext, confluenceContext);
        
        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(systemPrompt));
        messages.add(UserMessage.from(request.getQuestion()));

        log.info("Sending request to LLM...");
        AiMessage aiMessage = chatLanguageModel.generate(messages).content();
        String answer = aiMessage.text();

        long processingTime = System.currentTimeMillis() - startTime;
        log.info("Chat request processed in {}ms", processingTime);

        // Build response with source information
        List<ChatResponse.SourceInfo> sources = new ArrayList<>();
        sources.addAll(convertToSourceInfo(localResults, "LOCAL"));
        sources.addAll(convertToSourceInfo(confluenceResults, "CONFLUENCE"));

        return ChatResponse.builder()
                .answer(answer)
                .sources(sources)
                .sessionId(sessionId)
                .processingTimeMs(processingTime)
                .build();
    }

    private String buildContext(List<KnowledgeDocument> documents) {
        if (documents.isEmpty()) {
            return "No relevant documents found.";
        }

        StringBuilder context = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            KnowledgeDocument doc = documents.get(i);
            context.append("\n--- Source ").append(i + 1).append(" ---\n");
            context.append("Title: ").append(doc.getTitle()).append("\n");
            context.append("Content: ").append(doc.getContent()).append("\n");
            if (doc.getSourceUrl() != null && !doc.getSourceUrl().isEmpty()) {
                context.append("URL: ").append(doc.getSourceUrl()).append("\n");
            }
        }
        return context.toString();
    }

    private List<ChatResponse.SourceInfo> convertToSourceInfo(List<KnowledgeDocument> documents, String sourceType) {
        return documents.stream()
                .map(doc -> ChatResponse.SourceInfo.builder()
                        .sourceType(sourceType)
                        .title(doc.getTitle())
                        .url(doc.getSourceUrl())
                        .relevanceScore(doc.getRelevanceScore())
                        .content(truncateContent(doc.getContent(), 500))
                        .build())
                .collect(Collectors.toList());
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }
}
