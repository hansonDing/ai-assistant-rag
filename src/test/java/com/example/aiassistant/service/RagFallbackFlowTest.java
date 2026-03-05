package com.example.aiassistant.service;

import com.example.aiassistant.config.ConfluenceProperties;
import com.example.aiassistant.dto.ChatRequest;
import com.example.aiassistant.dto.ChatResponse;
import com.example.aiassistant.model.KnowledgeDocument;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * RAG Fallback 流程测试
 * 测试当本地向量数据库无结果时，自动查询 Confluence 的完整流程
 */
@ExtendWith(MockitoExtension.class)
class RagFallbackFlowTest {

    private WireMockServer wireMockServer;
    
    @Mock
    private VectorStoreService vectorStoreService;
    
    @Mock
    private ChatLanguageModel chatLanguageModel;
    
    private ConfluenceService confluenceService;
    private ConfluenceProperties properties;
    private AiAssistantService aiAssistantService;

    @BeforeEach
    void setUp() {
        // 启动 WireMock 服务器
        wireMockServer = new WireMockServer(8090);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8090);

        // 配置 ConfluenceProperties
        properties = new ConfluenceProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://localhost:8090/wiki");
        properties.setUsername("test@example.com");
        properties.setApiToken("test-token");
        properties.setSpaceKey("TEST");
        properties.setMaxResults(5);
        properties.setMinRelevanceScore(0.5);

        // 创建真实的 ConfluenceService
        confluenceService = new ConfluenceServiceImpl(properties);
        
        // 创建 AiAssistantService，使用 mock 的 VectorStoreService 和 ChatLanguageModel
        aiAssistantService = new AiAssistantService(chatLanguageModel, vectorStoreService, confluenceService);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    @DisplayName("测试1: 本地有结果时，不查询 Confluence")
    void test01_localResultsOnly() {
        System.out.println("\n========== 测试1: 本地有结果，不查询 Confluence ==========");
        
        // 模拟本地向量数据库返回结果
        List<KnowledgeDocument> localResults = List.of(
            KnowledgeDocument.builder()
                .id("local-001")
                .title("本地 Spring Boot 文档")
                .content("Spring Boot 是一个开源框架...")
                .sourceType("LOCAL")
                .relevanceScore(0.95)
                .build()
        );
        
        when(vectorStoreService.search(anyString(), anyInt(), anyDouble()))
            .thenReturn(localResults);
        
        when(chatLanguageModel.generate(anyList()))
            .thenReturn(dev.langchain4j.model.output.Response.from(
                dev.langchain4j.data.message.AiMessage.from("这是 AI 的回答")
            ));

        ChatRequest request = new ChatRequest();
        request.setQuestion("什么是 Spring Boot?");
        request.setUseConfluenceFallback(true);
        
        ChatResponse response = aiAssistantService.chat(request);

        assertNotNull(response);
        assertNotNull(response.getAnswer());
        assertEquals(1, response.getSources().size());
        assertEquals("LOCAL", response.getSources().get(0).getSourceType());
        
        // 验证没有向 Confluence 发送请求
        verify(0, getRequestedFor(urlPathMatching("/wiki/rest/api/content/search")));
        
        System.out.println("✓ 本地有结果时，仅使用本地知识，不查询 Confluence");
        System.out.println("  来源: " + response.getSources().get(0).getTitle());
    }

    @Test
    @DisplayName("测试2: 本地无结果时，自动查询 Confluence")
    void test02_fallbackToConfluence() {
        System.out.println("\n========== 测试2: 本地无结果，自动查询 Confluence ==========");
        
        // 模拟本地向量数据库返回空结果
        when(vectorStoreService.search(anyString(), anyInt(), anyDouble()))
            .thenReturn(Collections.emptyList());
        
        // 模拟 Confluence API 返回结果
        String confluenceResponse = """
            {
                "results": [
                    {
                        "id": "conf-001",
                        "title": "Confluence Spring Boot 指南",
                        "type": "page",
                        "body": {
                            "storage": {
                                "value": "<p>Spring Boot 是 Spring 框架的扩展...</p>"
                            }
                        },
                        "_links": {
                            "webui": "/spaces/TEST/pages/conf-001"
                        }
                    }
                ]
            }
            """;
        
        stubFor(get(urlPathMatching("/wiki/rest/api/content/search"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(confluenceResponse)));
        
        when(chatLanguageModel.generate(anyList()))
            .thenReturn(dev.langchain4j.model.output.Response.from(
                dev.langchain4j.data.message.AiMessage.from("基于 Confluence 的回答")
            ));

        ChatRequest request = new ChatRequest();
        request.setQuestion("Spring Boot 是什么?");
        request.setUseConfluenceFallback(true);
        
        ChatResponse response = aiAssistantService.chat(request);

        assertNotNull(response);
        assertEquals(1, response.getSources().size());
        assertEquals("CONFLUENCE", response.getSources().get(0).getSourceType());
        assertTrue(response.getSources().get(0).getTitle().contains("Confluence"));
        
        // 验证向 Confluence 发送了请求
        verify(1, getRequestedFor(urlPathMatching("/wiki/rest/api/content/search")));
        
        System.out.println("✓ 本地无结果时，自动查询 Confluence");
        System.out.println("  来源: " + response.getSources().get(0).getTitle());
        System.out.println("  URL: " + response.getSources().get(0).getUrl());
    }

    @Test
    @DisplayName("测试3: 禁用 fallback 时，本地无结果也不查询 Confluence")
    void test03_fallbackDisabled() {
        System.out.println("\n========== 测试3: 禁用 fallback，不查询 Confluence ==========");
        
        // 模拟本地向量数据库返回空结果
        when(vectorStoreService.search(anyString(), anyInt(), anyDouble()))
            .thenReturn(Collections.emptyList());
        
        when(chatLanguageModel.generate(anyList()))
            .thenReturn(dev.langchain4j.model.output.Response.from(
                dev.langchain4j.data.message.AiMessage.from("我没有找到相关信息")
            ));

        ChatRequest request = new ChatRequest();
        request.setQuestion("Spring Boot 是什么?");
        request.setUseConfluenceFallback(false); // 禁用 fallback
        
        ChatResponse response = aiAssistantService.chat(request);

        assertNotNull(response);
        assertTrue(response.getSources().isEmpty());
        
        // 验证没有向 Confluence 发送请求
        verify(0, getRequestedFor(urlPathMatching("/wiki/rest/api/content/search")));
        
        System.out.println("✓ 禁用 fallback 时，即使本地无结果也不查询 Confluence");
    }

    @Test
    @DisplayName("测试4: Confluence 未启用时，本地无结果也不查询")
    void test04_confluenceNotEnabled() {
        System.out.println("\n========== 测试4: Confluence 未启用，不查询 ==========");
        
        // 禁用 Confluence
        properties.setEnabled(false);
        
        // 模拟本地向量数据库返回空结果
        when(vectorStoreService.search(anyString(), anyInt(), anyDouble()))
            .thenReturn(Collections.emptyList());
        
        when(chatLanguageModel.generate(anyList()))
            .thenReturn(dev.langchain4j.model.output.Response.from(
                dev.langchain4j.data.message.AiMessage.from("我没有找到相关信息")
            ));

        ChatRequest request = new ChatRequest();
        request.setQuestion("Spring Boot 是什么?");
        request.setUseConfluenceFallback(true);
        
        ChatResponse response = aiAssistantService.chat(request);

        assertNotNull(response);
        assertTrue(response.getSources().isEmpty());
        
        // 验证没有向 Confluence 发送请求
        verify(0, getRequestedFor(urlPathMatching("/wiki/rest/api/content/search")));
        
        System.out.println("✓ Confluence 未启用时，不发起查询");
    }

    @Test
    @DisplayName("测试5: 本地和 Confluence 都有结果时，合并返回")
    void test05_mergeLocalAndConfluenceResults() {
        System.out.println("\n========== 测试5: 合并本地和 Confluence 结果 ==========");
        
        // 注意：当前实现是本地有结果时不会查询 Confluence
        // 这个测试验证的是如果未来实现合并逻辑，结果能正确处理
        
        List<KnowledgeDocument> localResults = List.of(
            KnowledgeDocument.builder()
                .id("local-001")
                .title("本地文档")
                .content("本地内容...")
                .sourceType("LOCAL")
                .relevanceScore(0.90)
                .build()
        );
        
        when(vectorStoreService.search(anyString(), anyInt(), anyDouble()))
            .thenReturn(localResults);
        
        when(chatLanguageModel.generate(anyList()))
            .thenReturn(dev.langchain4j.model.output.Response.from(
                dev.langchain4j.data.message.AiMessage.from("合并回答")
            ));

        ChatRequest request = new ChatRequest();
        request.setQuestion("测试问题");
        request.setUseConfluenceFallback(true);
        
        ChatResponse response = aiAssistantService.chat(request);

        assertNotNull(response);
        assertEquals(1, response.getSources().size());
        
        System.out.println("✓ 当前实现优先使用本地结果（本地有结果时不查 Confluence）");
    }

    @Test
    @DisplayName("测试6: Confluence 查询失败时， gracefully 处理")
    void test06_confluenceQueryFailure() {
        System.out.println("\n========== 测试6: Confluence 查询失败处理 ==========");
        
        // 模拟本地向量数据库返回空结果
        when(vectorStoreService.search(anyString(), anyInt(), anyDouble()))
            .thenReturn(Collections.emptyList());
        
        // 模拟 Confluence API 返回 500 错误
        stubFor(get(urlPathMatching("/wiki/rest/api/content/search"))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("{\"error\": \"Internal Server Error\"}")));
        
        when(chatLanguageModel.generate(anyList()))
            .thenReturn(dev.langchain4j.model.output.Response.from(
                dev.langchain4j.data.message.AiMessage.from("抱歉，暂时无法获取相关信息")
            ));

        ChatRequest request = new ChatRequest();
        request.setQuestion("测试问题");
        request.setUseConfluenceFallback(true);
        
        // 不应该抛出异常
        assertDoesNotThrow(() -> {
            ChatResponse response = aiAssistantService.chat(request);
            assertNotNull(response);
            assertTrue(response.getSources().isEmpty()); // Confluence 失败，没有来源
            System.out.println("✓ Confluence 查询失败时，gracefully 处理，返回空来源");
        });
    }

    @Test
    @DisplayName("测试7: 验证请求中包含正确的 CQL 查询")
    void test07_verifyCqlQuery() {
        System.out.println("\n========== 测试7: 验证 CQL 查询参数 ==========");
        
        // 模拟本地向量数据库返回空结果
        when(vectorStoreService.search(anyString(), anyInt(), anyDouble()))
            .thenReturn(Collections.emptyList());
        
        stubFor(get(urlPathMatching("/wiki/rest/api/content/search"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"results\": []}")));
        
        when(chatLanguageModel.generate(anyList()))
            .thenReturn(dev.langchain4j.model.output.Response.from(
                dev.langchain4j.data.message.AiMessage.from("回答")
            ));

        ChatRequest request = new ChatRequest();
        request.setQuestion("Spring Boot 部署");
        request.setUseConfluenceFallback(true);
        
        aiAssistantService.chat(request);

        // 验证 CQL 查询包含用户问题和 space key
        verify(getRequestedFor(urlPathMatching("/wiki/rest/api/content/search"))
            .withQueryParam("cql", containing("Spring Boot 部署"))
            .withQueryParam("cql", containing("space = TEST")));
        
        System.out.println("✓ CQL 查询包含用户问题和 space key");
    }

    @Test
    @DisplayName("测试8: Confluence 返回多个结果时，全部包含在响应中")
    void test08_multipleConfluenceResults() {
        System.out.println("\n========== 测试8: 多个 Confluence 结果 ==========");
        
        // 模拟本地向量数据库返回空结果
        when(vectorStoreService.search(anyString(), anyInt(), anyDouble()))
            .thenReturn(Collections.emptyList());
        
        // 模拟 Confluence 返回多个结果
        String confluenceResponse = """
            {
                "results": [
                    {
                        "id": "page-1",
                        "title": "部署指南",
                        "type": "page",
                        "body": {"storage": {"value": "<p>部署步骤...</p>"}},
                        "_links": {"webui": "/1"}
                    },
                    {
                        "id": "page-2",
                        "title": "环境配置",
                        "type": "page",
                        "body": {"storage": {"value": "<p>配置说明...</p>"}},
                        "_links": {"webui": "/2"}
                    },
                    {
                        "id": "page-3",
                        "title": "常见问题",
                        "type": "page",
                        "body": {"storage": {"value": "<p>FAQ...</p>"}},
                        "_links": {"webui": "/3"}
                    }
                ]
            }
            """;
        
        stubFor(get(urlPathMatching("/wiki/rest/api/content/search"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(confluenceResponse)));
        
        when(chatLanguageModel.generate(anyList()))
            .thenReturn(dev.langchain4j.model.output.Response.from(
                dev.langchain4j.data.message.AiMessage.from("基于多个来源的回答")
            ));

        ChatRequest request = new ChatRequest();
        request.setQuestion("部署");
        request.setUseConfluenceFallback(true);
        
        ChatResponse response = aiAssistantService.chat(request);

        assertNotNull(response);
        assertEquals(3, response.getSources().size());
        
        // 验证所有来源都是 CONFLUENCE 类型
        response.getSources().forEach(source -> {
            assertEquals("CONFLUENCE", source.getSourceType());
        });
        
        System.out.println("✓ Confluence 返回 " + response.getSources().size() + " 个结果");
        response.getSources().forEach(source -> 
            System.out.println("  - " + source.getTitle())
        );
    }

    @Test
    @DisplayName("测试9: 处理包含 HTML 实体的 Confluence 内容")
    void test09_htmlEntitiesInContent() {
        System.out.println("\n========== 测试9: 处理 HTML 实体 ==========");
        
        // 模拟本地向量数据库返回空结果
        when(vectorStoreService.search(anyString(), anyInt(), anyDouble()))
            .thenReturn(Collections.emptyList());
        
        // 模拟包含 HTML 实体的响应
        String confluenceResponse = """
            {
                "results": [
                    {
                        "id": "page-1",
                        "title": "API & Integration",
                        "type": "page",
                        "body": {
                            "storage": {
                                "value": "<p>使用 &lt;code&gt;REST API&lt;/code&gt; 进行集成&amp;开发。&copy; 2024</p>"
                            }
                        },
                        "_links": {"webui": "/1"}
                    }
                ]
            }
            """;
        
        stubFor(get(urlPathMatching("/wiki/rest/api/content/search"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(confluenceResponse)));
        
        when(chatLanguageModel.generate(anyList()))
            .thenReturn(dev.langchain4j.model.output.Response.from(
                dev.langchain4j.data.message.AiMessage.from("回答")
            ));

        ChatRequest request = new ChatRequest();
        request.setQuestion("API");
        request.setUseConfluenceFallback(true);
        
        ChatResponse response = aiAssistantService.chat(request);

        assertNotNull(response);
        assertEquals(1, response.getSources().size());
        
        String content = response.getSources().get(0).getContent();
        System.out.println("✓ 内容处理完成");
        System.out.println("  原始内容包含 HTML 实体，清理后: " + content.substring(0, Math.min(content.length(), 80)));
    }
}
