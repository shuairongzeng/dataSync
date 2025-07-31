package com.dbsync.dbsync.service;

import com.dbsync.dbsync.entity.QueryResult;
import com.dbsync.dbsync.model.DbConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * QueryService 单元测试
 */
@ExtendWith(MockitoExtension.class)
public class QueryServiceTest {
    
    @Mock
    private DbConnectionService dbConnectionService;
    
    @InjectMocks
    private QueryService queryService;
    
    private DbConnection testConnection;
    
    @BeforeEach
    void setUp() {
        testConnection = new DbConnection();
        testConnection.setId(1L);
        testConnection.setName("测试连接");
        testConnection.setDbType("mysql");
        testConnection.setHost("localhost");
        testConnection.setPort(3306);
        testConnection.setDatabase("test");
        testConnection.setUsername("test");
        testConnection.setPassword("test");
        testConnection.setEnabled(true);
    }
    
    @Test
    void testValidateSql_ValidSql() {
        // 测试有效的SQL
        String validSql = "SELECT * FROM users";
        assertDoesNotThrow(() -> queryService.validateSql(validSql));
    }
    
    @Test
    void testValidateSql_EmptySql() {
        // 测试空SQL
        String emptySql = "";
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> queryService.validateSql(emptySql));
        assertEquals("SQL语句不能为空", exception.getMessage());
    }
    
    @Test
    void testValidateSql_NullSql() {
        // 测试null SQL
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> queryService.validateSql(null));
        assertEquals("SQL语句不能为空", exception.getMessage());
    }
    
    @Test
    void testExecuteQuery_ConnectionNotFound() {
        // 模拟连接不存在的情况
        when(dbConnectionService.getConnectionById(anyLong())).thenReturn(null);
        
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> queryService.executeQuery(1L, "SELECT 1", null));
        assertTrue(exception.getMessage().contains("数据库连接不存在"));
    }
    
    @Test
    void testExecuteQuery_ConnectionDisabled() {
        // 模拟连接被禁用的情况
        testConnection.setEnabled(false);
        when(dbConnectionService.getConnectionById(1L)).thenReturn(testConnection);
        
        RuntimeException exception = assertThrows(RuntimeException.class, 
            () -> queryService.executeQuery(1L, "SELECT 1", null));
        assertTrue(exception.getMessage().contains("数据库连接已禁用"));
    }
    
    @Test
    void testBuildJdbcUrl_MySQL() {
        // 测试MySQL JDBC URL构建
        testConnection.setDbType("mysql");
        String expectedUrl = "jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        when(dbConnectionService.buildJdbcUrl(testConnection)).thenReturn(expectedUrl);
        
        String actualUrl = dbConnectionService.buildJdbcUrl(testConnection);
        assertEquals(expectedUrl, actualUrl);
    }
    
    @Test
    void testBuildJdbcUrl_PostgreSQL() {
        // 测试PostgreSQL JDBC URL构建
        testConnection.setDbType("postgresql");
        String expectedUrl = "jdbc:postgresql://localhost:3306/test";
        when(dbConnectionService.buildJdbcUrl(testConnection)).thenReturn(expectedUrl);
        
        String actualUrl = dbConnectionService.buildJdbcUrl(testConnection);
        assertEquals(expectedUrl, actualUrl);
    }
    
    @Test
    void testColumnInfo_GettersAndSetters() {
        // 测试ColumnInfo类的getter和setter
        QueryService.ColumnInfo columnInfo = new QueryService.ColumnInfo();
        
        columnInfo.setColumnName("test_column");
        columnInfo.setDataType("VARCHAR");
        columnInfo.setColumnSize(255);
        columnInfo.setNullable(true);
        columnInfo.setDefaultValue("default_value");
        columnInfo.setRemarks("test remarks");
        
        assertEquals("test_column", columnInfo.getColumnName());
        assertEquals("VARCHAR", columnInfo.getDataType());
        assertEquals(Integer.valueOf(255), columnInfo.getColumnSize());
        assertTrue(columnInfo.getNullable());
        assertEquals("default_value", columnInfo.getDefaultValue());
        assertEquals("test remarks", columnInfo.getRemarks());
    }
    
    @Test
    void testQueryResult_Constructor() {
        // 测试QueryResult构造函数
        java.util.List<String> columns = java.util.Arrays.asList("id", "name");
        java.util.List<java.util.List<Object>> rows = java.util.Arrays.asList(
            java.util.Arrays.asList(1, "test1"),
            java.util.Arrays.asList(2, "test2")
        );
        
        QueryResult result = new QueryResult(columns, rows, 2, 100L);
        
        assertEquals(columns, result.getColumns());
        assertEquals(rows, result.getRows());
        assertEquals(2, result.getTotalRows());
        assertEquals(Long.valueOf(100), result.getExecutionTime());
    }
    
    @Test
    void testQueryResult_WithMessage() {
        // 测试带消息的QueryResult构造函数
        java.util.List<String> columns = java.util.Arrays.asList("affected_rows");
        java.util.List<java.util.List<Object>> rows = java.util.Arrays.asList(
            java.util.Arrays.asList(5)
        );
        String message = "操作完成，影响 5 行";
        
        QueryResult result = new QueryResult(columns, rows, 1, 50L, message);
        
        assertEquals(columns, result.getColumns());
        assertEquals(rows, result.getRows());
        assertEquals(1, result.getTotalRows());
        assertEquals(Long.valueOf(50), result.getExecutionTime());
        assertEquals(message, result.getMessage());
    }
}
