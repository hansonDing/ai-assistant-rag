package com.example.aiassistant.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class KnowledgeDocument {
    
    private String id;
    private String title;
    private String content;
    private String sourceType;  // LOCAL or CONFLUENCE
    private String sourceUrl;
    private double relevanceScore;
    private long timestamp;
}
