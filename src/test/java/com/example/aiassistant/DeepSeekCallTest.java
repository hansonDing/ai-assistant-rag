package com.example.aiassistant;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DeepSeek 大模型调用测试
 */
public class DeepSeekCallTest {

    private static final String DEEPSEEK_API_KEY = "sk-9e8c2611b860469ea927f53d93a2ee77";
    private static final String DEEPSEEK_BASE_URL = "https://api.deepseek.com/v1";
    private static final String DEEPSEEK_MODEL = "deepseek-chat";

    /**
     * 测试 DeepSeek 模型调用
     */
    @Test
    void testDeepSeek() {
        System.out.println("\n========== 测试 DeepSeek 调用 ==========");
        System.out.println("模型: " + DEEPSEEK_MODEL);
        System.out.println("API地址: " + DEEPSEEK_BASE_URL);
        
        // 创建 DeepSeek 模型实例 (兼容 OpenAI API 格式)
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .apiKey(DEEPSEEK_API_KEY)
                .modelName(DEEPSEEK_MODEL)
                .baseUrl(DEEPSEEK_BASE_URL)
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
        
        System.out.println("\n✓ DeepSeek 调用测试通过!");
    }

    /**
     * 测试 RAG 场景下的 DeepSeek 调用
     */
    @Test
    void testRAGWithDeepSeek() {
        System.out.println("\n========== 测试 RAG + DeepSeek 调用 ==========");
        
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .apiKey(DEEPSEEK_API_KEY)
                .modelName(DEEPSEEK_MODEL)
                .baseUrl(DEEPSEEK_BASE_URL)
                .temperature(0.7)
                .build();
        
        // 模拟 RAG 上下文
        String context = """
            你是一个有帮助的 AI 助手。请根据提供的上下文回答用户问题。
            
            上下文信息:
            --- Source 1 ---
            Title: Spring Boot 简介
            Content: Spring Boot 是一个开源的 Java 框架，用于快速构建 Spring 应用。
            
            --- Source 2 ---
            Title: Spring Boot 特性
            Content: Spring Boot 提供了自动配置、嵌入式服务器和开箱即用的功能。
            
            用户问题: 什么是 Spring Boot?
            """;
        
        System.out.println("发送带上下文的请求...");
        
        long startTime = System.currentTimeMillis();
        String response = chatModel.generate(context);
        long endTime = System.currentTimeMillis();
        
        System.out.println("\n模型回复:");
        System.out.println(response);
        System.out.println("\n响应时间: " + (endTime - startTime) + "ms");
        
        assertNotNull(response);
        assertFalse(response.isEmpty());
        
        System.out.println("\n✓ RAG + DeepSeek 调用测试通过!");
    }

    /**
     * 测试中文对话
     */
    @Test
    void testChineseConversation() {
        System.out.println("\n========== 测试中文对话 ==========");
        
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .apiKey(DEEPSEEK_API_KEY)
                .modelName(DEEPSEEK_MODEL)
                .baseUrl(DEEPSEEK_BASE_URL)
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
        
        System.out.println("\n✓ 中文对话测试通过!");
    }

    /**
     * 测试代码生成能力
     */
    @Test
    void testCodeGeneration() {
        System.out.println("\n========== 测试代码生成能力 ==========");
        
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .apiKey(DEEPSEEK_API_KEY)
                .modelName(DEEPSEEK_MODEL)
                .baseUrl(DEEPSEEK_BASE_URL)
                .temperature(0.3)
                .build();
        
        String prompt = "写一个 Java 方法，实现两个数的加法，包含完整的注释。";
        System.out.println("提示: " + prompt);
        
        long startTime = System.currentTimeMillis();
        String response = chatModel.generate(prompt);
        long endTime = System.currentTimeMillis();
        
        System.out.println("\n生成的代码:");
        System.out.println(response);
        System.out.println("\n响应时间: " + (endTime - startTime) + "ms");
        
        assertNotNull(response);
        assertTrue(response.contains("public") || response.contains("class") || response.contains("add"),
                "应该包含 Java 代码关键字");
        
        System.out.println("\n✓ 代码生成测试通过!");
    }
}
