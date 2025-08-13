package com.dbsync.dbsync.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 增强查询结果测试
 */
public class EnhancedQueryResultTest {
    
    private QueryResult originalResult;
    private EnhancedQueryResult enhancedResult;
    
    @BeforeEach
    void setUp() {
        // 创建原始查询结果
        List<String> columns = Arrays.asList("id", "name", "email", "created_at");
        List<List<Object>> rows = Arrays.asList(
            Arrays.asList(1, "张三", "zhangsan@example.com", "2023-01-01"),
            Arrays.asList(2, "李四", "lisi@example.com", "2023-01-02")
        );
        
        originalResult = new QueryResult(columns, rows, 2, 100L);
        enhancedResult = new EnhancedQueryResult(originalResult);
    }
    
    @Test
    public void testConstructorFromOriginalResult() {
        // 验证基础属性
        assertEquals(originalResult.getColumns(), enhancedResult.getColumns());
        assertEquals(originalResult.getRows(), enhancedResult.getRows());
        assertEquals(originalResult.getTotalRows(), enhancedResult.getTotalRows());
        assertEquals(originalResult.getExecutionTime(), enhancedResult.getExecutionTime());
        
        // 验证增强属性
        assertNotNull(enhancedResult.getColumnDisplayNames());
        assertNotNull(enhancedResult.getColumnsMetadata());
        assertTrue(enhancedResult.isEnableChineseColumnNames());
        assertEquals(4, enhancedResult.getColumnsMetadata().size());
    }
    
    @Test
    public void testSetFieldDisplayName() {
        // 设置字段显示名称
        enhancedResult.setFieldDisplayName("id", "用户ID", "用户ID");
        enhancedResult.setFieldDisplayName("name", "用户姓名", "用户姓名");
        enhancedResult.setFieldDisplayName("email", "邮箱地址", "邮箱地址");
        
        // 验证显示名称
        assertEquals("用户ID", enhancedResult.getColumnDisplayName("id"));
        assertEquals("用户姓名", enhancedResult.getColumnDisplayName("name"));
        assertEquals("邮箱地址", enhancedResult.getColumnDisplayName("email"));
        assertEquals("created_at", enhancedResult.getColumnDisplayName("created_at")); // 未设置，返回原始名
        
        // 验证是否有中文名
        assertTrue(enhancedResult.hasChineseColumnName("id"));
        assertTrue(enhancedResult.hasChineseColumnName("name"));
        assertTrue(enhancedResult.hasChineseColumnName("email"));
        assertFalse(enhancedResult.hasChineseColumnName("created_at"));
    }
    
    @Test
    public void testBatchSetFieldDisplayNames() {
        // 批量设置字段显示名称
        Map<String, String> displayNames = new HashMap<>();
        displayNames.put("id", "用户ID");
        displayNames.put("name", "用户姓名");
        displayNames.put("email", "邮箱地址");
        
        enhancedResult.setFieldDisplayNames(displayNames);
        
        // 验证结果
        assertEquals(3, enhancedResult.getChineseColumnCount());
        assertEquals(0.75, enhancedResult.getChineseColumnCoverage(), 0.01); // 3/4 = 0.75
    }
    
    @Test
    public void testGetDisplayColumns() {
        // 设置部分字段的中文名
        enhancedResult.setFieldDisplayName("id", "用户ID", "用户ID");
        enhancedResult.setFieldDisplayName("name", "用户姓名", "用户姓名");
        
        List<String> displayColumns = enhancedResult.getDisplayColumns();
        
        assertEquals(4, displayColumns.size());
        assertEquals("用户ID", displayColumns.get(0));
        assertEquals("用户姓名", displayColumns.get(1));
        assertEquals("email", displayColumns.get(2)); // 未设置，保持原始名
        assertEquals("created_at", displayColumns.get(3)); // 未设置，保持原始名
    }
    
    @Test
    public void testGetColumnDisplayMapping() {
        // 设置字段显示名称
        enhancedResult.setFieldDisplayName("id", "用户ID", "用户ID");
        enhancedResult.setFieldDisplayName("name", "用户姓名", "用户姓名");
        
        Map<String, String> mapping = enhancedResult.getColumnDisplayMapping();
        
        assertEquals(2, mapping.size());
        assertEquals("用户ID", mapping.get("id"));
        assertEquals("用户姓名", mapping.get("name"));
        assertNull(mapping.get("email")); // 未设置
    }
    
    @Test
    public void testGetReverseColumnMapping() {
        // 设置字段显示名称
        enhancedResult.setFieldDisplayName("id", "用户ID", "用户ID");
        enhancedResult.setFieldDisplayName("name", "用户姓名", "用户姓名");
        
        Map<String, String> reverseMapping = enhancedResult.getReverseColumnMapping();
        
        assertEquals(2, reverseMapping.size());
        assertEquals("id", reverseMapping.get("用户ID"));
        assertEquals("name", reverseMapping.get("用户姓名"));
    }
    
    @Test
    public void testChineseColumnStatistics() {
        // 初始状态
        assertEquals(0, enhancedResult.getChineseColumnCount());
        assertEquals(0.0, enhancedResult.getChineseColumnCoverage(), 0.01);
        
        // 设置1个中文字段名
        enhancedResult.setFieldDisplayName("id", "用户ID", "用户ID");
        assertEquals(1, enhancedResult.getChineseColumnCount());
        assertEquals(0.25, enhancedResult.getChineseColumnCoverage(), 0.01); // 1/4 = 0.25
        
        // 设置3个中文字段名
        enhancedResult.setFieldDisplayName("name", "用户姓名", "用户姓名");
        enhancedResult.setFieldDisplayName("email", "邮箱地址", "邮箱地址");
        assertEquals(3, enhancedResult.getChineseColumnCount());
        assertEquals(0.75, enhancedResult.getChineseColumnCoverage(), 0.01); // 3/4 = 0.75
        
        // 设置全部中文字段名
        enhancedResult.setFieldDisplayName("created_at", "创建时间", "创建时间");
        assertEquals(4, enhancedResult.getChineseColumnCount());
        assertEquals(1.0, enhancedResult.getChineseColumnCoverage(), 0.01); // 4/4 = 1.0
    }
    
    @Test
    public void testDisableChineseColumnNames() {
        // 设置字段显示名称
        enhancedResult.setFieldDisplayName("id", "用户ID", "用户ID");
        enhancedResult.setFieldDisplayName("name", "用户姓名", "用户姓名");
        
        // 禁用中文列名功能
        enhancedResult.setEnableChineseColumnNames(false);
        
        // 验证功能被禁用
        assertFalse(enhancedResult.isEnableChineseColumnNames());
        assertFalse(enhancedResult.hasChineseColumnName("id"));
        assertEquals("id", enhancedResult.getColumnDisplayName("id")); // 返回原始名
        assertEquals(0, enhancedResult.getColumnDisplayMapping().size()); // 空映射
        
        List<String> displayColumns = enhancedResult.getDisplayColumns();
        assertEquals(Arrays.asList("id", "name", "email", "created_at"), displayColumns); // 原始列名
    }
    
    @Test
    public void testToFrontendFormat() {
        // 设置字段显示名称
        enhancedResult.setFieldDisplayName("id", "用户ID", "用户ID");
        enhancedResult.setFieldDisplayName("name", "用户姓名", "用户姓名");
        
        Map<String, Object> frontendFormat = enhancedResult.toFrontendFormat();
        
        // 验证基础字段
        assertNotNull(frontendFormat.get("columns"));
        assertNotNull(frontendFormat.get("displayColumns"));
        assertNotNull(frontendFormat.get("columnDisplayMapping"));
        assertNotNull(frontendFormat.get("rows"));
        assertNotNull(frontendFormat.get("data"));
        
        // 验证增强字段
        assertEquals(true, frontendFormat.get("enableChineseColumnNames"));
        assertEquals(2, frontendFormat.get("chineseColumnCount"));
        assertEquals(0.5, (Double) frontendFormat.get("chineseColumnCoverage"), 0.01);
        
        // 验证数据格式
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) frontendFormat.get("data");
        assertEquals(2, data.size());
        assertEquals(1, data.get(0).get("id"));
        assertEquals("张三", data.get(0).get("name"));
    }
    
    @Test
    public void testColumnMetadata() {
        // 获取列元数据
        List<EnhancedQueryResult.ColumnMetadata> metadata = enhancedResult.getColumnsMetadata();
        
        assertEquals(4, metadata.size());
        
        // 验证初始状态
        for (EnhancedQueryResult.ColumnMetadata meta : metadata) {
            assertEquals(meta.getOriginalName(), meta.getDisplayName());
            assertFalse(meta.isHasChineseName());
        }
        
        // 设置中文名后验证
        enhancedResult.setFieldDisplayName("id", "用户ID", "用户ID");
        
        EnhancedQueryResult.ColumnMetadata idMetadata = metadata.stream()
            .filter(m -> "id".equals(m.getOriginalName()))
            .findFirst()
            .orElse(null);
        
        assertNotNull(idMetadata);
        assertEquals("用户ID", idMetadata.getDisplayName());
        assertEquals("用户ID", idMetadata.getChineseName());
        assertTrue(idMetadata.isHasChineseName());
    }
}