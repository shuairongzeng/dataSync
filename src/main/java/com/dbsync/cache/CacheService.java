package com.dbsync.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Cache service for managing database query results and metadata
 */
@Service
public class CacheService {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);
    
    @Autowired
    private CacheRepository cacheRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Get cached data by key
     */
    public <T> Optional<T> get(String cacheKey, Class<T> clazz) {
        try {
            Optional<CacheData> cacheData = cacheRepository.findByCacheKey(cacheKey);
            if (cacheData.isPresent() && !isExpired(cacheData.get())) {
                // Update hit count
                cacheRepository.incrementHitCount(cacheKey);
                
                String jsonData = cacheData.get().getDataContent();
                T result = objectMapper.readValue(jsonData, clazz);
                return Optional.of(result);
            }
        } catch (Exception e) {
            logger.error("Error retrieving cache for key: {}", cacheKey, e);
        }
        return Optional.empty();
    }
    
    /**
     * Get cached data by key with TypeReference support for generic types
     */
    public <T> Optional<T> get(String cacheKey, TypeReference<T> typeReference) {
        try {
            Optional<CacheData> cacheData = cacheRepository.findByCacheKey(cacheKey);
            if (cacheData.isPresent() && !isExpired(cacheData.get())) {
                // Update hit count
                cacheRepository.incrementHitCount(cacheKey);
                
                String jsonData = cacheData.get().getDataContent();
                T result = objectMapper.readValue(jsonData, typeReference);
                return Optional.of(result);
            }
        } catch (Exception e) {
            logger.error("Error retrieving cache for key: {}", cacheKey, e);
        }
        return Optional.empty();
    }
    
    /**
     * Store data in cache
     */
    public <T> void put(String cacheKey, T data, CacheType cacheType, String dataSource, int ttlMinutes) {
        try {
            String jsonData = objectMapper.writeValueAsString(data);
            String checksum = calculateChecksum(jsonData);
            
            LocalDateTime expiresAt = ttlMinutes > 0 ? 
                LocalDateTime.now().plusMinutes(ttlMinutes) : null;
            
            cacheRepository.saveCache(cacheKey, cacheType.name(), dataSource, 
                jsonData, jsonData.length(), checksum, expiresAt);
                
            logger.debug("Cached data for key: {} (size: {} bytes)", cacheKey, jsonData.length());
        } catch (JsonProcessingException e) {
            logger.error("Error serializing data for cache key: {}", cacheKey, e);
        }
    }
    
    /**
     * Remove cache entry
     */
    public void evict(String cacheKey) {
        cacheRepository.deleteByKey(cacheKey);
        logger.debug("Evicted cache for key: {}", cacheKey);
    }
    
    /**
     * Clear all cache entries of specific type
     */
    public void evictByType(CacheType cacheType) {
        cacheRepository.deleteByType(cacheType.name());
        logger.info("Evicted all cache entries of type: {}", cacheType);
    }
    
    /**
     * Clear all cache entries for a data source
     */
    public void evictByDataSource(String dataSource) {
        cacheRepository.deleteByDataSource(dataSource);
        logger.info("Evicted all cache entries for data source: {}", dataSource);
    }
    
    /**
     * Get cache statistics
     */
    public CacheStats getStats() {
        return cacheRepository.getCacheStats();
    }
    
    /**
     * Clean up expired cache entries
     */
    public int cleanupExpired() {
        int deletedCount = cacheRepository.deleteExpired();
        logger.info("Cleaned up {} expired cache entries", deletedCount);
        return deletedCount;
    }
    
    /**
     * Get cache metadata for monitoring
     */
    public List<CacheMetadata> getCacheMetadata() {
        return cacheRepository.getAllCacheMetadata();
    }
    
    /**
     * Check if cache entry is expired
     */
    private boolean isExpired(CacheData cacheData) {
        if (cacheData.getExpiresAt() == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(cacheData.getExpiresAt());
    }
    
    /**
     * Calculate checksum for data integrity
     */
    private String calculateChecksum(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            logger.warn("MD5 algorithm not available, using simple hash", e);
            return String.valueOf(data.hashCode());
        }
    }
    
    /**
     * Generate cache key for query results
     */
    public String generateQueryCacheKey(String connectionId, String sql, String schema) {
        String normalizedSql = sql.trim().toLowerCase().replaceAll("\\s+", " ");
        String keyData = connectionId + ":" + (schema != null ? schema : "") + ":" + normalizedSql;
        return "query:" + calculateChecksum(keyData);
    }
    
    /**
     * Generate cache key for table list
     */
    public String generateTableListCacheKey(String connectionId, String schema) {
        return "tables:" + connectionId + ":" + (schema != null ? schema : "default");
    }
    
    /**
     * Generate cache key for table schema
     */
    public String generateTableSchemaCacheKey(String connectionId, String tableName, String schema) {
        return "schema:" + connectionId + ":" + (schema != null ? schema : "default") + ":" + tableName;
    }
}