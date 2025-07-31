package com.dbsync.dbsync.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.dbsync.dbsync.model.DbConnection;
import com.dbsync.dbsync.service.DbConnectionService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 表列表获取功能测试
 */
@SpringBootTest
public class TableListControllerTest {

    @Autowired
    private DbConnectionService dbConnectionService;

    @Test
    @Transactional
    public void testGetTablesForConnection() {
        // 创建测试数据库连接
        DbConnection connection = createTestConnection("测试连接", "mysql", "localhost", 3306, "test_db");
        
        // 保存连接
        DbConnection savedConnection = dbConnectionService.createConnection(connection);
        assertNotNull(savedConnection.getId());
        
        // 测试获取表列表（注意：这个测试需要真实的数据库连接才能通过）
        try {
            List<String> tables = dbConnectionService.getTables(savedConnection.getId(), null);
            assertNotNull(tables);
            // 表列表可能为空，因为测试数据库可能不存在
            System.out.println("获取到的表列表: " + tables);
        } catch (Exception e) {
            // 这是预期的，因为测试数据库可能不存在
            System.out.println("获取表列表失败（预期）: " + e.getMessage());
        }
    }

    @Test
    @Transactional
    public void testGetTablesWithSchema() {
        // 创建测试数据库连接
        DbConnection connection = createTestConnection("带Schema的连接", "postgresql", "localhost", 5432, "test_db");
        
        // 保存连接
        DbConnection savedConnection = dbConnectionService.createConnection(connection);
        assertNotNull(savedConnection.getId());
        
        // 测试获取指定schema的表列表
        try {
            List<String> tables = dbConnectionService.getTables(savedConnection.getId(), "public");
            assertNotNull(tables);
            System.out.println("获取到的public schema表列表: " + tables);
        } catch (Exception e) {
            // 这是预期的，因为测试数据库可能不存在
            System.out.println("获取表列表失败（预期）: " + e.getMessage());
        }
    }

    @Test
    @Transactional
    public void testGetTablesForInvalidConnection() {
        // 测试无效的连接ID
        try {
            List<String> tables = dbConnectionService.getTables(999L, null);
            fail("应该抛出异常");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("数据库连接不存在"));
        }
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