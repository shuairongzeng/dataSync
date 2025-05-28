package com.dbsync.dbsync.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.jdbc.SQL;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TableMetadataSqlProvider {
    private static final Logger logger = LoggerFactory.getLogger(TableMetadataSqlProvider.class);

    public String getAllTableComments(@Param("dbType") String dbType, @Param("schemaName") String schemaName) {
        // schemaName might be null or not applicable for all DBs (e.g. MySQL default schema)
        // For Oracle, schemaName would typically be the user if USER_TAB_COMMENTS is used.
        // For PostgreSQL, schemaName is important (e.g. 'public').
        // For SQLServer, schemaName is important (e.g. 'dbo').
        // For MySQL, schemaName is the database name.
        
        switch (dbType.toLowerCase()) {
            case "oracle":
            case "dameng": // Assuming Dameng uses Oracle-like system views
                return "SELECT table_name AS \"TABLE_NAME\", comments AS \"COMMENTS\" FROM user_tab_comments";
            case "postgresql":
            case "vastbase": // Assuming Vastbase uses PostgreSQL-like system views
                return new SQL() {{
                    SELECT("t.table_name AS \"TABLE_NAME\", pg_catalog.obj_description(c.oid, 'pg_class') AS \"COMMENTS\"");
                    FROM("information_schema.tables t");
                    JOIN("pg_catalog.pg_class c ON c.relname = t.table_name");
                    JOIN("pg_catalog.pg_namespace n ON n.oid = c.relnamespace");
                    WHERE("t.table_schema = COALESCE(#{schemaName}, current_schema())");
                    AND();
                    WHERE("t.table_type = 'BASE TABLE'");
                    AND();
                    WHERE("n.nspname = COALESCE(#{schemaName}, current_schema())");
                }}.toString();
            case "mysql":
                return "SELECT table_name AS \"TABLE_NAME\", table_comment AS \"COMMENTS\" FROM information_schema.TABLES WHERE table_schema = COALESCE(#{schemaName}, DATABASE())";
            case "sqlserver":
                return new SQL() {{
                    SELECT("t.TABLE_NAME AS \"TABLE_NAME\", CAST(ep.value AS VARCHAR(MAX)) AS \"COMMENTS\"");
                    FROM("INFORMATION_SCHEMA.TABLES t");
                    LEFT_OUTER_JOIN("sys.extended_properties ep ON ep.major_id = OBJECT_ID(t.TABLE_SCHEMA + '.' + t.TABLE_NAME) AND ep.minor_id = 0 AND ep.name = 'MS_Description'");
                    WHERE("t.TABLE_SCHEMA = COALESCE(#{schemaName}, SCHEMA_NAME())");
                    AND();
                    WHERE("t.TABLE_TYPE = 'BASE TABLE'");
                }}.toString();
            default:
                throw new IllegalArgumentException("Unsupported database type for getAllTableComments: " + dbType);
        }
    }

    public String getTableStructure(@Param("dbType") String dbType, @Param("tableName") String tableName, @Param("schemaName") String schemaName) {
        switch (dbType.toLowerCase()) {
            case "oracle":
            case "dameng":
                // Note: DATA_DEFAULT for default values, PK info from USER_CONSTRAINTS/USER_CONS_COLUMNS
                return "SELECT column_name AS \"COLUMN_NAME\", data_type AS \"DATA_TYPE\", " +
                       "data_length AS \"DATA_LENGTH\", data_precision AS \"DATA_PRECISION\", " +
                       "data_scale AS \"DATA_SCALE\", nullable AS \"NULLABLE\" " +
                       "FROM user_tab_columns WHERE table_name = #{tableName}";
            case "postgresql":
            case "vastbase":
                return new SQL() {{
                    SELECT("column_name AS \"COLUMN_NAME\", udt_name AS \"DATA_TYPE\", " + // udt_name is often more specific e.g. int4, varchar
                           "character_maximum_length AS \"DATA_LENGTH\", " +
                           "numeric_precision AS \"DATA_PRECISION\", " +
                           "numeric_scale AS \"DATA_SCALE\", " +
                           "CASE WHEN is_nullable = 'YES' THEN 'Y' ELSE 'N' END AS \"NULLABLE\"");
                    FROM("information_schema.columns");
                    WHERE("table_name = #{tableName}");
                    AND();
                    WHERE("table_schema = COALESCE(#{schemaName}, current_schema())");
                    ORDER_BY("ordinal_position");
                }}.toString();
            case "mysql":
                // COLUMN_TYPE can give full type e.g. VARCHAR(255), INT(11)
                // CHARACTER_MAXIMUM_LENGTH, NUMERIC_PRECISION, NUMERIC_SCALE are standard
                return new SQL() {{
                    SELECT("COLUMN_NAME AS \"COLUMN_NAME\", DATA_TYPE AS \"DATA_TYPE\", " +
                           "CHARACTER_MAXIMUM_LENGTH AS \"DATA_LENGTH\", " +
                           "NUMERIC_PRECISION AS \"DATA_PRECISION\", " +
                           "NUMERIC_SCALE AS \"DATA_SCALE\", " +
                           "CASE WHEN IS_NULLABLE = 'YES' THEN 'Y' ELSE 'N' END AS \"NULLABLE\"");
                    FROM("information_schema.COLUMNS");
                    WHERE("TABLE_NAME = #{tableName}");
                    AND();
                    WHERE("TABLE_SCHEMA = COALESCE(#{schemaName}, DATABASE())");
                    ORDER_BY("ORDINAL_POSITION");
                }}.toString();
            case "sqlserver":
                return new SQL() {{
                    SELECT("COLUMN_NAME AS \"COLUMN_NAME\", DATA_TYPE AS \"DATA_TYPE\", " +
                           "CHARACTER_MAXIMUM_LENGTH AS \"DATA_LENGTH\", " +
                           "NUMERIC_PRECISION AS \"DATA_PRECISION\", " +
                           "NUMERIC_SCALE AS \"DATA_SCALE\", " +
                           "CASE WHEN IS_NULLABLE = 'YES' THEN 'Y' ELSE 'N' END AS \"NULLABLE\"");
                    FROM("INFORMATION_SCHEMA.COLUMNS");
                    WHERE("TABLE_NAME = #{tableName}");
                    AND();
                    WHERE("TABLE_SCHEMA = COALESCE(#{schemaName}, SCHEMA_NAME())"); // Assuming schemaName is passed or use default
                    ORDER_BY("ORDINAL_POSITION");
                }}.toString();
            default:
                throw new IllegalArgumentException("Unsupported database type for getTableStructure: " + dbType);
        }
    }

    public String getColumnComments(@Param("dbType") String dbType, @Param("tableName") String tableName, @Param("schemaName") String schemaName) {
        switch (dbType.toLowerCase()) {
            case "oracle":
            case "dameng":
                return "SELECT column_name AS \"COLUMN_NAME\", comments AS \"COMMENTS\" FROM user_col_comments WHERE table_name = #{tableName}";
            case "postgresql":
            case "vastbase":
                 return new SQL() {{
                    SELECT("c.column_name AS \"COLUMN_NAME\", pgd.description AS \"COMMENTS\"");
                    FROM("information_schema.columns c");
                    JOIN("pg_catalog.pg_stat_all_tables st ON st.schemaname = c.table_schema AND st.relname = c.table_name");
                    JOIN("pg_catalog.pg_description pgd ON pgd.objoid = st.relid AND pgd.objsubid = c.ordinal_position");
                    WHERE("c.table_name = #{tableName}");
                    AND();
                    WHERE("c.table_schema = COALESCE(#{schemaName}, current_schema())");
                }}.toString();
            case "mysql":
                return "SELECT COLUMN_NAME AS \"COLUMN_NAME\", COLUMN_COMMENT AS \"COMMENTS\" FROM information_schema.COLUMNS " +
                       "WHERE TABLE_NAME = #{tableName} AND TABLE_SCHEMA = COALESCE(#{schemaName}, DATABASE())";
            case "sqlserver":
                return new SQL() {{
                    SELECT("c.name AS \"COLUMN_NAME\", CAST(ep.value AS VARCHAR(MAX)) AS \"COMMENTS\"");
                    FROM("sys.columns c");
                    INNER_JOIN("sys.tables t ON c.object_id = t.object_id");
                    INNER_JOIN("sys.schemas s ON t.schema_id = s.schema_id");
                    LEFT_OUTER_JOIN("sys.extended_properties ep ON ep.major_id = c.object_id AND ep.minor_id = c.column_id AND ep.name = 'MS_Description'");
                    WHERE("t.name = #{tableName}");
                    AND();
                    WHERE("s.name = COALESCE(#{schemaName}, SCHEMA_NAME())");
                }}.toString();
            default:
                throw new IllegalArgumentException("Unsupported database type for getColumnComments: " + dbType);
        }
    }
    
    public String getTableCount(@Param("dbType") String dbType, @Param("tableName") String tableName, @Param("schemaName") String schemaName) {
        // schemaName might not be needed if tableName is fully qualified or default schema is used.
        // For safety, some DBs might need it in dynamic SQL if not part of tableName.
        // However, simple "SELECT COUNT(*) FROM ${tableName}" is often okay if tableName can be schema.table.
        // The provider here just returns the string, assuming tableName might be schema-qualified by the caller.
        // If schemaName is provided and tableName is not schema-qualified, construction would be more complex.
        // For now, keeping it simple as the original was just "SELECT COUNT(*) FROM ${tableName}".
        // The use of ${tableName} implies the caller might be responsible for schema qualification.
        return "SELECT COUNT(*) FROM " + tableName;
    }

    public String getTableDataWithPagination(Map<String, Object> params) {
        String dbType = (String) params.get("dbType");
        String tableName = (String) params.get("tableName");
        // String schemaName = (String) params.get("schemaName"); // If needed for qualifying table
        Long current = (Long) params.get("current");
        Long size = (Long) params.get("size");
        long offset = (current - 1) * size;

        // Basic column selection, can be enhanced to select specific columns
        String columns = "*"; 

        switch (dbType.toLowerCase()) {
            case "oracle":
            case "dameng":
                // 使用子查询来确保只选择原始列
                return "WITH numbered_rows AS (" +
                       "  SELECT a.*, ROW_NUMBER() OVER (ORDER BY 1) as rn FROM " + tableName + " a" +
                       ") SELECT " + columns + " FROM numbered_rows WHERE rn > " + offset + " AND rn <= " + (offset + size);
            case "postgresql":
            case "vastbase":
            case "mysql": // MySQL also uses LIMIT OFFSET
                return "SELECT " + columns + " FROM " + tableName + // Qualify tableName with schemaName if needed
                       " LIMIT " + size + " OFFSET " + offset;
            case "sqlserver":
                // SQL Server requires an ORDER BY clause for OFFSET FETCH.
                String orderBy = (String) params.getOrDefault("orderByColumn", null);
                if (orderBy == null || orderBy.isEmpty()) {
                    logger.warn("SQL Server pagination is used without an explicit ORDER BY clause. This might lead to unpredictable results or errors. Please provide an 'orderByColumn' parameter.");
                    return "SELECT " + columns + " FROM " + tableName + // Qualify if needed
                           " ORDER BY (SELECT NULL) " + // THIS IS A POTENTIAL ISSUE / PLACEHOLDER
                           " OFFSET " + offset + " ROWS FETCH NEXT " + size + " ROWS ONLY";
                }
                return "SELECT " + columns + " FROM " + tableName + // Qualify if needed
                       " ORDER BY " + orderBy +
                       " OFFSET " + offset + " ROWS FETCH NEXT " + size + " ROWS ONLY";
            default:
                throw new IllegalArgumentException("Unsupported database type for getTableDataWithPagination: " + dbType);
        }
    }
    
    public String checkPgTableExists(@Param("tableName") String tableName) {
        return new SQL() {{
            SELECT("COUNT(*)");
            FROM("information_schema.tables");
            WHERE("table_name = LOWER(#{tableName})");
            AND();
            WHERE("table_schema = current_schema()");
        }}.toString();
    }
    
    // Helper for SQL Server pagination to get a sort key if not provided
    // This is complex and context-dependent, so not fully implemented here.
    // private String getSqlServerOrderByClause(Map<String, Object> params) { ... }
}
