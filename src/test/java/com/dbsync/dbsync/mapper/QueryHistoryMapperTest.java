package com.dbsync.dbsync.mapper;

import com.dbsync.dbsync.entity.QueryHistory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QueryHistoryMapper测试类
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class QueryHistoryMapperTest {

    @Autowired
    private QueryHistoryMapper queryHistoryMapper;

    @Test
    public void testInsertQueryHistoryWithNullValues() {
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
        
        queryHistory.setExecutedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        queryHistory.setExecutionTime(100);
        queryHistory.setStatus("SUCCESS");
        queryHistory.setErrorMessage(null);
        queryHistory.setResultRows(10);
        queryHistory.setCreatedBy("testuser");

        // 执行插入操作
        int result = queryHistoryMapper.insertQueryHistory(queryHistory);

        // 验证插入成功
        assertEquals(1, result);
        assertNotNull(queryHistory.getId());
        assertTrue(queryHistory.getId() > 0);
    }

    @Test
    public void testInsertQueryHistoryWithAllValues() {
        // 创建包含所有值的QueryHistory对象
        QueryHistory queryHistory = new QueryHistory();
        queryHistory.setSql("INSERT INTO target_table SELECT * FROM source_table");
        queryHistory.setSourceConnectionId(1L);
        queryHistory.setSourceConnectionName("源连接");
        queryHistory.setTargetConnectionId(2L);
        queryHistory.setTargetConnectionName("目标连接");
        queryHistory.setTargetTableName("target_table");
        queryHistory.setTargetSchemaName("public");
        queryHistory.setExecutedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        queryHistory.setExecutionTime(500);
        queryHistory.setStatus("SUCCESS");
        queryHistory.setErrorMessage(null);
        queryHistory.setResultRows(100);
        queryHistory.setCreatedBy("testuser");

        // 执行插入操作
        int result = queryHistoryMapper.insertQueryHistory(queryHistory);

        // 验证插入成功
        assertEquals(1, result);
        assertNotNull(queryHistory.getId());
        assertTrue(queryHistory.getId() > 0);
    }

    @Test
    public void testInsertQueryHistoryWithError() {
        // 创建包含错误信息的QueryHistory对象
        QueryHistory queryHistory = new QueryHistory();
        queryHistory.setSql("SELECT * FROM non_existent_table");
        queryHistory.setSourceConnectionId(1L);
        queryHistory.setSourceConnectionName("测试连接");
        queryHistory.setTargetConnectionId(null);
        queryHistory.setTargetConnectionName(null);
        queryHistory.setTargetTableName(null);
        queryHistory.setTargetSchemaName(null);
        queryHistory.setExecutedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        queryHistory.setExecutionTime(50);
        queryHistory.setStatus("ERROR");
        queryHistory.setErrorMessage("Table 'non_existent_table' doesn't exist");
        queryHistory.setResultRows(0);
        queryHistory.setCreatedBy("testuser");

        // 执行插入操作
        int result = queryHistoryMapper.insertQueryHistory(queryHistory);

        // 验证插入成功
        assertEquals(1, result);
        assertNotNull(queryHistory.getId());
        assertTrue(queryHistory.getId() > 0);
    }
}
