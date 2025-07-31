package com.dbsync.dbsync.integration;

import com.dbsync.dbsync.model.DbConnection;
import com.dbsync.dbsync.entity.ColumnInfo;
import com.dbsync.dbsync.service.DbConnectionService;
import com.dbsync.dbsync.util.DatabaseMetadataUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 表结构功能集成测试
 * 测试数据库表结构获取的完整流程
 */
@SpringBootTest
@ActiveProfiles("test")
public class TableStructureIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private DbConnectionService dbConnectionService;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private DbConnection testConnection;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // 创建测试数据库连接
        testConnection = new DbConnection();
        testConnection.setName("Test SQLite DB");
        testConnection.setDbType("sqlite");
        testConnection.setHost("localhost");
        testConnection.setPort(0);
        testConnection.setDatabase(":memory:");
        testConnection.setUsername("");
        testConnection.setPassword("");
        
        // 创建测试表
        setupTestDatabase();
    }

    private void setupTestDatabase() throws Exception {
        String jdbcUrl = "jdbc:sqlite::memory:";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {
            
            // 创建测试表
            stmt.execute("CREATE TABLE users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(255) UNIQUE, " +
                    "age INTEGER, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")");

            stmt.execute("CREATE TABLE orders (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER NOT NULL, " +
                    "total_amount DECIMAL(10,2), " +
                    "status VARCHAR(50) DEFAULT 'pending', " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id)" +
                    ")");

            stmt.execute("CREATE TABLE products (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name VARCHAR(200) NOT NULL, " +
                    "price DECIMAL(10,2) NOT NULL, " +
                    "description TEXT, " +
                    "category_id INTEGER" +
                    ")");
        }
    }

    @Test
    void testGetTables() throws Exception {
        // 保存测试连接
        DbConnection savedConnection = dbConnectionService.createConnection(testConnection);
        
        // 测试获取表列表
        mockMvc.perform(get("/api/database/connections/{id}/tables", savedConnection.getId())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$", hasItem("users")))
                .andExpect(jsonPath("$", hasItem("orders")))
                .andExpect(jsonPath("$", hasItem("products")));
    }

    @Test
    void testGetTableColumns() throws Exception {
        // 保存测试连接
        DbConnection savedConnection = dbConnectionService.createConnection(testConnection);
        
        // 测试获取users表的列信息
        mockMvc.perform(get("/api/database/connections/{id}/tables/{tableName}/columns", 
                savedConnection.getId(), "users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].columnName", is("id")))
                .andExpect(jsonPath("$[0].isPrimaryKey", is(true)))
                .andExpect(jsonPath("$[0].isAutoIncrement", is(true)))
                .andExpect(jsonPath("$[1].columnName", is("name")))
                .andExpect(jsonPath("$[1].nullable", is(false)))
                .andExpect(jsonPath("$[2].columnName", is("email")))
                .andExpect(jsonPath("$[3].columnName", is("age")))
                .andExpect(jsonPath("$[4].columnName", is("created_at")));
    }

    @Test
    void testGetTableColumnsWithSchema() throws Exception {
        // 保存测试连接
        DbConnection savedConnection = dbConnectionService.createConnection(testConnection);

        // 测试带schema参数的列信息获取
        mockMvc.perform(get("/api/database/connections/{id}/tables/{tableName}/columns", 
                savedConnection.getId(), "orders")
                .param("schema", "main")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].columnName", is("id")))
                .andExpect(jsonPath("$[1].columnName", is("user_id")))
                .andExpect(jsonPath("$[2].columnName", is("total_amount")))
                .andExpect(jsonPath("$[3].columnName", is("status")))
                .andExpect(jsonPath("$[4].columnName", is("created_at")));
    }

    @Test
    void testDatabaseMetadataUtil() throws Exception {
        String jdbcUrl = "jdbc:sqlite::memory:";
        try (Connection conn = DriverManager.getConnection(jdbcUrl)) {
            // 重新创建测试表
            setupTestDatabase();
            
            // 测试获取表列表
            List<String> tables = DatabaseMetadataUtil.getTables(conn, "sqlite", null, null);
            assertNotNull(tables);
            assertTrue(tables.size() >= 3);
            assertTrue(tables.contains("users"));
            assertTrue(tables.contains("orders"));
            assertTrue(tables.contains("products"));
            
            // 测试获取列信息
            List<ColumnInfo> columns = DatabaseMetadataUtil.getTableColumns(conn, "sqlite", "users", null, null);
            assertNotNull(columns);
            assertEquals(5, columns.size());
            
            // 验证主键列
            ColumnInfo idColumn = columns.stream()
                    .filter(col -> "id".equals(col.getColumnName()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(idColumn);
            assertTrue(idColumn.getIsPrimaryKey());
            assertTrue(idColumn.getIsAutoIncrement());
            
            // 验证非空列
            ColumnInfo nameColumn = columns.stream()
                    .filter(col -> "name".equals(col.getColumnName()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(nameColumn);
            assertFalse(nameColumn.getNullable());
            
            // 验证可空列
            ColumnInfo ageColumn = columns.stream()
                    .filter(col -> "age".equals(col.getColumnName()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(ageColumn);
            assertTrue(ageColumn.getNullable());
        }
    }

    @Test
    void testErrorHandling() throws Exception {
        // 测试不存在的连接ID
        mockMvc.perform(get("/api/database/connections/{id}/tables", 99999L)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
        
        // 保存测试连接
        DbConnection savedConnection = dbConnectionService.createConnection(testConnection);

        // 测试不存在的表名
        mockMvc.perform(get("/api/database/connections/{id}/tables/{tableName}/columns", 
                savedConnection.getId(), "nonexistent_table")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testPerformanceWithLargeTables() throws Exception {
        // 创建包含更多列的表来测试性能
        String jdbcUrl = "jdbc:sqlite::memory:";
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement stmt = conn.createStatement()) {
            
            StringBuilder createTableSql = new StringBuilder("CREATE TABLE large_table (");
            for (int i = 1; i <= 50; i++) {
                createTableSql.append("column").append(i).append(" VARCHAR(100)");
                if (i < 50) {
                    createTableSql.append(", ");
                }
            }
            createTableSql.append(")");
            
            stmt.execute(createTableSql.toString());
        }
        
        // 保存测试连接
        DbConnection savedConnection = dbConnectionService.createConnection(testConnection);

        // 测试获取大表的列信息
        long startTime = System.currentTimeMillis();
        mockMvc.perform(get("/api/database/connections/{id}/tables/{tableName}/columns", 
                savedConnection.getId(), "large_table")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(50)));
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // 确保响应时间在合理范围内（小于5秒）
        assertTrue(duration < 5000, "Table structure loading should complete within 5 seconds");
    }

    @Test
    void testConcurrentAccess() throws Exception {
        // 保存测试连接
        DbConnection savedConnection = dbConnectionService.createConnection(testConnection);

        // 模拟并发访问
        Thread[] threads = new Thread[5];
        boolean[] results = new boolean[5];
        
        for (int i = 0; i < 5; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    mockMvc.perform(get("/api/database/connections/{id}/tables", savedConnection.getId())
                            .contentType(MediaType.APPLICATION_JSON))
                            .andExpect(status().isOk());
                    results[index] = true;
                } catch (Exception e) {
                    results[index] = false;
                }
            });
        }
        
        // 启动所有线程
        for (Thread thread : threads) {
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 验证所有请求都成功
        for (boolean result : results) {
            assertTrue(result, "All concurrent requests should succeed");
        }
    }
}
