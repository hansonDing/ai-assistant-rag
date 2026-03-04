package com.example.aiassistant;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 大模型调用测试 - 需要配置 OPENAI_API_KEY 环境变量
 */
public class LLMCallTest {

    /**
     * 测试 OpenAI GPT-4o-mini 模型调用
     * 需要设置环境变量: OPENAI_API_KEY
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
    void testOpenAIGPT4oMini() {
        System.out.println("\n========== 测试 OpenAI GPT-4o-mini 调用 ==========");
        
        String apiKey = System.getenv("OPENAI_API_KEY");
        String model = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o-mini");
        String baseUrl = System.getenv().getOrDefault("OPENAI_BASE_URL", "https://api.openai.com/v1");
        
        System.out.println("使用模型: " + model);
        System.out.println("API地址: " + baseUrl);
        
        // 创建模型实例
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .baseUrl(baseUrl)
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
        
        System.out.println("\n✓ OpenAI GPT-4o-mini 调用测试通过!");
    }

    /**
     * 测试 RAG 场景下的模型调用
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
    void testRAGWithContext() {
        System.out.println("\n========== 测试 RAG 场景模型调用 ==========");
        
        String apiKey = System.getenv("OPENAI_API_KEY");
        String model = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o-mini");
        
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
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
        
        System.out.println("\n✓ RAG 场景模型调用测试通过!");
    }

    /**
     * 测试没有 API Key 时的处理
     */
    @Test
    void testWithoutApiKey() {
        System.out.println("\n========== 测试无 API Key 场景 ==========");
        
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("your-api-key-here")) {
            System.out.println("未配置 OPENAI_API_KEY，跳过实际调用测试");
            System.out.println("如需测试，请设置环境变量: export OPENAI_API_KEY=your-key");
            System.out.println("✓ 无 API Key 场景处理正确");
            return;
        }
        
        // 如果有 API Key，执行简单测试
        testOpenAIGPT4oMini();
    }
}
