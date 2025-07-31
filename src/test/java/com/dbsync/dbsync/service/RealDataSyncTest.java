package com.dbsync.dbsync.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.dbsync.dbsync.model.SyncTask;
import com.dbsync.dbsync.model.DbConnection;
import com.dbsync.dbsync.mapper.auth.DbConnectionMapper;
import com.dbsync.dbsync.mapper.auth.SyncTaskMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 真实数据同步功能测试
 */
@SpringBootTest
public class RealDataSyncTest {

    @Autowired
    private SyncTaskService syncTaskService;

    @Autowired
    private DbConnectionMapper dbConnectionMapper;

    @Autowired
    private SyncTaskMapper syncTaskMapper;

    @Test
    @Transactional
    public void testRealSyncTaskExecution() {
        // 创建源和目标数据库连接
        DbConnection sourceConnection = createTestConnection("源MySQL库", "mysql", "localhost", 3306, "source_db");
        DbConnection targetConnection = createTestConnection("目标PG库", "postgresql", "localhost", 5432, "target_db");
        
        // 保存连接
        dbConnectionMapper.insertConnection(sourceConnection);
        dbConnectionMapper.insertConnection(targetConnection);
        
        // 创建同步任务
        SyncTask task = new SyncTask();
        task.setName("真实数据同步测试任务");
        task.setSourceConnectionId(sourceConnection.getId());
        task.setTargetConnectionId(targetConnection.getId());
        task.setSourceSchemaName("test_schema");
        task.setTargetSchemaName("public");
        task.setTables(Arrays.asList("test_table"));
        task.setTruncateBeforeSync(true);
        
        // 保存任务
        syncTaskMapper.insertTask(task);
        assertNotNull(task.getId());
        
        // 验证任务状态
        assertEquals("PENDING", task.getStatus());
        assertEquals(0, task.getProgress());
        assertEquals(1, task.getTotalTables());
        
        // 注意：这里不执行实际的同步，因为需要真实的数据库连接
        // 但我们可以验证同步方法的调用是否正确
        try {
            // 这个调用会因为数据库连接失败而抛出异常，但我们可以验证逻辑是否正确
            syncTaskService.executeTask(task.getId());
            fail("应该因为数据库连接失败而抛出异常");
        } catch (RuntimeException e) {
            // 预期的异常，因为测试数据库可能不存在
            assertTrue(e.getMessage().contains("表同步失败") || 
                      e.getMessage().contains("连接失败") ||
                      e.getMessage().contains("数据库连接不存在"));
        }
    }

    @Test
    @Transactional
    public void testSyncTaskWithInvalidConnections() {
        // 创建包含无效连接ID的同步任务
        SyncTask task = new SyncTask();
        task.setName("无效连接测试任务");
        task.setSourceConnectionId(999L); // 无效的ID
        task.setTargetConnectionId(999L); // 无效的ID
        task.setTables(Arrays.asList("test_table"));
        
        // 保存任务
        syncTaskMapper.insertTask(task);
        assertNotNull(task.getId());
        
        // 验证任务状态
        assertEquals("PENDING", task.getStatus());
        
        // 尝试执行任务，应该因为连接不存在而失败
        try {
            syncTaskService.executeTask(task.getId());
            fail("应该因为连接不存在而抛出异常");
        } catch (RuntimeException e) {
            // 预期的异常
            assertTrue(e.getMessage().contains("源数据库连接不存在") || 
                      e.getMessage().contains("目标数据库连接不存在"));
        }
    }

    @Test
    @Transactional
    public void testSyncTaskProgressTracking() {
        // 创建源和目标数据库连接
        DbConnection sourceConnection = createTestConnection("进度测试源库", "mysql", "localhost", 3306, "progress_db");
        DbConnection targetConnection = createTestConnection("进度测试目标库", "postgresql", "localhost", 5432, "progress_db");
        
        // 保存连接
        dbConnectionMapper.insertConnection(sourceConnection);
        dbConnectionMapper.insertConnection(targetConnection);
        
        // 创建包含多个表的同步任务
        SyncTask task = new SyncTask();
        task.setName("多表进度测试任务");
        task.setSourceConnectionId(sourceConnection.getId());
        task.setTargetConnectionId(targetConnection.getId());
        task.setTables(Arrays.asList("table1", "table2", "table3"));
        task.setTruncateBeforeSync(false);
        
        // 保存任务
        syncTaskMapper.insertTask(task);
        assertNotNull(task.getId());
        
        // 验证初始状态
        assertEquals("PENDING", task.getStatus());
        assertEquals(0, task.getProgress());
        assertEquals(3, task.getTotalTables());
        assertEquals(0, task.getCompletedTables());
        
        // 尝试执行任务
        try {
            syncTaskService.executeTask(task.getId());
            fail("应该因为数据库连接失败而抛出异常");
        } catch (RuntimeException e) {
            // 预期的异常，但我们可以验证任务状态是否被更新
            System.out.println("预期的异常: " + e.getMessage());
        }
        
        // 验证任务状态（即使失败，也应该有状态更新）
        SyncTask updatedTask = syncTaskMapper.findById(task.getId());
        assertNotNull(updatedTask);
        
        // 验证进度信息
        Map<String, Object> progress = syncTaskService.getTaskProgress(task.getId());
        assertNotNull(progress);
        assertEquals(task.getId(), progress.get("taskId"));
        assertEquals(3, progress.get("totalTables"));
    }

    /**
     * 创建测试数据库连接
     */
    private DbConnection createTestConnection(String name, String dbType, String host, int port, String database) {
        DbConnection connection = new DbConnection();
        connection.setName(name);
        connection.setDbType(dbType);
        connection.setHost(host);
        connection.setPort(port);
        connection.setDatabase(database);
        connection.setUsername("test_user");
        connection.setPassword("test_password");
        connection.setDescription("测试连接");
        connection.setEnabled(true);
        connection.setCreatedAt(java.time.LocalDateTime.now().toString());
        connection.setUpdatedAt(java.time.LocalDateTime.now().toString());
        return connection;
    }
}