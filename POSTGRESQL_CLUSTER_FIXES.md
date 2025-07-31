# PostgreSQL 集群同步问题修复

## 问题描述

在数据库同步过程中，当目标数据库是PostgreSQL集群环境时，出现了以下错误：

```
ERROR: relation "bmc_bd_fix_source" does not exist on node1
```

这个错误表明表结构创建成功，但在执行数据插入时，PostgreSQL报告表在"node1"上不存在，说明存在集群节点间的同步问题。

## 根本原因分析

1. **PostgreSQL集群节点不一致**：表创建操作在一个节点上成功，但数据插入操作尝试在另一个节点（node1）上执行
2. **事务管理不当**：DDL操作（CREATE TABLE）和DML操作（INSERT）可能在不同的事务上下文中执行
3. **列过滤逻辑过于严格**：批量插入时过滤掉了有效的列，导致INSERT语句缺少必要的列
4. **缺乏集群感知的连接验证**：没有确保DDL和DML操作使用相同的连接/节点

## 修复方案

### 1. 表创建和数据插入同步 ✅

**文件**: `src/main/java/com/dbsync/dbsync/service/DatabaseSyncService.java`

- 添加了 `waitForTableReplication()` 方法，确保表创建后等待集群复制完成
- 使用集群感知的表存在性检查 `checkPgTableExistsClusterAware()`
- 实现重试机制，最多重试10次，每次间隔1秒

```java
private void waitForTableReplication(String taskId, SqlSession targetSession, String tableName) throws Exception {
    // 等待表在所有集群节点上可用
    for (int attempt = 1; attempt <= maxRetries; attempt++) {
        Map<String, Object> tableInfo = targetMapper.checkPgTableExistsClusterAware(tableName, this.targetSchemaName);
        if (tableInfo != null && isTableFullyReplicated(tableInfo)) {
            return; // 表已在所有节点上可用
        }
        Thread.sleep(retryDelayMs);
    }
}
```

### 2. 列过滤逻辑修复 ✅

**文件**: `src/main/java/com/dbsync/dbsync/service/DatabaseSyncService.java`

- 改进了 `executeAndReportBatchInsert()` 方法中的列过滤逻辑
- 添加了 `isPaginationColumn()` 方法，更精确地识别分页列
- 对列进行排序以确保INSERT语句的一致性

```java
private boolean isPaginationColumn(String columnName) {
    String lowerColumnName = columnName.toLowerCase();
    return lowerColumnName.equals("rnum") ||
           lowerColumnName.equals("rownum") ||
           lowerColumnName.equals("rnum_") ||
           lowerColumnName.equals("rn") ||
           // ... 其他分页列模式
}
```

### 3. PostgreSQL集群连接验证 ✅

**文件**: `src/main/java/com/dbsync/dbsync/mapper/TableMetadataSqlProvider.java`

- 添加了 `checkPgTableExistsClusterAware()` 方法
- 检查表在information_schema和pg_stat_user_tables中的存在性
- 确保表不仅存在于元数据中，还在统计信息中可见（表明已完全复制）

```java
public String checkPgTableExistsClusterAware(@Param("tableName") String tableName, @Param("schemaName") String schemaName) {
    return new SQL() {{
        SELECT("COUNT(*) as table_count, " +
               "CASE WHEN COUNT(*) > 0 THEN " +
               "  (SELECT COUNT(*) FROM pg_stat_user_tables WHERE relname = LOWER(#{tableName}) AND schemaname = COALESCE(#{schemaName}, current_schema())) " +
               "ELSE 0 END as stats_count");
        // ... SQL构建逻辑
    }}.toString();
}
```

### 4. 错误处理和日志改进 ✅

**文件**: `src/main/java/com/dbsync/dbsync/service/DatabaseSyncService.java`

- 添加了 `isPostgreSQLClusterError()` 方法识别集群相关错误
- 增强了错误日志，提供更详细的诊断信息
- 在批量插入失败时验证表存在性

```java
private boolean isPostgreSQLClusterError(String errorMessage) {
    String lowerError = errorMessage.toLowerCase();
    return lowerError.contains("relation") && lowerError.contains("does not exist") && lowerError.contains("node") ||
           lowerError.contains("cluster") ||
           lowerError.contains("replication");
}
```

### 5. DDL操作事务管理 ✅

**文件**: `src/main/java/com/dbsync/dbsync/service/DatabaseSyncService.java`

- 添加了 `executeDDLWithClusterSupport()` 方法
- 实现了DDL操作的显式事务管理
- 确保CREATE TABLE、COMMENT等操作在同一事务中执行
- 添加了事务回滚机制

```java
private void executeDDLWithClusterSupport(String taskId, SqlSession targetSession, String tableName, 
                                        List<Map<String, Object>> sourceStructure, String tableComment, 
                                        List<Map<String, String>> sourceColumnComments, String targetTableNameForCheck) throws Exception {
    try {
        // 执行DDL操作
        targetMapper.executeDDL(createTableSql);
        // 添加注释
        // ...
        // 提交事务
        targetSession.commit();
        // 等待集群复制
        waitForTableReplication(taskId, targetSession, tableName);
    } catch (Exception e) {
        targetSession.rollback();
        throw e;
    }
}
```

## 测试验证

### 单元测试 ✅

**文件**: `src/test/java/com/dbsync/dbsync/service/PostgreSQLClusterSyncTest.java`

- 测试表复制等待机制
- 测试列过滤逻辑
- 测试PostgreSQL集群错误检测
- 测试DDL事务管理

### 集成测试 ✅

**文件**: `src/test/java/com/dbsync/dbsync/integration/PostgreSQLClusterIntegrationTest.java`

- 完整的集群同步测试
- 表创建与复制验证测试
- 批量插入与集群验证测试

### 测试配置 ✅

**文件**: `src/test/resources/application-cluster-test.properties`

- PostgreSQL集群连接配置
- 集群特定设置
- 事务和批处理配置

## 运行测试

### 前提条件

1. PostgreSQL集群环境可用（192.168.106.103:5432）
2. Oracle源数据库可用（192.168.107.101:1525）
3. 测试数据存在于源数据库中

### 执行测试

```bash
# 设置环境变量启用集群测试
export ENABLE_CLUSTER_TESTS=true

# 运行单元测试
mvn test -Dtest=PostgreSQLClusterSyncTest

# 运行集成测试
mvn test -Dtest=PostgreSQLClusterIntegrationTest

# 运行所有测试
mvn test -Dspring.profiles.active=cluster-test
```

## 预期结果

修复后的系统应该表现出以下行为：

1. **表创建等待复制**：创建表后会等待集群复制完成再进行数据插入
2. **列过滤保留有效列**：批量插入时保留所有有效的数据列，只过滤分页列
3. **集群特定错误诊断**：提供详细的集群相关错误信息和诊断
4. **DDL事务管理**：DDL操作使用适当的事务管理确保一致性
5. **批量插入验证**：在执行批量插入前验证表在当前连接上的存在性

## 配置建议

对于生产环境的PostgreSQL集群，建议添加以下配置：

```properties
# 集群复制等待超时（毫秒）
dbsync.cluster.replication-wait-timeout=30000

# 复制检查间隔（毫秒）
dbsync.cluster.replication-check-interval=1000

# 最大重试次数
dbsync.cluster.max-retry-attempts=10

# DDL事务超时
dbsync.ddl.transaction-timeout=60000
```

## 监控和故障排除

1. **启用DEBUG日志**：设置 `logging.level.com.dbsync.dbsync=DEBUG`
2. **监控集群状态**：检查PostgreSQL集群的复制延迟
3. **检查连接池**：确保连接池配置适合集群环境
4. **验证网络连接**：确保应用程序可以访问所有集群节点
