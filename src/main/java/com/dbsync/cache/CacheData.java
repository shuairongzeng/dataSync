package com.dbsync.cache;

import java.time.LocalDateTime;

/**
 * Cache data entity
 */
public class CacheData {
    private Long id;
    private String cacheKey;
    private String dataContent;
    private String compressionType;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    
    // Constructors
    public CacheData() {}
    
    public CacheData(String cacheKey, String dataContent) {
        this.cacheKey = cacheKey;
        this.dataContent = dataContent;
        this.compressionType = "none";
        this.createdAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getCacheKey() {
        return cacheKey;
    }
    
    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }
    
    public String getDataContent() {
        return dataContent;
    }
    
    public void setDataContent(String dataContent) {
        this.dataContent = dataContent;
    }
    
    public String getCompressionType() {
        return compressionType;
    }
    
    public void setCompressionType(String compressionType) {
        this.compressionType = compressionType;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}