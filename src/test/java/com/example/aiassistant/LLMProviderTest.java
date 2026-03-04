package com.example.aiassistant;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 大模型调用测试
 * 需要配置对应的 API Key 环境变量
 */
public class LLMProviderTest {

    /**
     * 测试 Moonshot (月之暗面 Kimi)
     * 环境变量: MOONSHOT_API_KEY
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "MOONSHOT_API_KEY", matches = ".+")
    void testMoonshot() {
        System.out.println("\n========== 测试 Moonshot (月之暗面 Kimi) ==========");
        
        String apiKey = System.getenv("MOONSHOT_API_KEY");
        String model = System.getenv().getOrDefault("MOONSHOT_MODEL", "moonshot-v1-8k");
        String baseUrl = "https://api.moonshot.cn/v1";
        
        testLLM("Moonshot", apiKey, model, baseUrl);
    }

    /**
     * 测试 DeepSeek
     * 环境变量: DEEPSEEK_API_KEY
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "DEEPSEEK_API_KEY", matches = ".+")
    void testDeepSeek() {
        System.out.println("\n========== 测试 DeepSeek ==========");
        
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        String model = System.getenv().getOrDefault("DEEPSEEK_MODEL", "deepseek-chat");
        String baseUrl = "https://api.deepseek.com/v1";
        
        testLLM("DeepSeek", apiKey, model, baseUrl);
    }

    /**
     * 测试 OpenAI
     * 环境变量: OPENAI_API_KEY
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
    void testOpenAI() {
        System.out.println("\n========== 测试 OpenAI ==========");
        
        String apiKey = System.getenv("OPENAI_API_KEY");
        String model = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o-mini");
        String baseUrl = "https://api.openai.com/v1";
        
        testLLM("OpenAI", apiKey, model, baseUrl);
    }

    /**
     * 测试阿里云百炼 (通义千问)
     * 环境变量: DASHSCOPE_API_KEY
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY", matches = ".+")
    void testDashScope() {
        System.out.println("\n========== 测试阿里云百炼 (通义千问) ==========");
        
        String apiKey = System.getenv("DASHSCOPE_API_KEY");
        String model = System.getenv().getOrDefault("DASHSCOPE_MODEL", "qwen-turbo");
        String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";
        
        testLLM("DashScope", apiKey, model, baseUrl);
    }

    private void testLLM(String provider, String apiKey, String model, String baseUrl) {
        System.out.println("提供商: " + provider);
        System.out.println("模型: " + model);
        System.out.println("API地址: " + baseUrl);
        
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .baseUrl(baseUrl)
                .temperature(0.7)
                .build();
        
        String testQuestion = "你好，请用一句话介绍自己。";
        System.out.println("\n发送问题: " + testQuestion);
        
        long startTime = System.currentTimeMillis();
        String response = chatModel.generate(testQuestion);
        long endTime = System.currentTimeMillis();
        
        System.out.println("\n模型回复: " + response);
        System.out.println("响应时间: " + (endTime - startTime) + "ms");
        
        assertNotNull(response, "响应不应为空");
        assertFalse(response.isEmpty(), "响应不应为空字符串");
        
        System.out.println("\n✓ " + provider + " 调用测试通过!");
    }

    /**
     * 测试 RAG 场景
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "MOONSHOT_API_KEY", matches = ".+")
    void testRAGWithContext() {
        System.out.println("\n========== 测试 RAG 场景 ==========");
        
        String apiKey = System.getenv("MOONSHOT_API_KEY");
        String model = System.getenv().getOrDefault("MOONSHOT_MODEL", "moonshot-v1-8k");
        
        ChatLanguageModel chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .baseUrl("https://api.moonshot.cn/v1")
                .temperature(0.7)
                .build();
        
        String context = """
            上下文信息:
            --- Source 1 ---
            Title: Spring Boot 简介
            Content: Spring Boot 是一个开源的 Java 框架，用于快速构建 Spring 应用。
            
            用户问题: 什么是 Spring Boot?
            """;
        
        long startTime = System.currentTimeMillis();
        String response = chatModel.generate(context);
        long endTime = System.currentTimeMillis();
        
        System.out.println("模型回复: " + response);
        System.out.println("响应时间: " + (endTime - startTime) + "ms");
        
        assertNotNull(response);
        System.out.println("\n✓ RAG 场景测试通过!");
    }
}
