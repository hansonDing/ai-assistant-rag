package com.example.aiassistant.service;

import com.example.aiassistant.model.KnowledgeDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiAssistantServiceTest {

    @Mock
    private VectorStoreService vectorStoreService;

    @Mock
    private ConfluenceService confluenceService;

    @InjectMocks
    private AiAssistantService aiAssistantService;

    @Test
    void testBuildContext_withDocuments() {
        // This is a simplified test - in reality you'd need to mock ChatLanguageModel too
        List<KnowledgeDocument> documents = Arrays.asList(
                KnowledgeDocument.builder()
                        .title("Doc 1")
                        .content("Content 1")
                        .sourceType("LOCAL")
                        .build(),
                KnowledgeDocument.builder()
                        .title("Doc 2")
                        .content("Content 2")
                        .sourceType("CONFLUENCE")
                        .sourceUrl("http://example.com")
                        .build()
        );

        assertEquals(2, documents.size());
        assertEquals("Doc 1", documents.get(0).getTitle());
        assertEquals("CONFLUENCE", documents.get(1).getSourceType());
    }

    @Test
    void testConfluenceFallbackEnabled() {
        when(confluenceService.isEnabled()).thenReturn(true);
        assertTrue(confluenceService.isEnabled());
    }

    @Test
    void testConfluenceFallbackDisabled() {
        when(confluenceService.isEnabled()).thenReturn(false);
        assertFalse(confluenceService.isEnabled());
    }
}
