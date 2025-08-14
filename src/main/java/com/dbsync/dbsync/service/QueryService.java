package com.dbsync.dbsync.service;

import com.dbsync.dbsync.model.DbConnection;
import com.dbsync.dbsync.entity.QueryResult;
import com.dbsync.dbsync.entity.EnhancedQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.dbsync.dbsync.util.SqlScriptParser;

/**
 * 查询执行服务类
 */
@Service
public class QueryService {
    
    private static final Logger logger = LoggerFactory.getLogger(QueryService.class);
    
    @Autowired
    private DbConnectionService dbConnectionService;

    @Autowired
    private DatabaseMetadataCacheService cacheService;
    
    @Autowired
    private SqlScriptService sqlScriptService;
    
    
    /**
     * 执行SQL查询
     * 自动检测多语句脚本并使用适当的执行器
     */
    public QueryResult executeQuery(Long connectionId, String sql, String schema) {
        long startTime = System.currentTimeMillis();
        
        try {
            DbConnection connection = dbConnectionService.getConnectionById(connectionId);
            if (connection == null) {
                throw new RuntimeException("数据库连接不存在: " + connectionId);
            }
            
            if (!connection.getEnabled()) {
                throw new RuntimeException("数据库连接已禁用: " + connection.getName());
            }
            
            // 检测是否为多语句脚本
            if (isMultiStatementScript(sql, connection.getDbType())) {
                logger.info("检测到多语句脚本，使用脚本执行器处理");
                return executeAsScript(connectionId, sql, schema, startTime);
            }
            
            // 单语句执行原有逻辑
            
            String jdbcUrl = buildJdbcUrl(connection, schema);
            
            try (Connection conn = DriverManager.getConnection(jdbcUrl, connection.getUsername(), connection.getPassword());
                 Statement stmt = conn.createStatement()) {
                
                // 设置查询超时时间（30秒）
                stmt.setQueryTimeout(30);
                
                // 限制结果集大小，防止内存溢出
                stmt.setMaxRows(10000);
                
                boolean hasResultSet = stmt.execute(sql);
                long executionTime = System.currentTimeMillis() - startTime;
                
                if (hasResultSet) {
                    // 处理SELECT查询结果
                    try (ResultSet rs = stmt.getResultSet()) {
                        return processResultSet(rs, executionTime);
                    }
                } else {
                    // 处理INSERT/UPDATE/DELETE等操作
                    int updateCount = stmt.getUpdateCount();
                    return createUpdateResult(updateCount, executionTime);
                }
                
            }
            
        } catch (SQLException e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("SQL执行失败: {}", e.getMessage(), e);
            throw new RuntimeException("SQL执行失败: " + e.getMessage());
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("查询执行异常: {}", e.getMessage(), e);
            throw new RuntimeException("查询执行异常: " + e.getMessage());
        }
    }
    
    /**
     * 处理ResultSet，转换为QueryResult
     */
    private QueryResult processResultSet(ResultSet rs, long executionTime) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        // 获取列名
        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            columns.add(metaData.getColumnLabel(i));
        }
        
        // 获取数据行
        List<List<Object>> rows = new ArrayList<>();
        int rowCount = 0;
        
        while (rs.next() && rowCount < 10000) { // 限制最大行数
            List<Object> row = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                Object value = rs.getObject(i);
                // 处理特殊类型
                if (value instanceof Clob) {
                    Clob clob = (Clob) value;
                    value = clob.getSubString(1, (int) clob.length());
                } else if (value instanceof Blob) {
                    value = "[BLOB数据]";
                }
                row.add(value);
            }
            rows.add(row);
            rowCount++;
        }
        
        return new QueryResult(columns, rows, rowCount, executionTime);
    }
    
    /**
     * 创建更新操作结果
     */
    private QueryResult createUpdateResult(int updateCount, long executionTime) {
        List<String> columns = new ArrayList<>();
        columns.add("affected_rows");
        
        List<List<Object>> rows = new ArrayList<>();
        List<Object> row = new ArrayList<>();
        row.add(updateCount);
        rows.add(row);
        
        String message = String.format("操作完成，影响 %d 行", updateCount);
        
        return new QueryResult(columns, rows, 1, executionTime, message);
    }
    
    /**
     * 构建JDBC URL
     */
    private String buildJdbcUrl(DbConnection connection, String schema) {
        String baseUrl = dbConnectionService.buildJdbcUrl(connection);
        
        // 如果指定了schema，添加到URL中
        if (schema != null && !schema.trim().isEmpty()) {
            String dbType = connection.getDbType().toLowerCase();
            switch (dbType) {
                case "postgresql":
                case "vastbase":
                    baseUrl += "?currentSchema=" + schema;
                    break;
                case "mysql":
                    // MySQL使用database概念，schema通常等同于database
                    break;
                case "oracle":
                    // Oracle中schema通常通过用户名指定
                    break;
                case "sqlserver":
                    // SQL Server可以在查询中指定schema
                    break;
                default:
                    break;
            }
        }
        
        return baseUrl;
    }
    
    /**
     * 验证SQL语句安全性（基础检查）
     */
    public void validateSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new RuntimeException("SQL语句不能为空");
        }
        
        String upperSql = sql.toUpperCase().trim();
        
        // 检查危险操作（仅警告，不禁止执行）
        String[] dangerousKeywords = {
            "DROP DATABASE", "DROP SCHEMA", "TRUNCATE"
        };
        
        for (String keyword : dangerousKeywords) {
            if (upperSql.contains(keyword)) {
                logger.warn("检测到潜在高风险SQL操作: {}", keyword);
                // 仅警告，允许执行常规DDL操作
            }
        }
    }
    
    /**
     * 获取数据库表列表
     */
    public List<String> getTables(Long connectionId, String schema) {
        try {
            DbConnection connection = dbConnectionService.getConnectionById(connectionId);
            if (connection == null) {
                throw new RuntimeException("数据库连接不存在: " + connectionId);
            }
            
            return cacheService.getTables(connectionId, schema);
            
        } catch (Exception e) {
            logger.error("获取表列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取表列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取表结构信息
     */
    public List<ColumnInfo> getTableColumns(Long connectionId, String tableName, String schema) {
        try {
            DbConnection connection = dbConnectionService.getConnectionById(connectionId);
            if (connection == null) {
                throw new RuntimeException("数据库连接不存在: " + connectionId);
            }
            
            String jdbcUrl = buildJdbcUrl(connection, schema);
            
            try (Connection conn = DriverManager.getConnection(jdbcUrl, connection.getUsername(), connection.getPassword())) {
                DatabaseMetaData metaData = conn.getMetaData();
                
                List<ColumnInfo> columns = new ArrayList<>();
                
                try (ResultSet rs = metaData.getColumns(null, schema, tableName, null)) {
                    while (rs.next()) {
                        ColumnInfo columnInfo = new ColumnInfo();
                        columnInfo.setColumnName(rs.getString("COLUMN_NAME"));
                        columnInfo.setDataType(rs.getString("TYPE_NAME"));
                        columnInfo.setColumnSize(rs.getInt("COLUMN_SIZE"));
                        columnInfo.setNullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                        columnInfo.setDefaultValue(rs.getString("COLUMN_DEF"));
                        columnInfo.setRemarks(rs.getString("REMARKS"));
                        columns.add(columnInfo);
                    }
                }
                
                return columns;
            }
            
        } catch (SQLException e) {
            logger.error("获取表结构失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取表结构失败: " + e.getMessage());
        }
    }
    
    /**
     * 检测是否为多语句脚本
     */
    private boolean isMultiStatementScript(String sql, String dbType) {
        if (sql == null || sql.trim().isEmpty()) {
            return false;
        }
        
        String cleanedSql = sql.trim();
        
        // 检测多个分号分隔的语句
        String[] parts = cleanedSql.split(";");
        if (parts.length > 1) {
            // 过滤空语句
            int nonEmptyParts = 0;
            for (String part : parts) {
                if (part.trim().length() > 0) {
                    nonEmptyParts++;
                }
            }
            if (nonEmptyParts > 1) {
                return true;
            }
        }
        
        // Oracle特定检测：包含/分隔符或复杂块语句
        if ("oracle".equalsIgnoreCase(dbType)) {
            if (cleanedSql.contains("/\n") || cleanedSql.endsWith("/") ||
                cleanedSql.toUpperCase().contains("CREATE OR REPLACE TRIGGER") ||
                cleanedSql.toUpperCase().contains("BEGIN") && cleanedSql.toUpperCase().contains("END")) {
                return true;
            }
        }
        
        // 检测多行SQL注释
        if (cleanedSql.contains("--") && cleanedSql.split("\n").length > 3) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 使用脚本执行器执行多语句脚本
     */
    private QueryResult executeAsScript(Long connectionId, String sql, String schema, long startTime) {
        try {
            SqlScriptService.ScriptExecutionResult scriptResult = sqlScriptService.executeScript(
                connectionId, sql, schema, true // 默认在事务中执行
            );
            
            // 将脚本执行结果转换为QueryResult格式
            List<String> columns = new ArrayList<>();
            columns.add("语句编号");
            columns.add("类型");
            columns.add("状态");
            columns.add("消息");
            columns.add("执行时间(ms)");
            columns.add("影响行数");
            
            List<List<Object>> rows = new ArrayList<>();
            
            for (int i = 0; i < scriptResult.getStatementResults().size(); i++) {
                SqlScriptService.StatementResult stmtResult = scriptResult.getStatementResults().get(i);
                List<Object> row = new ArrayList<>();
                
                row.add(i + 1);
                row.add(stmtResult.getStatement().getType() + "/" + stmtResult.getStatement().getCategory());
                row.add(stmtResult.isSuccess() ? "成功" : "失败");
                row.add(stmtResult.getMessage());
                row.add(stmtResult.getExecutionTime());
                row.add(stmtResult.getAffectedRows());
                
                rows.add(row);
            }
            
            String message = String.format("脚本执行完成：总计 %d 条语句，成功 %d 条，失败 %d 条", 
                                          scriptResult.getTotalCount(), 
                                          scriptResult.getSuccessCount(), 
                                          scriptResult.getFailedCount());
            
            return new QueryResult(columns, rows, rows.size(), scriptResult.getTotalTime(), message);
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("脚本执行失败: {}", e.getMessage(), e);
            throw new RuntimeException("脚本执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 列信息内部类
     */
    public static class ColumnInfo {
        private String columnName;
        private String dataType;
        private Integer columnSize;
        private Boolean nullable;
        private String defaultValue;
        private String remarks;
        
        // Getters and Setters
        public String getColumnName() {
            return columnName;
        }
        
        public void setColumnName(String columnName) {
            this.columnName = columnName;
        }
        
        public String getDataType() {
            return dataType;
        }
        
        public void setDataType(String dataType) {
            this.dataType = dataType;
        }
        
        public Integer getColumnSize() {
            return columnSize;
        }
        
        public void setColumnSize(Integer columnSize) {
            this.columnSize = columnSize;
        }
        
        public Boolean getNullable() {
            return nullable;
        }
        
        public void setNullable(Boolean nullable) {
            this.nullable = nullable;
        }
        
        public String getDefaultValue() {
            return defaultValue;
        }
        
        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }
        
        public String getRemarks() {
            return remarks;
        }
        
        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }
    }
    
}
