package com.dbsync.dbsync.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.dbsync.dbsync.model.SyncTask;
import com.dbsync.dbsync.service.SyncTaskService;
import com.dbsync.dbsync.service.DbConnectionService;
import com.dbsync.dbsync.model.DbConnection;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 同步任务管理功能测试
 */
@SpringBootTest
public class SyncTaskControllerTest {

    @Autowired
    private SyncTaskService syncTaskService;

    @Autowired
    private DbConnectionService dbConnectionService;

    @Test
    @Transactional
    public void testCreateSyncTask() {
        // 首先创建源和目标数据库连接
        DbConnection sourceConnection = createTestConnection("源测试库", "mysql", "localhost", 3306, "test_db");
        DbConnection targetConnection = createTestConnection("目标测试库", "postgresql", "localhost", 5432, "target_db");
        
        DbConnection savedSourceConnection = dbConnectionService.createConnection(sourceConnection);
        DbConnection savedTargetConnection = dbConnectionService.createConnection(targetConnection);
        
        // 创建同步任务
        SyncTask task = new SyncTask();
        task.setName("测试同步任务");
        task.setSourceConnectionId(savedSourceConnection.getId());
        task.setTargetConnectionId(savedTargetConnection.getId());
        task.setSourceSchemaName("public");
        task.setTargetSchemaName("backup");
        task.setTables(Arrays.asList("users", "products", "orders"));
        task.setTruncateBeforeSync(true);
        
        SyncTask savedTask = syncTaskService.createTask(task);
        
        // 验证保存结果
        assertNotNull(savedTask.getId());
        assertEquals("测试同步任务", savedTask.getName());
        assertEquals(savedSourceConnection.getId(), savedTask.getSourceConnectionId());
        assertEquals(savedTargetConnection.getId(), savedTask.getTargetConnectionId());
        assertEquals("public", savedTask.getSourceSchemaName());
        assertEquals("backup", savedTask.getTargetSchemaName());
        assertTrue(savedTask.getTruncateBeforeSync());
        assertEquals("PENDING", savedTask.getStatus());
        assertEquals(3, savedTask.getTotalTables());
        assertNotNull(savedTask.getCreatedAt());
        assertNotNull(savedTask.getUpdatedAt());
    }

    @Test
    @Transactional
    public void testGetSyncTaskById() {
        // 创建源和目标连接
        DbConnection sourceConnection = createTestConnection("源库", "mysql", "localhost", 3306, "source");
        DbConnection targetConnection = createTestConnection("目标库", "postgresql", "localhost", 5432, "target");
        
        DbConnection savedSourceConnection = dbConnectionService.createConnection(sourceConnection);
        DbConnection savedTargetConnection = dbConnectionService.createConnection(targetConnection);
        
        // 创建任务
        SyncTask task = new SyncTask();
        task.setName("查询测试任务");
        task.setSourceConnectionId(savedSourceConnection.getId());
        task.setTargetConnectionId(savedTargetConnection.getId());
        task.setTables(Arrays.asList("test_table"));
        
        SyncTask savedTask = syncTaskService.createTask(task);
        
        // 根据ID获取任务
        SyncTask foundTask = syncTaskService.getTaskById(savedTask.getId());
        
        // 验证查询结果
        assertNotNull(foundTask);
        assertEquals(savedTask.getId(), foundTask.getId());
        assertEquals("查询测试任务", foundTask.getName());
        assertEquals(savedSourceConnection.getId(), foundTask.getSourceConnectionId());
        assertEquals(savedTargetConnection.getId(), foundTask.getTargetConnectionId());
    }

    @Test
    @Transactional
    public void testUpdateSyncTask() {
        // 创建源和目标连接
        DbConnection sourceConnection = createTestConnection("源库", "mysql", "localhost", 3306, "source");
        DbConnection targetConnection = createTestConnection("目标库", "postgresql", "localhost", 5432, "target");
        
        DbConnection savedSourceConnection = dbConnectionService.createConnection(sourceConnection);
        DbConnection savedTargetConnection = dbConnectionService.createConnection(targetConnection);
        
        // 创建任务
        SyncTask task = new SyncTask();
        task.setName("原始任务");
        task.setSourceConnectionId(savedSourceConnection.getId());
        task.setTargetConnectionId(savedTargetConnection.getId());
        task.setTables(Arrays.asList("table1"));
        
        SyncTask savedTask = syncTaskService.createTask(task);
        
        // 更新任务信息
        savedTask.setName("更新后的任务");
        savedTask.setSourceSchemaName("new_schema");
        savedTask.setTables(Arrays.asList("table1", "table2", "table3"));
        savedTask.setTruncateBeforeSync(false);
        
        SyncTask updatedTask = syncTaskService.updateTask(savedTask.getId(), savedTask);
        
        // 验证更新结果
        assertEquals("更新后的任务", updatedTask.getName());
        assertEquals("new_schema", updatedTask.getSourceSchemaName());
        assertEquals(3, updatedTask.getTotalTables());
        assertFalse(updatedTask.getTruncateBeforeSync());
        assertEquals(savedTask.getId(), updatedTask.getId());
    }

    @Test
    @Transactional
    public void testDeleteSyncTask() {
        // 创建源和目标连接
        DbConnection sourceConnection = createTestConnection("源库", "mysql", "localhost", 3306, "source");
        DbConnection targetConnection = createTestConnection("目标库", "postgresql", "localhost", 5432, "target");
        
        DbConnection savedSourceConnection = dbConnectionService.createConnection(sourceConnection);
        DbConnection savedTargetConnection = dbConnectionService.createConnection(targetConnection);
        
        // 创建任务
        SyncTask task = new SyncTask();
        task.setName("待删除任务");
        task.setSourceConnectionId(savedSourceConnection.getId());
        task.setTargetConnectionId(savedTargetConnection.getId());
        task.setTables(Arrays.asList("test_table"));
        
        SyncTask savedTask = syncTaskService.createTask(task);
        Long taskId = savedTask.getId();
        
        // 删除任务
        boolean result = syncTaskService.deleteTask(taskId);
        
        // 验证删除结果
        assertTrue(result);
        
        // 验证任务确实已被删除
        SyncTask deletedTask = syncTaskService.getTaskById(taskId);
        assertNull(deletedTask);
    }

    @Test
    @Transactional
    public void testGetAllTasks() {
        // 创建源和目标连接
        DbConnection sourceConnection = createTestConnection("源库", "mysql", "localhost", 3306, "source");
        DbConnection targetConnection = createTestConnection("目标库", "postgresql", "localhost", 5432, "target");
        
        DbConnection savedSourceConnection = dbConnectionService.createConnection(sourceConnection);
        DbConnection savedTargetConnection = dbConnectionService.createConnection(targetConnection);
        
        // 创建多个任务
        SyncTask task1 = new SyncTask();
        task1.setName("任务1");
        task1.setSourceConnectionId(savedSourceConnection.getId());
        task1.setTargetConnectionId(savedTargetConnection.getId());
        task1.setTables(Arrays.asList("table1"));
        
        SyncTask task2 = new SyncTask();
        task2.setName("任务2");
        task2.setSourceConnectionId(savedSourceConnection.getId());
        task2.setTargetConnectionId(savedTargetConnection.getId());
        task2.setTables(Arrays.asList("table2"));
        
        syncTaskService.createTask(task1);
        syncTaskService.createTask(task2);
        
        // 获取所有任务
        List<SyncTask> allTasks = syncTaskService.getAllTasks();
        
        // 验证查询结果
        assertNotNull(allTasks);
        assertTrue(allTasks.size() >= 2);
        
        // 验证任务名称存在
        boolean foundTask1 = allTasks.stream().anyMatch(t -> "任务1".equals(t.getName()));
        boolean foundTask2 = allTasks.stream().anyMatch(t -> "任务2".equals(t.getName()));
        assertTrue(foundTask1);
        assertTrue(foundTask2);
    }

    @Test
    @Transactional
    public void testTaskNameUniqueness() {
        // 创建源和目标连接
        DbConnection sourceConnection = createTestConnection("源库", "mysql", "localhost", 3306, "source");
        DbConnection targetConnection = createTestConnection("目标库", "postgresql", "localhost", 5432, "target");
        
        DbConnection savedSourceConnection = dbConnectionService.createConnection(sourceConnection);
        DbConnection savedTargetConnection = dbConnectionService.createConnection(targetConnection);
        
        // 创建第一个任务
        SyncTask task1 = new SyncTask();
        task1.setName("唯一任务名");
        task1.setSourceConnectionId(savedSourceConnection.getId());
        task1.setTargetConnectionId(savedTargetConnection.getId());
        task1.setTables(Arrays.asList("table1"));
        
        SyncTask savedTask1 = syncTaskService.createTask(task1);
        
        // 尝试创建同名任务
        SyncTask task2 = new SyncTask();
        task2.setName("唯一任务名"); // 相同名称
        task2.setSourceConnectionId(savedSourceConnection.getId());
        task2.setTargetConnectionId(savedTargetConnection.getId());
        task2.setTables(Arrays.asList("table2"));
        
        // 应该抛出异常
        assertThrows(RuntimeException.class, () -> {
            syncTaskService.createTask(task2);
        });
    }

    @Test
    @Transactional
    public void testGetTaskProgress() {
        // 创建源和目标连接
        DbConnection sourceConnection = createTestConnection("源库", "mysql", "localhost", 3306, "source");
        DbConnection targetConnection = createTestConnection("目标库", "postgresql", "localhost", 5432, "target");
        
        DbConnection savedSourceConnection = dbConnectionService.createConnection(sourceConnection);
        DbConnection savedTargetConnection = dbConnectionService.createConnection(targetConnection);
        
        // 创建任务
        SyncTask task = new SyncTask();
        task.setName("进度测试任务");
        task.setSourceConnectionId(savedSourceConnection.getId());
        task.setTargetConnectionId(savedTargetConnection.getId());
        task.setTables(Arrays.asList("table1", "table2"));
        task.setStatus("RUNNING");
        task.setProgress(50);
        task.setCompletedTables(1);
        
        SyncTask savedTask = syncTaskService.createTask(task);
        
        // 获取任务进度
        Map<String, Object> progress = syncTaskService.getTaskProgress(savedTask.getId());

        // 验证进度信息
        assertNotNull(progress);
        assertEquals(savedTask.getId(), progress.get("taskId"));
        assertEquals("RUNNING", progress.get("status"));
        assertEquals(50, progress.get("progress"));
        assertEquals(2, progress.get("totalTables"));
        assertEquals(1, progress.get("completedTables"));
    }

    @Test
    @Transactional
    public void testGetTaskLogs() {
        // 创建源和目标连接
        DbConnection sourceConnection = createTestConnection("源库", "mysql", "localhost", 3306, "source");
        DbConnection targetConnection = createTestConnection("目标库", "postgresql", "localhost", 5432, "target");
        
        DbConnection savedSourceConnection = dbConnectionService.createConnection(sourceConnection);
        DbConnection savedTargetConnection = dbConnectionService.createConnection(targetConnection);
        
        // 创建任务
        SyncTask task = new SyncTask();
        task.setName("日志测试任务");
        task.setSourceConnectionId(savedSourceConnection.getId());
        task.setTargetConnectionId(savedTargetConnection.getId());
        task.setTables(Arrays.asList("table1"));
        
        SyncTask savedTask = syncTaskService.createTask(task);
        
        // 获取任务日志
        List<String> logs = syncTaskService.getTaskLogs(savedTask.getId());
        
        // 验证日志列表
        assertNotNull(logs);
        // 日志可能为空，因为任务还没有执行
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
        return connection;
    }
}