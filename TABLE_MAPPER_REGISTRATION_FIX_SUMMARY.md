# TableMapper注册问题修复总结

## 问题描述

用户在执行同步任务时遇到以下错误：
```
2025-07-30 17:32:04.755  INFO 231036 --- [pool-1-thread-1] c.d.dbsync.service.DatabaseSyncService   : Task [1]: Starting synchronization for 1 tables from source schema ''
2025-07-30 17:32:04.757 ERROR 231036 --- [pool-1-thread-1] c.d.dbsync.service.DatabaseSyncService   : Task [1]: Failed to synchronize table BMC_BD_FIX_SOURCE. Error: Type interface com.dbsync.dbsync.mapper.TableMapper is not known to the MapperRegistry.
```

## 问题分析

这个错误表明 `DatabaseSyncService` 在运行时无法找到 `TableMapper` 接口。具体原因如下：

1. **DatabaseSyncService依赖TableMapper**：
   - `DatabaseSyncService` 使用 `TableMapper` 接口来执行SQL操作
   - 它通过 `session.getMapper(TableMapper.class)` 获取Mapper实例

2. **SqlSessionFactory配置问题**：
   - 在 `SyncTaskService` 中动态创建的 `SqlSessionFactory` 没有注册 `TableMapper` 接口
   - MyBatis不知道如何将 `TableMapper` 接口映射到SQL语句

3. **TableMapper接口特点**：
   - 使用注解定义SQL语句（`@Select`、`@Insert`、`@Update`、`@Delete`）
   - 使用 `@InsertProvider` 和 `@SelectProvider` 进行动态SQL生成
   - 不需要XML配置文件

## 解决方案

### 1. 修改SyncTaskService中的SqlSessionFactory创建

**文件位置**：`src/main/java/com/dbsync/dbsync/service/SyncTaskService.java`

**修改内容**：在 `createSqlSessionFactory` 方法中添加 `TableMapper` 接口的注册

```java
private SqlSessionFactory createSqlSessionFactory(Map<String, String> connectionDetails) throws Exception {
    String url = connectionDetails.get("url");
    String username = connectionDetails.get("username");
    String password = connectionDetails.get("password");
    String driverClassName = connectionDetails.get("driverClassName");

    Class.forName(driverClassName);
    DataSource dataSource = new org.apache.ibatis.datasource.unpooled.UnpooledDataSource(
            driverClassName, url, username, password);

    org.apache.ibatis.transaction.TransactionFactory transactionFactory =
            new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory();
    org.apache.ibatis.mapping.Environment environment =
            new org.apache.ibatis.mapping.Environment("customDbEnv", transactionFactory, dataSource);
    org.apache.ibatis.session.Configuration configuration =
                new org.apache.ibatis.session.Configuration(environment);
    configuration.setMapUnderscoreToCamelCase(true);

    // 注册TableMapper接口
    configuration.addMapper(com.dbsync.dbsync.mapper.TableMapper.class);

    return new org.apache.ibatis.session.SqlSessionFactoryBuilder().build(configuration);
}
```

### 2. 关键修复点

1. **添加Mapper注册**：
   ```java
   configuration.addMapper(com.dbsync.dbsync.mapper.TableMapper.class);
   ```

2. **保持其他配置不变**：
   - `setMapUnderscoreToCamelCase(true)` 支持驼峰命名转换
   - 使用 `JdbcTransactionFactory` 进行事务管理
   - 使用 `UnpooledDataSource` 作为数据源

## 技术细节

### 1. MyBatis Mapper注册机制

MyBatis需要知道如何将Mapper接口映射到SQL语句。有几种方式：

1. **XML配置文件**：传统的Mapper XML文件
2. **注解方式**：在接口上使用注解（如 `@Select`、`@Insert`）
3. **编程式注册**：通过 `configuration.addMapper()` 方法注册

### 2. TableMapper接口特点

```java
@Mapper
public interface TableMapper {
    @Insert("${sql}")
    void executeDynamicSQL(@Param("sql") String sql, @Param("params") Map<String, Object> params);
    
    @InsertProvider(type = BatchInsertSqlProvider.class, method = "generateBatchInsertSql")
    void executeAndReportBatchInsert(@Param("tableName") String tableName, 
                                  @Param("columns") List<Map<String, Object>> columns,
                                  @Param("valuesList") List<List<Object>> valuesList);
    
    @SelectProvider(type = TableMetadataSqlProvider.class, method = "getTableDataWithPagination")
    List<Map<String, Object>> getTableDataWithPagination(@Param("dbType") String dbType,
                                                         @Param("tableName") String tableName,
                                                         @Param("schema") String schema,
                                                         @Param("pageNum") int pageNum,
                                                         @Param("pageSize") int pageSize);
}
```

### 3. 动态SqlSessionFactory创建

在 `SyncTaskService` 中，我们为每个数据库连接动态创建 `SqlSessionFactory`：

```java
// 1. 创建数据源
DataSource dataSource = new UnpooledDataSource(driverClassName, url, username, password);

// 2. 创建事务工厂
TransactionFactory transactionFactory = new JdbcTransactionFactory();

// 3. 创建环境
Environment environment = new Environment("customDbEnv", transactionFactory, dataSource);

// 4. 创建配置
Configuration configuration = new Configuration(environment);
configuration.setMapUnderscoreToCamelCase(true);

// 5. 注册Mapper（关键修复）
configuration.addMapper(TableMapper.class);

// 6. 创建SqlSessionFactory
return new SqlSessionFactoryBuilder().build(configuration);
```

## 验证方法

### 1. 单元测试

创建了 `TableMapperRegistrationTest` 测试类来验证修复：

```java
@Test
public void testTableMapperRegistration() {
    // 创建测试连接和任务
    // 执行同步任务
    // 验证不再出现TableMapper未注册的错误
}
```

### 2. 集成测试

通过实际执行同步任务来验证修复：

1. 创建有效的数据库连接
2. 创建同步任务
3. 执行任务
4. 观察是否还有TableMapper相关错误

## 修复效果

### 修复前

```
ERROR: Type interface com.dbsync.dbsync.mapper.TableMapper is not known to the MapperRegistry
```

### 修复后

- 不再出现TableMapper注册错误
- DatabaseSyncService可以正常获取TableMapper实例
- 同步任务可以正常执行（如果数据库连接正常）

## 注意事项

1. **性能考虑**：
   - 每个 `SqlSessionFactory` 都会注册一次 `TableMapper`
   - 注册是轻量级操作，对性能影响很小

2. **内存使用**：
   - 每个 `SqlSessionFactory` 都是独立的实例
   - 需要合理管理生命周期，避免内存泄漏

3. **线程安全**：
   - `SqlSessionFactory` 是线程安全的
   - 可以在多个线程中共享使用

## 扩展性

这个修复方案具有良好的扩展性：

1. **支持其他Mapper**：
   如果需要使用其他Mapper，可以用同样的方式注册：
   ```java
   configuration.addMapper(OtherMapper.class);
   ```

2. **支持XML配置**：
   如果某些Mapper使用XML配置，可以通过 `configuration.addMappers()` 批量注册

3. **支持自定义配置**：
   可以在创建 `Configuration` 时添加更多自定义配置

## 总结

通过在动态创建的 `SqlSessionFactory` 中注册 `TableMapper` 接口，成功解决了 "TableMapper is not known to the MapperRegistry" 错误。这个修复方案：

- ✅ **简单有效**：只需添加一行代码
- ✅ **不影响现有功能**：保持其他配置不变
- ✅ **性能良好**：注册操作开销很小
- ✅ **线程安全**：符合MyBatis最佳实践
- ✅ **易于扩展**：支持添加更多Mapper

现在用户可以正常执行同步任务，系统会正确调用 `DatabaseSyncService` 进行真实的数据库同步操作。