package com.dbsync.dbsync.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.dbsync.dbsync.model.DbConnection;
import com.dbsync.dbsync.service.DbConnectionService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 数据库连接管理功能测试
 */
@SpringBootTest
public class DbConnectionControllerTest {

    @Autowired
    private DbConnectionService dbConnectionService;

    @Test
    @Transactional
    public void testCreateConnection() {
        // 创建测试连接
        DbConnection connection = new DbConnection();
        connection.setName("测试连接");
        connection.setDbType("mysql");
        connection.setHost("localhost");
        connection.setPort(3306);
        connection.setDatabase("test_db");
        connection.setUsername("test_user");
        connection.setPassword("test_pass");
        connection.setDescription("测试数据库连接");

        // 保存连接
        DbConnection savedConnection = dbConnectionService.createConnection(connection);
        
        // 验证保存结果
        assertNotNull(savedConnection.getId());
        assertEquals("测试连接", savedConnection.getName());
        assertEquals("mysql", savedConnection.getDbType());
        assertTrue(savedConnection.getEnabled());
        assertNotNull(savedConnection.getCreatedAt());
        assertNotNull(savedConnection.getUpdatedAt());
    }

    @Test
    @Transactional
    public void testGetConnectionById() {
        // 先创建一个连接
        DbConnection connection = new DbConnection();
        connection.setName("测试连接2");
        connection.setDbType("postgresql");
        connection.setHost("localhost");
        connection.setPort(5432);
        connection.setDatabase("test_db");
        connection.setUsername("test_user");
        connection.setPassword("test_pass");
        
        DbConnection savedConnection = dbConnectionService.createConnection(connection);
        
        // 根据ID获取连接
        DbConnection foundConnection = dbConnectionService.getConnectionById(savedConnection.getId());
        
        // 验证查询结果
        assertNotNull(foundConnection);
        assertEquals(savedConnection.getId(), foundConnection.getId());
        assertEquals("测试连接2", foundConnection.getName());
        assertEquals("postgresql", foundConnection.getDbType());
    }

    @Test
    @Transactional
    public void testUpdateConnection() {
        // 先创建一个连接
        DbConnection connection = new DbConnection();
        connection.setName("原始连接");
        connection.setDbType("mysql");
        connection.setHost("localhost");
        connection.setPort(3306);
        connection.setDatabase("test_db");
        connection.setUsername("test_user");
        connection.setPassword("test_pass");
        
        DbConnection savedConnection = dbConnectionService.createConnection(connection);
        
        // 更新连接信息
        savedConnection.setName("更新后的连接");
        savedConnection.setDescription("更新描述");
        
        DbConnection updatedConnection = dbConnectionService.updateConnection(savedConnection.getId(), savedConnection);
        
        // 验证更新结果
        assertEquals("更新后的连接", updatedConnection.getName());
        assertEquals("更新描述", updatedConnection.getDescription());
        assertEquals(savedConnection.getId(), updatedConnection.getId());
    }

    @Test
    @Transactional
    public void testDeleteConnection() {
        // 先创建一个连接
        DbConnection connection = new DbConnection();
        connection.setName("待删除连接");
        connection.setDbType("mysql");
        connection.setHost("localhost");
        connection.setPort(3306);
        connection.setDatabase("test_db");
        connection.setUsername("test_user");
        connection.setPassword("test_pass");
        
        DbConnection savedConnection = dbConnectionService.createConnection(connection);
        Long connectionId = savedConnection.getId();
        
        // 删除连接
        boolean result = dbConnectionService.deleteConnection(connectionId);
        
        // 验证删除结果
        assertTrue(result);
        
        // 验证连接确实已被删除
        DbConnection deletedConnection = dbConnectionService.getConnectionById(connectionId);
        assertNull(deletedConnection);
    }

    @Test
    @Transactional
    public void testConnectionNameUniqueness() {
        // 创建第一个连接
        DbConnection connection1 = new DbConnection();
        connection1.setName("唯一名称");
        connection1.setDbType("mysql");
        connection1.setHost("localhost");
        connection1.setPort(3306);
        connection1.setDatabase("test_db1");
        connection1.setUsername("test_user1");
        connection1.setPassword("test_pass1");
        
        DbConnection savedConnection1 = dbConnectionService.createConnection(connection1);
        
        // 尝试创建同名连接
        DbConnection connection2 = new DbConnection();
        connection2.setName("唯一名称"); // 相同名称
        connection2.setDbType("postgresql");
        connection2.setHost("localhost");
        connection2.setPort(5432);
        connection2.setDatabase("test_db2");
        connection2.setUsername("test_user2");
        connection2.setPassword("test_pass2");
        
        // 应该抛出异常
        assertThrows(RuntimeException.class, () -> {
            dbConnectionService.createConnection(connection2);
        });
    }
}