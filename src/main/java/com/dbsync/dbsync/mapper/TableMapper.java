package com.dbsync.dbsync.mapper;


import com.dbsync.dbsync.service.BatchInsertSqlProvider;
import org.apache.ibatis.annotations.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page; // 导入 Page 类

import java.util.List;
import java.util.Map;

public interface TableMapper {

    /**
     * 执行动态SQL
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
     * Oracle数据库分页获取表数据
     * @param current 分页参数
     * @param size 每页大小
     * @param tableName 表名
     * @return 分页数据
     */
    @Select("SELECT * FROM (" +
            "  SELECT a.*, ROWNUM rnum FROM (" +
            "    SELECT * FROM ${tableName}" +
            "  ) a WHERE ROWNUM <= #{current} * #{size}" +
            ") WHERE rnum > (#{current} - 1) * #{size}")
    List<Map<String, Object>> getTableDataWithPagination(@Param("current") Long current,
                                                         @Param("size") Long size,
                                                         @Param("tableName") String tableName);

    /**
     * 添加执行DDL的方法
     * @param sql
     */
    @Update("${sql}")
    void executeDDL(String sql);

    /**
     * 添加执行DML的方法
     * @param sql
     */
    @Update("${sql}")
    void executeDML(String sql);

    /**
     * 获取所有表的注释
     * @return
     */
    @Select("SELECT table_name, comments FROM user_tab_comments")
    List<Map<String, String>> getAllTableComments();

    /**
     * 获取指定表的列注释
     * @param tableName
     * @return
     */
    @Select("SELECT column_name, comments FROM user_col_comments WHERE table_name = #{tableName}")
    List<Map<String, String>> getColumnComments(String tableName);

    /**
     * 获取指定表的结构信息
     * @param tableName
     * @return
     */
    @Select("SELECT column_name, data_type, data_length, nullable FROM user_tab_columns WHERE table_name = #{tableName}")
    List<Map<String, Object>> getTableStructure(String tableName);

    /**
     * 获取指定表的数据
     * @param tableName
     * @return
     */
    @Select("SELECT * FROM ${tableName}")
    List<Map<String, Object>> getTableData(String tableName);

    /**
     * Oracle检查表是否存在
     * @param tableName
     * @return
     */
    @Select("SELECT COUNT(*) FROM user_tables WHERE table_name = #{tableName}")
    int checkOracleTableExists(String tableName);

    /**
     * PostgreSQL检查表是否存在
     * @param tableName
     * @return
     */
    @Select("SELECT COUNT(*) FROM information_schema.tables WHERE table_name = lower(#{tableName}) AND table_schema = current_schema()")
    int checkPgTableExists(String tableName);

    /**
     * 清空表数据
     * @param tableName
     */
    @Select("TRUNCATE TABLE ${tableName}")
    void truncateTable(String tableName);

    /**
     * 获取表的总记录数
     * @param tableName
     * @return
     */
    @Select("SELECT COUNT(*) FROM ${tableName}")
    long getTableCount(String tableName);
}