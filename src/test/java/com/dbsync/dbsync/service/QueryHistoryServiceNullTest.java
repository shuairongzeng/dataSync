package com.dbsync.dbsync.service;

import com.dbsync.dbsync.entity.QueryHistory;
import com.dbsync.dbsync.mapper.QueryHistoryMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * QueryHistoryService NULL值处理测试
 */
@ExtendWith(MockitoExtension.class)
public class QueryHistoryServiceNullTest {

    @Mock
    private QueryHistoryMapper queryHistoryMapper;

    @InjectMocks
    private QueryHistoryService queryHistoryService;

    @Test
    public void testSaveQueryHistoryWithNullValues() {
        // 模拟mapper返回成功
        when(queryHistoryMapper.insertQueryHistory(any(QueryHistory.class))).thenReturn(1);

        // 创建包含NULL值的QueryHistory对象
        QueryHistory queryHistory = new QueryHistory();
        queryHistory.setSql("SELECT * FROM test_table");
        queryHistory.setSourceConnectionId(1L);
        queryHistory.setSourceConnectionName("测试连接");
        
        // 明确设置NULL值字段
        queryHistory.setTargetConnectionId(null);
        queryHistory.setTargetConnectionName(null);
        queryHistory.setTargetTableName(null);
        queryHistory.setTargetSchemaName(null);
        queryHistory.setErrorMessage(null);
        
        queryHistory.setExecutionTime(100);
        queryHistory.setResultRows(10);
        queryHistory.setCreatedBy("testuser");

        // 执行保存操作
        QueryHistory result = queryHistoryService.saveQueryHistory(queryHistory);

        // 验证结果
        assertNotNull(result);
        assertEquals("SELECT * FROM test_table", result.getSql());
        assertEquals(Long.valueOf(1L), result.getSourceConnectionId());
        assertEquals("测试连接", result.getSourceConnectionName());
        assertNull(result.getTargetConnectionId());
        assertNull(result.getTargetConnectionName());
        assertNull(result.getTargetTableName());
        assertNull(result.getTargetSchemaName());
        assertNull(result.getErrorMessage());
        assertEquals("SUCCESS", result.getStatus()); // 默认状态
        assertNotNull(result.getExecutedAt()); // 自动设置时间

        // 验证mapper被调用
        verify(queryHistoryMapper, times(1)).insertQueryHistory(any(QueryHistory.class));
    }

    @Test
    public void testCreateQueryHistoryWithNullValues() {
        // 测试createQueryHistory方法处理NULL值
        QueryHistory result = queryHistoryService.createQueryHistory(
            "SELECT * FROM users",
            1L,
            "源连接",
            null, // targetConnectionId为NULL
            null, // targetConnectionName为NULL
            null, // targetTableName为NULL
            null, // targetSchemaName为NULL
            150,
            "SUCCESS",
            null, // errorMessage为NULL
            25,
            "testuser"
        );

        // 验证结果
        assertNotNull(result);
        assertEquals("SELECT * FROM users", result.getSql());
        assertEquals(Long.valueOf(1L), result.getSourceConnectionId());
        assertEquals("源连接", result.getSourceConnectionName());
        assertNull(result.getTargetConnectionId());
        assertNull(result.getTargetConnectionName());
        assertNull(result.getTargetTableName());
        assertNull(result.getTargetSchemaName());
        assertEquals(Integer.valueOf(150), result.getExecutionTime());
        assertEquals("SUCCESS", result.getStatus());
        assertNull(result.getErrorMessage());
        assertEquals(Integer.valueOf(25), result.getResultRows());
        assertEquals("testuser", result.getCreatedBy());
        assertNotNull(result.getExecutedAt());
    }

    @Test
    public void testSaveQueryHistoryWithErrorAndNullValues() {
        // 模拟mapper返回成功
        when(queryHistoryMapper.insertQueryHistory(any(QueryHistory.class))).thenReturn(1);

        // 创建包含错误信息和NULL值的QueryHistory对象
        QueryHistory queryHistory = new QueryHistory();
        queryHistory.setSql("SELECT * FROM non_existent_table");
        queryHistory.setSourceConnectionId(1L);
        queryHistory.setSourceConnectionName("测试连接");
        queryHistory.setTargetConnectionId(null);
        queryHistory.setTargetConnectionName(null);
        queryHistory.setTargetTableName(null);
        queryHistory.setTargetSchemaName(null);
        queryHistory.setExecutionTime(50);
        queryHistory.setStatus("ERROR");
        queryHistory.setErrorMessage("Table doesn't exist");
        queryHistory.setResultRows(0);
        queryHistory.setCreatedBy("testuser");

        // 执行保存操作
        QueryHistory result = queryHistoryService.saveQueryHistory(queryHistory);

        // 验证结果
        assertNotNull(result);
        assertEquals("ERROR", result.getStatus());
        assertEquals("Table doesn't exist", result.getErrorMessage());
        assertNull(result.getTargetConnectionId());
        assertNull(result.getTargetConnectionName());
        assertNull(result.getTargetTableName());
        assertNull(result.getTargetSchemaName());

        // 验证mapper被调用
        verify(queryHistoryMapper, times(1)).insertQueryHistory(any(QueryHistory.class));
    }
}
