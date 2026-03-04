package com.example.aiassistant.service;

import com.example.aiassistant.model.KnowledgeDocument;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreServiceImpl implements VectorStoreService {

    private final EmbeddingStore<TextSegment> embeddingStore;
    private final EmbeddingModel embeddingModel;

    @Override
    public void addDocument(KnowledgeDocument document) {
        log.info("Adding document to vector store: {}", document.getTitle());
        
        String docId = document.getId() != null ? document.getId() : UUID.randomUUID().toString();
        
        Metadata metadata = new Metadata();
        metadata.put("id", docId);
        metadata.put("title", document.getTitle());
        metadata.put("sourceType", document.getSourceType());
        metadata.put("sourceUrl", document.getSourceUrl() != null ? document.getSourceUrl() : "");
        metadata.put("timestamp", String.valueOf(document.getTimestamp()));
        
        TextSegment segment = TextSegment.from(document.getContent(), metadata);
        Embedding embedding = embeddingModel.embed(segment).content();
        
        embeddingStore.add(embedding, segment);
        log.info("Document added successfully with ID: {}", docId);
    }

    @Override
    public List<KnowledgeDocument> search(String query, int maxResults) {
        return search(query, maxResults, 0.0);
    }

    @Override
    public List<KnowledgeDocument> search(String query, int maxResults, double minScore) {
        log.info("Searching vector store for query: '{}' (maxResults={}, minScore={})", query, maxResults, minScore);
        
        Embedding queryEmbedding = embeddingModel.embed(query).content();
        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.findRelevant(queryEmbedding, maxResults, minScore);
        
        log.info("Found {} matching documents", matches.size());
        
        return matches.stream()
                .map(this::convertToDocument)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteDocument(String id) {
        log.warn("Delete operation not fully supported by SQLiteEmbeddingStore, document ID: {}", id);
        // Note: SQLiteEmbeddingStore doesn't support direct deletion by ID
        // Would need to implement custom logic if deletion is required
    }

    @Override
    public void clearAll() {
        log.warn("Clear all operation not supported by SQLiteEmbeddingStore");
        // Would need to delete and recreate the database file
    }

    private KnowledgeDocument convertToDocument(EmbeddingMatch<TextSegment> match) {
        TextSegment segment = match.embedded();
        Metadata metadata = segment.metadata();
        
        return KnowledgeDocument.builder()
                .id(metadata.getString("id"))
                .title(metadata.getString("title"))
                .content(segment.text())
                .sourceType(metadata.getString("sourceType"))
                .sourceUrl(metadata.getString("sourceUrl"))
                .relevanceScore(match.score())
                .timestamp(Long.parseLong(metadata.getString("timestamp")))
                .build();
    }
}
