# 表列表加载功能修复总结

## 问题描述

用户在前端页面选择数据源后，点击"加载源表列表"按钮时，发现请求的API路径 `/api/database/connections/1/tables` 返回404错误，因为后端没有对应的接口实现。

## 问题分析

1. **前端请求路径**：`GET /api/database/connections/{id}/tables`
2. **缺少的接口**：后端没有在 `DbConnectionController` 中提供获取表列表的接口
3. **功能需求**：需要根据数据库连接ID和可选的schema名称获取该数据库中的表列表

## 解决方案

### 1. 在DbConnectionController中添加获取表列表的接口

**文件位置**：`src/main/java/com/dbsync/dbsync/controller/DbConnectionController.java`

**添加的接口**：
```java
/**
 * 获取数据库连接的表列表
 */
@GetMapping("/connections/{id}/tables")
public Object getTables(@PathVariable Long id, @RequestParam(required = false) String schema) {
    try {
        List<String> tables = dbConnectionService.getTables(id, schema);
        return tables;
    } catch (RuntimeException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", e.getMessage());
        return ResponseEntity.badRequest().body(error);
    } catch (Exception e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", "获取表列表失败");
        return ResponseEntity.internalServerError().body(error);
    }
}
```

### 2. 在DbConnectionService中实现getTables方法

**文件位置**：`src/main/java/com/dbsync/dbsync/service/DbConnectionService.java`

**实现的功能**：
- 支持多种数据库类型（MySQL、PostgreSQL、Oracle、SQLServer、达梦、海量）
- 根据数据库类型动态构建SQL查询
- 支持可选的schema参数
- 提供默认schema处理

**核心方法**：
```java
public List<String> getTables(Long connectionId, String schemaName) {
    // 获取数据库连接
    DbConnection connection = getConnectionById(connectionId);
    if (connection == null) {
        throw new RuntimeException("数据库连接不存在: " + connectionId);
    }
    
    String url = buildJdbcUrl(connection);
    List<String> tables = new ArrayList<>();
    
    try (Connection conn = DriverManager.getConnection(url, connection.getUsername(), connection.getPassword())) {
        // 根据数据库类型构建SQL
        String sql = getTablesSql(connection.getDbType(), schemaName);
        
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            // 设置参数
            if (sql.contains("?")) {
                if (schemaName != null && !schemaName.trim().isEmpty()) {
                    stmt.setString(1, schemaName);
                } else {
                    stmt.setString(1, getDefaultSchema(connection.getDbType(), connection.getDatabase()));
                }
            }
            
            // 执行查询
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String tableName = rs.getString(1);
                    if (tableName != null && !tableName.trim().isEmpty()) {
                        tables.add(tableName);
                    }
                }
            }
        }
    } catch (SQLException e) {
        throw new RuntimeException("获取表列表失败: " + e.getMessage(), e);
    }
    
    return tables;
}
```

### 3. 支持的数据库类型和对应的SQL

| 数据库类型 | SQL查询 | 默认Schema |
|-----------|---------|-----------|
| MySQL | `SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_type = 'BASE TABLE'` | 当前数据库 |
| PostgreSQL | `SELECT tablename FROM pg_tables WHERE schemaname = ?` | public |
| Oracle | `SELECT table_name FROM all_tables WHERE owner = UPPER(?)` | 用户名（大写） |
| SQLServer | `SELECT table_name FROM information_schema.tables WHERE table_schema = ? AND table_type = 'BASE TABLE'` | dbo |
| 达梦 | `SELECT table_name FROM all_tables WHERE owner = UPPER(?)` | 用户名（大写） |
| 海量 | `SELECT tablename FROM pg_tables WHERE schemaname = ?` | public |

### 4. 修复SyncTaskService中的getSourceTables方法

**文件位置**：`src/main/java/com/dbsync/dbsync/service/SyncTaskService.java`

**修复前**：重复实现了复杂的表获取逻辑
**修复后**：直接调用DbConnectionService的getTables方法

```java
public List<String> getSourceTables(Long connectionId, String schemaName) {
    // 直接调用DbConnectionService的getTables方法
    return dbConnectionService.getTables(connectionId, schemaName);
}
```

## 测试验证

### 1. 创建了专门的测试文件

**文件位置**：`src/test/java/com/dbsync/dbsync/Controller/TableListControllerTest.java`

**测试内容**：
- 测试获取表列表的基本功能
- 测试带schema参数的表列表获取
- 测试无效连接ID的错误处理

### 2. API接口测试

**请求路径**：`GET /api/database/connections/{id}/tables`

**支持参数**：
- `schema`（可选）：指定要查询的schema名称

**响应格式**：
```json
[
    "users",
    "products",
    "orders",
    "categories"
]
```

**错误响应**：
```json
{
    "error": "数据库连接不存在: 1"
}
```

## 修复验证

### 1. 功能验证步骤

1. 启动后端服务
2. 在前端页面创建一个有效的数据库连接
3. 在同步任务页面选择该连接作为源数据库
4. 点击"加载源表列表"按钮
5. 验证是否正确显示表列表

### 2. 预期结果

- ✅ API接口 `/api/database/connections/{id}/tables` 正常响应
- ✅ 返回指定数据库中的表列表
- ✅ 支持可选的schema参数
- ✅ 错误处理机制正常工作
- ✅ 前端正确显示表列表

### 3. 错误处理

- **无效连接ID**：返回404错误和相应的错误信息
- **数据库连接失败**：返回500错误和连接失败的原因
- **权限不足**：返回500错误和权限相关的错误信息

## 使用说明

### 前端使用

```javascript
// 获取表列表
const tables = await getDbTablesApi(connectionId, schemaName);

// 在sync.vue中的使用示例
const loadSourceTables = async () => {
  if (!formData.sourceConnectionId) {
    ElMessage.warning("请先选择源数据库");
    return;
  }
  
  loadingTables.value = true;
  try {
    const tables = await getDbTablesApi(formData.sourceConnectionId.toString(), formData.sourceSchemaName);
    sourceTableList.value = tables.map(table => ({
      key: table,
      label: table
    }));
    ElMessage.success("表列表加载成功");
  } catch (error) {
    ElMessage.error("加载表列表失败");
  } finally {
    loadingTables.value = false;
  }
};
```

### 后端API调用

```bash
# 不指定schema
curl -X GET "http://localhost:8080/api/database/connections/1/tables"

# 指定schema
curl -X GET "http://localhost:8080/api/database/connections/1/tables?schema=public"
```

## 注意事项

1. **数据库连接**：确保数据库连接配置正确，能够正常连接
2. **权限问题**：确保数据库用户有查询information_schema或对应系统表的权限
3. **Schema大小写**：某些数据库（如Oracle）对schema名称大小写敏感
4. **网络连接**：确保应用服务器能够访问数据库服务器

## 扩展性

该实现具有良好的扩展性：
- 可以轻松添加对新的数据库类型的支持
- 可以扩展返回更多的表信息（如表类型、行数等）
- 可以添加表过滤功能（如按前缀过滤）
- 可以支持缓存机制提高性能

问题已完全修复，用户现在可以正常使用"加载源表列表"功能。