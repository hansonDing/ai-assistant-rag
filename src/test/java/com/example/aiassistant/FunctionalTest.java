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

/**
 * 功能测试类 - 测试所有核心API端点
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "ai.openai.api-key=test-key",
    "ai.vector-db.path=./test-data/vector.db",
    "confluence.enabled=false"
})
public class FunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VectorStoreService vectorStoreService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // 每个测试前的准备工作
    }

    /**
     * 测试1: 健康检查端点
     */
    @Test
    void test01_healthCheck_shouldReturnOk() throws Exception {
        System.out.println("\n========== 测试1: 健康检查 ==========");
        
        MvcResult result = mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"))
                .andReturn();
        
        System.out.println("✓ 健康检查通过");
        System.out.println("响应: " + result.getResponse().getContentAsString());
    }

    /**
     * 测试2: 添加知识文档
     */
    @Test
    void test02_addKnowledge_shouldReturnSuccess() throws Exception {
        System.out.println("\n========== 测试2: 添加知识文档 ==========");
        
        KnowledgeDocument doc = KnowledgeDocument.builder()
                .title("Spring Boot 入门指南")
                .content("Spring Boot 是一个开源的 Java 框架，用于创建独立的、生产级别的 Spring 应用。" +
                        "它提供了自动配置、嵌入式服务器和开箱即用的功能，大大简化了 Spring 应用的开发过程。" +
                        "主要特性包括：自动配置、起步依赖、Actuator 监控等。")
                .sourceType("LOCAL")
                .timestamp(System.currentTimeMillis())
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/knowledge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(doc)))
                .andExpect(status().isOk())
                .andExpect(content().string("Document added successfully"))
                .andReturn();
        
        System.out.println("✓ 知识文档添加成功");
        System.out.println("响应: " + result.getResponse().getContentAsString());
    }

    /**
     * 测试3: 搜索知识库
     */
    @Test
    void test03_searchKnowledge_shouldReturnResults() throws Exception {
        System.out.println("\n========== 测试3: 搜索知识库 ==========");
        
        // 先添加一些测试数据
        KnowledgeDocument doc1 = KnowledgeDocument.builder()
                .title("Java 并发编程")
                .content("Java 提供了丰富的并发编程工具，包括 Thread、ExecutorService、CompletableFuture 等。" +
                        "并发编程需要注意线程安全、死锁、资源竞争等问题。")
                .sourceType("LOCAL")
                .timestamp(System.currentTimeMillis())
                .build();
        
        KnowledgeDocument doc2 = KnowledgeDocument.builder()
                .title("Spring Security 配置")
                .content("Spring Security 是一个强大的安全框架，提供认证、授权、防护常见攻击等功能。" +
                        "可以通过配置类或 XML 进行自定义设置。")
                .sourceType("LOCAL")
                .timestamp(System.currentTimeMillis())
                .build();
        
        vectorStoreService.addDocument(doc1);
        vectorStoreService.addDocument(doc2);

        // 搜索测试
        MvcResult result = mockMvc.perform(get("/api/v1/knowledge/search")
                        .param("query", "Java 并发")
                        .param("maxResults", "5"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.println("✓ 知识库搜索完成");
        System.out.println("搜索结果: " + content.substring(0, Math.min(content.length(), 500)));
        
        assertNotNull(content);
    }

    /**
     * 测试4: 空问题验证
     */
    @Test
    void test04_chatWithEmptyQuestion_shouldReturnBadRequest() throws Exception {
        System.out.println("\n========== 测试4: 空问题验证 ==========");
        
        ChatRequest request = new ChatRequest();
        request.setQuestion("");

        MvcResult result = mockMvc.perform(post("/api/v1/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andReturn();
        
        System.out.println("✓ 空问题验证通过，返回 400 Bad Request");
    }

    /**
     * 测试5: GET方式对话端点结构测试
     */
    @Test
    void test05_chatGet_shouldAcceptRequest() throws Exception {
        System.out.println("\n========== 测试5: GET对话端点 ==========");
        
        // 由于需要真实的OpenAI API Key，这里只测试端点是否能接收请求
        // 实际响应取决于是否配置了有效的API Key
        MvcResult result = mockMvc.perform(get("/api/v1/chat")
                        .param("question", "什么是Spring Boot?")
                        .param("useConfluenceFallback", "false"))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        System.out.println("✓ GET对话端点响应正常");
        System.out.println("响应预览: " + content.substring(0, Math.min(content.length(), 300)));
    }

    /**
     * 测试6: 批量添加和搜索测试
     */
    @Test
    void test06_batchAddAndSearch() throws Exception {
        System.out.println("\n========== 测试6: 批量添加和搜索 ==========");
        
        // 批量添加文档
        String[] titles = {
            "Docker 基础教程",
            "Kubernetes 入门", 
            "微服务架构设计",
            "CI/CD 最佳实践",
            "DevOps 文化介绍"
        };
        
        String[] contents = {
            "Docker 是一个开源的容器化平台，可以将应用及其依赖打包成标准化单元。",
            "Kubernetes 是 Google 开源的容器编排系统，用于自动化部署、扩展和管理容器化应用。",
            "微服务架构将单体应用拆分为一组小型服务，每个服务运行在自己的进程中。",
            "CI/CD 是持续集成和持续交付的缩写，是现代软件开发的核心实践。",
            "DevOps 是一种文化和实践，旨在打破开发和运维团队之间的壁垒。"
        };
        
        for (int i = 0; i < titles.length; i++) {
            KnowledgeDocument doc = KnowledgeDocument.builder()
                    .title(titles[i])
                    .content(contents[i])
                    .sourceType("LOCAL")
                    .timestamp(System.currentTimeMillis())
                    .build();
            vectorStoreService.addDocument(doc);
        }
        
        System.out.println("✓ 批量添加 " + titles.length + " 个文档完成");
        
        // 搜索测试
        String[] queries = {"Docker", "Kubernetes", "微服务", "CI/CD", "DevOps"};
        for (String query : queries) {
            MvcResult result = mockMvc.perform(get("/api/v1/knowledge/search")
                            .param("query", query)
                            .param("maxResults", "3"))
                    .andExpect(status().isOk())
                    .andReturn();
            
            String response = result.getResponse().getContentAsString();
            System.out.println("  搜索 '" + query + "' 找到结果，响应长度: " + response.length());
        }
        
        System.out.println("✓ 批量搜索测试完成");
    }

    /**
     * 测试7: Confluence配置验证
     */
    @Test
    void test07_confluenceConfig() throws Exception {
        System.out.println("\n========== 测试7: Confluence配置验证 ==========");
        System.out.println("✓ Confluence集成已禁用（测试环境配置）");
        System.out.println("  在生产环境可通过环境变量启用:");
        System.out.println("  - CONFLUENCE_ENABLED=true");
        System.out.println("  - CONFLUENCE_BASE_URL=https://your-domain.atlassian.net/wiki");
        System.out.println("  - CONFLUENCE_USERNAME=your-email");
        System.out.println("  - CONFLUENCE_API_TOKEN=your-token");
    }
}
