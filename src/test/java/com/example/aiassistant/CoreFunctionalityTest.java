package com.example.aiassistant;

import com.example.aiassistant.config.ConfluenceProperties;
import com.example.aiassistant.dto.ChatRequest;
import com.example.aiassistant.dto.ChatResponse;
import com.example.aiassistant.model.KnowledgeDocument;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 核心功能单元测试 - 不依赖Spring容器
 */
public class CoreFunctionalityTest {

    /**
     * 测试1: ChatRequest DTO功能
     */
    @Test
    void test01_chatRequestDto() {
        System.out.println("\n========== 测试1: ChatRequest DTO ==========");
        
        ChatRequest request = new ChatRequest();
        request.setQuestion("什么是Spring Boot?");
        request.setSessionId("test-session-001");
        request.setUseConfluenceFallback(true);
        
        assertEquals("什么是Spring Boot?", request.getQuestion());
        assertEquals("test-session-001", request.getSessionId());
        assertTrue(request.isUseConfluenceFallback());
        
        System.out.println("✓ ChatRequest DTO 测试通过");
        System.out.println("  Question: " + request.getQuestion());
        System.out.println("  SessionId: " + request.getSessionId());
        System.out.println("  UseConfluenceFallback: " + request.isUseConfluenceFallback());
    }

    /**
     * 测试2: ChatResponse DTO功能
     */
    @Test
    void test02_chatResponseDto() {
        System.out.println("\n========== 测试2: ChatResponse DTO ==========");
        
        List<ChatResponse.SourceInfo> sources = new ArrayList<>();
        sources.add(ChatResponse.SourceInfo.builder()
                .sourceType("LOCAL")
                .title("Spring Boot文档")
                .url("")
                .relevanceScore(0.95)
                .content("Spring Boot是一个开源Java框架...")
                .build());
        
        sources.add(ChatResponse.SourceInfo.builder()
                .sourceType("CONFLUENCE")
                .title("开发规范")
                .url("https://wiki.example.com/dev-guide")
                .relevanceScore(0.88)
                .content("项目开发规范说明...")
                .build());
        
        ChatResponse response = ChatResponse.builder()
                .answer("Spring Boot是一个开源的Java框架，用于快速构建Spring应用。")
                .sources(sources)
                .sessionId(UUID.randomUUID().toString())
                .processingTimeMs(1234)
                .build();
        
        assertNotNull(response.getAnswer());
        assertEquals(2, response.getSources().size());
        assertEquals("LOCAL", response.getSources().get(0).getSourceType());
        assertEquals("CONFLUENCE", response.getSources().get(1).getSourceType());
        
        System.out.println("✓ ChatResponse DTO 测试通过");
        System.out.println("  Answer: " + response.getAnswer());
        System.out.println("  Sources count: " + response.getSources().size());
        System.out.println("  Processing time: " + response.getProcessingTimeMs() + "ms");
    }

    /**
     * 测试3: KnowledgeDocument模型
     */
    @Test
    void test03_knowledgeDocument() {
        System.out.println("\n========== 测试3: KnowledgeDocument 模型 ==========");
        
        KnowledgeDocument doc = KnowledgeDocument.builder()
                .id("doc-001")
                .title("Docker基础教程")
                .content("Docker是一个开源的容器化平台...")
                .sourceType("LOCAL")
                .sourceUrl("")
                .relevanceScore(0.92)
                .timestamp(System.currentTimeMillis())
                .build();
        
        assertEquals("doc-001", doc.getId());
        assertEquals("Docker基础教程", doc.getTitle());
        assertEquals("LOCAL", doc.getSourceType());
        assertTrue(doc.getRelevanceScore() > 0.9);
        
        System.out.println("✓ KnowledgeDocument 模型测试通过");
        System.out.println("  ID: " + doc.getId());
        System.out.println("  Title: " + doc.getTitle());
        System.out.println("  SourceType: " + doc.getSourceType());
        System.out.println("  RelevanceScore: " + doc.getRelevanceScore());
    }

    /**
     * 测试4: ConfluenceProperties配置
     */
    @Test
    void test04_confluenceProperties() {
        System.out.println("\n========== 测试4: ConfluenceProperties 配置 ==========");
        
        ConfluenceProperties props = new ConfluenceProperties();
        props.setEnabled(true);
        props.setBaseUrl("https://example.atlassian.net/wiki");
        props.setUsername("admin@example.com");
        props.setApiToken("test-token-123");
        props.setSpaceKey("DEV");
        props.setMaxResults(10);
        props.setMinRelevanceScore(0.7);
        
        assertTrue(props.isEnabled());
        assertEquals("https://example.atlassian.net/wiki", props.getBaseUrl());
        assertEquals("admin@example.com", props.getUsername());
        assertEquals("test-token-123", props.getApiToken());
        assertEquals("DEV", props.getSpaceKey());
        assertEquals(10, props.getMaxResults());
        assertEquals(0.7, props.getMinRelevanceScore());
        
        System.out.println("✓ ConfluenceProperties 配置测试通过");
        System.out.println("  Enabled: " + props.isEnabled());
        System.out.println("  BaseUrl: " + props.getBaseUrl());
        System.out.println("  Username: " + props.getUsername());
        System.out.println("  SpaceKey: " + props.getSpaceKey());
        System.out.println("  MaxResults: " + props.getMaxResults());
    }

    /**
     * 测试5: RAG流程模拟
     */
    @Test
    void test05_ragFlowSimulation() {
        System.out.println("\n========== 测试5: RAG流程模拟 ==========");
        
        // 模拟用户问题
        String userQuestion = "什么是微服务架构?";
        System.out.println("用户问题: " + userQuestion);
        
        // 模拟本地向量数据库搜索结果
        List<KnowledgeDocument> localResults = new ArrayList<>();
        localResults.add(KnowledgeDocument.builder()
                .id("local-001")
                .title("微服务架构设计")
                .content("微服务架构是一种将单体应用拆分为一组小型服务的方法...")
                .sourceType("LOCAL")
                .relevanceScore(0.95)
                .build());
        
        System.out.println("本地向量数据库搜索结果: " + localResults.size() + " 条");
        
        // 模拟Confluence搜索结果（当本地无结果时）
        List<KnowledgeDocument> confluenceResults = new ArrayList<>();
        if (localResults.isEmpty()) {
            confluenceResults.add(KnowledgeDocument.builder()
                    .id("conf-001")
                    .title("微服务最佳实践")
                    .content("来自Confluence的微服务实践指南...")
                    .sourceType("CONFLUENCE")
                    .sourceUrl("https://wiki.example.com/microservices")
                    .relevanceScore(0.88)
                    .build());
        }
        
        System.out.println("Confluence搜索结果: " + confluenceResults.size() + " 条");
        
        // 构建上下文
        StringBuilder context = new StringBuilder();
        context.append("=== 本地知识 ===\n");
        for (KnowledgeDocument doc : localResults) {
            context.append("标题: ").append(doc.getTitle()).append("\n");
            context.append("内容: ").append(doc.getContent()).append("\n\n");
        }
        
        if (!confluenceResults.isEmpty()) {
            context.append("=== Confluence知识 ===\n");
            for (KnowledgeDocument doc : confluenceResults) {
                context.append("标题: ").append(doc.getTitle()).append("\n");
                context.append("URL: ").append(doc.getSourceUrl()).append("\n");
                context.append("内容: ").append(doc.getContent()).append("\n\n");
            }
        }
        
        System.out.println("\n构建的上下文:");
        System.out.println(context.toString());
        
        // 验证流程
        assertFalse(localResults.isEmpty(), "应该有本地搜索结果");
        assertTrue(localResults.get(0).getRelevanceScore() > 0.9, "相关度应该很高");
        
        System.out.println("✓ RAG流程模拟测试通过");
    }

    /**
     * 测试6: 知识文档批量操作
     */
    @Test
    void test06_batchDocumentOperations() {
        System.out.println("\n========== 测试6: 知识文档批量操作 ==========");
        
        List<KnowledgeDocument> documents = new ArrayList<>();
        
        String[] titles = {
            "Docker基础", "Kubernetes入门", "CI/CD实践",
            "DevOps文化", "云原生架构", "容器化部署"
        };
        
        for (int i = 0; i < titles.length; i++) {
            documents.add(KnowledgeDocument.builder()
                    .id("doc-" + String.format("%03d", i + 1))
                    .title(titles[i])
                    .content(titles[i] + "的详细内容...")
                    .sourceType("LOCAL")
                    .relevanceScore(0.8 + (i * 0.02))
                    .timestamp(System.currentTimeMillis())
                    .build());
        }
        
        assertEquals(6, documents.size());
        
        // 搜索包含"Docker"的文档
        List<KnowledgeDocument> dockerDocs = documents.stream()
                .filter(d -> d.getTitle().contains("Docker") || d.getContent().contains("Docker"))
                .toList();
        
        System.out.println("✓ 批量操作测试通过");
        System.out.println("  总文档数: " + documents.size());
        System.out.println("  Docker相关: " + dockerDocs.size());
        
        documents.forEach(d -> System.out.println("  - " + d.getId() + ": " + d.getTitle()));
    }

    /**
     * 测试7: 响应来源信息构建
     */
    @Test
    void test07_sourceInfoBuilding() {
        System.out.println("\n========== 测试7: 响应来源信息构建 ==========");
        
        List<ChatResponse.SourceInfo> sources = new ArrayList<>();
        
        // 本地来源
        sources.add(ChatResponse.SourceInfo.builder()
                .sourceType("LOCAL")
                .title("API设计规范")
                .url("")
                .relevanceScore(0.96)
                .content("RESTful API设计规范说明...")
                .build());
        
        // Confluence来源
        sources.add(ChatResponse.SourceInfo.builder()
                .sourceType("CONFLUENCE")
                .title("团队开发流程")
                .url("https://wiki.example.com/dev-process")
                .relevanceScore(0.85)
                .content("团队开发流程文档...")
                .build());
        
        // 验证来源分类
        long localCount = sources.stream()
                .filter(s -> "LOCAL".equals(s.getSourceType()))
                .count();
        long confluenceCount = sources.stream()
                .filter(s -> "CONFLUENCE".equals(s.getSourceType()))
                .count();
        
        assertEquals(1, localCount);
        assertEquals(1, confluenceCount);
        
        System.out.println("✓ 来源信息构建测试通过");
        System.out.println("  本地来源: " + localCount);
        System.out.println("  Confluence来源: " + confluenceCount);
        
        sources.forEach(s -> {
            System.out.println("  - [" + s.getSourceType() + "] " + s.getTitle() + 
                    " (相关度: " + s.getRelevanceScore() + ")");
        });
    }
}
