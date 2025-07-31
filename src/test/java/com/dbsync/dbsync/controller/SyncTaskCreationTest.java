package com.dbsync.dbsync.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.dbsync.dbsync.model.SyncTaskRequest;
import com.dbsync.dbsync.model.SyncTask;
import com.dbsync.dbsync.service.SyncTaskService;
import com.dbsync.dbsync.service.DbConnectionService;
import com.dbsync.dbsync.model.DbConnection;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 同步任务创建功能测试
 */
@SpringBootTest
public class SyncTaskCreationTest {

    @Autowired
    private SyncTaskService syncTaskService;

    @Autowired
    private DbConnectionService dbConnectionService;

    @Test
    @Transactional
    public void testSyncTaskRequestConversion() {
        // 创建测试数据库连接
        DbConnection sourceConnection = createTestConnection("源Oracle库", "oracle", "localhost", 1521, "source_db");
        DbConnection targetConnection = createTestConnection("目标海量库", "vastbase", "localhost", 5432, "target_db");
        
        DbConnection savedSourceConnection = dbConnectionService.createConnection(sourceConnection);
        DbConnection savedTargetConnection = dbConnectionService.createConnection(targetConnection);
        
        // 创建同步任务请求对象
        SyncTaskRequest request = new SyncTaskRequest();
        request.setName("从oracle同步到海量数据库任务");
        request.setSourceConnectionId(savedSourceConnection.getId().toString());
        request.setTargetConnectionId(savedTargetConnection.getId().toString());
        request.setSourceSchemaName("");
        request.setTargetSchemaName("cqdm_basic");
        request.setTables(new String[]{"BMC_BD_FIX_SOURCE"});
        request.setTruncateBeforeSync(true);
        request.setStatus("PENDING");
        
        // 转换为SyncTask实体类
        SyncTask task = request.toSyncTask();
        
        // 验证转换结果
        assertEquals("从oracle同步到海量数据库任务", task.getName());
        assertEquals(savedSourceConnection.getId(), task.getSourceConnectionId());
        assertEquals(savedTargetConnection.getId(), task.getTargetConnectionId());
        assertEquals("", task.getSourceSchemaName());
        assertEquals("cqdm_basic", task.getTargetSchemaName());
        assertEquals("[\"BMC_BD_FIX_SOURCE\"]", task.getTables());
        assertTrue(task.getTruncateBeforeSync());
        assertEquals("PENDING", task.getStatus());
        assertEquals(1, task.getTotalTables());
        
        // 测试创建任务
        SyncTask createdTask = syncTaskService.createTask(task);
        assertNotNull(createdTask.getId());
        assertEquals("从oracle同步到海量数据库任务", createdTask.getName());
        assertEquals("[\"BMC_BD_FIX_SOURCE\"]", createdTask.getTables());
    }

    @Test
    @Transactional
    public void testSyncTaskRequestWithMultipleTables() {
        // 创建测试数据库连接
        DbConnection sourceConnection = createTestConnection("源MySQL库", "mysql", "localhost", 3306, "source_db");
        DbConnection targetConnection = createTestConnection("目标PG库", "postgresql", "localhost", 5432, "target_db");
        
        DbConnection savedSourceConnection = dbConnectionService.createConnection(sourceConnection);
        DbConnection savedTargetConnection = dbConnectionService.createConnection(targetConnection);
        
        // 创建包含多个表的同步任务请求
        SyncTaskRequest request = new SyncTaskRequest();
        request.setName("多表同步任务");
        request.setSourceConnectionId(savedSourceConnection.getId().toString());
        request.setTargetConnectionId(savedTargetConnection.getId().toString());
        request.setTables(new String[]{"users", "products", "orders", "categories"});
        request.setTruncateBeforeSync(false);
        
        // 转换为SyncTask实体类
        SyncTask task = request.toSyncTask();
        
        // 验证转换结果
        assertEquals("多表同步任务", task.getName());
        assertEquals("[\"users\",\"products\",\"orders\",\"categories\"]", task.getTables());
        assertEquals(4, task.getTotalTables());
        assertFalse(task.getTruncateBeforeSync());
    }

    @Test
    @Transactional
    public void testSyncTaskRequestWithEmptyTables() {
        // 创建测试数据库连接
        DbConnection sourceConnection = createTestConnection("源库", "mysql", "localhost", 3306, "source_db");
        DbConnection targetConnection = createTestConnection("目标库", "postgresql", "localhost", 5432, "target_db");
        
        DbConnection savedSourceConnection = dbConnectionService.createConnection(sourceConnection);
        DbConnection savedTargetConnection = dbConnectionService.createConnection(targetConnection);
        
        // 创建空表列表的同步任务请求
        SyncTaskRequest request = new SyncTaskRequest();
        request.setName("空表同步任务");
        request.setSourceConnectionId(savedSourceConnection.getId().toString());
        request.setTargetConnectionId(savedTargetConnection.getId().toString());
        request.setTables(new String[]{});
        request.setTruncateBeforeSync(false);
        
        // 转换为SyncTask实体类
        SyncTask task = request.toSyncTask();
        
        // 验证转换结果
        assertEquals("空表同步任务", task.getName());
        assertEquals("[]", task.getTables());
        assertEquals(0, task.getTotalTables());
        assertFalse(task.getTruncateBeforeSync());
    }

    @Test
    @Transactional
    public void testSyncTaskRequestWithNullTables() {
        // 创建测试数据库连接
        DbConnection sourceConnection = createTestConnection("源库", "mysql", "localhost", 3306, "source_db");
        DbConnection targetConnection = createTestConnection("目标库", "postgresql", "localhost", 5432, "target_db");
        
        DbConnection savedSourceConnection = dbConnectionService.createConnection(sourceConnection);
        DbConnection savedTargetConnection = dbConnectionService.createConnection(targetConnection);
        
        // 创建null表列表的同步任务请求
        SyncTaskRequest request = new SyncTaskRequest();
        request.setName("Null表同步任务");
        request.setSourceConnectionId(savedSourceConnection.getId().toString());
        request.setTargetConnectionId(savedTargetConnection.getId().toString());
        request.setTables(null);
        request.setTruncateBeforeSync(false);
        
        // 转换为SyncTask实体类
        SyncTask task = request.toSyncTask();
        
        // 验证转换结果
        assertEquals("Null表同步任务", task.getName());
        assertEquals("[]", task.getTables());
        assertEquals(0, task.getTotalTables());
        assertFalse(task.getTruncateBeforeSync());
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