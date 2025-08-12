package com.dbsync.dbsync.service;

import com.dbsync.cache.CacheService;
import com.dbsync.cache.CacheType;
import com.dbsync.dbsync.model.DbConnection;
import com.dbsync.dbsync.entity.QueryResult;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Enhanced query service with backend caching support
 */
@Service
public class EnhancedQueryService {
    
    private static final Logger logger = LoggerFactory.getLogger(EnhancedQueryService.class);
    
    @Autowired
    private DbConnectionService dbConnectionService;
    
    @Autowired
    private CacheService cacheService;
    
    @Autowired
    private DatabaseMetadataCacheService legacyCacheService;
    
    // Cache TTL settings (in minutes)
    private static final int QUERY_RESULT_TTL = 30;
    private static final int TABLE_LIST_TTL = 60;
    private static final int TABLE_SCHEMA_TTL = 120;
    
    /**
     * Execute SQL query with caching support
     */
    public QueryResult executeQuery(Long connectionId, String sql, String schema, boolean useCache) {
        return executeQuery(connectionId, sql, schema, useCache, null, null);
    }
    
    /**
     * Execute SQL query with pagination support
     */
    public QueryResult executeQuery(Long connectionId, String sql, String schema, boolean useCache, Integer page, Integer pageSize) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 设置默认分页参数
            boolean isPaginated = (page != null && pageSize != null);
            int currentPage = isPaginated ? Math.max(1, page) : 1;
            int size = isPaginated ? Math.max(1, Math.min(pageSize, 1000)) : 10000; // 限制最大页面大小
            
            // Generate cache key for this query (include pagination info)
            String cacheKey = isPaginated ? 
                cacheService.generateQueryCacheKey(connectionId.toString(), sql + "_page_" + currentPage + "_size_" + size, schema) :
                cacheService.generateQueryCacheKey(connectionId.toString(), sql, schema);
            
            // Try to get from cache first if enabled
            if (useCache) {
                Optional<QueryResult> cachedResult = cacheService.get(cacheKey, QueryResult.class);
                if (cachedResult.isPresent()) {
                    QueryResult result = cachedResult.get();
                    result.setFromCache(true);
                    logger.debug("Query result retrieved from cache: {}", cacheKey);
                    return result;
                }
            }
            
            // Execute query if not in cache
            DbConnection connection = dbConnectionService.getConnectionById(connectionId);
            if (connection == null) {
                throw new RuntimeException("数据库连接不存在: " + connectionId);
            }
            
            if (!connection.getEnabled()) {
                throw new RuntimeException("数据库连接已禁用: " + connection.getName());
            }
            
            String jdbcUrl = buildJdbcUrl(connection, schema);
            
            try (Connection conn = DriverManager.getConnection(jdbcUrl, connection.getUsername(), connection.getPassword())) {
                
                QueryResult result;
                boolean hasResultSet = false;
                long executionTime = System.currentTimeMillis() - startTime;
                
                if (isPaginated && isSelectQuery(sql)) {
                    // 分页查询处理
                    result = executePaginatedQuery(conn, sql, currentPage, size, executionTime);
                    hasResultSet = true; // 分页查询总是返回结果集
                } else {
                    // 非分页查询处理
                    try (Statement stmt = conn.createStatement()) {
                        stmt.setQueryTimeout(30);
                        stmt.setMaxRows(10000);
                        
                        hasResultSet = stmt.execute(sql);
                        executionTime = System.currentTimeMillis() - startTime;
                        
                        if (hasResultSet) {
                            try (ResultSet rs = stmt.getResultSet()) {
                                result = processResultSet(rs, executionTime);
                            }
                        } else {
                            int updateCount = stmt.getUpdateCount();
                            result = createUpdateResult(updateCount, executionTime);
                        }
                    }
                }
                
                result.setFromCache(false);
                
                // Cache the result if it's a SELECT query and caching is enabled
                if (useCache && hasResultSet && isSelectQuery(sql)) {
                    cacheService.put(cacheKey, result, CacheType.QUERY_RESULT, 
                                   connectionId.toString(), QUERY_RESULT_TTL);
                    logger.debug("Query result cached: {}", cacheKey);
                }
                
                return result;
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
     * Get table list with caching
     */
    public List<String> getTables(Long connectionId, String schema, boolean useCache) {
        try {
            String cacheKey = cacheService.generateTableListCacheKey(connectionId.toString(), schema);
            
            // Try cache first
            if (useCache) {
                Optional<List<String>> cachedTables = cacheService.get(cacheKey, new TypeReference<List<String>>() {});
                if (cachedTables.isPresent()) {
                    logger.debug("Table list retrieved from cache: {}", cacheKey);
                    return cachedTables.get();
                }
            }
            
            // Fallback to legacy cache service for now
            List<String> tables = legacyCacheService.getTables(connectionId, schema);
            
            // Cache the result
            if (useCache) {
                cacheService.put(cacheKey, tables, CacheType.TABLE_LIST, 
                               connectionId.toString(), TABLE_LIST_TTL);
                logger.debug("Table list cached: {}", cacheKey);
            }
            
            return tables;
            
        } catch (Exception e) {
            logger.error("获取表列表失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取表列表失败: " + e.getMessage());
        }
    }
    
    /**
     * Get table columns with caching
     */
    public List<ColumnInfo> getTableColumns(Long connectionId, String tableName, String schema, boolean useCache) {
        try {
            String cacheKey = cacheService.generateTableSchemaCacheKey(connectionId.toString(), tableName, schema);
            
            // Try cache first
            if (useCache) {
                Optional<List<ColumnInfo>> cachedColumns = cacheService.get(cacheKey, new TypeReference<List<ColumnInfo>>() {});
                if (cachedColumns.isPresent()) {
                    logger.debug("Table schema retrieved from cache: {}", cacheKey);
                    return cachedColumns.get();
                }
            }
            
            // Execute query to get columns
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
                
                // Cache the result
                if (useCache) {
                    cacheService.put(cacheKey, columns, CacheType.TABLE_SCHEMA, 
                                   connectionId.toString(), TABLE_SCHEMA_TTL);
                    logger.debug("Table schema cached: {}", cacheKey);
                }
                
                return columns;
            }
            
        } catch (SQLException e) {
            logger.error("获取表结构失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取表结构失败: " + e.getMessage());
        }
    }
    
    /**
     * Clear cache for specific connection
     */
    public void clearConnectionCache(Long connectionId) {
        cacheService.evictByDataSource(connectionId.toString());
        logger.info("Cleared cache for connection: {}", connectionId);
    }
    
    /**
     * Preload cache for connection (warmup)
     */
    public void warmupCache(Long connectionId, String schema) {
        try {
            // Preload table list
            getTables(connectionId, schema, true);
            
            // Preload schema for first few tables
            List<String> tables = getTables(connectionId, schema, true);
            int maxTables = Math.min(tables.size(), 5); // Limit to first 5 tables
            
            for (int i = 0; i < maxTables; i++) {
                try {
                    getTableColumns(connectionId, tables.get(i), schema, true);
                } catch (Exception e) {
                    logger.warn("Failed to preload schema for table {}: {}", tables.get(i), e.getMessage());
                }
            }
            
            logger.info("Cache warmup completed for connection {} schema {}", connectionId, schema);
            
        } catch (Exception e) {
            logger.error("Cache warmup failed for connection {}: {}", connectionId, e.getMessage());
        }
    }
    
    /**
     * Execute paginated query
     */
    private QueryResult executePaginatedQuery(Connection conn, String sql, int page, int pageSize, long executionTime) throws SQLException {
        // 首先获取总记录数
        int totalCount = getTotalCount(conn, sql);
        int totalPages = (int) Math.ceil((double) totalCount / pageSize);
        boolean hasMore = page < totalPages;
        
        // 构建分页SQL
        String paginatedSql = buildPaginatedSql(sql, page, pageSize, conn.getMetaData().getDatabaseProductName());
        
        try (PreparedStatement stmt = conn.prepareStatement(paginatedSql)) {
            stmt.setQueryTimeout(30);
            
            try (ResultSet rs = stmt.executeQuery()) {
                QueryResult result = processResultSet(rs, executionTime);
                
                // 设置分页信息
                result.setCurrentPage(page);
                result.setPageSize(pageSize);
                result.setTotalPages(totalPages);
                result.setHasMore(hasMore);
                result.setTotalRows(totalCount); // 设置为总记录数而不是当前页记录数
                
                return result;
            }
        }
    }
    
    /**
     * Get total count for pagination
     */
    private int getTotalCount(Connection conn, String sql) throws SQLException {
        // 构建COUNT查询
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") AS count_query";
        
        try (PreparedStatement stmt = conn.prepareStatement(countSql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }
    
    /**
     * Build paginated SQL based on database type
     */
    private String buildPaginatedSql(String sql, int page, int pageSize, String databaseProductName) {
        int offset = (page - 1) * pageSize;
        
        String dbType = databaseProductName.toLowerCase();
        
        if (dbType.contains("mysql")) {
            return sql + " LIMIT " + pageSize + " OFFSET " + offset;
        } else if (dbType.contains("postgresql") || dbType.contains("vastbase")) {
            return sql + " LIMIT " + pageSize + " OFFSET " + offset;
        } else if (dbType.contains("oracle")) {
            // Oracle 12c+ syntax
            return sql + " OFFSET " + offset + " ROWS FETCH NEXT " + pageSize + " ROWS ONLY";
        } else if (dbType.contains("sql server")) {
            return sql + " OFFSET " + offset + " ROWS FETCH NEXT " + pageSize + " ROWS ONLY";
        } else {
            // 默认使用LIMIT OFFSET语法
            return sql + " LIMIT " + pageSize + " OFFSET " + offset;
        }
    }
    
    // Private helper methods (same as original QueryService)
    private QueryResult processResultSet(ResultSet rs, long executionTime) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        List<String> columns = new ArrayList<>();
        for (int i = 1; i <= columnCount; i++) {
            columns.add(metaData.getColumnLabel(i));
        }
        
        List<List<Object>> rows = new ArrayList<>();
        int rowCount = 0;
        
        while (rs.next() && rowCount < 10000) {
            List<Object> row = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                Object value = rs.getObject(i);
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
    
    private String buildJdbcUrl(DbConnection connection, String schema) {
        String baseUrl = dbConnectionService.buildJdbcUrl(connection);
        
        if (schema != null && !schema.trim().isEmpty()) {
            String dbType = connection.getDbType().toLowerCase();
            switch (dbType) {
                case "postgresql":
                case "vastbase":
                    baseUrl += "?currentSchema=" + schema;
                    break;
                case "mysql":
                case "oracle":
                case "sqlserver":
                default:
                    break;
            }
        }
        
        return baseUrl;
    }
    
    private boolean isSelectQuery(String sql) {
        return sql.trim().toLowerCase().startsWith("select");
    }
    
    /**
     * Column info class
     */
    public static class ColumnInfo {
        private String columnName;
        private String dataType;
        private Integer columnSize;
        private Boolean nullable;
        private String defaultValue;
        private String remarks;
        
        // Getters and Setters
        public String getColumnName() { return columnName; }
        public void setColumnName(String columnName) { this.columnName = columnName; }
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        public Integer getColumnSize() { return columnSize; }
        public void setColumnSize(Integer columnSize) { this.columnSize = columnSize; }
        public Boolean getNullable() { return nullable; }
        public void setNullable(Boolean nullable) { this.nullable = nullable; }
        public String getDefaultValue() { return defaultValue; }
        public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
    }
}