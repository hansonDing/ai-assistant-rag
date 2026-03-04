package com.example.aiassistant.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatResponse {
    
    private String answer;
    private List<SourceInfo> sources;
    private String sessionId;
    private long processingTimeMs;
    
    @Data
    @Builder
    public static class SourceInfo {
        private String sourceType;  // LOCAL or CONFLUENCE
        private String title;
        private String url;
        private double relevanceScore;
        private String content;
    }
}
