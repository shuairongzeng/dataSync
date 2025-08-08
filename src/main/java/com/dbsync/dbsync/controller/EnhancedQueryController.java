package com.dbsync.dbsync.controller;

import com.dbsync.dbsync.entity.QueryHistory;
import com.dbsync.dbsync.entity.QueryResult;
import com.dbsync.dbsync.service.EnhancedQueryService;
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
            
            // Execute query with caching
            QueryResult result = enhancedQueryService.executeQuery(
                request.getConnectionId(), 
                request.getSql(), 
                request.getSchema(),
                request.isUseCache() != null ? request.isUseCache() : true
            );
            
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
     * Clear cache for specific connection
     */
    @DeleteMapping("/cache/{connectionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> clearConnectionCache(@PathVariable Long connectionId) {
        try {
            enhancedQueryService.clearConnectionCache(connectionId);
            Map<String, String> response = new HashMap<>();
            response.put("message", "缓存已清除");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to clear cache: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * Warmup cache for connection
     */
    @PostMapping("/cache/warmup/{connectionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> warmupCache(@PathVariable Long connectionId,
                                       @RequestParam(required = false) String schema) {
        try {
            enhancedQueryService.warmupCache(connectionId, schema);
            Map<String, String> response = new HashMap<>();
            response.put("message", "缓存预热完成");
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
    }
}