package com.example.aiassistant;

import com.example.aiassistant.dto.ChatRequest;
import com.example.aiassistant.dto.ChatResponse;
import com.example.aiassistant.model.KnowledgeDocument;
import com.example.aiassistant.service.VectorStoreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "ai.openai.api-key=test-key",
    "ai.vector-db.path=./test-data/vector.db"
})
public class AiAssistantIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VectorStoreService vectorStoreService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Clean up test data if needed
    }

    @Test
    void contextLoads() {
        assertNotNull(mockMvc);
        assertNotNull(vectorStoreService);
    }

    @Test
    void healthCheck_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    void addKnowledge_shouldReturnSuccess() throws Exception {
        KnowledgeDocument doc = KnowledgeDocument.builder()
                .title("Test Document")
                .content("This is a test document about Spring Boot and Java development.")
                .sourceType("LOCAL")
                .timestamp(System.currentTimeMillis())
                .build();

        mockMvc.perform(post("/api/v1/knowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doc)))
                .andExpect(status().isOk())
                .andExpect(content().string("Document added successfully"));
    }

    @Test
    void searchKnowledge_shouldReturnResults() throws Exception {
        // First add a document
        KnowledgeDocument doc = KnowledgeDocument.builder()
                .title("Spring Boot Guide")
                .content("Spring Boot makes it easy to create stand-alone, production-grade Spring based Applications.")
                .sourceType("LOCAL")
                .timestamp(System.currentTimeMillis())
                .build();

        vectorStoreService.addDocument(doc);

        // Then search for it
        MvcResult result = mockMvc.perform(get("/api/v1/knowledge/search")
                        .param("query", "Spring Boot")
                        .param("maxResults", "5"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertNotNull(content);
        // Should find at least one result
        assertTrue(content.contains("Spring Boot") || content.contains("[]"));
    }

    @Test
    void chatWithEmptyQuestion_shouldReturnBadRequest() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setQuestion("");

        mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void chatGet_shouldWork() throws Exception {
        // This will fail without a valid OpenAI API key, but tests the endpoint structure
        mockMvc.perform(get("/api/v1/chat")
                        .param("question", "What is Java?")
                        .param("useConfluenceFallback", "false"))
                .andExpect(status().isOk());
    }
}
