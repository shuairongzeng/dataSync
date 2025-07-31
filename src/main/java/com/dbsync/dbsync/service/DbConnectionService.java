package com.dbsync.dbsync.service;

import com.dbsync.dbsync.entity.ColumnInfo;
import com.dbsync.dbsync.entity.TableInfo;
import com.dbsync.dbsync.dto.TablePageRequest;
import com.dbsync.dbsync.dto.TablePageResponse;
import com.dbsync.dbsync.mapper.auth.DbConnectionMapper;
import com.dbsync.dbsync.model.DbConnection;
import com.dbsync.dbsync.util.DatabaseMetadataUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 数据库连接管理服务
 */
@Service
public class DbConnectionService {

    private static final Logger logger = LoggerFactory.getLogger(DbConnectionService.class);

    @Autowired
    private DbConnectionMapper dbConnectionMapper;

    /**
     * 获取所有数据库连接
     */
    public List<DbConnection> getAllConnections() {
        return dbConnectionMapper.findAllConnections();
    }

    /**
     * 获取所有启用的数据库连接
     */
    public List<DbConnection> getEnabledConnections() {
        return dbConnectionMapper.findAllEnabled();
    }

    /**
     * 根据ID获取数据库连接
     */
    public DbConnection getConnectionById(Long id) {
        return dbConnectionMapper.findById(id);
    }

    /**
     * 根据名称获取数据库连接
     */
    public DbConnection getConnectionByName(String name) {
        return dbConnectionMapper.findByName(name);
    }

    /**
     * 创建数据库连接
     */
    @Transactional
    public DbConnection createConnection(DbConnection connection) {
        // 检查名称是否已存在
        if (dbConnectionMapper.existsByName(connection.getName())) {
            throw new RuntimeException("连接名称已存在: " + connection.getName());
        }
        
        // 设置创建时间和更新时间
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        connection.setCreatedAt(now);
        connection.setUpdatedAt(now);
        
        // 插入数据库
        int result = dbConnectionMapper.insertConnection(connection);
        if (result > 0) {
            return connection;
        } else {
            throw new RuntimeException("创建数据库连接失败");
        }
    }

    /**
     * 更新数据库连接
     */
    @Transactional
    public DbConnection updateConnection(Long id, DbConnection connection) {
        // 检查连接是否存在
        DbConnection existingConnection = dbConnectionMapper.findById(id);
        if (existingConnection == null) {
            throw new RuntimeException("数据库连接不存在: " + id);
        }
        
        // 检查名称是否已被其他连接使用
        if (dbConnectionMapper.existsByNameExcludingId(connection.getName(), id)) {
            throw new RuntimeException("连接名称已存在: " + connection.getName());
        }
        
        // 设置ID和更新时间
        connection.setId(id);
        connection.setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // 更新数据库
        int result = dbConnectionMapper.updateConnection(connection);
        if (result > 0) {
            return connection;
        } else {
            throw new RuntimeException("更新数据库连接失败");
        }
    }

    /**
     * 删除数据库连接
     */
    @Transactional
    public boolean deleteConnection(Long id) {
        // 检查连接是否存在
        DbConnection existingConnection = dbConnectionMapper.findById(id);
        if (existingConnection == null) {
            throw new RuntimeException("数据库连接不存在: " + id);
        }
        
        int result = dbConnectionMapper.deleteById(id);
        return result > 0;
    }

    /**
     * 启用/禁用数据库连接
     */
    @Transactional
    public boolean toggleConnectionStatus(Long id, Boolean enabled) {
        // 检查连接是否存在
        DbConnection existingConnection = dbConnectionMapper.findById(id);
        if (existingConnection == null) {
            throw new RuntimeException("数据库连接不存在: " + id);
        }
        
        int result = dbConnectionMapper.updateConnectionStatus(id, enabled);
        return result > 0;
    }

    /**
     * 测试数据库连接
     */
    public DbTestResult testConnection(DbConnection connection) {
        String url = buildJdbcUrl(connection);
        long startTime = System.currentTimeMillis();
        
        try (Connection conn = DriverManager.getConnection(url, connection.getUsername(), connection.getPassword())) {
            long connectionTime = System.currentTimeMillis() - startTime;
            
            // 测试连接是否有效
            if (conn.isValid(5)) {
                return new DbTestResult(true, "连接测试成功", connectionTime);
            } else {
                return new DbTestResult(false, "连接无效", connectionTime);
            }
        } catch (SQLException e) {
            long connectionTime = System.currentTimeMillis() - startTime;
            return new DbTestResult(false, "连接失败: " + e.getMessage(), connectionTime);
        }
    }

    /**
     * 检查数据库连接健康状态
     */
    public boolean checkConnectionHealth(Long connectionId) {
        DbConnection connection = getConnectionById(connectionId);
        if (connection == null) {
            logger.warn("数据库连接不存在: {}", connectionId);
            return false;
        }

        String url = buildJdbcUrl(connection);

        try (Connection conn = createConnectionWithTimeout(url, connection.getUsername(), connection.getPassword())) {
            // 使用isValid方法检查连接是否有效，超时时间5秒
            boolean isValid = conn.isValid(5);
            logger.info("数据库连接健康检查，连接ID: {}, 状态: {}", connectionId, isValid ? "健康" : "异常");
            return isValid;
        } catch (SQLException e) {
            logger.error("数据库连接健康检查失败，连接ID: {}, 错误: {}", connectionId, e.getMessage());
            return false;
        }
    }

    /**
     * 获取数据库连接的表列表
     */
    public List<String> getTables(Long connectionId, String schemaName) {
        // 获取数据库连接
        DbConnection connection = getConnectionById(connectionId);
        if (connection == null) {
            throw new RuntimeException("数据库连接不存在: " + connectionId);
        }

        // 先进行连接健康检查
        if (!checkConnectionHealth(connectionId)) {
            throw new RuntimeException("数据库连接异常，请检查连接配置和网络状态");
        }

        String url = buildJdbcUrl(connection);

        try (Connection conn = createConnectionWithTimeout(url, connection.getUsername(), connection.getPassword())) {
            logger.info("开始获取表列表，连接ID: {}, Schema: {}", connectionId, schemaName);
            List<String> tables = DatabaseMetadataUtil.getTables(conn, connection.getDbType(), schemaName, connection.getDatabase());
            logger.info("成功获取表列表，连接ID: {}, 表数量: {}", connectionId, tables.size());
            return tables;
        } catch (SQLException e) {
            logger.error("获取表列表失败，连接ID: {}, 错误: {}", connectionId, e.getMessage(), e);

            String errorMessage = "获取表列表失败";
            if (e.getMessage().contains("timeout")) {
                errorMessage += ": 连接超时，请检查网络连接或数据库性能";
            } else if (e.getMessage().contains("Access denied")) {
                errorMessage += ": 数据库访问权限不足";
            } else if (e.getMessage().contains("Unknown database")) {
                errorMessage += ": 数据库不存在";
            } else {
                errorMessage += ": " + e.getMessage();
            }

            throw new RuntimeException(errorMessage, e);
        }
    }

    /**
     * 分页获取数据库连接的表列表
     */
    public TablePageResponse<TableInfo> getTablesWithPagination(Long connectionId, TablePageRequest request) {
        // 获取数据库连接
        DbConnection connection = getConnectionById(connectionId);
        if (connection == null) {
            throw new RuntimeException("数据库连接不存在: " + connectionId);
        }

        String url = buildJdbcUrl(connection);

        try (Connection conn = createConnectionWithTimeout(url, connection.getUsername(), connection.getPassword())) {
            return DatabaseMetadataUtil.getTablesWithPagination(conn, connection.getDbType(),
                                                               request.getSchema(), connection.getDatabase(), request);
        } catch (SQLException e) {
            throw new RuntimeException("获取表列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取表的列信息
     */
    public List<ColumnInfo> getTableColumns(Long connectionId, String tableName, String schemaName) {
        // 获取数据库连接
        DbConnection connection = getConnectionById(connectionId);
        if (connection == null) {
            throw new RuntimeException("数据库连接不存在: " + connectionId);
        }

        String url = buildJdbcUrl(connection);

        try (Connection conn = createConnectionWithTimeout(url, connection.getUsername(), connection.getPassword())) {
            return DatabaseMetadataUtil.getTableColumns(conn, connection.getDbType(), tableName, schemaName, connection.getDatabase());
        } catch (SQLException e) {
            throw new RuntimeException("获取表列信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取数据库的Schema列表
     */
    public List<String> getSchemas(Long connectionId) {
        // 获取数据库连接
        DbConnection connection = getConnectionById(connectionId);
        if (connection == null) {
            throw new RuntimeException("数据库连接不存在: " + connectionId);
        }

        String url = buildJdbcUrl(connection);

        try (Connection conn = createConnectionWithTimeout(url, connection.getUsername(), connection.getPassword())) {
            return DatabaseMetadataUtil.getSchemas(conn, connection.getDbType());
        } catch (SQLException e) {
            throw new RuntimeException("获取Schema列表失败: " + e.getMessage(), e);
        }
    }




    /**
     * 构建JDBC URL
     */
    public String buildJdbcUrl(DbConnection connection) {
        String dbType = connection.getDbType();
        String host = connection.getHost();
        Integer port = connection.getPort();
        String database = connection.getDatabase();
        String schema = connection.getSchema();
        
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
     * 创建带超时设置的数据库连接
     */
    private Connection createConnectionWithTimeout(String url, String username, String password) throws SQLException {
        // 设置连接超时时间（30秒）
        DriverManager.setLoginTimeout(30);

        Connection conn = DriverManager.getConnection(url, username, password);

        // 设置网络超时时间（45秒）
        if (conn.isValid(5)) {
            try {
                conn.setNetworkTimeout(null, 45000);
            } catch (SQLException e) {
                // 某些数据库驱动可能不支持setNetworkTimeout，忽略此异常
                logger.warn("无法设置网络超时: " + e.getMessage());
            }
        }

        return conn;
    }

    /**
     * 数据库连接测试结果
     */
    public static class DbTestResult {
        private boolean success;
        private String message;
        private long connectionTime;

        public DbTestResult(boolean success, String message, long connectionTime) {
            this.success = success;
            this.message = message;
            this.connectionTime = connectionTime;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public long getConnectionTime() {
            return connectionTime;
        }

        public void setConnectionTime(long connectionTime) {
            this.connectionTime = connectionTime;
        }
    }
}