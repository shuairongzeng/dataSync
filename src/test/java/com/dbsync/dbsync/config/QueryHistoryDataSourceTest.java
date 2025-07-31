package com.dbsync.dbsync.config;

import com.dbsync.dbsync.entity.QueryHistory;
import com.dbsync.dbsync.mapper.QueryHistoryMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试QueryHistoryMapper使用正确的数据源（SQLite auth数据源）
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class QueryHistoryDataSourceTest {

    @Autowired
    @Qualifier("queryHistoryMapper")
    private QueryHistoryMapper queryHistoryMapper;

    @Test
    public void testQueryHistoryMapperDataSource() {
        // 验证QueryHistoryMapper Bean存在
        assertNotNull(queryHistoryMapper, "QueryHistoryMapper应该被正确注入");
        
        // 创建测试数据
        QueryHistory queryHistory = new QueryHistory();
        queryHistory.setSql("SELECT * FROM test_table");
        queryHistory.setSourceConnectionId(1L);
        queryHistory.setSourceConnectionName("测试连接");
        queryHistory.setTargetConnectionId(null);
        queryHistory.setTargetConnectionName(null);
        queryHistory.setTargetTableName(null);
        queryHistory.setTargetSchemaName(null);
        queryHistory.setExecutedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        queryHistory.setExecutionTime(100);
        queryHistory.setStatus("SUCCESS");
        queryHistory.setErrorMessage(null);
        queryHistory.setResultRows(10);
        queryHistory.setCreatedBy("testuser");

        // 测试插入操作 - 这应该使用SQLite数据源，不会抛出Oracle表不存在的错误
        int result = queryHistoryMapper.insertQueryHistory(queryHistory);
        
        // 验证插入成功
        assertEquals(1, result, "插入操作应该成功");
        assertNotNull(queryHistory.getId(), "ID应该被自动生成");
        assertTrue(queryHistory.getId() > 0, "ID应该大于0");
        
        // 测试查询操作
        QueryHistory retrieved = queryHistoryMapper.selectById(queryHistory.getId());
        assertNotNull(retrieved, "应该能够查询到插入的记录");
        assertEquals("SELECT * FROM test_table", retrieved.getSql());
        assertEquals(Long.valueOf(1L), retrieved.getSourceConnectionId());
        assertEquals("测试连接", retrieved.getSourceConnectionName());
        assertNull(retrieved.getTargetConnectionId());
        assertNull(retrieved.getTargetConnectionName());
        assertEquals("SUCCESS", retrieved.getStatus());
        assertEquals("testuser", retrieved.getCreatedBy());
    }

    @Test
    public void testQueryHistoryMapperWithNullValues() {
        // 测试NULL值处理 - 这是之前导致JDBC类型错误的场景
        QueryHistory queryHistory = new QueryHistory();
        queryHistory.setSql("SELECT COUNT(*) FROM users");
        queryHistory.setSourceConnectionId(2L);
        queryHistory.setSourceConnectionName("另一个连接");
        // 所有target相关字段保持NULL
        queryHistory.setTargetConnectionId(null);
        queryHistory.setTargetConnectionName(null);
        queryHistory.setTargetTableName(null);
        queryHistory.setTargetSchemaName(null);
        queryHistory.setExecutedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        queryHistory.setExecutionTime(50);
        queryHistory.setStatus("SUCCESS");
        queryHistory.setErrorMessage(null);
        queryHistory.setResultRows(1);
        queryHistory.setCreatedBy("nulltest");

        // 这个操作之前会抛出JDBC类型异常，现在应该成功
        assertDoesNotThrow(() -> {
            int result = queryHistoryMapper.insertQueryHistory(queryHistory);
            assertEquals(1, result);
        }, "插入包含NULL值的记录不应该抛出异常");
    }

    @Test
    public void testQueryHistoryMapperWithErrorRecord() {
        // 测试错误记录的保存
        QueryHistory queryHistory = new QueryHistory();
        queryHistory.setSql("SELECT * FROM non_existent_table");
        queryHistory.setSourceConnectionId(3L);
        queryHistory.setSourceConnectionName("错误测试连接");
        queryHistory.setTargetConnectionId(null);
        queryHistory.setTargetConnectionName(null);
        queryHistory.setTargetTableName(null);
        queryHistory.setTargetSchemaName(null);
        queryHistory.setExecutedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        queryHistory.setExecutionTime(25);
        queryHistory.setStatus("ERROR");
        queryHistory.setErrorMessage("Table 'non_existent_table' doesn't exist");
        queryHistory.setResultRows(0);
        queryHistory.setCreatedBy("errortest");

        // 测试错误记录的插入
        int result = queryHistoryMapper.insertQueryHistory(queryHistory);
        assertEquals(1, result);
        
        // 验证错误记录能正确保存和查询
        QueryHistory retrieved = queryHistoryMapper.selectById(queryHistory.getId());
        assertNotNull(retrieved);
        assertEquals("ERROR", retrieved.getStatus());
        assertEquals("Table 'non_existent_table' doesn't exist", retrieved.getErrorMessage());
        assertEquals(Integer.valueOf(0), retrieved.getResultRows());
    }
}
