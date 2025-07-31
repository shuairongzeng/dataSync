package com.dbsync.dbsync.mapper.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dbsync.dbsync.model.SyncTask;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 同步任务数据访问接口
 */
@Repository
public interface SyncTaskMapper extends BaseMapper<SyncTask> {
    
    /**
     * 根据任务名称查找任务
     */
    @Select("SELECT * FROM sync_tasks WHERE name = #{name}")
    SyncTask findByName(@Param("name") String name);
    
    /**
     * 根据源连接ID查找任务
     */
    @Select("SELECT * FROM sync_tasks WHERE source_connection_id = #{sourceConnectionId} ORDER BY created_at DESC")
    List<SyncTask> findBySourceConnectionId(@Param("sourceConnectionId") Long sourceConnectionId);
    
    /**
     * 根据目标连接ID查找任务
     */
    @Select("SELECT * FROM sync_tasks WHERE target_connection_id = #{targetConnectionId} ORDER BY created_at DESC")
    List<SyncTask> findByTargetConnectionId(@Param("targetConnectionId") Long targetConnectionId);
    
    /**
     * 根据状态查找任务
     */
    @Select("SELECT * FROM sync_tasks WHERE status = #{status} ORDER BY created_at DESC")
    List<SyncTask> findByStatus(@Param("status") String status);
    
    /**
     * 查找运行中的任务
     */
    @Select("SELECT * FROM sync_tasks WHERE status = 'RUNNING' ORDER BY created_at DESC")
    List<SyncTask> findRunningTasks();
    
    /**
     * 检查任务名称是否存在
     */
    @Select("SELECT COUNT(*) FROM sync_tasks WHERE name = #{name}")
    boolean existsByName(@Param("name") String name);
    
    /**
     * 检查任务名称是否存在（排除指定ID）
     */
    @Select("SELECT COUNT(*) FROM sync_tasks WHERE name = #{name} AND id != #{id}")
    boolean existsByNameExcludingId(@Param("name") String name, @Param("id") Long id);
    
    /**
     * 插入新的同步任务
     */
    @Insert("INSERT INTO sync_tasks (name, source_connection_id, target_connection_id, source_schema_name, " +
            "target_schema_name, tables, truncate_before_sync, status, progress, total_tables, " +
            "completed_tables, error_message, created_at, updated_at) " +
            "VALUES (#{name}, #{sourceConnectionId}, #{targetConnectionId}, #{sourceSchemaName}, " +
            "#{targetSchemaName}, #{tables}, #{truncateBeforeSync}, #{status}, #{progress}, " +
            "#{totalTables}, #{completedTables}, #{errorMessage}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertTask(SyncTask task);
    
    /**
     * 更新同步任务
     */
    @Update("UPDATE sync_tasks SET name = #{name}, source_connection_id = #{sourceConnectionId}, " +
            "target_connection_id = #{targetConnectionId}, source_schema_name = #{sourceSchemaName}, " +
            "target_schema_name = #{targetSchemaName}, tables = #{tables}, " +
            "truncate_before_sync = #{truncateBeforeSync}, status = #{status}, progress = #{progress}, " +
            "total_tables = #{totalTables}, completed_tables = #{completedTables}, " +
            "error_message = #{errorMessage}, updated_at = #{updatedAt}, last_run_at = #{lastRunAt} " +
            "WHERE id = #{id}")
    int updateTask(SyncTask task);
    
    /**
     * 更新任务状态
     */
    @Update("UPDATE sync_tasks SET status = #{status}, progress = #{progress}, " +
            "completed_tables = #{completedTables}, error_message = #{errorMessage}, " +
            "updated_at = #{updatedAt}, last_run_at = #{lastRunAt} WHERE id = #{id}")
    int updateTaskStatus(@Param("id") Long id, @Param("status") String status, 
                        @Param("progress") Integer progress, @Param("completedTables") Integer completedTables,
                        @Param("errorMessage") String errorMessage, @Param("updatedAt") String updatedAt,
                        @Param("lastRunAt") String lastRunAt);
    
    /**
     * 根据ID删除任务
     */
    @Delete("DELETE FROM sync_tasks WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
    
    /**
     * 根据ID查找任务
     */
    @Select("SELECT * FROM sync_tasks WHERE id = #{id}")
    SyncTask findById(@Param("id") Long id);
    
    /**
     * 获取所有任务
     */
    @Select("SELECT * FROM sync_tasks ORDER BY created_at DESC")
    List<SyncTask> findAllTasks();
    
    /**
     * 更新任务进度
     */
    @Update("UPDATE sync_tasks SET progress = #{progress}, completed_tables = #{completedTables}, " +
            "updated_at = #{updatedAt} WHERE id = #{id}")
    int updateTaskProgress(@Param("id") Long id, @Param("progress") Integer progress, 
                          @Param("completedTables") Integer completedTables, @Param("updatedAt") String updatedAt);
    
    /**
     * 停止运行中的任务
     */
    @Update("UPDATE sync_tasks SET status = 'FAILED', error_message = #{errorMessage}, " +
            "updated_at = #{updatedAt} WHERE id = #{id} AND status = 'RUNNING'")
    int stopRunningTask(@Param("id") Long id, @Param("errorMessage") String errorMessage, 
                       @Param("updatedAt") String updatedAt);
}