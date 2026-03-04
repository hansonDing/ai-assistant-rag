package com.example.aiassistant.service;

import com.example.aiassistant.model.KnowledgeDocument;

import java.util.List;

public interface ConfluenceService {
    
    /**
     * Search Confluence pages by query
     */
    List<KnowledgeDocument> searchPages(String query);
    
    /**
     * Search Confluence pages with limit
     */
    List<KnowledgeDocument> searchPages(String query, int limit);
    
    /**
     * Get page content by page ID
     */
    KnowledgeDocument getPageContent(String pageId);
    
    /**
     * Check if Confluence integration is enabled and configured
     */
    boolean isEnabled();
}
