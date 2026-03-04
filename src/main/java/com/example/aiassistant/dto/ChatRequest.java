package com.example.aiassistant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    
    @NotBlank(message = "Question cannot be empty")
    private String question;
    
    private String sessionId;
    
    private boolean useConfluenceFallback = true;
}
