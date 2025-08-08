package com.dbsync.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cache management REST API controller
 */
@RestController
@RequestMapping("/api/cache")
@CrossOrigin(origins = "*")
public class CacheController {
    
    @Autowired
    private CacheService cacheService;
    
    /**
     * Get cache statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        try {
            CacheStats stats = cacheService.getStats();
            List<CacheMetadata> metadata = cacheService.getCacheMetadata();
            
            Map<String, Object> response = new HashMap<>();
            response.put("stats", stats);
            response.put("metadata", metadata);
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Clear all cache
     */
    @DeleteMapping("/clear")
    public ResponseEntity<Map<String, Object>> clearAllCache() {
        try {
            // Clear all cache types
            for (CacheType type : CacheType.values()) {
                cacheService.evictByType(type);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "All cache cleared successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Clear cache by type
     */
    @DeleteMapping("/clear/{type}")
    public ResponseEntity<Map<String, Object>> clearCacheByType(@PathVariable String type) {
        try {
            CacheType cacheType = CacheType.valueOf(type.toUpperCase());
            cacheService.evictByType(cacheType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Cache type " + type + " cleared successfully");
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Invalid cache type: " + type);
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Clear cache by data source
     */
    @DeleteMapping("/clear/datasource/{dataSource}")
    public ResponseEntity<Map<String, Object>> clearCacheByDataSource(@PathVariable String dataSource) {
        try {
            cacheService.evictByDataSource(dataSource);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Cache for data source " + dataSource + " cleared successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Clear specific cache entry
     */
    @DeleteMapping("/clear/key/{cacheKey}")
    public ResponseEntity<Map<String, Object>> clearCacheByKey(@PathVariable String cacheKey) {
        try {
            cacheService.evict(cacheKey);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Cache entry " + cacheKey + " cleared successfully");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Cleanup expired cache entries
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupExpiredCache() {
        try {
            int deletedCount = cacheService.cleanupExpired();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Cleanup completed");
            response.put("deletedCount", deletedCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Get cache health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getCacheHealth() {
        try {
            CacheStats stats = cacheService.getStats();
            
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("totalEntries", stats.getTotalEntries());
            health.put("totalSize", stats.getFormattedSize());
            health.put("hitRate", stats.getFormattedHitRate());
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("health", health);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
    
    /**
     * Warmup cache for specific connection
     */
    @PostMapping("/warmup/{connectionId}")
    public ResponseEntity<Map<String, Object>> warmupCache(
            @PathVariable String connectionId,
            @RequestParam(required = false) String schema) {
        try {
            // This would trigger preloading of commonly used data
            // Implementation depends on your specific caching strategy
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Cache warmup initiated for connection " + connectionId);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}