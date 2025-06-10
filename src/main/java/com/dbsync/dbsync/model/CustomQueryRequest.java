package com.dbsync.dbsync.model;

public class CustomQueryRequest {
    private DbConfig sourceDbConfig;
    private DbConfig targetDbConfig;
    private String customSql;
    private String targetTableName;
    private String targetSchemaName; // Optional, could have default

    // Constructors
    public CustomQueryRequest() {
    }

    public CustomQueryRequest(DbConfig sourceDbConfig, DbConfig targetDbConfig, String customSql, String targetTableName, String targetSchemaName) {
        this.sourceDbConfig = sourceDbConfig;
        this.targetDbConfig = targetDbConfig;
        this.customSql = customSql;
        this.targetTableName = targetTableName;
        this.targetSchemaName = targetSchemaName;
    }

    // Getters and Setters
    public DbConfig getSourceDbConfig() {
        return sourceDbConfig;
    }

    public void setSourceDbConfig(DbConfig sourceDbConfig) {
        this.sourceDbConfig = sourceDbConfig;
    }

    public DbConfig getTargetDbConfig() {
        return targetDbConfig;
    }

    public void setTargetDbConfig(DbConfig targetDbConfig) {
        this.targetDbConfig = targetDbConfig;
    }

    public String getCustomSql() {
        return customSql;
    }

    public void setCustomSql(String customSql) {
        this.customSql = customSql;
    }

    public String getTargetTableName() {
        return targetTableName;
    }

    public void setTargetTableName(String targetTableName) {
        this.targetTableName = targetTableName;
    }

    public String getTargetSchemaName() {
        return targetSchemaName;
    }

    public void setTargetSchemaName(String targetSchemaName) {
        this.targetSchemaName = targetSchemaName;
    }
}
