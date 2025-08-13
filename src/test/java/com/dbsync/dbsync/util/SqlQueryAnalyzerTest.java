package com.dbsync.dbsync.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SQL查询解析器测试
 */
@ExtendWith(MockitoExtension.class)
public class SqlQueryAnalyzerTest {
    
    private SqlQueryAnalyzer sqlQueryAnalyzer;
    
    @BeforeEach
    void setUp() {
        sqlQueryAnalyzer = new SqlQueryAnalyzer();
    }
    
    @Test
    public void testSimpleSelectQuery() {
        String sql = "SELECT id, name, email FROM users WHERE status = 1";
        SqlQueryAnalyzer.QueryAnalysisResult result = sqlQueryAnalyzer.analyzeQuery(sql);
        
        assertFalse(result.isComplexQuery());
        assertEquals(1, result.getTables().size());
        assertTrue(result.getTables().contains("users"));
        assertEquals(3, result.getFields().size());
        
        // 验证字段信息
        assertEquals("id", result.getFields().get(0).getFieldName());
        assertEquals("name", result.getFields().get(1).getFieldName());
        assertEquals("email", result.getFields().get(2).getFieldName());
    }
    
    @Test
    public void testSelectAllQuery() {
        String sql = "SELECT * FROM products";
        SqlQueryAnalyzer.QueryAnalysisResult result = sqlQueryAnalyzer.analyzeQuery(sql);
        
        assertFalse(result.isComplexQuery());
        assertEquals(1, result.getTables().size());
        assertTrue(result.getTables().contains("products"));
        assertEquals(1, result.getFields().size());
        assertTrue(result.getFields().get(0).isAllFields());
    }
    
    @Test
    public void testJoinQuery() {
        String sql = "SELECT u.name, p.title FROM users u JOIN posts p ON u.id = p.user_id";
        SqlQueryAnalyzer.QueryAnalysisResult result = sqlQueryAnalyzer.analyzeQuery(sql);
        
        assertFalse(result.isComplexQuery());
        assertEquals(2, result.getTables().size());
        assertTrue(result.getTables().contains("users"));
        assertTrue(result.getTables().contains("posts"));
        assertEquals(2, result.getFields().size());
    }
    
    @Test
    public void testQueryWithAlias() {
        String sql = "SELECT name AS user_name, email AS user_email FROM users";
        SqlQueryAnalyzer.QueryAnalysisResult result = sqlQueryAnalyzer.analyzeQuery(sql);
        
        assertFalse(result.isComplexQuery());
        assertEquals(1, result.getTables().size());
        assertTrue(result.getTables().contains("users"));
        assertEquals(2, result.getFields().size());
        
        assertEquals("user_name", result.getFields().get(0).getAlias());
        assertEquals("user_email", result.getFields().get(1).getAlias());
    }
    
    @Test
    public void testQueryWithFunction() {
        String sql = "SELECT COUNT(*), MAX(created_at) FROM orders";
        SqlQueryAnalyzer.QueryAnalysisResult result = sqlQueryAnalyzer.analyzeQuery(sql);
        
        assertEquals(1, result.getTables().size());
        assertTrue(result.getTables().contains("orders"));
        assertEquals(2, result.getFields().size());
        
        assertTrue(result.getFields().get(0).isFunction());
        assertTrue(result.getFields().get(1).isFunction());
    }
    
    @Test
    public void testComplexQuery() {
        String sql = "SELECT * FROM users WHERE id IN (SELECT user_id FROM orders)";
        SqlQueryAnalyzer.QueryAnalysisResult result = sqlQueryAnalyzer.analyzeQuery(sql);
        
        // 子查询应该被标记为复杂查询
        assertTrue(result.isComplexQuery());
    }
    
    @Test
    public void testQueryWithSchemaPrefix() {
        String sql = "SELECT id, name FROM public.users";
        SqlQueryAnalyzer.QueryAnalysisResult result = sqlQueryAnalyzer.analyzeQuery(sql);
        
        assertFalse(result.isComplexQuery());
        assertEquals(1, result.getTables().size());
        assertTrue(result.getTables().contains("users")); // schema前缀应该被移除
    }
    
    @Test
    public void testQueryWithQuotedIdentifiers() {
        String sql = "SELECT `id`, \"name\" FROM [users]";
        SqlQueryAnalyzer.QueryAnalysisResult result = sqlQueryAnalyzer.analyzeQuery(sql);
        
        assertFalse(result.isComplexQuery());
        assertEquals(1, result.getTables().size());
        assertTrue(result.getTables().contains("users"));
        assertEquals(2, result.getFields().size());
    }
    
    @Test
    public void testEmptyOrInvalidSql() {
        // 空SQL
        SqlQueryAnalyzer.QueryAnalysisResult result1 = sqlQueryAnalyzer.analyzeQuery("");
        assertTrue(result1.isComplexQuery());
        
        // null SQL
        SqlQueryAnalyzer.QueryAnalysisResult result2 = sqlQueryAnalyzer.analyzeQuery(null);
        assertTrue(result2.isComplexQuery());
        
        // 无效SQL
        SqlQueryAnalyzer.QueryAnalysisResult result3 = sqlQueryAnalyzer.analyzeQuery("INVALID SQL");
        assertTrue(result3.isComplexQuery());
    }
    
    @Test
    public void testUnionQuery() {
        String sql = "SELECT id, name FROM users UNION SELECT id, name FROM customers";
        SqlQueryAnalyzer.QueryAnalysisResult result = sqlQueryAnalyzer.analyzeQuery(sql);
        
        // UNION查询应该被标记为复杂查询
        assertTrue(result.isComplexQuery());
    }
    
    @Test
    public void testMultipleTableJoin() {
        String sql = "SELECT u.name, p.title, c.name FROM users u " +
                    "JOIN posts p ON u.id = p.user_id " +
                    "JOIN categories c ON p.category_id = c.id " +
                    "JOIN tags t ON p.id = t.post_id";
        SqlQueryAnalyzer.QueryAnalysisResult result = sqlQueryAnalyzer.analyzeQuery(sql);
        
        // 超过3个表的JOIN应该被标记为复杂查询
        assertTrue(result.isComplexQuery());
        assertTrue(result.getTables().size() > 3);
    }
}