package com.dbsync.dbsync.service;

import com.dbsync.dbsync.util.SqlQueryAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
// Redis依赖已移除，使用本地缓存

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 字段映射服务测试
 */
@ExtendWith(MockitoExtension.class)
public class FieldMappingServiceTest {
    
    @Mock
    private QueryService queryService;
    
    // Redis相关的Mock已移除，使用本地缓存
    
    @Mock
    private SqlQueryAnalyzer sqlQueryAnalyzer;
    
    @InjectMocks
    private FieldMappingService fieldMappingService;
    
    @BeforeEach
    void setUp() {
        // 初始化字段映射服务
        fieldMappingService.init();
    }
    
    @Test
    public void testGetFieldChineseNames() throws Exception {
        // 准备测试数据
        Long connectionId = 1L;
        String schema = "public";
        Set<String> tableNames = new HashSet<>(Arrays.asList("users", "orders"));
        
        // 模拟users表的字段信息
        List<QueryService.ColumnInfo> usersColumns = Arrays.asList(
            createColumnInfo("id", "INTEGER", "用户ID"),
            createColumnInfo("name", "VARCHAR", "用户姓名"),
            createColumnInfo("email", "VARCHAR", "邮箱地址")
        );
        
        // 模拟orders表的字段信息
        List<QueryService.ColumnInfo> ordersColumns = Arrays.asList(
            createColumnInfo("id", "INTEGER", "订单ID"),
            createColumnInfo("user_id", "INTEGER", "用户ID"),
            createColumnInfo("total_amount", "DECIMAL", "订单总额")
        );
        
        // 配置mock
        when(queryService.getTableColumns(connectionId, "users", schema))
            .thenReturn(usersColumns);
        when(queryService.getTableColumns(connectionId, "orders", schema))
            .thenReturn(ordersColumns);
        
        // 执行测试
        Map<String, String> result = fieldMappingService.getFieldChineseNames(connectionId, tableNames, schema);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(6, result.size()); // 6个字段
        
        // 验证字段映射
        assertEquals("用户ID", result.get("id"));
        assertEquals("用户姓名", result.get("name"));
        assertEquals("邮箱地址", result.get("email"));
        assertEquals("用户ID", result.get("user_id"));
        assertEquals("订单总额", result.get("total_amount"));
        
        // 验证带表名前缀的映射
        assertEquals("用户ID", result.get("users.id"));
        assertEquals("订单ID", result.get("orders.id"));
    }
    
    @Test
    public void testGetQueryFieldDisplayNames_SimpleQuery() throws Exception {
        // 准备测试数据
        Long connectionId = 1L;
        String sql = "SELECT id, name, email FROM users";
        String schema = "public";
        List<String> resultColumns = Arrays.asList("id", "name", "email");
        
        // 模拟SQL解析结果
        SqlQueryAnalyzer.QueryAnalysisResult analysisResult = new SqlQueryAnalyzer.QueryAnalysisResult();
        analysisResult.setComplexQuery(false);
        analysisResult.setTables(new HashSet<>(Arrays.asList("users")));
        
        // 创建字段信息
        List<SqlQueryAnalyzer.FieldInfo> fields = new ArrayList<>();
        fields.add(createFieldInfo("id", null, "users"));
        fields.add(createFieldInfo("name", null, "users"));
        fields.add(createFieldInfo("email", null, "users"));
        analysisResult.setFields(fields);
        
        // 模拟表字段信息
        List<QueryService.ColumnInfo> columns = Arrays.asList(
            createColumnInfo("id", "INTEGER", "用户ID"),
            createColumnInfo("name", "VARCHAR", "用户姓名"),
            createColumnInfo("email", "VARCHAR", "邮箱地址")
        );
        
        // 配置mock
        when(sqlQueryAnalyzer.analyzeQuery(sql)).thenReturn(analysisResult);
        when(queryService.getTableColumns(connectionId, "users", schema)).thenReturn(columns);
        
        // 执行测试
        Map<String, String> result = fieldMappingService.getQueryFieldDisplayNames(
            connectionId, sql, schema, resultColumns);
        
        // 验证结果
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("用户ID", result.get("id"));
        assertEquals("用户姓名", result.get("name"));
        assertEquals("邮箱地址", result.get("email"));
    }
    
    @Test
    public void testGetQueryFieldDisplayNames_ComplexQuery() throws Exception {
        // 准备测试数据
        Long connectionId = 1L;
        String sql = "SELECT COUNT(*) as total FROM users WHERE id IN (SELECT user_id FROM orders)";
        String schema = "public";
        List<String> resultColumns = Arrays.asList("total");
        
        // 模拟SQL解析结果（复杂查询）
        SqlQueryAnalyzer.QueryAnalysisResult analysisResult = new SqlQueryAnalyzer.QueryAnalysisResult();
        analysisResult.setComplexQuery(true);
        
        // 模拟获取所有表
        when(sqlQueryAnalyzer.analyzeQuery(sql)).thenReturn(analysisResult);
        when(queryService.getTables(connectionId, schema)).thenReturn(Arrays.asList("users", "orders"));
        
        // 配置字段信息（模拟找不到匹配）
        when(queryService.getTableColumns(eq(connectionId), anyString(), eq(schema)))
            .thenReturn(Arrays.asList(createColumnInfo("id", "INTEGER", "ID")));
        
        // 执行测试
        Map<String, String> result = fieldMappingService.getQueryFieldDisplayNames(
            connectionId, sql, schema, resultColumns);
        
        // 验证结果（复杂查询可能找不到匹配）
        assertNotNull(result);
        // total是计算字段，可能没有对应的数据库字段
    }
    
    @Test
    public void testCacheOperations() {
        // 测试本地缓存功能
        // 由于缓存方法是私有的，我们通过公共接口测试缓存功能
        Long connectionId = 1L;
        String schema = "public";
        Set<String> tableNames = Collections.singleton("users");
        
        // 准备测试数据
        List<QueryService.ColumnInfo> columns = Arrays.asList(
            createColumnInfo("id", "INTEGER", "用户ID"),
            createColumnInfo("name", "VARCHAR", "用户姓名")
        );
        
        // 配置mock
        when(queryService.getTableColumns(connectionId, "users", schema)).thenReturn(columns);
        
        // 第一次调用，应该查询数据库并缓存
        Map<String, String> result1 = fieldMappingService.getFieldChineseNames(connectionId, tableNames, schema);
        
        // 第二次调用，应该从缓存获取
        Map<String, String> result2 = fieldMappingService.getFieldChineseNames(connectionId, tableNames, schema);
        
        // 验证结果一致
        assertEquals(result1, result2);
        assertEquals("用户ID", result1.get("id"));
        assertEquals("用户姓名", result1.get("name"));
    }
    
    @Test
    public void testClearConnectionCache() {
        Long connectionId = 1L;
        String schema = "public";
        Set<String> tableNames = new HashSet<>(Arrays.asList("users", "orders"));
        
        // 先添加一些缓存数据
        List<QueryService.ColumnInfo> usersColumns = Arrays.asList(
            createColumnInfo("id", "INTEGER", "用户ID"),
            createColumnInfo("name", "VARCHAR", "用户姓名")
        );
        List<QueryService.ColumnInfo> ordersColumns = Arrays.asList(
            createColumnInfo("id", "INTEGER", "订单ID"),
            createColumnInfo("user_id", "INTEGER", "用户ID")
        );
        
        when(queryService.getTableColumns(connectionId, "users", schema)).thenReturn(usersColumns);
        when(queryService.getTableColumns(connectionId, "orders", schema)).thenReturn(ordersColumns);
        
        // 先获取数据以填充缓存
        fieldMappingService.getFieldChineseNames(connectionId, tableNames, schema);
        
        // 清除缓存
        fieldMappingService.clearConnectionCache(connectionId);
        
        // 验证缓存已被清除 - 再次获取应该重新查询数据库
        fieldMappingService.getFieldChineseNames(connectionId, tableNames, schema);
        
        // 验证数据库被查询了两次（第一次填充缓存，清除后第二次重新查询）
        verify(queryService, times(2)).getTableColumns(connectionId, "users", schema);
        verify(queryService, times(2)).getTableColumns(connectionId, "orders", schema);
    }
    
    // 辅助方法
    private QueryService.ColumnInfo createColumnInfo(String name, String type, String remarks) {
        QueryService.ColumnInfo columnInfo = new QueryService.ColumnInfo();
        columnInfo.setColumnName(name);
        columnInfo.setDataType(type);
        columnInfo.setRemarks(remarks);
        return columnInfo;
    }
    
    private SqlQueryAnalyzer.FieldInfo createFieldInfo(String fieldName, String alias, String tableName) {
        SqlQueryAnalyzer.FieldInfo fieldInfo = new SqlQueryAnalyzer.FieldInfo();
        fieldInfo.setFieldName(fieldName);
        fieldInfo.setAlias(alias);
        fieldInfo.setTableName(tableName);
        return fieldInfo;
    }
}