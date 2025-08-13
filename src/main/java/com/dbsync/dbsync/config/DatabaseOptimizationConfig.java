package com.dbsync.dbsync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 数据库优化配置类
 * 用于配置数据库连接池和锁等待超时等参数
 */
@Configuration
@ConfigurationProperties(prefix = "dbsync.database.optimization")
public class DatabaseOptimizationConfig {

    /**
     * 锁等待超时配置（秒）
     */
    private int lockWaitTimeoutSeconds = 60;

    /**
     * 批量操作大小
     */
    private int batchSize = 500;

    /**
     * 批量提交频率（每N个批次提交一次）
     */
    private int commitFrequency = 2;

    /**
     * 连接池最大连接数
     */
    private int maxPoolSize = 20;

    /**
     * 连接池最小连接数
     */
    private int minPoolSize = 5;

    /**
     * 连接超时时间（毫秒）
     */
    private int connectionTimeoutMs = 30000;

    /**
     * 查询超时时间（秒）
     */
    private int queryTimeoutSeconds = 300;

    /**
     * 是否启用连接池监控
     */
    private boolean enablePoolMonitoring = true;

    /**
     * 元数据查询超时时间（秒）
     */
    private int metadataTimeoutSeconds = 120;

    /**
     * 重试配置
     */
    private RetryConfig retry = new RetryConfig();

    /**
     * PostgreSQL特定配置
     */
    private PostgreSQLConfig postgresql = new PostgreSQLConfig();

    /**
     * SQL Server特定配置
     */
    private SqlServerConfig sqlserver = new SqlServerConfig();

    /**
     * Oracle特定配置
     */
    private OracleConfig oracle = new OracleConfig();

    // Getters and Setters
    public int getLockWaitTimeoutSeconds() {
        return lockWaitTimeoutSeconds;
    }

    public void setLockWaitTimeoutSeconds(int lockWaitTimeoutSeconds) {
        this.lockWaitTimeoutSeconds = lockWaitTimeoutSeconds;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getCommitFrequency() {
        return commitFrequency;
    }

    public void setCommitFrequency(int commitFrequency) {
        this.commitFrequency = commitFrequency;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public int getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public int getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(int connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public int getQueryTimeoutSeconds() {
        return queryTimeoutSeconds;
    }

    public void setQueryTimeoutSeconds(int queryTimeoutSeconds) {
        this.queryTimeoutSeconds = queryTimeoutSeconds;
    }

    public boolean isEnablePoolMonitoring() {
        return enablePoolMonitoring;
    }

    public void setEnablePoolMonitoring(boolean enablePoolMonitoring) {
        this.enablePoolMonitoring = enablePoolMonitoring;
    }

    public int getMetadataTimeoutSeconds() {
        return metadataTimeoutSeconds;
    }

    public void setMetadataTimeoutSeconds(int metadataTimeoutSeconds) {
        this.metadataTimeoutSeconds = metadataTimeoutSeconds;
    }

    public RetryConfig getRetry() {
        return retry;
    }

    public void setRetry(RetryConfig retry) {
        this.retry = retry;
    }

    public PostgreSQLConfig getPostgresql() {
        return postgresql;
    }

    public void setPostgresql(PostgreSQLConfig postgresql) {
        this.postgresql = postgresql;
    }

    public SqlServerConfig getSqlserver() {
        return sqlserver;
    }

    public void setSqlserver(SqlServerConfig sqlserver) {
        this.sqlserver = sqlserver;
    }

    public OracleConfig getOracle() {
        return oracle;
    }

    public void setOracle(OracleConfig oracle) {
        this.oracle = oracle;
    }

    /**
     * 重试配置
     */
    public static class RetryConfig {
        private int maxRetries = 3;
        private long baseDelayMs = 1000;
        private long maxDelayMs = 10000;
        private double backoffMultiplier = 2.0;
        private boolean enableJitter = true;

        // Getters and Setters
        public int getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        public long getBaseDelayMs() {
            return baseDelayMs;
        }

        public void setBaseDelayMs(long baseDelayMs) {
            this.baseDelayMs = baseDelayMs;
        }

        public long getMaxDelayMs() {
            return maxDelayMs;
        }

        public void setMaxDelayMs(long maxDelayMs) {
            this.maxDelayMs = maxDelayMs;
        }

        public double getBackoffMultiplier() {
            return backoffMultiplier;
        }

        public void setBackoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
        }

        public boolean isEnableJitter() {
            return enableJitter;
        }

        public void setEnableJitter(boolean enableJitter) {
            this.enableJitter = enableJitter;
        }
    }

    /**
     * PostgreSQL特定配置
     */
    public static class PostgreSQLConfig {
        private int statementTimeoutMs = 300000; // 5分钟
        private int lockTimeoutMs = 60000; // 1分钟
        private boolean enableWalLevel = true;
        private int maxConnections = 100;

        // Getters and Setters
        public int getStatementTimeoutMs() {
            return statementTimeoutMs;
        }

        public void setStatementTimeoutMs(int statementTimeoutMs) {
            this.statementTimeoutMs = statementTimeoutMs;
        }

        public int getLockTimeoutMs() {
            return lockTimeoutMs;
        }

        public void setLockTimeoutMs(int lockTimeoutMs) {
            this.lockTimeoutMs = lockTimeoutMs;
        }

        public boolean isEnableWalLevel() {
            return enableWalLevel;
        }

        public void setEnableWalLevel(boolean enableWalLevel) {
            this.enableWalLevel = enableWalLevel;
        }

        public int getMaxConnections() {
            return maxConnections;
        }

        public void setMaxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
        }
    }

    /**
     * SQL Server特定配置
     */
    public static class SqlServerConfig {
        private int lockTimeoutMs = 60000; // 1分钟
        private int queryTimeoutSeconds = 300; // 5分钟
        private boolean enableSnapshotIsolation = false;
        private int maxDegreeOfParallelism = 0; // 0表示使用默认值

        // Getters and Setters
        public int getLockTimeoutMs() {
            return lockTimeoutMs;
        }

        public void setLockTimeoutMs(int lockTimeoutMs) {
            this.lockTimeoutMs = lockTimeoutMs;
        }

        public int getQueryTimeoutSeconds() {
            return queryTimeoutSeconds;
        }

        public void setQueryTimeoutSeconds(int queryTimeoutSeconds) {
            this.queryTimeoutSeconds = queryTimeoutSeconds;
        }

        public boolean isEnableSnapshotIsolation() {
            return enableSnapshotIsolation;
        }

        public void setEnableSnapshotIsolation(boolean enableSnapshotIsolation) {
            this.enableSnapshotIsolation = enableSnapshotIsolation;
        }

        public int getMaxDegreeOfParallelism() {
            return maxDegreeOfParallelism;
        }

        public void setMaxDegreeOfParallelism(int maxDegreeOfParallelism) {
            this.maxDegreeOfParallelism = maxDegreeOfParallelism;
        }
    }

    /**
     * Oracle特定配置
     */
    public static class OracleConfig {
        private int lockTimeoutSeconds = 60;
        private int queryTimeoutSeconds = 300;
        private boolean enableParallelDml = false;
        private int arraySize = 1000;

        // Getters and Setters
        public int getLockTimeoutSeconds() {
            return lockTimeoutSeconds;
        }

        public void setLockTimeoutSeconds(int lockTimeoutSeconds) {
            this.lockTimeoutSeconds = lockTimeoutSeconds;
        }

        public int getQueryTimeoutSeconds() {
            return queryTimeoutSeconds;
        }

        public void setQueryTimeoutSeconds(int queryTimeoutSeconds) {
            this.queryTimeoutSeconds = queryTimeoutSeconds;
        }

        public boolean isEnableParallelDml() {
            return enableParallelDml;
        }

        public void setEnableParallelDml(boolean enableParallelDml) {
            this.enableParallelDml = enableParallelDml;
        }

        public int getArraySize() {
            return arraySize;
        }

        public void setArraySize(int arraySize) {
            this.arraySize = arraySize;
        }
    }
}
