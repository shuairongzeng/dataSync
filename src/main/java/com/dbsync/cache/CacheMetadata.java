package com.dbsync.cache;

import java.time.LocalDateTime;

/**
 * Cache metadata entity
 */
public class CacheMetadata {
    private Long id;
    private String cacheKey;
    private String cacheType;
    private String dataSource;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    private Integer hitCount;
    private Integer dataSize;
    private String checksum;
    
    // Constructors
    public CacheMetadata() {}
    
    public CacheMetadata(String cacheKey, String cacheType, String dataSource) {
        this.cacheKey = cacheKey;
        this.cacheType = cacheType;
        this.dataSource = dataSource;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.hitCount = 0;
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
    
    public String getCacheType() {
        return cacheType;
    }
    
    public void setCacheType(String cacheType) {
        this.cacheType = cacheType;
    }
    
    public String getDataSource() {
        return dataSource;
    }
    
    public void setDataSource(String dataSource) {
        this.dataSource = dataSource;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public Integer getHitCount() {
        return hitCount;
    }
    
    public void setHitCount(Integer hitCount) {
        this.hitCount = hitCount;
    }
    
    public Integer getDataSize() {
        return dataSize;
    }
    
    public void setDataSize(Integer dataSize) {
        this.dataSize = dataSize;
    }
    
    public String getChecksum() {
        return checksum;
    }
    
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }
}