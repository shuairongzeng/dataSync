package com.dbsync.dbsync.controller;

import com.dbsync.dbsync.entity.ColumnInfo;
import com.dbsync.dbsync.model.DbConnection;
import com.dbsync.dbsync.service.DbConnectionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 数据库连接控制器表结构API测试
 */
@WebMvcTest(DbConnectionController.class)
public class DbConnectionControllerTableStructureTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DbConnectionService dbConnectionService;

    @Autowired
    private ObjectMapper objectMapper;

    private DbConnection testConnection;
    private List<String> testTables;
    private List<ColumnInfo> testColumns;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // 创建测试数据库连接
        testConnection = new DbConnection();
        testConnection.setId(1L);
        testConnection.setName("测试连接");
        testConnection.setDbType("mysql");
        testConnection.setHost("localhost");
        testConnection.setPort(3306);
        testConnection.setDatabase("test_db");
        testConnection.setUsername("test_user");
        testConnection.setPassword("test_pass");
        
        // 创建测试表列表
        testTables = Arrays.asList("users", "orders", "products");
        
        // 创建测试列信息
        testColumns = Arrays.asList(
            createColumnInfo("id", "BIGINT", 20, 0, false, null, "主键ID", true, true, 1),
            createColumnInfo("username", "VARCHAR", 50, 0, false, null, "用户名", false, false, 2),
            createColumnInfo("email", "VARCHAR", 100, 0, true, null, "邮箱地址", false, false, 3),
            createColumnInfo("created_at", "TIMESTAMP", 0, 0, false, "CURRENT_TIMESTAMP", "创建时间", false, false, 4)
        );
    }

    private ColumnInfo createColumnInfo(String columnName, String dataType, int columnSize, 
                                      int decimalDigits, boolean nullable, String defaultValue, 
                                      String remarks, boolean isPrimaryKey, boolean isAutoIncrement, 
                                      int ordinalPosition) {
        ColumnInfo columnInfo = new ColumnInfo();
        columnInfo.setColumnName(columnName);
        columnInfo.setDataType(dataType);
        columnInfo.setColumnSize(columnSize);
        columnInfo.setDecimalDigits(decimalDigits);
        columnInfo.setNullable(nullable);
        columnInfo.setDefaultValue(defaultValue);
        columnInfo.setRemarks(remarks);
        columnInfo.setIsPrimaryKey(isPrimaryKey);
        columnInfo.setIsAutoIncrement(isAutoIncrement);
        columnInfo.setOrdinalPosition(ordinalPosition);
        return columnInfo;
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testGetTables() throws Exception {
        // Mock服务方法
        when(dbConnectionService.getTables(eq(1L), isNull())).thenReturn(testTables);

        // 执行请求
        mockMvc.perform(get("/api/database/connections/1/tables")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value("users"))
                .andExpect(jsonPath("$[1]").value("orders"))
                .andExpect(jsonPath("$[2]").value("products"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testGetTablesWithSchema() throws Exception {
        // Mock服务方法
        when(dbConnectionService.getTables(eq(1L), eq("public"))).thenReturn(testTables);

        // 执行请求
        mockMvc.perform(get("/api/database/connections/1/tables")
                .param("schema", "public")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testGetTableColumns() throws Exception {
        // Mock服务方法
        when(dbConnectionService.getTableColumns(eq(1L), eq("users"), isNull())).thenReturn(testColumns);

        // 执行请求
        mockMvc.perform(get("/api/database/connections/1/tables/users/columns")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].columnName").value("id"))
                .andExpect(jsonPath("$[0].dataType").value("BIGINT"))
                .andExpect(jsonPath("$[0].columnSize").value(20))
                .andExpect(jsonPath("$[0].nullable").value(false))
                .andExpect(jsonPath("$[0].isPrimaryKey").value(true))
                .andExpect(jsonPath("$[0].isAutoIncrement").value(true))
                .andExpect(jsonPath("$[1].columnName").value("username"))
                .andExpect(jsonPath("$[1].dataType").value("VARCHAR"))
                .andExpect(jsonPath("$[1].columnSize").value(50))
                .andExpect(jsonPath("$[1].nullable").value(false))
                .andExpect(jsonPath("$[1].isPrimaryKey").value(false))
                .andExpect(jsonPath("$[2].columnName").value("email"))
                .andExpect(jsonPath("$[2].nullable").value(true))
                .andExpect(jsonPath("$[3].columnName").value("created_at"))
                .andExpect(jsonPath("$[3].defaultValue").value("CURRENT_TIMESTAMP"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testGetTableColumnsWithSchema() throws Exception {
        // Mock服务方法
        when(dbConnectionService.getTableColumns(eq(1L), eq("users"), eq("public"))).thenReturn(testColumns);

        // 执行请求
        mockMvc.perform(get("/api/database/connections/1/tables/users/columns")
                .param("schema", "public")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testGetTablesError() throws Exception {
        // Mock服务方法抛出异常
        when(dbConnectionService.getTables(eq(1L), isNull()))
                .thenThrow(new RuntimeException("数据库连接失败"));

        // 执行请求
        mockMvc.perform(get("/api/database/connections/1/tables")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("数据库连接失败"));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    public void testGetTableColumnsError() throws Exception {
        // Mock服务方法抛出异常
        when(dbConnectionService.getTableColumns(eq(1L), eq("users"), isNull()))
                .thenThrow(new RuntimeException("获取表结构失败"));

        // 执行请求
        mockMvc.perform(get("/api/database/connections/1/tables/users/columns")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("获取表结构失败: 获取表结构失败"));
    }
}
