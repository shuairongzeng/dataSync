package com.dbsync.dbsync.controller;

import com.dbsync.dbsync.entity.QueryHistory;
import com.dbsync.dbsync.entity.QueryResult;
import com.dbsync.dbsync.entity.EnhancedQueryResult;
import com.dbsync.dbsync.service.EnhancedQueryService;
import com.dbsync.dbsync.service.QueryService;
import com.dbsync.dbsync.service.FieldMappingService;
import com.dbsync.dbsync.service.QueryHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced query controller with caching support
 */
@RestController
@RequestMapping("/api/enhanced-query")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public class EnhancedQueryController {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedQueryController.class);
    
    @Autowired
    private EnhancedQueryService enhancedQueryService;
    
    @Autowired
    private QueryService queryService;
    
    @Autowired
    private FieldMappingService fieldMappingService;
    
    @Autowired
    private QueryHistoryService queryHistoryService;
    
    /**
     * Execute SQL query with caching support
     */
    @PostMapping("/execute")
    public ResponseEntity<?> executeQuery(@RequestBody QueryRequest request) {
        try {
            // Validate request
            if (request.getConnectionId() == null || request.getSql() == null || request.getSql().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "连接ID和SQL语句不能为空");
                return ResponseEntity.badRequest().body(error);
            }
            
            // Execute query with Chinese column names support
            EnhancedQueryResult result;
            
            // 首先执行原始查询
            QueryResult originalResult = enhancedQueryService.executeQuery(
                request.getConnectionId(), 
                request.getSql(), 
                request.getSchema(),
                request.isUseCache() != null ? request.isUseCache() : true,
                request.getPage(),
                request.getPageSize()
            );
            
            // 创建增强结果
            result = new EnhancedQueryResult(originalResult);
            
            // 尝试添加中文字段名
            try {
                if (originalResult.getColumns() != null && !originalResult.getColumns().isEmpty()) {
                    Map<String, String> displayNames = fieldMappingService.getQueryFieldDisplayNames(
                        request.getConnectionId(), 
                        request.getSql(), 
                        request.getSchema(), 
                        originalResult.getColumns());
                    
                    // 设置字段显示名称
                    result.setFieldDisplayNames(displayNames);
                    
                    logger.info("成功为 {} 个字段设置了中文显示名称，总字段数: {}", 
                               displayNames.size(), originalResult.getColumns().size());
                    
                    // 记录中文字段覆盖率
                    double coverage = result.getChineseColumnCoverage();
                    logger.debug("中文字段覆盖率: {:.2f}%", coverage * 100);
                }
            } catch (Exception e) {
                logger.error("设置字段中文名称失败: {}", e.getMessage(), e);
                // 失败时禁用中文列名功能，不影响查询功能
                result.setEnableChineseColumnNames(false);
            }
            
            // Save to query history if requested
            if (request.isSaveHistory()) {
                try {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    QueryHistory history = new QueryHistory();
                    history.setSourceConnectionId(request.getConnectionId());
                    history.setSql(request.getSql());
                    history.setExecutionTime(result.getExecutionTime().intValue());
                    history.setResultRows(result.getTotalRows());
                    history.setStatus("SUCCESS");
                    if (auth != null) {
                        history.setCreatedBy(auth.getName());
                    }
                    queryHistoryService.saveQueryHistory(history);
                } catch (Exception e) {
                    logger.warn("Failed to save query history: {}", e.getMessage());
                }
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            logger.error("Query execution failed: {}", e.getMessage(), e);
            
            // Save failed query to history if requested
            if (request.isSaveHistory()) {
                try {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    QueryHistory history = new QueryHistory();
                    history.setSourceConnectionId(request.getConnectionId());
                    history.setSql(request.getSql());
                    history.setStatus("FAILED");
                    history.setErrorMessage(e.getMessage());
                    if (auth != null) {
                        history.setCreatedBy(auth.getName());
                    }
                    queryHistoryService.saveQueryHistory(history);
                } catch (Exception ex) {
                    logger.warn("Failed to save failed query history: {}", ex.getMessage());
                }
            }
            
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Get table list with caching
     */
    @GetMapping("/tables/{connectionId}")
    public ResponseEntity<?> getTables(@PathVariable Long connectionId,
                                     @RequestParam(required = false) String schema,
                                     @RequestParam(defaultValue = "true") boolean useCache) {
        try {
            List<String> tables = enhancedQueryService.getTables(connectionId, schema, useCache);
            return ResponseEntity.ok(tables);
        } catch (Exception e) {
            logger.error("Failed to get tables: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Get table columns with caching
     */
    @GetMapping("/tables/{connectionId}/{tableName}/columns")
    public ResponseEntity<?> getTableColumns(@PathVariable Long connectionId,
                                           @PathVariable String tableName,
                                           @RequestParam(required = false) String schema,
                                           @RequestParam(defaultValue = "true") boolean useCache) {
        try {
            List<EnhancedQueryService.ColumnInfo> columns = enhancedQueryService.getTableColumns(
                connectionId, tableName, schema, useCache);
            return ResponseEntity.ok(columns);
        } catch (Exception e) {
            logger.error("Failed to get table columns: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Clear cache for specific connection (including field mapping cache)
     */
    @DeleteMapping("/cache/{connectionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> clearConnectionCache(@PathVariable Long connectionId) {
        try {
            // 清除原有缓存
            enhancedQueryService.clearConnectionCache(connectionId);
            
            // 清除字段映射缓存
            try {
                fieldMappingService.clearConnectionCache(connectionId);
            } catch (Exception e) {
                logger.warn("字段映射缓存清除失败: {}", e.getMessage());
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "缓存已清除（包括字段映射缓存）");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to clear cache: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Warmup cache for connection (including field mapping cache)
     */
    @PostMapping("/cache/warmup/{connectionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> warmupCache(@PathVariable Long connectionId,
                                       @RequestParam(required = false) String schema) {
        try {
            // 原有缓存预热
            enhancedQueryService.warmupCache(connectionId, schema);
            
            // 字段映射缓存预热
            try {
                fieldMappingService.warmupConnectionCache(connectionId, schema);
            } catch (Exception e) {
                logger.warn("字段映射缓存预热失败: {}", e.getMessage());
            }
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "缓存预热完成（包括字段映射缓存）");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to warmup cache: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Query request DTO
     */
    public static class QueryRequest {
        private Long connectionId;
        private String sql;
        private String schema;
        private Boolean useCache = true;
        private boolean saveHistory = true;
        private Integer page;
        private Integer pageSize;
        
        // Getters and Setters
        public Long getConnectionId() { return connectionId; }
        public void setConnectionId(Long connectionId) { this.connectionId = connectionId; }
        public String getSql() { return sql; }
        public void setSql(String sql) { this.sql = sql; }
        public String getSchema() { return schema; }
        public void setSchema(String schema) { this.schema = schema; }
        public Boolean isUseCache() { return useCache; }
        public void setUseCache(Boolean useCache) { this.useCache = useCache; }
        public boolean isSaveHistory() { return saveHistory; }
        public void setSaveHistory(boolean saveHistory) { this.saveHistory = saveHistory; }
        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    }
    
    /**
     * 清除字段映射缓存
     */
    @PostMapping("/clear-field-mapping-cache")
    public ResponseEntity<?> clearFieldMappingCache(@RequestBody Map<String, Object> request) {
        try {
            Long connectionId = ((Number) request.get("connectionId")).longValue();
            fieldMappingService.clearConnectionCache(connectionId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "字段映射缓存已清除");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("清除字段映射缓存失败: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "清除缓存失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 预热字段映射缓存
     */
    @PostMapping("/warmup-field-mapping-cache")
    public ResponseEntity<?> warmupFieldMappingCache(@RequestBody Map<String, Object> request) {
        try {
            Long connectionId = ((Number) request.get("connectionId")).longValue();
            String schema = (String) request.get("schema");
            fieldMappingService.warmupConnectionCache(connectionId, schema);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "字段映射缓存预热完成");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("预热字段映射缓存失败: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "预热缓存失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}