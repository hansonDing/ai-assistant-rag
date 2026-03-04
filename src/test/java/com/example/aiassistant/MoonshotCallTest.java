package com.example.aiassistant;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Moonshot (月之暗面 Kimi) 大模型调用测试
 */
public class MoonshotCallTest {

    private static final String MOONSHOT_API_KEY = "sk-kimi-A50nf4VrQqWO43ebZNhK6fIJUSzbEmxKPWzKCFWslPXP36NUTvQwUUrNIvCn5fRe";
    private static final String MOONSHOT_BASE_URL = "https://api.moonshot.cn/v1";
    private static final String MOONSHOT_MODEL = "moonshot-v1-8k";

    /**
     * 测试 Moonshot (Kimi) 模型调用
     */
    @Test
    void testMoonshotKimi() {
        System.out.println("\n========== 测试 Moonshot (月之暗面 Kimi) 调用 ==========");
        System.out.println("模型: " + MOONSHOT_MODEL);
        System.out.println("API地址: " + MOONSHOT_BASE_URL);
        
        // 创建 Moonshot 模型实例 (兼容 OpenAI API 格式)
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .apiKey(MOONSHOT_API_KEY)
                .modelName(MOONSHOT_MODEL)
                .baseUrl(MOONSHOT_BASE_URL)
                .temperature(0.7)
                .build();
        
        // 发送测试消息
        String testQuestion = "你好，请用一句话介绍自己。";
        System.out.println("\n发送问题: " + testQuestion);
        
        long startTime = System.currentTimeMillis();
        String response = chatModel.generate(testQuestion);
        long endTime = System.currentTimeMillis();
        
        System.out.println("\n模型回复: " + response);
        System.out.println("响应时间: " + (endTime - startTime) + "ms");
        
        // 验证响应
        assertNotNull(response, "响应不应为空");
        assertFalse(response.isEmpty(), "响应不应为空字符串");
        assertTrue(response.length() > 5, "响应长度应大于5个字符");
        
        System.out.println("\n✓ Moonshot (Kimi) 调用测试通过!");
    }

    /**
     * 测试 RAG 场景下的 Moonshot 调用
     */
    @Test
    void testRAGWithMoonshot() {
        System.out.println("\n========== 测试 RAG + Moonshot 调用 ==========");
        
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .apiKey(MOONSHOT_API_KEY)
                .modelName(MOONSHOT_MODEL)
                .baseUrl(MOONSHOT_BASE_URL)
                .temperature(0.7)
                .build();
        
        // 模拟 RAG 上下文
        String context = """
            上下文信息:
            --- Source 1 ---
            Title: Spring Boot 简介
            Content: Spring Boot 是一个开源的 Java 框架，用于快速构建 Spring 应用。
            
            --- Source 2 ---
            Title: Spring Boot 特性
            Content: Spring Boot 提供了自动配置、嵌入式服务器和开箱即用的功能。
            """;
        
        String question = "什么是 Spring Boot?";
        
        String prompt = """
            你是一个有帮助的 AI 助手。请根据提供的上下文回答用户问题。
            
            %s
            
            用户问题: %s
            """.formatted(context, question);
        
        System.out.println("发送带上下文的请求...");
        
        long startTime = System.currentTimeMillis();
        String response = chatModel.generate(prompt);
        long endTime = System.currentTimeMillis();
        
        System.out.println("\n模型回复:");
        System.out.println(response);
        System.out.println("\n响应时间: " + (endTime - startTime) + "ms");
        
        assertNotNull(response);
        assertFalse(response.isEmpty());
        
        System.out.println("\n✓ RAG + Moonshot 调用测试通过!");
    }

    /**
     * 测试中文对话
     */
    @Test
    void testChineseConversation() {
        System.out.println("\n========== 测试中文对话 ==========");
        
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .apiKey(MOONSHOT_API_KEY)
                .modelName(MOONSHOT_MODEL)
                .baseUrl(MOONSHOT_BASE_URL)
                .temperature(0.7)
                .build();
        
        String question = "请介绍一下RAG（检索增强生成）技术是什么？";
        System.out.println("问题: " + question);
        
        long startTime = System.currentTimeMillis();
        String response = chatModel.generate(question);
        long endTime = System.currentTimeMillis();
        
        System.out.println("\n回答:");
        System.out.println(response);
        System.out.println("\n响应时间: " + (endTime - startTime) + "ms");
        
        assertNotNull(response);
        assertFalse(response.isEmpty());
        assertTrue(response.contains("检索") || response.contains("生成") || response.contains("RAG"),
                "回答应该包含 RAG 相关内容");
        
        System.out.println("\n✓ 中文对话测试通过!");
    }
}
