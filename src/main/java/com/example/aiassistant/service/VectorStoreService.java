package com.example.aiassistant.service;

import com.example.aiassistant.model.KnowledgeDocument;

import java.util.List;

public interface VectorStoreService {
    
    /**
     * Add a document to the vector store
     */
    void addDocument(KnowledgeDocument document);
    
    /**
     * Search for similar documents
     */
    List<KnowledgeDocument> search(String query, int maxResults);
    
    /**
     * Search for similar documents with minimum score threshold
     */
    List<KnowledgeDocument> search(String query, int maxResults, double minScore);
    
    /**
     * Delete a document by ID
     */
    void deleteDocument(String id);
    
    /**
     * Clear all documents
     */
    void clearAll();
}
