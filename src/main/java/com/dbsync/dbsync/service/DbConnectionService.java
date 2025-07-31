package com.dbsync.dbsync.service;

import com.dbsync.dbsync.mapper.auth.DbConnectionMapper;
import com.dbsync.dbsync.model.DbConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 数据库连接管理服务
 */
@Service
public class DbConnectionService {

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
     * 构建JDBC URL
     */
    private String buildJdbcUrl(DbConnection connection) {
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