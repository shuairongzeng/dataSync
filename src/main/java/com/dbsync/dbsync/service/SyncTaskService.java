package com.dbsync.dbsync.service;

import com.dbsync.dbsync.mapper.auth.DbConnectionMapper;
import com.dbsync.dbsync.mapper.auth.SyncTaskLogMapper;
import com.dbsync.dbsync.mapper.auth.SyncTaskMapper;
import com.dbsync.dbsync.model.DbConnection;
import com.dbsync.dbsync.model.SyncTask;
import com.dbsync.dbsync.model.SyncTaskLog;
import com.dbsync.dbsync.progress.ProgressManager;
import com.dbsync.dbsync.typemapping.TypeMappingRegistry;
import com.dbsync.dbsync.service.DatabaseSyncService;
import com.dbsync.dbsync.service.DatabaseSyncServiceFactory;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 同步任务管理服务
 */
@Service
public class SyncTaskService {

    @Autowired
    private SyncTaskMapper syncTaskMapper;

    @Autowired
    private SyncTaskLogMapper syncTaskLogMapper;

    @Autowired
    private DbConnectionMapper dbConnectionMapper;

    @Autowired
    private DbConnectionService dbConnectionService;

    @Autowired
    private TypeMappingRegistry typeMappingRegistry;

    @Autowired
    private ProgressManager progressManager;
    @Autowired
    private DatabaseSyncServiceFactory databaseSyncServiceFactory;

    // 线程池用于异步执行任务
    private final ExecutorService taskExecutor = Executors.newFixedThreadPool(5);

    // 存储正在运行的任务
    private final Map<Long, CompletableFuture<Void>> runningTasks = new ConcurrentHashMap<>();

    /**
     * 获取所有同步任务
     */
    public List<SyncTask> getAllTasks() {
        return syncTaskMapper.findAllTasks();
    }

    /**
     * 根据ID获取同步任务
     */
    public SyncTask getTaskById(Long id) {
        return syncTaskMapper.findById(id);
    }

    /**
     * 创建同步任务
     */
    @Transactional
    public SyncTask createTask(SyncTask task) {
        // 检查任务名称是否已存在
        if (syncTaskMapper.existsByName(task.getName())) {
            throw new RuntimeException("任务名称已存在: " + task.getName());
        }

        // 验证源和目标连接是否存在
        DbConnection sourceConnection = dbConnectionMapper.findById(task.getSourceConnectionId());
        DbConnection targetConnection = dbConnectionMapper.findById(task.getTargetConnectionId());

        if (sourceConnection == null) {
            throw new RuntimeException("源数据库连接不存在");
        }
        if (targetConnection == null) {
            throw new RuntimeException("目标数据库连接不存在");
        }

        // 设置创建时间和更新时间
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        // 插入数据库
        int result = syncTaskMapper.insertTask(task);
        if (result > 0) {
            return task;
        } else {
            throw new RuntimeException("创建同步任务失败");
        }
    }

    /**
     * 更新同步任务
     */
    @Transactional
    public SyncTask updateTask(Long id, SyncTask task) {
        // 检查任务是否存在
        SyncTask existingTask = syncTaskMapper.findById(id);
        if (existingTask == null) {
            throw new RuntimeException("同步任务不存在: " + id);
        }

        // 检查任务名称是否已被其他任务使用
        if (syncTaskMapper.existsByNameExcludingId(task.getName(), id)) {
            throw new RuntimeException("任务名称已存在: " + task.getName());
        }

        // 验证源和目标连接是否存在
        DbConnection sourceConnection = dbConnectionMapper.findById(task.getSourceConnectionId());
        DbConnection targetConnection = dbConnectionMapper.findById(task.getTargetConnectionId());

        if (sourceConnection == null) {
            throw new RuntimeException("源数据库连接不存在");
        }
        if (targetConnection == null) {
            throw new RuntimeException("目标数据库连接不存在");
        }

        // 如果任务正在运行，不能更新
        if ("RUNNING".equals(existingTask.getStatus())) {
            throw new RuntimeException("任务正在运行中，不能更新");
        }

        // 设置ID和更新时间
        task.setId(id);
        task.setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        // 更新数据库
        int result = syncTaskMapper.updateTask(task);
        if (result > 0) {
            return task;
        } else {
            throw new RuntimeException("更新同步任务失败");
        }
    }

    /**
     * 删除同步任务
     */
    @Transactional
    public boolean deleteTask(Long id) {
        // 检查任务是否存在
        SyncTask existingTask = syncTaskMapper.findById(id);
        if (existingTask == null) {
            throw new RuntimeException("同步任务不存在: " + id);
        }

        // 如果任务正在运行，先停止
        if ("RUNNING".equals(existingTask.getStatus())) {
            stopTask(id);
        }

        // 删除相关日志
        syncTaskLogMapper.deleteByTaskId(id);

        // 删除任务
        int result = syncTaskMapper.deleteById(id);
        return result > 0;
    }

    /**
     * 执行同步任务
     */
    public void executeTask(Long id) {
        SyncTask task = syncTaskMapper.findById(id);
        if (task == null) {
            throw new RuntimeException("同步任务不存在: " + id);
        }

        // 如果任务已经在运行，不重复执行
        if ("RUNNING".equals(task.getStatus()) || runningTasks.containsKey(id)) {
            throw new RuntimeException("任务正在运行中");
        }

        // 异步执行任务
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
            try {
                doExecuteTask(task);
            } catch (Exception e) {
                logError(id, "任务执行失败: " + e.getMessage());
            } finally {
                runningTasks.remove(id);
            }
        }, taskExecutor);

        runningTasks.put(id, future);
    }

    /**
     * 停止同步任务
     */
    public void stopTask(Long id) {
        SyncTask task = syncTaskMapper.findById(id);
        if (task == null) {
            throw new RuntimeException("同步任务不存在: " + id);
        }

        if (!"RUNNING".equals(task.getStatus())) {
            throw new RuntimeException("任务未在运行中");
        }

        // 停止任务
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        syncTaskMapper.stopRunningTask(id, "任务已手动停止", now);

        // 记录日志
        logInfo(id, "任务已手动停止");

        // 取消异步任务
        CompletableFuture<Void> future = runningTasks.get(id);
        if (future != null) {
            future.cancel(true);
            runningTasks.remove(id);
        }
    }

    /**
     * 获取任务进度
     */
    public Map<String, Object> getTaskProgress(Long id) {
        SyncTask task = syncTaskMapper.findById(id);
        if (task == null) {
            throw new RuntimeException("同步任务不存在: " + id);
        }

        Map<String, Object> progress = new HashMap<>();
        progress.put("taskId", id);
        progress.put("status", task.getStatus());
        progress.put("progress", task.getProgress());
        progress.put("totalTables", task.getTotalTables());
        progress.put("completedTables", task.getCompletedTables());
        progress.put("errorMessage", task.getErrorMessage());

        return progress;
    }

    /**
     * 获取任务日志
     */
    public List<String> getTaskLogs(Long id) {
        List<SyncTaskLog> logs = syncTaskLogMapper.findByTaskId(id);
        List<String> result = new ArrayList<>();

        for (SyncTaskLog log : logs) {
            String logEntry = String.format("[%s] [%s] %s",
                    log.getCreatedAt(), log.getLevel(), log.getMessage());
            result.add(logEntry);
        }

        return result;
    }

    /**
     * 获取源数据库的表列表
     */
    public List<String> getSourceTables(Long connectionId, String schemaName) {
        // 直接调用DbConnectionService的getTables方法
        return dbConnectionService.getTables(connectionId, schemaName);
    }

    /**
     * 实际执行同步任务
     */
    private void doExecuteTask(SyncTask task) {
        Long taskId = task.getId();
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        try {
            // 更新任务状态为运行中
            syncTaskMapper.updateTaskStatus(taskId, "RUNNING", 0, 0, null, now, now);

            // 记录开始日志
            logInfo(taskId, "任务开始执行");

            // 获取源和目标连接
            DbConnection sourceConnection = dbConnectionMapper.findById(task.getSourceConnectionId());
            DbConnection targetConnection = dbConnectionMapper.findById(task.getTargetConnectionId());

            // 构建连接详情
            Map<String, String> sourceDetails = buildConnectionDetails(sourceConnection);
            Map<String, String> targetDetails = buildConnectionDetails(targetConnection);

            // 获取表列表
            List<String> tables = task.getTablesList();
            int totalTables = tables.size();

            // 更新任务信息
            task.setTotalTables(totalTables);
            task.setCompletedTables(0);
            task.setProgress(0);

            // 记录表信息
            logInfo(taskId, String.format("准备同步 %d 个表: %s", totalTables, tables));

            // 执行同步
            int completedTables = 0;
            for (String tableName : tables) {
                try {
                    logInfo(taskId, String.format("开始同步表: %s", tableName));

                    // 这里调用现有的DatabaseSyncService进行同步
                    // 由于DatabaseSyncService需要特定的参数，我们需要适配
                    syncSingleTable(taskId, sourceDetails, targetDetails, tableName,
                            task.getSourceSchemaName(), task.getTargetSchemaName(),
                            task.getTruncateBeforeSync());

                    completedTables++;
                    int progress = (int) ((double) completedTables / totalTables * 100);

                    // 更新进度
                    String updateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    syncTaskMapper.updateTaskProgress(taskId, progress, completedTables, updateTime);

                    logInfo(taskId, String.format("表 %s 同步完成", tableName));

                } catch (Exception e) {
                    logError(taskId, String.format("表 %s 同步失败: %s", tableName, e.getMessage()));
                    // 继续同步下一个表
                }
            }

            // 任务完成
            String completionTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            syncTaskMapper.updateTaskStatus(taskId, "COMPLETED_SUCCESS", 100, completedTables, null, completionTime, completionTime);

            logInfo(taskId, "任务执行完成");

        } catch (Exception e) {
            // 任务失败
            String errorTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            syncTaskMapper.updateTaskStatus(taskId, "FAILED", task.getProgress(), task.getCompletedTables(),
                    e.getMessage(), errorTime, errorTime);

            logError(taskId, "任务执行失败: " + e.getMessage());
        }
    }

    /**
     * 同步单个表
     */
    private void syncSingleTable(Long taskId, Map<String, String> sourceDetails, Map<String, String> targetDetails, String tableName, String sourceSchema, String targetSchema, Boolean truncateBeforeSync) {
        try {
            logInfo(taskId, String.format("开始同步表 %s", tableName));
            logInfo(taskId, String.format("源数据库: %s:%d/%s",
                    sourceDetails.get("host"), Integer.parseInt(sourceDetails.get("port")),
                    sourceDetails.get("database")));
            logInfo(taskId, String.format("目标数据库: %s:%d/%s",
                    targetDetails.get("host"), Integer.parseInt(targetDetails.get("port")),
                    targetDetails.get("database")));                        // 获取源和目标数据库连接
            DbConnection sourceConnection = dbConnectionMapper.findById(Long.parseLong(sourceDetails.get("connectionId")));
            DbConnection targetConnection = dbConnectionMapper.findById(Long.parseLong(targetDetails.get("connectionId")));
            if (sourceConnection == null) {
                throw new RuntimeException("源数据库连接不存在: " + sourceDetails.get("connectionId"));
            }
            if (targetConnection == null) {
                throw new RuntimeException("目标数据库连接不存在: " + targetDetails.get("connectionId"));
            }
            // 创建SqlSessionFactory
            SqlSessionFactory sourceFactory = createSqlSessionFactory(sourceDetails);
            SqlSessionFactory targetFactory = createSqlSessionFactory(targetDetails);
            // 创建DatabaseSyncService实例
            DatabaseSyncService syncService = databaseSyncServiceFactory.createSyncService(sourceFactory, targetFactory,
                    sourceConnection, targetConnection);
            // 执行表同步
            List<String> tablesToSync = Collections.singletonList(tableName);
            syncService.syncDatabase(taskId.toString(), tablesToSync, sourceSchema);
            logInfo(taskId, String.format("表 %s 同步完成", tableName));
        } catch (Exception e) {
            logError(taskId, String.format("表 %s 同步失败: %s", tableName, e.getMessage()));
            throw new RuntimeException("表同步失败: " + e.getMessage(), e);
        }
    }

    /**
     * 构建数据库连接详情
     */
    private Map<String, String> buildConnectionDetails(DbConnection connection) {
        Map<String, String> details = new HashMap<>();
        details.put("connectionId", connection.getId().toString());
        details.put("dbType", connection.getDbType());
        details.put("host", connection.getHost());
        details.put("port", connection.getPort().toString());
        details.put("database", connection.getDatabase());
        details.put("username", connection.getUsername());
        details.put("password", connection.getPassword());
        details.put("schema", connection.getSchema() != null ? connection.getSchema() : "");
        details.put("url", buildJdbcUrl(connection));
        details.put("driverClassName", getDriverClassName(connection.getDbType()));
        return details;
    }



    /**
     * 构建JDBC URL
     */
    private String buildJdbcUrl(DbConnection connection) {
        String dbType = connection.getDbType();
        String host = connection.getHost();
        Integer port = connection.getPort();
        String database = connection.getDatabase();

        switch (dbType.toLowerCase()) {
            case "mysql":
                return String.format("jdbc:mysql://%s:%d/%s?serverTimezone=UTC&useSSL=false", host, port, database);
            case "postgresql":
            case "vastbase":
                return String.format("jdbc:postgresql://%s:%d/%s", host, port, database);
            case "oracle":
                return String.format("jdbc:oracle:thin:@//%s:%d/%s", host, port, database);
            case "sqlserver":
                return String.format("jdbc:sqlserver://%s:%d;databaseName=%s", host, port, database);
            case "dameng":
                return String.format("jdbc:dm://%s:%d/%s", host, port, database);
            default:
                throw new RuntimeException("不支持的数据库类型: " + dbType);
        }
    }

    /**
     * 获取驱动类名
     */
    private String getDriverClassName(String dbType) {
        switch (dbType.toLowerCase()) {
            case "mysql":
                return "com.mysql.cj.jdbc.Driver";
            case "postgresql":
            case "vastbase":
                return "org.postgresql.Driver";
            case "oracle":
                return "oracle.jdbc.driver.OracleDriver";
            case "sqlserver":
                return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            case "dameng":
                return "dm.jdbc.driver.DmDriver";
            default:
                throw new RuntimeException("不支持的数据库类型: " + dbType);
        }
    }

    /**
     * 创建SqlSessionFactory
     */
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
        configuration.addMapper(com.dbsync.dbsync.mapper.TableMapper.class);

        return new org.apache.ibatis.session.SqlSessionFactoryBuilder().build(configuration);
    }

    /**
     * 记录INFO日志
     */
    private void logInfo(Long taskId, String message) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        syncTaskLogMapper.insertInfoLog(taskId, message, now);
    }

    /**
     * 记录WARN日志
     */
    private void logWarn(Long taskId, String message) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        syncTaskLogMapper.insertWarnLog(taskId, message, now);
    }

    /**
     * 记录ERROR日志
     */
    private void logError(Long taskId, String message) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        syncTaskLogMapper.insertErrorLog(taskId, message, now);
    }
}