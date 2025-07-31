package com.dbsync.dbsync.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dbsync.dbsync.entity.QueryHistory;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 查询历史数据访问接口
 * 注意：此Mapper使用auth数据源（SQLite），不是主数据源
 */
@Repository
public interface QueryHistoryMapper extends BaseMapper<QueryHistory> {
    
    /**
     * 根据用户查找查询历史
     */
    @Select("SELECT * FROM query_history WHERE created_by = #{createdBy} ORDER BY executed_at DESC")
    List<QueryHistory> findByCreatedBy(@Param("createdBy") String createdBy);
    
    /**
     * 根据源连接ID查找查询历史
     */
    @Select("SELECT * FROM query_history WHERE source_connection_id = #{sourceConnectionId} ORDER BY executed_at DESC")
    List<QueryHistory> findBySourceConnectionId(@Param("sourceConnectionId") Long sourceConnectionId);
    
    /**
     * 根据状态查找查询历史
     */
    @Select("SELECT * FROM query_history WHERE status = #{status} ORDER BY executed_at DESC")
    List<QueryHistory> findByStatus(@Param("status") String status);
    
    /**
     * 获取所有查询历史，按执行时间倒序
     */
    @Select("SELECT * FROM query_history ORDER BY executed_at DESC")
    List<QueryHistory> findAllOrderByExecutedAtDesc();
    
    /**
     * 获取最近的查询历史
     */
    @Select("SELECT * FROM query_history ORDER BY executed_at DESC LIMIT #{limit}")
    List<QueryHistory> findRecentHistory(@Param("limit") Integer limit);
    
    /**
     * 根据用户获取最近的查询历史
     */
    @Select("SELECT * FROM query_history WHERE created_by = #{createdBy} ORDER BY executed_at DESC LIMIT #{limit}")
    List<QueryHistory> findRecentHistoryByUser(@Param("createdBy") String createdBy, @Param("limit") Integer limit);
    
    /**
     * 插入查询历史
     */
    @Insert("INSERT INTO query_history (sql, source_connection_id, source_connection_name, " +
            "target_connection_id, target_connection_name, target_table_name, target_schema_name, " +
            "executed_at, execution_time, status, error_message, result_rows, created_by) " +
            "VALUES (#{sql}, #{sourceConnectionId}, #{sourceConnectionName}, " +
            "#{targetConnectionId,jdbcType=BIGINT}, #{targetConnectionName,jdbcType=VARCHAR}, " +
            "#{targetTableName,jdbcType=VARCHAR}, #{targetSchemaName,jdbcType=VARCHAR}, " +
            "#{executedAt}, #{executionTime}, #{status}, #{errorMessage,jdbcType=VARCHAR}, " +
            "#{resultRows,jdbcType=INTEGER}, #{createdBy,jdbcType=VARCHAR})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertQueryHistory(QueryHistory queryHistory);
    
    /**
     * 根据ID删除查询历史
     */
    @Delete("DELETE FROM query_history WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
    
    /**
     * 根据用户删除查询历史
     */
    @Delete("DELETE FROM query_history WHERE created_by = #{createdBy}")
    int deleteByCreatedBy(@Param("createdBy") String createdBy);
    
    /**
     * 删除指定时间之前的查询历史
     */
    @Delete("DELETE FROM query_history WHERE executed_at < #{cutoffTime}")
    int deleteHistoryBeforeTime(@Param("cutoffTime") String cutoffTime);
    
    /**
     * 获取查询历史统计信息
     */
    @Select("SELECT COUNT(*) as total, " +
            "SUM(CASE WHEN status = 'SUCCESS' THEN 1 ELSE 0 END) as success_count, " +
            "SUM(CASE WHEN status = 'ERROR' THEN 1 ELSE 0 END) as error_count, " +
            "AVG(execution_time) as avg_execution_time " +
            "FROM query_history WHERE created_by = #{createdBy}")
    @Results({
        @Result(property = "total", column = "total"),
        @Result(property = "successCount", column = "success_count"),
        @Result(property = "errorCount", column = "error_count"),
        @Result(property = "avgExecutionTime", column = "avg_execution_time")
    })
    QueryHistoryStats getStatsByUser(@Param("createdBy") String createdBy);
    
    /**
     * 查询历史统计信息内部类
     */
    class QueryHistoryStats {
        private Long total;
        private Long successCount;
        private Long errorCount;
        private Double avgExecutionTime;
        
        // Getters and Setters
        public Long getTotal() {
            return total;
        }
        
        public void setTotal(Long total) {
            this.total = total;
        }
        
        public Long getSuccessCount() {
            return successCount;
        }
        
        public void setSuccessCount(Long successCount) {
            this.successCount = successCount;
        }
        
        public Long getErrorCount() {
            return errorCount;
        }
        
        public void setErrorCount(Long errorCount) {
            this.errorCount = errorCount;
        }
        
        public Double getAvgExecutionTime() {
            return avgExecutionTime;
        }
        
        public void setAvgExecutionTime(Double avgExecutionTime) {
            this.avgExecutionTime = avgExecutionTime;
        }
    }
}
