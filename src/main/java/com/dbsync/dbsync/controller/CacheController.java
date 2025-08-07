package com.dbsync.dbsync.controller;

import com.dbsync.dbsync.service.DatabaseMetadataCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存管理控制器
 * 提供缓存清除、统计等管理功能
 */
@RestController
@RequestMapping("/api/cache")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class CacheController {

    private static final Logger logger = LoggerFactory.getLogger(CacheController.class);

    @Autowired
    private DatabaseMetadataCacheService cacheService;

    /**
     * 获取缓存统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<?> getCacheStats() {
        try {
            DatabaseMetadataCacheService.CacheStats stats = cacheService.getCacheStats();
            return ResponseEntity.ok(stats.getCacheInfos());
        } catch (Exception e) {
            logger.error("获取缓存统计失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "获取缓存统计失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 清除所有缓存
     */
    @DeleteMapping("/all")
    public ResponseEntity<?> evictAllCache() {
        try {
            cacheService.evictAllCache();
            logger.info("已清除所有数据库元数据缓存");
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "已清除所有缓存");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("清除所有缓存失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "清除缓存失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 清除指定连接的缓存
     */
    @DeleteMapping("/connection/{connectionId}")
    public ResponseEntity<?> evictConnectionCache(@PathVariable Long connectionId) {
        try {
            cacheService.evictConnectionCache(connectionId);
            logger.info("已清除连接{}的缓存", connectionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "已清除连接 " + connectionId + " 的缓存");
            response.put("connectionId", connectionId);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("清除连接{}缓存失败", connectionId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "清除连接缓存失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 清除指定表的缓存
     */
    @DeleteMapping("/connection/{connectionId}/table/{tableName}")
    public ResponseEntity<?> evictTableCache(@PathVariable Long connectionId,
                                           @PathVariable String tableName,
                                           @RequestParam(required = false) String schema) {
        try {
            cacheService.evictTableCache(connectionId, tableName, schema);
            logger.info("已清除表缓存，连接ID: {}, 表名: {}, Schema: {}", connectionId, tableName, schema);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "已清除表 " + tableName + " 的缓存");
            response.put("connectionId", connectionId);
            response.put("tableName", tableName);
            response.put("schema", schema);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("清除表缓存失败，连接ID: {}, 表名: {}", connectionId, tableName, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "清除表缓存失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 预热缓存 - 为指定连接预加载常用数据
     */
    @PostMapping("/warmup/{connectionId}")
    public ResponseEntity<?> warmupCache(@PathVariable Long connectionId,
                                       @RequestParam(required = false) String schema) {
        try {
            logger.info("开始预热缓存，连接ID: {}, Schema: {}", connectionId, schema);
            
            // 预加载表列表
            cacheService.getTables(connectionId, schema);
            
            // 预加载Schema列表
            cacheService.getSchemas(connectionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "缓存预热完成");
            response.put("connectionId", connectionId);
            response.put("schema", schema);
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("缓存预热完成，连接ID: {}", connectionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("缓存预热失败，连接ID: {}", connectionId, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "缓存预热失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 获取缓存健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<?> getCacheHealth() {
        try {
            DatabaseMetadataCacheService.CacheStats stats = cacheService.getCacheStats();
            Map<String, Object> cacheInfos = stats.getCacheInfos();
            
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("cacheCount", cacheInfos.size());
            health.put("timestamp", System.currentTimeMillis());
            
            // 计算总体命中率
            double totalHitRate = 0.0;
            int cacheWithStats = 0;
            
            for (Object cacheInfo : cacheInfos.values()) {
                if (cacheInfo instanceof Map) {
                    Map<String, Object> info = (Map<String, Object>) cacheInfo;
                    Object hitRate = info.get("hitRate");
                    if (hitRate instanceof Double) {
                        totalHitRate += (Double) hitRate;
                        cacheWithStats++;
                    }
                }
            }
            
            if (cacheWithStats > 0) {
                health.put("averageHitRate", totalHitRate / cacheWithStats);
            } else {
                health.put("averageHitRate", 0.0);
            }
            
            health.put("details", cacheInfos);
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            logger.error("获取缓存健康状态失败", e);
            Map<String, Object> health = new HashMap<>();
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(503).body(health);
        }
    }
}
