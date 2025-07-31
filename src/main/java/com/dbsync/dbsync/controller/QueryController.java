package com.dbsync.dbsync.controller;

import com.dbsync.dbsync.entity.QueryHistory;
import com.dbsync.dbsync.entity.QueryResult;
import com.dbsync.dbsync.mapper.QueryHistoryMapper;
import com.dbsync.dbsync.service.QueryHistoryService;
import com.dbsync.dbsync.service.QueryService;
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
 * 查询控制器
 */
@RestController
@RequestMapping("/api/query")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
public class QueryController {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryController.class);
    
    @Autowired
    private QueryService queryService;
    
    @Autowired
    private QueryHistoryService queryHistoryService;
    
    /**
     * 获取查询历史
     */
    @GetMapping("/history")
    public ResponseEntity<?> getQueryHistory(@RequestParam(required = false) String createdBy,
                                           @RequestParam(required = false) Integer limit) {
        try {
            List<QueryHistory> history;
            
            if (createdBy != null && !createdBy.trim().isEmpty()) {
                if (limit != null && limit > 0) {
                    history = queryHistoryService.getRecentQueryHistoryByUser(createdBy, limit);
                } else {
                    history = queryHistoryService.getQueryHistoryByUser(createdBy);
                }
            } else {
                if (limit != null && limit > 0) {
                    history = queryHistoryService.getRecentQueryHistory(limit);
                } else {
                    history = queryHistoryService.getAllQueryHistory();
                }
            }
            
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("获取查询历史失败: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "获取查询历史失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 根据ID获取查询历史
     */
    @GetMapping("/history/{id}")
    public ResponseEntity<?> getQueryHistoryById(@PathVariable Long id) {
        try {
            QueryHistory history = queryHistoryService.getQueryHistoryById(id);
            if (history != null) {
                return ResponseEntity.ok(history);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "查询历史不存在");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("获取查询历史失败: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "获取查询历史失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 保存查询历史
     */
    @PostMapping("/history")
    public ResponseEntity<?> saveQueryHistory(@RequestBody QueryHistory queryHistory) {
        try {
            // 获取当前用户
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && queryHistory.getCreatedBy() == null) {
                queryHistory.setCreatedBy(auth.getName());
            }
            
            QueryHistory savedHistory = queryHistoryService.saveQueryHistory(queryHistory);
            return ResponseEntity.ok(savedHistory);
        } catch (Exception e) {
            logger.error("保存查询历史失败: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "保存查询历史失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 删除查询历史
     */
    @DeleteMapping("/history/{id}")
    public ResponseEntity<?> deleteQueryHistory(@PathVariable Long id) {
        try {
            boolean deleted = queryHistoryService.deleteQueryHistory(id);
            if (deleted) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "删除成功");
                return ResponseEntity.ok(response);
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "查询历史不存在");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("删除查询历史失败: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "删除查询历史失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 获取查询历史统计信息
     */
    @GetMapping("/history/stats")
    public ResponseEntity<?> getQueryHistoryStats(@RequestParam(required = false) String createdBy) {
        try {
            // 如果没有指定用户，使用当前用户
            if (createdBy == null || createdBy.trim().isEmpty()) {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null) {
                    createdBy = auth.getName();
                }
            }

            QueryHistoryMapper.QueryHistoryStats stats = queryHistoryService.getQueryHistoryStats(createdBy);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("获取查询历史统计信息失败: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 清理过期查询历史
     */
    @DeleteMapping("/history/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> cleanupExpiredHistory(@RequestParam(defaultValue = "30") int daysToKeep) {
        try {
            int deletedCount = queryHistoryService.cleanupExpiredHistory(daysToKeep);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "清理完成");
            response.put("deletedCount", deletedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("清理过期查询历史失败: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "清理失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 根据连接获取查询历史
     */
    @GetMapping("/history/connection/{connectionId}")
    public ResponseEntity<?> getQueryHistoryByConnection(@PathVariable Long connectionId) {
        try {
            List<QueryHistory> history = queryHistoryService.getQueryHistoryBySourceConnection(connectionId);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("根据连接获取查询历史失败: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "获取查询历史失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    /**
     * 根据状态获取查询历史
     */
    @GetMapping("/history/status/{status}")
    public ResponseEntity<?> getQueryHistoryByStatus(@PathVariable String status) {
        try {
            List<QueryHistory> history = queryHistoryService.getQueryHistoryByStatus(status);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("根据状态获取查询历史失败: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "获取查询历史失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
