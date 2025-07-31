# 同步任务创建JSON反序列化问题修复总结

## 问题描述

用户在前端创建同步任务时，后端返回400错误，错误信息显示：
```
JSON parse error: Cannot deserialize value of type `java.lang.String` from Array value (token `JsonToken.START_ARRAY`)
```

**请求参数**：
```json
{
    "name": "从oracle同步到海量数据库任务",
    "sourceConnectionId": 2,
    "targetConnectionId": 1,
    "sourceSchemaName": "",
    "targetSchemaName": "cqdm_basic",
    "tables": ["BMC_BD_FIX_SOURCE"],
    "truncateBeforeSync": true,
    "status": "PENDING"
}
```

**问题根因**：
- 前端发送的 `tables` 字段是数组格式 `["BMC_BD_FIX_SOURCE"]`
- 后端 `SyncTask` 实体类的 `tables` 字段是 `String` 类型，期望接收JSON字符串格式
- Jackson反序列化器无法直接将数组转换为字符串

## 解决方案

### 1. 创建专门的请求对象

**文件位置**：`src/main/java/com/dbsync/dbsync/model/SyncTaskRequest.java`

**功能**：
- 接收前端发送的数组格式 `tables` 字段
- 提供转换为 `SyncTask` 实体类的方法
- 处理数组到JSON字符串的转换

**核心代码**：
```java
public class SyncTaskRequest {
    private String[] tables; // 前端发送的数组格式
    
    // 转换为SyncTask实体类
    public SyncTask toSyncTask() {
        SyncTask task = new SyncTask();
        // ... 设置其他字段
        
        // 将数组转换为JSON字符串
        if (this.tables != null) {
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < this.tables.length; i++) {
                if (i > 0) {
                    json.append(",");
                }
                json.append("\"").append(this.tables[i]).append("\"");
            }
            json.append("]");
            task.setTables(json.toString());
        } else {
            task.setTables("[]");
        }
        
        return task;
    }
}
```

### 2. 修改Controller层使用请求对象

**文件位置**：`src/main/java/com/dbsync/dbsync/controller/SyncTaskController.java`

**修改内容**：
- 创建任务方法参数改为 `SyncTaskRequest`
- 更新任务方法参数改为 `SyncTaskRequest`
- 在方法内部转换请求对象为实体类

**修改后的代码**：
```java
@PostMapping("/tasks")
public ResponseEntity<?> createTask(@RequestBody SyncTaskRequest taskRequest) {
    try {
        // 将请求对象转换为实体类
        SyncTask task = taskRequest.toSyncTask();
        SyncTask createdTask = syncTaskService.createTask(task);
        return ResponseEntity.ok(createdTask);
    } catch (RuntimeException e) {
        Map<String, String> error = new HashMap<>();
        error.put("error", e.getMessage());
        return ResponseEntity.badRequest().body(error);
    } catch (Exception e) {
        return ResponseEntity.internalServerError().build();
    }
}
```

### 3. 保持前端接口不变

**优势**：
- 前端代码无需修改
- 前端继续使用数组格式的 `tables` 字段
- 后端透明处理转换逻辑

## 修复验证

### 1. 创建了专门的测试文件

**文件位置**：`src/test/java/com/dbsync/dbsync/Controller/SyncTaskCreationTest.java`

**测试内容**：
- 测试请求对象到实体类的转换
- 测试单表的转换
- 测试多表的转换
- 测试空表列表的转换
- 测试null表列表的转换

### 2. 测试用例覆盖

```java
@Test
public void testSyncTaskRequestConversion() {
    // 创建包含用户原始数据的请求对象
    SyncTaskRequest request = new SyncTaskRequest();
    request.setName("从oracle同步到海量数据库任务");
    request.setSourceConnectionId("2");
    request.setTargetConnectionId("1");
    request.setSourceSchemaName("");
    request.setTargetSchemaName("cqdm_basic");
    request.setTables(new String[]{"BMC_BD_FIX_SOURCE"});
    request.setTruncateBeforeSync(true);
    request.setStatus("PENDING");
    
    // 转换为SyncTask实体类
    SyncTask task = request.toSyncTask();
    
    // 验证转换结果
    assertEquals("[\"BMC_BD_FIX_SOURCE\"]", task.getTables());
    assertEquals(1, task.getTotalTables());
}
```

### 3. API接口测试

**请求格式**（保持不变）：
```http
POST /api/sync/tasks
Content-Type: application/json

{
    "name": "从oracle同步到海量数据库任务",
    "sourceConnectionId": "2",
    "targetConnectionId": "1",
    "sourceSchemaName": "",
    "targetSchemaName": "cqdm_basic",
    "tables": ["BMC_BD_FIX_SOURCE"],
    "truncateBeforeSync": true,
    "status": "PENDING"
}
```

**响应格式**：
```json
{
    "id": 1,
    "name": "从oracle同步到海量数据库任务",
    "sourceConnectionId": 2,
    "targetConnectionId": 1,
    "sourceSchemaName": "",
    "targetSchemaName": "cqdm_basic",
    "tables": "[\"BMC_BD_FIX_SOURCE\"]",
    "truncateBeforeSync": true,
    "status": "PENDING",
    "progress": 0,
    "totalTables": 1,
    "completedTables": 0,
    "createdAt": "2025-07-30 13:45:00",
    "updatedAt": "2025-07-30 13:45:00"
}
```

## 兼容性说明

### 1. 前向兼容
- 现有的前端代码无需修改
- API接口保持不变
- 请求和响应格式保持一致

### 2. 后向兼容
- 支持数组格式的 `tables` 字段
- 支持空数组和null值
- 支持多表数组

### 3. 错误处理
- 无效的连接ID会返回400错误
- 缺失的必需字段会返回400错误
- 数据库错误会返回500错误

## 使用示例

### 1. 创建单表同步任务
```javascript
const task = {
  name: "用户数据同步",
  sourceConnectionId: "1",
  targetConnectionId: "2",
  tables: ["users"],
  truncateBeforeSync: true,
  status: "PENDING"
};

const response = await createSyncTaskApi(task);
```

### 2. 创建多表同步任务
```javascript
const task = {
  name: "多表数据同步",
  sourceConnectionId: "1",
  targetConnectionId: "2",
  tables: ["users", "products", "orders"],
  truncateBeforeSync: false,
  status: "PENDING"
};

const response = await createSyncTaskApi(task);
```

### 3. 创建带Schema的同步任务
```javascript
const task = {
  name: "跨Schema同步",
  sourceConnectionId: "1",
  targetConnectionId: "2",
  sourceSchemaName: "public",
  targetSchemaName: "backup",
  tables: ["table1", "table2"],
  truncateBeforeSync: true,
  status: "PENDING"
};

const response = await createSyncTaskApi(task);
```

## 技术细节

### 1. JSON转换逻辑
```java
// 数组转JSON字符串
String[] tables = ["users", "products"];
// 转换为
String json = "[\"users\",\"products\"]";
```

### 2. 边界情况处理
- 空数组：`[]` → `"[]"`
- null值：`null` → `"[]"`
- 单元素数组：`["table1"]` → `"[\"table1\"]"`
- 多元素数组：`["t1", "t2"]` → `"[\"t1\",\"t2\"]"`

### 3. 类型转换
- `String` → `Long` (connectionId)
- `String[]` → `String` (tables)
- `Boolean` → `Boolean` (truncateBeforeSync)

## 性能考虑

1. **内存使用**：转换过程只创建临时对象，不影响性能
2. **处理速度**：字符串拼接操作很快，对性能影响可忽略
3. **垃圾回收**：临时对象会被快速回收，不会造成内存压力

## 扩展性

1. **支持更多格式**：可以扩展支持其他JSON格式
2. **自定义转换**：可以添加自定义的转换逻辑
3. **验证逻辑**：可以添加字段验证和格式检查
4. **日志记录**：可以添加转换过程的日志记录

## 总结

这个修复方案：
- ✅ 解决了JSON反序列化错误
- ✅ 保持了前端API的兼容性
- ✅ 提供了灵活的数据转换
- ✅ 包含了完整的测试覆盖
- ✅ 处理了各种边界情况
- ✅ 提供了良好的错误处理

用户现在可以正常创建同步任务，系统会正确处理数组格式的 `tables` 字段并转换为数据库存储所需的JSON字符串格式。