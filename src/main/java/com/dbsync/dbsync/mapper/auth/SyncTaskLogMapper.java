package com.dbsync.dbsync.mapper.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dbsync.dbsync.model.SyncTaskLog;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 同步任务日志数据访问接口
 */
@Repository
public interface SyncTaskLogMapper extends BaseMapper<SyncTaskLog> {
    
    /**
     * 根据任务ID查找日志
     */
    @Select("SELECT * FROM sync_task_logs WHERE task_id = #{taskId} ORDER BY created_at DESC")
    List<SyncTaskLog> findByTaskId(@Param("taskId") Long taskId);
    
    /**
     * 根据任务ID和日志级别查找日志
     */
    @Select("SELECT * FROM sync_task_logs WHERE task_id = #{taskId} AND level = #{level} ORDER BY created_at DESC")
    List<SyncTaskLog> findByTaskIdAndLevel(@Param("taskId") Long taskId, @Param("level") String level);
    
    /**
     * 插入新的日志
     */
    @Insert("INSERT INTO sync_task_logs (task_id, level, message, created_at) " +
            "VALUES (#{taskId}, #{level}, #{message}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertLog(SyncTaskLog log);
    
    /**
     * 批量插入日志
     */
    @Insert("<script>" +
            "INSERT INTO sync_task_logs (task_id, level, message, created_at) VALUES " +
            "<foreach collection='logs' item='log' separator=','>" +
            "(#{log.taskId}, #{log.level}, #{log.message}, #{log.createdAt})" +
            "</foreach>" +
            "</script>")
    int batchInsertLogs(@Param("logs") List<SyncTaskLog> logs);
    
    /**
     * 根据任务ID删除日志
     */
    @Delete("DELETE FROM sync_task_logs WHERE task_id = #{taskId}")
    int deleteByTaskId(@Param("taskId") Long taskId);
    
    /**
     * 删除指定时间之前的日志
     */
    @Delete("DELETE FROM sync_task_logs WHERE task_id = #{taskId} AND created_at < #{cutoffTime}")
    int deleteLogsBeforeTime(@Param("taskId") Long taskId, @Param("cutoffTime") String cutoffTime);
    
    /**
     * 获取最近的日志
     */
    @Select("SELECT * FROM sync_task_logs WHERE task_id = #{taskId} ORDER BY created_at DESC LIMIT #{limit}")
    List<SyncTaskLog> findRecentLogs(@Param("taskId") Long taskId, @Param("limit") Integer limit);
    
    /**
     * 获取错误日志
     */
    @Select("SELECT * FROM sync_task_logs WHERE task_id = #{taskId} AND level = 'ERROR' ORDER BY created_at DESC")
    List<SyncTaskLog> findErrorLogs(@Param("taskId") Long taskId);
    
    /**
     * 记录INFO级别日志
     */
    @Insert("INSERT INTO sync_task_logs (task_id, level, message, created_at) " +
            "VALUES (#{taskId}, 'INFO', #{message}, #{createdAt})")
    int insertInfoLog(@Param("taskId") Long taskId, @Param("message") String message, 
                      @Param("createdAt") String createdAt);
    
    /**
     * 记录WARN级别日志
     */
    @Insert("INSERT INTO sync_task_logs (task_id, level, message, created_at) " +
            "VALUES (#{taskId}, 'WARN', #{message}, #{createdAt})")
    int insertWarnLog(@Param("taskId") Long taskId, @Param("message") String message, 
                      @Param("createdAt") String createdAt);
    
    /**
     * 记录ERROR级别日志
     */
    @Insert("INSERT INTO sync_task_logs (task_id, level, message, created_at) " +
            "VALUES (#{taskId}, 'ERROR', #{message}, #{createdAt})")
    int insertErrorLog(@Param("taskId") Long taskId, @Param("message") String message, 
                       @Param("createdAt") String createdAt);
}