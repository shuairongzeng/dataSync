package com.dbsync.dbsync.mapper;

import com.dbsync.dbsync.service.BatchInsertSqlProvider;
import org.apache.ibatis.annotations.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page; // 导入 Page 类

import java.util.List;
import java.util.Map;

@Mapper
public interface TableMapper {

    /**
     * 执行动态 SQL
     * @param sql
     * @param params
     */
    @Insert("${sql}")
    void executeDynamicSQL(
            @Param("sql") String sql,
            @Param("params") Object[] params  // 显式命名参数数组
    );


    @InsertProvider(type = BatchInsertSqlProvider.class, method = "generateBatchInsertSql")
    void executeBatchInsert(@Param("tableName") String tableName, @Param("data") List<Map<String, Object>> data);

    /**
     * Database-agnostic pagination.
     * Parameters 'current', 'size', 'tableName', 'dbType', 'schemaName' (optional), 'orderByColumn' (optional for SQLServer)
     * must be passed in a Map due to provider method signature limitations with multiple params.
     */
    @SelectProvider(type = TableMetadataSqlProvider.class, method = "getTableDataWithPagination")
    List<Map<String, Object>> getTableDataWithPagination(Map<String, Object> params);

    /**
     * 添加执行 DDL 的方法
     * @param sql
     */
    @Update("${sql}")
    void executeDDL(String sql);

    /**
     * 添加执行 DML 的方法
     * @param sql
     */
    @Update("${sql}")
    void executeDML(String sql);

    /**
     * 获取所有表的注释
     * @return
     */
    @SelectProvider(type = TableMetadataSqlProvider.class, method = "getAllTableComments")
    List<Map<String, String>> getAllTableComments(@Param("dbType") String dbType, @Param("schemaName") String schemaName);

    /**
     * 获取指定表的列注释
     * @param dbType Database type (e.g., "oracle", "postgresql")
     * @param tableName Name of the table
     * @param schemaName Optional schema name; may be null
     * @return List of column comments
     */
    @SelectProvider(type = TableMetadataSqlProvider.class, method = "getColumnComments")
    List<Map<String, String>> getColumnComments(@Param("dbType") String dbType, @Param("tableName") String tableName, @Param("schemaName") String schemaName);

    /**
     * 获取指定表的结构信息
     * @param dbType Database type
     * @param tableName Name of the table
     * @param schemaName Optional schema name
     * @return List of column structures
     */
    @SelectProvider(type = TableMetadataSqlProvider.class, method = "getTableStructure")
    List<Map<String, Object>> getTableStructure(@Param("dbType") String dbType, @Param("tableName") String tableName, @Param("schemaName") String schemaName);

    /**
     * 获取指定表的数据
     * @param tableName
     * @return
     */
    @Select("SELECT * FROM ${tableName}")
    List<Map<String, Object>> getTableData(String tableName);

    /**
     * 清空表数据 - This is generally cross-database, but TRUNCATE might need specific permissions.
     * Using ${tableName} is fine as it's controlled internally.
     * @param tableName
     */
    @Update("TRUNCATE TABLE ${tableName}")
    void truncateTable(String tableName);

    /**
     * 获取表的总记录数
     * @param dbType Database type
     * @param tableName Name of the table
     * @param schemaName Optional schema name
     * @return Total number of rows
     */
    @SelectProvider(type = TableMetadataSqlProvider.class, method = "getTableCount")
    long getTableCount(@Param("dbType") String dbType, @Param("tableName") String tableName, @Param("schemaName") String schemaName);

    /**
     * 检查 PostgreSQL 表是否存在
     * @param tableName 要检查的表名
     * @return 如果表存在返回 1，否则返回 0
     */
    @SelectProvider(type = TableMetadataSqlProvider.class, method = "checkPgTableExists")
    long checkPgTableExists(@Param("tableName") String tableName);
}