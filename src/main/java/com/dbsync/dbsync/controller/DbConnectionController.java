package com.dbsync.dbsync.controller;

import com.dbsync.dbsync.entity.ColumnInfo;
import com.dbsync.dbsync.entity.QueryHistory;
import com.dbsync.dbsync.entity.QueryResult;
import com.dbsync.dbsync.entity.TableInfo;
import com.dbsync.dbsync.dto.TablePageRequest;
import com.dbsync.dbsync.dto.TablePageResponse;
import com.dbsync.dbsync.model.DbConnection;
import com.dbsync.dbsync.service.DbConnectionService;
import com.dbsync.dbsync.service.QueryHistoryService;
import com.dbsync.dbsync.service.QueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据库连接管理控制器 - 简化版本用于测试
 */
@RestController
@RequestMapping("/api/database")
@CrossOrigin(origins = "*")
public class DbConnectionController {

    private static final Logger logger = LoggerFactory.getLogger(DbConnectionController.class);

    @Autowired
    private DbConnectionService dbConnectionService;

    @Autowired
    private QueryService queryService;

    @Autowired
    private QueryHistoryService queryHistoryService;

    /**
     * 获取所有数据库连接
     */
    @GetMapping("/connections")
    public List<DbConnection> getAllConnections() {
        return dbConnectionService.getAllConnections();
    }

    /**
     * 创建数据库连接
     */
    @PostMapping("/connections")
    public Object createConnection(@RequestBody DbConnection connection) {
        try {
            return dbConnectionService.createConnection(connection);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }

    /**
     * 更新数据库连接
     */
    @PutMapping("/connections/{id}")
    public Object updateConnection(@PathVariable Long id, @RequestBody DbConnection connection) {
        try {
            return dbConnectionService.updateConnection(id, connection);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }

    /**
     * 删除数据库连接
     */
    @DeleteMapping("/connections/{id}")
    public Object deleteConnection(@PathVariable Long id) {
        try {
            boolean success = dbConnectionService.deleteConnection(id);
            if (success) {
                Map<String, String> result = new HashMap<>();
                result.put("message", "删除成功");
                return result;
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "删除失败");
                return error;
            }
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }

    /**
     * 测试数据库连接
     */
    @PostMapping("/test-connection")
    public Object testConnection(@RequestBody DbConnection connection) {
        try {
            DbConnectionService.DbTestResult result = dbConnectionService.testConnection(connection);
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("message", result.getMessage());
            response.put("connectionTime", result.getConnectionTime());
            return response;
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "测试失败: " + e.getMessage());
            response.put("connectionTime", 0);
            return response;
        }
    }

    /**
     * 检查数据库连接健康状态
     */
    @GetMapping("/connections/{id}/health")
    public ResponseEntity<?> checkConnectionHealth(@PathVariable Long id) {
        try {
            boolean isHealthy = dbConnectionService.checkConnectionHealth(id);
            Map<String, Object> response = new HashMap<>();
            response.put("healthy", isHealthy);
            response.put("message", isHealthy ? "连接正常" : "连接异常");
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("连接健康检查失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("healthy", false);
            response.put("message", "健康检查失败: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * 根据ID获取数据库连接
     */
    @GetMapping("/connections/{id}")
    public Object getConnectionById(@PathVariable Long id) {
        try {
            DbConnection connection = dbConnectionService.getConnectionById(id);
            if (connection != null) {
                return connection;
            } else {
                Map<String, String> error = new HashMap<>();
                error.put("error", "数据库连接不存在");
                return error;
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "查询失败");
            return error;
        }
    }

    /**
     * 分页获取数据库连接的表列表
     */
    @GetMapping("/connections/{id}/tables/page")
    public ResponseEntity<?> getTablesWithPagination(@PathVariable Long id,
                                                     @RequestParam(defaultValue = "1") Integer page,
                                                     @RequestParam(defaultValue = "50") Integer size,
                                                     @RequestParam(required = false) String search,
                                                     @RequestParam(defaultValue = "name") String sortBy,
                                                     @RequestParam(defaultValue = "asc") String sortOrder,
                                                     @RequestParam(required = false) String schema) {
        try {
            TablePageRequest request = new TablePageRequest();
            request.setPage(page);
            request.setSize(size);
            request.setSearch(search);
            request.setSortBy(sortBy);
            request.setSortOrder(sortOrder);
            request.setSchema(schema);

            TablePageResponse<TableInfo> response = dbConnectionService.getTablesWithPagination(id, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取表列表失败", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "获取表列表失败");
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 获取数据库连接的表列表（保持向后兼容）
     */
    @GetMapping("/connections/{id}/tables")
    public Object getTables(@PathVariable Long id, @RequestParam(required = false) String schema) {
        try {
            List<String> tables = dbConnectionService.getTables(id, schema);
            return tables;
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "获取表列表失败");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 执行SQL查询
     */
    @PostMapping("/connections/{id}/query")
    public ResponseEntity<?> executeQuery(@PathVariable Long id, @RequestBody QueryRequest request) {
        long startTime = System.currentTimeMillis();
        String currentUser = getCurrentUser();

        try {
            // 验证SQL
            queryService.validateSql(request.getSql());

            // 执行查询
            QueryResult result = queryService.executeQuery(id, request.getSql(), request.getSchema());

            // 保存查询历史
            try {
                DbConnection connection = dbConnectionService.getConnectionById(id);
                if (connection != null) {
                    QueryHistory history = new QueryHistory();
                    history.setSql(request.getSql());
                    history.setSourceConnectionId(id);
                    history.setSourceConnectionName(connection.getName());
                    // 明确设置NULL值字段
                    history.setTargetConnectionId(null);
                    history.setTargetConnectionName(null);
                    history.setTargetTableName(null);
                    history.setTargetSchemaName(null);
                    history.setExecutionTime(result.getExecutionTime().intValue());
                    history.setStatus("SUCCESS");
                    history.setResultRows(result.getTotalRows());
                    history.setErrorMessage(null);
                    history.setCreatedBy(currentUser);

                    queryHistoryService.saveQueryHistory(history);
                }
            } catch (Exception e) {
                logger.warn("保存查询历史失败: {}", e.getMessage());
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("查询执行失败: {}", e.getMessage(), e);

            // 保存错误历史
            try {
                DbConnection connection = dbConnectionService.getConnectionById(id);
                if (connection != null) {
                    QueryHistory history = new QueryHistory();
                    history.setSql(request.getSql());
                    history.setSourceConnectionId(id);
                    history.setSourceConnectionName(connection.getName());
                    // 明确设置NULL值字段
                    history.setTargetConnectionId(null);
                    history.setTargetConnectionName(null);
                    history.setTargetTableName(null);
                    history.setTargetSchemaName(null);
                    history.setExecutionTime((int) executionTime);
                    history.setStatus("ERROR");
                    history.setErrorMessage(e.getMessage());
                    history.setResultRows(0);
                    history.setCreatedBy(currentUser);

                    queryHistoryService.saveQueryHistory(history);
                }
            } catch (Exception ex) {
                logger.warn("保存错误查询历史失败: {}", ex.getMessage());
            }

            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 获取表结构信息
     */
    @GetMapping("/connections/{id}/tables/{tableName}/columns")
    public ResponseEntity<?> getTableColumns(@PathVariable Long id,
                                           @PathVariable String tableName,
                                           @RequestParam(required = false) String schema) {
        try {
            List<ColumnInfo> columns = dbConnectionService.getTableColumns(id, tableName, schema);
            return ResponseEntity.ok(columns);
        } catch (Exception e) {
            logger.error("获取表结构失败: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "获取表结构失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 获取数据库Schema列表
     */
    @GetMapping("/connections/{id}/schemas")
    public ResponseEntity<?> getSchemas(@PathVariable Long id) {
        try {
            List<String> schemas = dbConnectionService.getSchemas(id);
            return ResponseEntity.ok(schemas);
        } catch (Exception e) {
            logger.error("获取Schema列表失败: {}", e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "获取Schema列表失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }

    /**
     * 获取当前用户
     */
    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "anonymous";
    }

    /**
     * 查询请求内部类
     */
    public static class QueryRequest {
        private String sql;
        private String schema;

        public String getSql() {
            return sql;
        }

        public void setSql(String sql) {
            this.sql = sql;
        }

        public String getSchema() {
            return schema;
        }

        public void setSchema(String schema) {
            this.schema = schema;
        }
    }
}