package com.dbsync.dbsync.controller;

import com.dbsync.dbsync.service.DatabaseMetadataCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Legacy cache controller - now delegates to new cache system
 * @deprecated Use /api/cache endpoints from com.dbsync.cache.CacheController instead
 */
@RestController
@RequestMapping("/api/legacy-cache")
@CrossOrigin(origins = "*")
@Deprecated
public class LegacyCacheController {

    @Autowired
    private DatabaseMetadataCacheService cacheService;

    @PostMapping("/clear")
    public ResponseEntity<Map<String, String>> clearCache() {
        try {
            cacheService.evictAllCache();
            Map<String, String> response = new HashMap<>();
            response.put("message", "缓存已清除");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "清除缓存失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PostMapping("/clear/{connectionId}")
    public ResponseEntity<Map<String, String>> clearConnectionCache(@PathVariable Long connectionId) {
        try {
            cacheService.evictConnectionCache(connectionId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "连接缓存已清除");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "清除连接缓存失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        try {
            DatabaseMetadataCacheService.CacheStats stats = cacheService.getCacheStats();
            Map<String, Object> response = new HashMap<>();
            response.put("cacheInfos", stats.getCacheInfos());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "获取缓存统计失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}