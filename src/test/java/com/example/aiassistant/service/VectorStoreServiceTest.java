package com.example.aiassistant.service;

import com.example.aiassistant.model.KnowledgeDocument;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VectorStoreServiceTest {

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @Mock
    private EmbeddingModel embeddingModel;

    private VectorStoreServiceImpl vectorStoreService;

    @BeforeEach
    void setUp() {
        vectorStoreService = new VectorStoreServiceImpl(embeddingStore, embeddingModel);
    }

    @Test
    void addDocument_shouldCallEmbeddingStore() {
        KnowledgeDocument doc = KnowledgeDocument.builder()
                .title("Test Doc")
                .content("Test content")
                .sourceType("LOCAL")
                .timestamp(System.currentTimeMillis())
                .build();

        Embedding mockEmbedding = mock(Embedding.class);
        when(embeddingModel.embed(any(TextSegment.class))).thenReturn(
                new dev.langchain4j.model.embedding.EmbeddingResponse(mockEmbedding)
        );

        vectorStoreService.addDocument(doc);

        verify(embeddingStore).add(any(Embedding.class), any(TextSegment.class));
    }

    @Test
    void search_shouldReturnResults() {
        String query = "test query";
        
        Embedding mockEmbedding = mock(Embedding.class);
        when(embeddingModel.embed(query)).thenReturn(
                new dev.langchain4j.model.embedding.EmbeddingResponse(mockEmbedding)
        );
        when(embeddingStore.findRelevant(any(), anyInt(), anyDouble()))
                .thenReturn(Collections.emptyList());

        List<KnowledgeDocument> results = vectorStoreService.search(query, 5, 0.7);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}
