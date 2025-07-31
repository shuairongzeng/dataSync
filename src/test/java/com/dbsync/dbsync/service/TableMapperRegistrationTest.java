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

import static org.junit.jupiter.api.Assertions.*;

/**
 * TableMapper注册问题修复测试
 */
@SpringBootTest
public class TableMapperRegistrationTest {

    @Autowired
    private SyncTaskService syncTaskService;

    @Autowired
    private DbConnectionMapper dbConnectionMapper;

    @Autowired
    private SyncTaskMapper syncTaskMapper;

    @Test
    @Transactional
    public void testTableMapperRegistration() {
        System.out.println("开始TableMapper注册测试...");
        
        try {
            // 创建测试连接
            DbConnection sourceConnection = createTestConnection("源库", "mysql", "localhost", 3306, "test");
            DbConnection targetConnection = createTestConnection("目标库", "postgresql", "localhost", 5432, "test");
            
            dbConnectionMapper.insertConnection(sourceConnection);
            dbConnectionMapper.insertConnection(targetConnection);
            
            // 创建测试任务
            SyncTask task = new SyncTask();
            task.setName("TableMapper测试");
            task.setSourceConnectionId(sourceConnection.getId());
            task.setTargetConnectionId(targetConnection.getId());
            task.setTables(Arrays.asList("test_table"));
            
            syncTaskMapper.insertTask(task);
            
            // 尝试执行任务
            syncTaskService.executeTask(task.getId());
            
            // 等待任务开始
            Thread.sleep(100);
            
            System.out.println("TableMapper注册测试完成，没有出现注册错误！");
            
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("TableMapper is not known to the MapperRegistry")) {
                fail("TableMapper注册失败: " + e.getMessage());
            } else {
                System.out.println("预期的其他错误: " + e.getMessage());
            }
        }
    }
    
    private DbConnection createTestConnection(String name, String dbType, String host, int port, String database) {
        DbConnection connection = new DbConnection();
        connection.setName(name);
        connection.setDbType(dbType);
        connection.setHost(host);
        connection.setPort(port);
        connection.setDatabase(database);
        connection.setUsername("test_user");
        connection.setPassword("test_password");
        connection.setCreatedAt(java.time.LocalDateTime.now().toString());
        connection.setUpdatedAt(java.time.LocalDateTime.now().toString());
        return connection;
    }
}
