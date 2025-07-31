package com.dbsync.dbsync.util;

import com.dbsync.dbsync.entity.ColumnInfo;
import com.dbsync.dbsync.entity.TableInfo;
import com.dbsync.dbsync.dto.TablePageRequest;
import com.dbsync.dbsync.dto.TablePageResponse;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据库元数据工具类
 * 提供跨数据库的元数据获取功能
 */
public class DatabaseMetadataUtil {

    /**
     * 分页获取表信息列表
     */
    public static TablePageResponse<TableInfo> getTablesWithPagination(Connection connection, String dbType,
                                                                       String schemaName, String databaseName,
                                                                       TablePageRequest request) throws SQLException {
        List<TableInfo> allTables = getAllTableInfo(connection, dbType, schemaName, databaseName);

        // 应用搜索过滤
        List<TableInfo> filteredTables = filterTables(allTables, request.getSearch());

        // 应用排序
        sortTables(filteredTables, request.getSortBy(), request.getSortOrder());

        // 应用分页
        long total = filteredTables.size();
        int offset = request.getOffset();
        int limit = request.getLimit();

        List<TableInfo> pagedTables = new ArrayList<>();
        if (offset < filteredTables.size()) {
            int endIndex = Math.min(offset + limit, filteredTables.size());
            pagedTables = filteredTables.subList(offset, endIndex);
        }

        return TablePageResponse.of(pagedTables, request.getPage(), request.getSize(), total, request.getSearch());
    }

    /**
     * 获取表列表（保持向后兼容）
     */
    public static List<String> getTables(Connection connection, String dbType, String schemaName, String databaseName) throws SQLException {
        List<String> tables = new ArrayList<>();
        
        switch (dbType.toLowerCase()) {
            case "mysql":
                return getMySQLTables(connection, schemaName, databaseName);
            case "postgresql":
            case "vastbase":
                return getPostgreSQLTables(connection, schemaName);
            case "oracle":
            case "dameng":
                return getOracleTables(connection, schemaName);
            case "sqlserver":
                return getSQLServerTables(connection, schemaName);
            default:
                return getGenericTables(connection, schemaName, databaseName);
        }
    }

    /**
     * 获取表的列信息
     */
    public static List<ColumnInfo> getTableColumns(Connection connection, String dbType, String tableName, String schemaName, String databaseName) throws SQLException {
        switch (dbType.toLowerCase()) {
            case "mysql":
                return getMySQLColumns(connection, tableName, schemaName, databaseName);
            case "postgresql":
            case "vastbase":
                return getPostgreSQLColumns(connection, tableName, schemaName);
            case "oracle":
            case "dameng":
                return getOracleColumns(connection, tableName, schemaName);
            case "sqlserver":
                return getSQLServerColumns(connection, tableName, schemaName);
            default:
                return getGenericColumns(connection, tableName, schemaName, databaseName);
        }
    }

    // MySQL特定实现
    private static List<String> getMySQLTables(Connection connection, String schemaName, String databaseName) throws SQLException {
        List<String> tables = new ArrayList<>();
        String schema = (schemaName != null && !schemaName.trim().isEmpty()) ? schemaName : databaseName;

        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_type = 'BASE TABLE' ORDER BY table_name";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            // 设置查询超时时间（45秒）
            stmt.setQueryTimeout(45);
            stmt.setString(1, schema);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString("table_name"));
                }
            }
        }
        return tables;
    }

    private static List<ColumnInfo> getMySQLColumns(Connection connection, String tableName, String schemaName, String databaseName) throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        String schema = (schemaName != null && !schemaName.trim().isEmpty()) ? schemaName : databaseName;
        
        String sql = "SELECT column_name, data_type, column_type, is_nullable, column_default, column_comment, " +
                    "ordinal_position, character_maximum_length, numeric_precision, numeric_scale, extra " +
                    "FROM information_schema.columns WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, schema);
            stmt.setString(2, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ColumnInfo columnInfo = new ColumnInfo();
                    columnInfo.setColumnName(rs.getString("column_name"));
                    columnInfo.setDataType(rs.getString("column_type")); // 使用column_type获取完整类型信息
                    columnInfo.setTypeName(rs.getString("data_type"));
                    columnInfo.setNullable("YES".equalsIgnoreCase(rs.getString("is_nullable")));
                    columnInfo.setDefaultValue(rs.getString("column_default"));
                    columnInfo.setRemarks(rs.getString("column_comment"));
                    columnInfo.setOrdinalPosition(rs.getInt("ordinal_position"));
                    
                    // 设置长度和精度
                    if (rs.getObject("character_maximum_length") != null) {
                        columnInfo.setColumnSize(rs.getInt("character_maximum_length"));
                    } else if (rs.getObject("numeric_precision") != null) {
                        columnInfo.setColumnSize(rs.getInt("numeric_precision"));
                    }
                    
                    if (rs.getObject("numeric_scale") != null) {
                        columnInfo.setDecimalDigits(rs.getInt("numeric_scale"));
                    }
                    
                    // 检查是否为自增列
                    String extra = rs.getString("extra");
                    columnInfo.setIsAutoIncrement(extra != null && extra.toLowerCase().contains("auto_increment"));
                    
                    columns.add(columnInfo);
                }
            }
        }
        
        // 获取主键信息
        Set<String> primaryKeys = getPrimaryKeys(connection, schema, tableName);
        for (ColumnInfo column : columns) {
            column.setIsPrimaryKey(primaryKeys.contains(column.getColumnName()));
        }
        
        return columns;
    }

    // PostgreSQL特定实现
    private static List<String> getPostgreSQLTables(Connection connection, String schemaName) throws SQLException {
        List<String> tables = new ArrayList<>();
        String schema = (schemaName != null && !schemaName.trim().isEmpty()) ? schemaName : "public";

        String sql = "SELECT tablename FROM pg_tables WHERE schemaname = ? ORDER BY tablename";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            // 设置查询超时时间（45秒）
            stmt.setQueryTimeout(45);
            stmt.setString(1, schema);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString("tablename"));
                }
            }
        }
        return tables;
    }

    private static List<ColumnInfo> getPostgreSQLColumns(Connection connection, String tableName, String schemaName) throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        String schema = (schemaName != null && !schemaName.trim().isEmpty()) ? schemaName : "public";
        
        String sql = "SELECT column_name, udt_name, data_type, is_nullable, column_default, " +
                    "ordinal_position, character_maximum_length, numeric_precision, numeric_scale " +
                    "FROM information_schema.columns WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, schema);
            stmt.setString(2, tableName);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ColumnInfo columnInfo = new ColumnInfo();
                    columnInfo.setColumnName(rs.getString("column_name"));
                    columnInfo.setDataType(rs.getString("udt_name")); // PostgreSQL使用udt_name更准确
                    columnInfo.setTypeName(rs.getString("data_type"));
                    columnInfo.setNullable("YES".equalsIgnoreCase(rs.getString("is_nullable")));
                    columnInfo.setDefaultValue(rs.getString("column_default"));
                    columnInfo.setOrdinalPosition(rs.getInt("ordinal_position"));
                    
                    // 设置长度和精度
                    if (rs.getObject("character_maximum_length") != null) {
                        columnInfo.setColumnSize(rs.getInt("character_maximum_length"));
                    } else if (rs.getObject("numeric_precision") != null) {
                        columnInfo.setColumnSize(rs.getInt("numeric_precision"));
                    }
                    
                    if (rs.getObject("numeric_scale") != null) {
                        columnInfo.setDecimalDigits(rs.getInt("numeric_scale"));
                    }
                    
                    // PostgreSQL自增列检查
                    String defaultValue = rs.getString("column_default");
                    columnInfo.setIsAutoIncrement(defaultValue != null && 
                        (defaultValue.contains("nextval") || defaultValue.contains("serial")));
                    
                    columns.add(columnInfo);
                }
            }
        }
        
        // 获取主键信息
        Set<String> primaryKeys = getPrimaryKeys(connection, schema, tableName);
        for (ColumnInfo column : columns) {
            column.setIsPrimaryKey(primaryKeys.contains(column.getColumnName()));
        }
        
        return columns;
    }

    // Oracle特定实现
    private static List<String> getOracleTables(Connection connection, String schemaName) throws SQLException {
        List<String> tables = new ArrayList<>();
        
        String sql;
        if (schemaName != null && !schemaName.trim().isEmpty()) {
            sql = "SELECT table_name FROM all_tables WHERE owner = UPPER(?) ORDER BY table_name";
        } else {
            sql = "SELECT table_name FROM user_tables ORDER BY table_name";
        }
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            // 设置查询超时时间（45秒）
            stmt.setQueryTimeout(45);
            if (schemaName != null && !schemaName.trim().isEmpty()) {
                stmt.setString(1, schemaName.toUpperCase());
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString("table_name"));
                }
            }
        }
        return tables;
    }

    private static List<ColumnInfo> getOracleColumns(Connection connection, String tableName, String schemaName) throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        
        String sql;
        if (schemaName != null && !schemaName.trim().isEmpty()) {
            sql = "SELECT column_name, data_type, data_length, data_precision, data_scale, nullable, " +
                  "data_default, ordinal_position FROM all_tab_columns WHERE owner = UPPER(?) AND table_name = UPPER(?) ORDER BY column_id";
        } else {
            sql = "SELECT column_name, data_type, data_length, data_precision, data_scale, nullable, " +
                  "data_default, column_id FROM user_tab_columns WHERE table_name = UPPER(?) ORDER BY column_id";
        }
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (schemaName != null && !schemaName.trim().isEmpty()) {
                stmt.setString(1, schemaName.toUpperCase());
                stmt.setString(2, tableName.toUpperCase());
            } else {
                stmt.setString(1, tableName.toUpperCase());
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ColumnInfo columnInfo = new ColumnInfo();
                    columnInfo.setColumnName(rs.getString("column_name"));
                    columnInfo.setDataType(rs.getString("data_type"));
                    columnInfo.setTypeName(rs.getString("data_type"));
                    columnInfo.setNullable("Y".equalsIgnoreCase(rs.getString("nullable")));
                    columnInfo.setDefaultValue(rs.getString("data_default"));
                    
                    // Oracle使用column_id作为位置
                    if (schemaName != null && !schemaName.trim().isEmpty()) {
                        columnInfo.setOrdinalPosition(rs.getInt("ordinal_position"));
                    } else {
                        columnInfo.setOrdinalPosition(rs.getInt("column_id"));
                    }
                    
                    // 设置长度和精度
                    if (rs.getObject("data_length") != null) {
                        columnInfo.setColumnSize(rs.getInt("data_length"));
                    }
                    if (rs.getObject("data_precision") != null) {
                        columnInfo.setColumnSize(rs.getInt("data_precision"));
                    }
                    if (rs.getObject("data_scale") != null) {
                        columnInfo.setDecimalDigits(rs.getInt("data_scale"));
                    }
                    
                    // Oracle没有标准的自增列，通常使用序列
                    columnInfo.setIsAutoIncrement(false);
                    
                    columns.add(columnInfo);
                }
            }
        }
        
        // 获取主键信息
        Set<String> primaryKeys = getPrimaryKeys(connection, schemaName, tableName);
        for (ColumnInfo column : columns) {
            column.setIsPrimaryKey(primaryKeys.contains(column.getColumnName()));
        }
        
        return columns;
    }

    // SQL Server特定实现
    private static List<String> getSQLServerTables(Connection connection, String schemaName) throws SQLException {
        List<String> tables = new ArrayList<>();
        String schema = (schemaName != null && !schemaName.trim().isEmpty()) ? schemaName : "dbo";

        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_type = 'BASE TABLE' ORDER BY table_name";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            // 设置查询超时时间（45秒）
            stmt.setQueryTimeout(45);
            stmt.setString(1, schema);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString("table_name"));
                }
            }
        }
        return tables;
    }

    private static List<ColumnInfo> getSQLServerColumns(Connection connection, String tableName, String schemaName) throws SQLException {
        // SQL Server实现类似于MySQL，使用information_schema
        return getMySQLColumns(connection, tableName, schemaName, null);
    }

    // 通用实现（使用JDBC元数据）
    private static List<String> getGenericTables(Connection connection, String schemaName, String databaseName) throws SQLException {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();
        
        try (ResultSet rs = metaData.getTables(databaseName, schemaName, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        return tables;
    }

    private static List<ColumnInfo> getGenericColumns(Connection connection, String tableName, String schemaName, String databaseName) throws SQLException {
        List<ColumnInfo> columns = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();
        
        try (ResultSet rs = metaData.getColumns(databaseName, schemaName, tableName, null)) {
            while (rs.next()) {
                ColumnInfo columnInfo = new ColumnInfo();
                columnInfo.setColumnName(rs.getString("COLUMN_NAME"));
                columnInfo.setDataType(rs.getString("TYPE_NAME"));
                columnInfo.setTypeName(rs.getString("TYPE_NAME"));
                columnInfo.setColumnSize(rs.getInt("COLUMN_SIZE"));
                columnInfo.setDecimalDigits(rs.getInt("DECIMAL_DIGITS"));
                columnInfo.setNullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                columnInfo.setDefaultValue(rs.getString("COLUMN_DEF"));
                columnInfo.setRemarks(rs.getString("REMARKS"));
                columnInfo.setOrdinalPosition(rs.getInt("ORDINAL_POSITION"));
                columnInfo.setJdbcType(rs.getInt("DATA_TYPE"));
                
                // 尝试获取自增信息
                try {
                    String isAutoIncrement = rs.getString("IS_AUTOINCREMENT");
                    columnInfo.setIsAutoIncrement("YES".equalsIgnoreCase(isAutoIncrement));
                } catch (SQLException e) {
                    columnInfo.setIsAutoIncrement(false);
                }
                
                columns.add(columnInfo);
            }
        }
        
        // 获取主键信息
        Set<String> primaryKeys = getPrimaryKeys(connection, schemaName, tableName);
        for (ColumnInfo column : columns) {
            column.setIsPrimaryKey(primaryKeys.contains(column.getColumnName()));
        }
        
        return columns;
    }

    /**
     * 获取主键信息
     */
    private static Set<String> getPrimaryKeys(Connection connection, String schemaName, String tableName) {
        Set<String> primaryKeys = new HashSet<>();
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet rs = metaData.getPrimaryKeys(null, schemaName, tableName)) {
                while (rs.next()) {
                    primaryKeys.add(rs.getString("COLUMN_NAME"));
                }
            }
        } catch (SQLException e) {
            // 忽略主键获取失败的情况
        }
        return primaryKeys;
    }

    /**
     * 获取所有表信息（包含详细信息）
     */
    private static List<TableInfo> getAllTableInfo(Connection connection, String dbType,
                                                   String schemaName, String databaseName) throws SQLException {
        List<TableInfo> tableInfos = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();

        // 根据数据库类型确定schema
        String schema = determineSchema(dbType, schemaName, databaseName);

        try (ResultSet rs = metaData.getTables(null, schema, "%", new String[]{"TABLE", "VIEW"})) {
            while (rs.next()) {
                TableInfo tableInfo = new TableInfo();
                tableInfo.setTableName(rs.getString("TABLE_NAME"));
                tableInfo.setTableType(rs.getString("TABLE_TYPE"));
                tableInfo.setRemarks(rs.getString("REMARKS"));
                tableInfo.setSchemaName(rs.getString("TABLE_SCHEM"));
                tableInfo.setCatalogName(rs.getString("TABLE_CAT"));

                // 获取列数和主键信息
                enrichTableInfo(connection, tableInfo, dbType);

                tableInfos.add(tableInfo);
            }
        }

        return tableInfos;
    }

    /**
     * 丰富表信息（添加列数、主键等信息）
     */
    private static void enrichTableInfo(Connection connection, TableInfo tableInfo, String dbType) {
        try {
            // 获取列数
            List<ColumnInfo> columns = getTableColumns(connection, dbType, tableInfo.getTableName(),
                                                      tableInfo.getSchemaName(), null);
            tableInfo.setColumnCount(columns.size());

            // 检查主键
            DatabaseMetaData metaData = connection.getMetaData();
            try (ResultSet pkRs = metaData.getPrimaryKeys(null, tableInfo.getSchemaName(), tableInfo.getTableName())) {
                if (pkRs.next()) {
                    tableInfo.setHasPrimaryKey(true);
                    tableInfo.setPrimaryKeyColumn(pkRs.getString("COLUMN_NAME"));
                } else {
                    tableInfo.setHasPrimaryKey(false);
                }
            }
        } catch (SQLException e) {
            // 如果获取详细信息失败，设置默认值
            tableInfo.setColumnCount(0);
            tableInfo.setHasPrimaryKey(false);
        }
    }

    /**
     * 过滤表列表
     */
    private static List<TableInfo> filterTables(List<TableInfo> tables, String search) {
        if (search == null || search.trim().isEmpty()) {
            return tables;
        }

        String searchLower = search.toLowerCase();
        List<TableInfo> filtered = new ArrayList<>();

        for (TableInfo table : tables) {
            if (table.getTableName().toLowerCase().contains(searchLower) ||
                (table.getRemarks() != null && table.getRemarks().toLowerCase().contains(searchLower))) {
                filtered.add(table);
            }
        }

        return filtered;
    }

    /**
     * 排序表列表
     */
    private static void sortTables(List<TableInfo> tables, String sortBy, String sortOrder) {
        boolean ascending = "asc".equalsIgnoreCase(sortOrder);

        tables.sort((t1, t2) -> {
            int result = 0;

            switch (sortBy.toLowerCase()) {
                case "name":
                case "tablename":
                    result = t1.getTableName().compareToIgnoreCase(t2.getTableName());
                    break;
                case "type":
                case "tabletype":
                    String type1 = t1.getTableType() != null ? t1.getTableType() : "";
                    String type2 = t2.getTableType() != null ? t2.getTableType() : "";
                    result = type1.compareToIgnoreCase(type2);
                    break;
                case "columns":
                case "columncount":
                    Integer count1 = t1.getColumnCount() != null ? t1.getColumnCount() : 0;
                    Integer count2 = t2.getColumnCount() != null ? t2.getColumnCount() : 0;
                    result = count1.compareTo(count2);
                    break;
                default:
                    result = t1.getTableName().compareToIgnoreCase(t2.getTableName());
                    break;
            }

            return ascending ? result : -result;
        });
    }

    /**
     * 获取数据库的Schema列表
     */
    public static List<String> getSchemas(Connection connection, String dbType) throws SQLException {
        List<String> schemas = new ArrayList<>();
        DatabaseMetaData metaData = connection.getMetaData();

        switch (dbType.toLowerCase()) {
            case "mysql":
                // MySQL使用数据库名作为schema
                try (ResultSet rs = metaData.getCatalogs()) {
                    while (rs.next()) {
                        schemas.add(rs.getString("TABLE_CAT"));
                    }
                }
                break;
            case "postgresql":
            case "vastbase":
                // PostgreSQL使用schema
                try (ResultSet rs = metaData.getSchemas()) {
                    while (rs.next()) {
                        String schemaName = rs.getString("TABLE_SCHEM");
                        // 过滤系统schema
                        if (!schemaName.startsWith("information_schema") &&
                            !schemaName.startsWith("pg_")) {
                            schemas.add(schemaName);
                        }
                    }
                }
                break;
            case "oracle":
            case "dameng":
                // Oracle使用用户名作为schema
                try (ResultSet rs = metaData.getSchemas()) {
                    while (rs.next()) {
                        schemas.add(rs.getString("TABLE_SCHEM"));
                    }
                }
                break;
            case "sqlserver":
                // SQL Server使用schema
                try (ResultSet rs = metaData.getSchemas()) {
                    while (rs.next()) {
                        schemas.add(rs.getString("TABLE_SCHEM"));
                    }
                }
                break;
            default:
                // 通用实现
                try (ResultSet rs = metaData.getSchemas()) {
                    while (rs.next()) {
                        schemas.add(rs.getString("TABLE_SCHEM"));
                    }
                }
                break;
        }

        return schemas;
    }

    /**
     * 确定数据库schema
     */
    private static String determineSchema(String dbType, String schemaName, String databaseName) {
        if (schemaName != null && !schemaName.trim().isEmpty()) {
            return schemaName;
        }

        switch (dbType.toLowerCase()) {
            case "mysql":
                return databaseName;
            case "postgresql":
                return "public";
            case "oracle":
                return null; // Oracle使用用户名作为schema
            case "sqlserver":
                return "dbo";
            default:
                return null;
        }
    }
}
