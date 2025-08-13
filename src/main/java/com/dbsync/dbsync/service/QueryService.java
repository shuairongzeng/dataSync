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
    
    
    /**
     * 执行SQL查询
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
        
        // 检查危险操作
        String[] dangerousKeywords = {
            "DROP TABLE", "DROP DATABASE", "DROP SCHEMA", "DROP INDEX",
            "TRUNCATE", "DELETE FROM", "ALTER TABLE", "CREATE TABLE",
            "CREATE DATABASE", "CREATE SCHEMA"
        };
        
        for (String keyword : dangerousKeywords) {
            if (upperSql.contains(keyword)) {
                logger.warn("检测到潜在危险SQL操作: {}", keyword);
                // 这里可以根据需要决定是否允许执行
                // throw new RuntimeException("不允许执行危险操作: " + keyword);
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
