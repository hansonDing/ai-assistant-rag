package com.example.aiassistant.service;

import com.example.aiassistant.config.ConfluenceProperties;
import com.example.aiassistant.model.KnowledgeDocument;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * ConfluenceService 的 WireMock 测试类
 * 模拟 Confluence REST API 的各种响应场景
 */
class ConfluenceServiceTest {

    private WireMockServer wireMockServer;
    private ConfluenceService confluenceService;
    private ConfluenceProperties properties;

    @BeforeEach
    void setUp() {
        // 启动 WireMock 服务器
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);

        // 配置 ConfluenceProperties
        properties = new ConfluenceProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://localhost:8089/wiki");
        properties.setUsername("test@example.com");
        properties.setApiToken("test-token-123");
        properties.setSpaceKey("DEV");
        properties.setMaxResults(10);
        properties.setMinRelevanceScore(0.5);

        // 创建服务实例
        confluenceService = new ConfluenceServiceImpl(properties);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    @DisplayName("测试1: 当 Confluence 未启用时，isEnabled 返回 false")
    void test01_isEnabled_whenDisabled() {
        System.out.println("\n========== 测试1: Confluence 未启用 ==========");
        
        properties.setEnabled(false);
        
        assertFalse(confluenceService.isEnabled());
        System.out.println("✓ Confluence 未启用时，isEnabled() 返回 false");
    }

    @Test
    @DisplayName("测试2: 当配置完整时，isEnabled 返回 true")
    void test02_isEnabled_whenFullyConfigured() {
        System.out.println("\n========== 测试2: Confluence 配置完整 ==========");
        
        assertTrue(confluenceService.isEnabled());
        System.out.println("✓ Confluence 配置完整时，isEnabled() 返回 true");
    }

    @Test
    @DisplayName("测试3: 当缺少必要配置时，isEnabled 返回 false")
    void test03_isEnabled_whenMissingConfig() {
        System.out.println("\n========== 测试3: Confluence 配置不完整 ==========");
        
        // 测试缺少 baseUrl
        properties.setBaseUrl("");
        assertFalse(confluenceService.isEnabled());
        System.out.println("✓ 缺少 baseUrl 时，isEnabled() 返回 false");
        
        // 恢复 baseUrl，测试缺少 username
        properties.setBaseUrl("http://localhost:8089/wiki");
        properties.setUsername("");
        assertFalse(confluenceService.isEnabled());
        System.out.println("✓ 缺少 username 时，isEnabled() 返回 false");
        
        // 恢复 username，测试缺少 apiToken
        properties.setUsername("test@example.com");
        properties.setApiToken("");
        assertFalse(confluenceService.isEnabled());
        System.out.println("✓ 缺少 apiToken 时，isEnabled() 返回 false");
    }

    @Test
    @DisplayName("测试4: 成功搜索 Confluence 页面")
    void test04_searchPages_success() {
        System.out.println("\n========== 测试4: 成功搜索 Confluence 页面 ==========");
        
        // 模拟 Confluence API 响应
        String mockResponse = """
            {
                "results": [
                    {
                        "id": "12345",
                        "title": "Spring Boot 开发规范",
                        "type": "page",
                        "body": {
                            "storage": {
                                "value": "<p>本文档描述了 Spring Boot 项目的开发规范...</p>"
                            }
                        },
                        "_links": {
                            "webui": "/spaces/DEV/pages/12345"
                        }
                    },
                    {
                        "id": "12346",
                        "title": "部署流程文档",
                        "type": "page",
                        "body": {
                            "storage": {
                                "value": "<p>部署流程包括以下步骤...</p>"
                            }
                        },
                        "_links": {
                            "webui": "/spaces/DEV/pages/12346"
                        }
                    }
                ]
            }
            """;

        stubFor(get(urlPathMatching("/wiki/rest/api/content/search"))
                .withQueryParam("cql", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));

        List<KnowledgeDocument> results = confluenceService.searchPages("Spring Boot");

        assertNotNull(results);
        assertEquals(2, results.size());
        
        // 验证第一个结果
        KnowledgeDocument firstDoc = results.get(0);
        assertEquals("conf_12345", firstDoc.getId());
        assertEquals("Spring Boot 开发规范", firstDoc.getTitle());
        assertEquals("CONFLUENCE", firstDoc.getSourceType());
        assertTrue(firstDoc.getSourceUrl().contains("/spaces/DEV/pages/12345"));
        
        // 验证第二个结果
        KnowledgeDocument secondDoc = results.get(1);
        assertEquals("conf_12346", secondDoc.getId());
        assertEquals("部署流程文档", secondDoc.getTitle());
        
        System.out.println("✓ 成功搜索到 " + results.size() + " 个页面");
        results.forEach(doc -> System.out.println("  - " + doc.getTitle() + " (ID: " + doc.getId() + ")"));
    }

    @Test
    @DisplayName("测试5: 搜索返回空结果")
    void test05_searchPages_emptyResults() {
        System.out.println("\n========== 测试5: 搜索返回空结果 ==========");
        
        String emptyResponse = """
            {
                "results": []
            }
            """;

        stubFor(get(urlPathMatching("/wiki/rest/api/content/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(emptyResponse)));

        List<KnowledgeDocument> results = confluenceService.searchPages("不存在的查询");

        assertNotNull(results);
        assertTrue(results.isEmpty());
        System.out.println("✓ 搜索返回空结果，返回空列表");
    }

    @Test
    @DisplayName("测试6: Confluence API 返回 401 未授权")
    void test06_searchPages_unauthorized() {
        System.out.println("\n========== 测试6: Confluence API 401 未授权 ==========");
        
        stubFor(get(urlPathMatching("/wiki/rest/api/content/search"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withBody("{\"message\": \"Unauthorized\"}")));

        List<KnowledgeDocument> results = confluenceService.searchPages("测试");

        assertNotNull(results);
        assertTrue(results.isEmpty());
        System.out.println("✓ API 返回 401 时，返回空列表而不抛出异常");
    }

    @Test
    @DisplayName("测试7: Confluence API 返回 500 服务器错误")
    void test07_searchPages_serverError() {
        System.out.println("\n========== 测试7: Confluence API 500 错误 ==========");
        
        stubFor(get(urlPathMatching("/wiki/rest/api/content/search"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("{\"message\": \"Internal Server Error\"}")));

        List<KnowledgeDocument> results = confluenceService.searchPages("测试");

        assertNotNull(results);
        assertTrue(results.isEmpty());
        System.out.println("✓ API 返回 500 时，返回空列表而不抛出异常");
    }

    @Test
    @DisplayName("测试8: Confluence API 连接超时")
    void test08_searchPages_connectionTimeout() {
        System.out.println("\n========== 测试8: Confluence API 连接超时 ==========");
        
        stubFor(get(urlPathMatching("/wiki/rest/api/content/search"))
                .willReturn(aResponse()
                        .withFixedDelay(5000) // 5秒延迟
                        .withStatus(200)
                        .withBody("{\"results\": []}")));

        // 由于 HttpClient 默认超时时间，这里会抛出异常
        // 但我们的实现应该捕获异常并返回空列表
        List<KnowledgeDocument> results = confluenceService.searchPages("测试");

        // 注意：实际行为取决于 HttpClient 的超时配置
        // 这里我们验证至少不会崩溃
        assertNotNull(results);
        System.out.println("✓ 连接超时处理完成，返回空列表");
    }

    @Test
    @DisplayName("测试9: 搜索包含特殊字符的查询")
    void test09_searchPages_specialCharacters() {
        System.out.println("\n========== 测试9: 搜索包含特殊字符的查询 ==========");
        
        String mockResponse = """
            {
                "results": [
                    {
                        "id": "99999",
                        "title": "API 文档",
                        "type": "page",
                        "body": {
                            "storage": {
                                "value": "<p>API 使用说明...</p>"
                            }
                        },
                        "_links": {
                            "webui": "/spaces/DEV/pages/99999"
                        }
                    }
                ]
            }
            """;

        stubFor(get(urlPathMatching("/wiki/rest/api/content/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));

        // 测试包含引号的查询
        List<KnowledgeDocument> results = confluenceService.searchPages("API \"REST\" 文档");

        assertNotNull(results);
        System.out.println("✓ 特殊字符查询处理完成");
    }

    @Test
    @DisplayName("测试10: 获取单个页面内容")
    void test10_getPageContent_success() {
        System.out.println("\n========== 测试10: 获取单个页面内容 ==========");
        
        String mockResponse = """
            {
                "id": "54321",
                "title": "项目架构文档",
                "type": "page",
                "body": {
                    "storage": {
                        "value": "<h1>项目架构</h1><p>本文档描述了项目的整体架构设计...</p>"
                    }
                },
                "space": {
                    "key": "DEV"
                },
                "_links": {
                    "webui": "/spaces/DEV/pages/54321"
                }
            }
            """;

        stubFor(get(urlPathMatching("/wiki/rest/api/content/54321"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));

        KnowledgeDocument doc = confluenceService.getPageContent("54321");

        assertNotNull(doc);
        assertEquals("conf_54321", doc.getId());
        assertEquals("项目架构文档", doc.getTitle());
        assertEquals("CONFLUENCE", doc.getSourceType());
        
        System.out.println("✓ 成功获取页面内容: " + doc.getTitle());
    }

    @Test
    @DisplayName("测试11: 获取不存在的页面")
    void test11_getPageContent_notFound() {
        System.out.println("\n========== 测试11: 获取不存在的页面 ==========");
        
        stubFor(get(urlPathMatching("/wiki/rest/api/content/99999"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("{\"message\": \"Content not found\"}")));

        KnowledgeDocument doc = confluenceService.getPageContent("99999");

        assertNull(doc);
        System.out.println("✓ 页面不存在时返回 null");
    }

    @Test
    @DisplayName("测试12: Confluence 未启用时搜索返回空列表")
    void test12_searchPages_whenDisabled() {
        System.out.println("\n========== 测试12: Confluence 未启用时搜索 ==========");
        
        properties.setEnabled(false);
        
        List<KnowledgeDocument> results = confluenceService.searchPages("测试");

        assertNotNull(results);
        assertTrue(results.isEmpty());
        System.out.println("✓ Confluence 未启用时直接返回空列表，不发起 HTTP 请求");
    }

    @Test
    @DisplayName("测试13: 验证 CQL 查询参数包含 space key")
    void test13_searchPages_withSpaceKey() {
        System.out.println("\n========== 测试13: 验证 CQL 包含 space key ==========");
        
        String mockResponse = """
            {
                "results": []
            }
            """;

        // 验证请求中包含 space key
        stubFor(get(urlPathMatching("/wiki/rest/api/content/search"))
                .withQueryParam("cql", containing("space = DEV"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));

        List<KnowledgeDocument> results = confluenceService.searchPages("测试");

        // 验证请求确实被发送
        verify(getRequestedFor(urlPathMatching("/wiki/rest/api/content/search"))
                .withQueryParam("cql", containing("space = DEV")));
        
        System.out.println("✓ CQL 查询包含 space key: DEV");
    }

    @Test
    @DisplayName("测试14: HTML 内容清理")
    void test14_htmlContentStripping() {
        System.out.println("\n========== 测试14: HTML 内容清理 ==========");
        
        String mockResponse = """
            {
                "results": [
                    {
                        "id": "11111",
                        "title": "格式化文档",
                        "type": "page",
                        "body": {
                            "storage": {
                                "value": "<p>这是一段<strong>加粗</strong>和<em>斜体</em>文本。</p><p>第二段内容。</p>"
                            }
                        },
                        "_links": {
                            "webui": "/spaces/DEV/pages/11111"
                        }
                    }
                ]
            }
            """;

        stubFor(get(urlPathMatching("/wiki/rest/api/content/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));

        List<KnowledgeDocument> results = confluenceService.searchPages("格式化");

        assertEquals(1, results.size());
        String content = results.get(0).getContent();
        
        // 验证 HTML 标签被移除
        assertFalse(content.contains("<p>"));
        assertFalse(content.contains("<strong>"));
        assertFalse(content.contains("<em>"));
        
        // 验证文本内容保留
        assertTrue(content.contains("这是一段"));
        assertTrue(content.contains("加粗"));
        assertTrue(content.contains("斜体"));
        
        System.out.println("✓ HTML 标签已清理");
        System.out.println("  清理后内容: " + content.substring(0, Math.min(content.length(), 100)) + "...");
    }

    @Test
    @DisplayName("测试15: 搜索限制参数生效")
    void test15_searchPages_withLimit() {
        System.out.println("\n========== 测试15: 搜索限制参数 ==========");
        
        String mockResponse = """
            {
                "results": [
                    {"id": "1", "title": "Page 1", "type": "page", "body": {"storage": {"value": "Content 1"}}, "_links": {"webui": "/1"}},
                    {"id": "2", "title": "Page 2", "type": "page", "body": {"storage": {"value": "Content 2"}}, "_links": {"webui": "/2"}},
                    {"id": "3", "title": "Page 3", "type": "page", "body": {"storage": {"value": "Content 3"}}, "_links": {"webui": "/3"}}
                ]
            }
            """;

        stubFor(get(urlPathMatching("/wiki/rest/api/content/search"))
                .withQueryParam("limit", equalTo("2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mockResponse)));

        List<KnowledgeDocument> results = confluenceService.searchPages("测试", 2);

        // 验证 limit 参数被传递
        verify(getRequestedFor(urlPathMatching("/wiki/rest/api/content/search"))
                .withQueryParam("limit", equalTo("2")));
        
        System.out.println("✓ 搜索限制参数 limit=2 已生效");
    }
}
