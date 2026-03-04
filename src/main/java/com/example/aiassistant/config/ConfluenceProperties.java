package com.example.aiassistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "confluence")
public class ConfluenceProperties {

    private boolean enabled = false;
    private String baseUrl;
    private String username;
    private String apiToken;
    private String spaceKey;
    private int maxResults = 10;
    private double minRelevanceScore = 0.5;
}
