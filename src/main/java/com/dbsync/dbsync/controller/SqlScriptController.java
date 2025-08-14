package com.dbsync.dbsync.controller;

import com.dbsync.dbsync.service.SqlScriptService;
import com.dbsync.dbsync.service.SqlScriptService.ScriptExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * SQL脚本执行控制器
 * 支持多语句脚本的执行，特别优化Oracle数据库支持
 */
@RestController
@RequestMapping("/api/database")
@CrossOrigin(origins = "*")
public class SqlScriptController {
    
    private static final Logger logger = LoggerFactory.getLogger(SqlScriptController.class);
    
    @Autowired
    private SqlScriptService sqlScriptService;
    
    /**
     * 执行SQL脚本
     * 支持多语句、事务控制、Oracle特定语法
     */
    @PostMapping("/connections/{id}/script/execute")
    public ResponseEntity<?> executeScript(@PathVariable Long id, @RequestBody ScriptExecutionRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 参数验证
            if (request.getScript() == null || request.getScript().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("SQL脚本不能为空"));
            }
            
            logger.info("接收到脚本执行请求，连接ID: {}, 脚本长度: {}, 事务模式: {}", 
                       id, request.getScript().length(), request.isExecuteInTransaction());
            
            // 执行脚本
            ScriptExecutionResult result = sqlScriptService.executeScript(
                id, 
                request.getScript(), 
                request.getSchema(), 
                request.isExecuteInTransaction()
            );
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("totalCount", result.getTotalCount());
            response.put("successCount", result.getSuccessCount());
            response.put("failedCount", result.getFailedCount());
            response.put("totalTime", result.getTotalTime());
            response.put("statementResults", result.getStatementResults());
            response.put("timestamp", System.currentTimeMillis());
            
            if (result.isSuccess()) {
                logger.info("脚本执行成功，连接ID: {}, 耗时: {}ms", id, System.currentTimeMillis() - startTime);
                return ResponseEntity.ok(response);
            } else {
                logger.warn("脚本执行部分失败，连接ID: {}, 成功: {}, 失败: {}", 
                           id, result.getSuccessCount(), result.getFailedCount());
                return ResponseEntity.ok(response); // 仍然返回200，但在结果中标明失败
            }
            
        } catch (Exception e) {
            logger.error("脚本执行异常，连接ID: {}, 错误: {}", id, e.getMessage(), e);
            return ResponseEntity.status(500).body(createErrorResponse("脚本执行失败: " + e.getMessage()));
        }
    }
    
    /**
     * 解析SQL脚本（不执行，仅分析）
     * 用于前端预览脚本结构
     */
    @PostMapping("/script/parse")
    public ResponseEntity<?> parseScript(@RequestBody ScriptParseRequest request) {
        try {
            // 参数验证
            if (request.getScript() == null || request.getScript().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("SQL脚本不能为空"));
            }
            
            if (request.getDbType() == null || request.getDbType().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("数据库类型不能为空"));
            }
            
            logger.info("接收到脚本解析请求，数据库类型: {}, 脚本长度: {}", 
                       request.getDbType(), request.getScript().length());
            
            // 解析脚本
            java.util.List<com.dbsync.dbsync.util.SqlScriptParser.SqlStatement> statements = 
                com.dbsync.dbsync.util.SqlScriptParser.parseScript(
                    request.getScript(), request.getDbType()
                );
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statementCount", statements.size());
            response.put("statements", statements);
            response.put("timestamp", System.currentTimeMillis());
            
            logger.info("脚本解析完成，语句数量: {}", statements.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("脚本解析异常: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(createErrorResponse("脚本解析失败: " + e.getMessage()));
        }
    }
    
    /**
     * 创建错误响应
     */
    private Map<String, Object> createErrorResponse(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }
    
    /**
     * 脚本执行请求DTO
     */
    public static class ScriptExecutionRequest {
        private String script;
        private String schema;
        private boolean executeInTransaction = true; // 默认在事务中执行
        
        // Getters and Setters
        public String getScript() { return script; }
        public void setScript(String script) { this.script = script; }
        
        public String getSchema() { return schema; }
        public void setSchema(String schema) { this.schema = schema; }
        
        public boolean isExecuteInTransaction() { return executeInTransaction; }
        public void setExecuteInTransaction(boolean executeInTransaction) { 
            this.executeInTransaction = executeInTransaction; 
        }
    }
    
    /**
     * 脚本解析请求DTO
     */
    public static class ScriptParseRequest {
        private String script;
        private String dbType;
        
        // Getters and Setters
        public String getScript() { return script; }
        public void setScript(String script) { this.script = script; }
        
        public String getDbType() { return dbType; }
        public void setDbType(String dbType) { this.dbType = dbType; }
    }
}